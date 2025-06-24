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
package org.jboss.bpm.incubator.task.send;

// $Id: SendTaskTest.java 3473 2008-12-20 09:31:54Z thomas.diesler@jboss.com $

import java.io.IOException;
import java.util.List;

import org.jboss.bpm.api.InvalidProcessException;
import org.jboss.bpm.api.model.Message;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.builder.MessageBuilder;
import org.jboss.bpm.api.runtime.BasicAttachments;
import org.jboss.bpm.api.test.CTSTestCase;
import org.jboss.bpm.incubator.client.ProcessInstanceExt;
import org.jboss.bpm.incubator.model.Task.TaskType;
import org.jboss.bpm.incubator.model.builder.ProcessBuilderExt;
import org.jboss.bpm.incubator.service.ProcessBuilderService;

/**
 * Test Send Task
 * 
 * @author thomas.diesler@jboss.com
 * @since 03-Jul-2008
 */
public class SendTaskTest extends CTSTestCase
{
  public void testSendTask() throws Exception
  {
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());
    ProcessInstanceExt proc = (ProcessInstanceExt)procDef.newInstance();
    
    BasicAttachments att = new BasicAttachments();
    att.addAttachment("foo", "bar");
    proc.startProcessAsync(att);
    proc.waitForEnd(5000);
    
    List<Message> messages = getMessages();
    assertEquals(1, messages.size());
    assertEquals("bar", messages.get(0).getProperty("foo").getValue());
  }

  public void testSendTaskWithNoMessage() throws Exception
  {
    ProcessBuilderService pbService = getProcessEngine().getService(ProcessBuilderService.class);
    ProcessBuilderExt procBuilder = (ProcessBuilderExt)pbService.getProcessBuilder();
    procBuilder.addProcess("SendTaskTest").addStartEvent("Start").addSequenceFlow("TaskA");
    procBuilder.addTaskExt("TaskA", TaskType.Send).addSequenceFlow("End").addEndEvent("End");
    try
    {
      procBuilder.getProcessDefinition();
      fail("A Message for the MessageRef attribute MUST be entered");
    }
    catch (InvalidProcessException ex)
    {
      // expected
    }
  }

  protected ProcessDefinition getProcessDefinition() throws IOException
  {
    ProcessBuilderService pbService = getProcessEngine().getService(ProcessBuilderService.class);
    ProcessBuilderExt procBuilder = (ProcessBuilderExt)pbService.getProcessBuilder();
    procBuilder.addProcess("SendTaskTest");
    MessageBuilder msgBuilder = procBuilder.addProcessMessage("SendTaskMessage");
    msgBuilder.addToRef(getTestID()).addProperty("foo", null, true);
    procBuilder.addStartEvent("Start").addSequenceFlow("TaskA");
    procBuilder.addTaskExt("TaskA", TaskType.Send).addOutMessageRef("SendTaskMessage");
    procBuilder.addSequenceFlow("End").addEndEvent("End");
    return procBuilder.getProcessDefinition();
  }
}
