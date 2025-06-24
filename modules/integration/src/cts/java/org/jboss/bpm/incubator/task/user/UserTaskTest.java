/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.bpm.incubator.task.user;

// $Id: UserTaskTest.java 3473 2008-12-20 09:31:54Z thomas.diesler@jboss.com $

import java.io.IOException;
import java.util.List;

import javax.management.ObjectName;

import org.jboss.bpm.api.Constants;
import org.jboss.bpm.api.model.Message;
import org.jboss.bpm.api.model.MessageListener;
import org.jboss.bpm.api.model.ObjectNameFactory;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.builder.MessageBuilder;
import org.jboss.bpm.api.runtime.BasicAttachments;
import org.jboss.bpm.api.service.MessageBuilderService;
import org.jboss.bpm.api.service.MessageService;
import org.jboss.bpm.api.service.ProcessInstanceService;
import org.jboss.bpm.api.test.CTSTestCase;
import org.jboss.bpm.incubator.client.ProcessInstanceExt;
import org.jboss.bpm.incubator.model.Event.EventDetailType;
import org.jboss.bpm.incubator.model.Task.TaskType;
import org.jboss.bpm.incubator.model.builder.ProcessBuilderExt;
import org.jboss.bpm.incubator.service.ProcessBuilderService;

/**
 * Test User Task
 * 
 * @author thomas.diesler@jboss.com
 * @since 07-Oct-2008
 */
public class UserTaskTest extends CTSTestCase
{
  static final ObjectName MSG_LISTENER_ID = ObjectNameFactory.create(Constants.ID_DOMAIN, "msgListener", "UserTaskTest");
  
  public void testUserTask() throws Exception
  {
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());
    ProcessInstanceExt proc = (ProcessInstanceExt)procDef.newInstance();
    
    // Register the process - this assigns the procID
    ProcessInstanceService procService = getProcessEngine().getService(ProcessInstanceService.class);
    procService.registerInstance(proc);
    
    // Add the user message listener
    MessageService msgService = MessageService.locateMessageService();
    msgService.addMessageListener(new UserMessageListener(proc.getKey()));
    
    try
    {
      BasicAttachments att = new BasicAttachments();
      att.addAttachment("foo", "xxx");
      proc.startProcessAsync(att);
      proc.waitForEnd(5000);
      
      List<Message> messages = getMessages();
      assertEquals(1, messages.size());
      assertEquals("xxx", messages.get(0).getProperty("bar").getValue());
    }
    finally
    {
      msgService.removeMessageListener(MSG_LISTENER_ID);
    }
  }

  protected ProcessDefinition getProcessDefinition() throws IOException
  {
    ProcessBuilderService pbService = getProcessEngine().getService(ProcessBuilderService.class);
    ProcessBuilderExt procBuilder = (ProcessBuilderExt)pbService.getProcessBuilder();
    procBuilder.addProcess("UserTaskTest");
    procBuilder.addProcessMessage("OutMessage").addToRef(MSG_LISTENER_ID).addProperty("foo", null, true);
    procBuilder.addProcessMessage("InMessage").addProperty("bar", null, true);
    procBuilder.addProcessMessage("EndMessage").addToRef(getTestID()).addProperty("bar", null, true);
    procBuilder.addStartEvent("Start").addSequenceFlow("UserTask");
    procBuilder.addTaskExt("UserTask", TaskType.User).addOutMessageRef("OutMessage").addInMessageRef("InMessage");
    procBuilder.addSequenceFlow("End");
    procBuilder.addEndEventExt("End", EventDetailType.Message).addMessageRef("EndMessage");
    return procBuilder.getProcessDefinition();
  }
  
  public static class UserMessageListener implements MessageListener
  {
    ObjectName procID;
    
    public UserMessageListener(ObjectName procID)
    {
      this.procID = procID;
    }

    public void catchMessage(Message message)
    {
      String propValue = message.getProperty("foo").getValue();
      MessageBuilder msgBuilder = MessageBuilderService.locateMessageBuilder();
      Message resMessage = msgBuilder.newMessage("InMessage").addProperty("bar", propValue, true).getMessage();
      
      MessageService msgService = MessageService.locateMessageService();
      msgService.sendMessage(procID, "UserTask", resMessage);
    }

    public ObjectName getKey()
    {
      return MSG_LISTENER_ID;
    }
    
  }
}
