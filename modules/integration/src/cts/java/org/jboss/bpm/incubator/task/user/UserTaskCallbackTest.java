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

// $Id: UserTaskCallbackTest.java 3473 2008-12-20 09:31:54Z thomas.diesler@jboss.com $

import java.io.IOException;
import java.util.List;

import org.jboss.bpm.api.model.Message;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.runtime.Attachments;
import org.jboss.bpm.api.runtime.BasicAttachments;
import org.jboss.bpm.api.test.CTSTestCase;
import org.jboss.bpm.incubator.client.ProcessInstanceExt;
import org.jboss.bpm.incubator.client.UserTaskCallback;
import org.jboss.bpm.incubator.model.UserTask;
import org.jboss.bpm.incubator.model.Event.EventDetailType;
import org.jboss.bpm.incubator.model.Task.TaskType;
import org.jboss.bpm.incubator.model.builder.ProcessBuilderExt;
import org.jboss.bpm.incubator.service.ProcessBuilderService;

/**
 * Test UserTask that uses the callback API
 * 
 * @author thomas.diesler@jboss.com
 * @since 07-Oct-2008
 */
public class UserTaskCallbackTest extends CTSTestCase
{
  public void testUserTask() throws Exception
  {
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());
    ProcessInstanceExt proc = (ProcessInstanceExt)procDef.newInstance();

    // Attach the callback to the UserTask
    UserTask userTask = proc.getNode(UserTask.class, "UserTask");
    userTask.setUserTaskCallback(new UserTaskCallbackImpl());

    BasicAttachments att = new BasicAttachments();
    att.addAttachment("foo", "xxx");
    proc.startProcessAsync(att);
    proc.waitForEnd(5000);

    List<Message> messages = getMessages();
    assertEquals(1, messages.size());
    assertEquals("xxx", messages.get(0).getProperty("bar").getValue());
  }

  protected ProcessDefinition getProcessDefinition() throws IOException
  {
    ProcessBuilderService pbService = getProcessEngine().getService(ProcessBuilderService.class);
    ProcessBuilderExt procBuilder = (ProcessBuilderExt)pbService.getProcessBuilder();
    procBuilder.addProcess("UserTaskTest");
    procBuilder.addProcessMessage("OutMessage").addProperty("foo", null, true);
    procBuilder.addProcessMessage("InMessage").addProperty("bar", null, true);
    procBuilder.addProcessMessage("EndMessage").addToRef(getTestID()).addProperty("bar", null, true);
    procBuilder.addStartEvent("Start").addSequenceFlow("UserTask");
    procBuilder.addTaskExt("UserTask", TaskType.User).addOutMessageRef("OutMessage").addInMessageRef("InMessage");
    procBuilder.addSequenceFlow("End");
    procBuilder.addEndEventExt("End", EventDetailType.Message).addMessageRef("EndMessage");
    return procBuilder.getProcessDefinition();
  }
  
  public static class UserTaskCallbackImpl extends UserTaskCallback
  {
    public void callback(Attachments att)
    {
      Object value = att.removeAttachment("foo");
      att.addAttachment("bar", value);
    }
  }
}
