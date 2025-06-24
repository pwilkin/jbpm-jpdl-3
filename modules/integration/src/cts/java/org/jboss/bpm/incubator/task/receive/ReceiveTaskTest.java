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
package org.jboss.bpm.incubator.task.receive;

// $Id: ReceiveTaskTest.java 3480 2008-12-20 12:56:31Z thomas.diesler@jboss.com $

import java.io.IOException;

import org.jboss.bpm.api.InvalidProcessException;
import org.jboss.bpm.api.model.Message;
import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.Signal;
import org.jboss.bpm.api.model.SignalListener;
import org.jboss.bpm.api.model.Signal.SignalType;
import org.jboss.bpm.api.model.builder.MessageBuilder;
import org.jboss.bpm.api.service.MessageBuilderService;
import org.jboss.bpm.api.service.MessageService;
import org.jboss.bpm.api.service.ProcessDefinitionService;
import org.jboss.bpm.api.service.ProcessInstanceService;
import org.jboss.bpm.api.service.SignalService;
import org.jboss.bpm.api.test.CTSTestCase;
import org.jboss.bpm.incubator.client.ProcessInstanceExt;
import org.jboss.bpm.incubator.model.Event.EventDetailType;
import org.jboss.bpm.incubator.model.Task.TaskType;
import org.jboss.bpm.incubator.model.builder.EventBuilder;
import org.jboss.bpm.incubator.model.builder.ProcessBuilderExt;
import org.jboss.bpm.incubator.service.ProcessBuilderService;

/**
 * Test Receive Task
 * 
 * @author thomas.diesler@jboss.com
 * @since 03-Jul-2008
 */
public class ReceiveTaskTest extends CTSTestCase
{
  public void testReceiveTaskWithNoMessage() throws Exception
  {
    ProcessBuilderService pbService = getProcessEngine().getService(ProcessBuilderService.class);
    ProcessBuilderExt procBuilder = (ProcessBuilderExt)pbService.getProcessBuilder();
    procBuilder.addProcess("ReceiveTaskTest").addStartEvent("Start").addSequenceFlow("TaskA");
    procBuilder.addTaskExt("TaskA", TaskType.Receive).addSequenceFlow("End").addEndEvent("End");
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

  public void testUnregisteredProcess() throws Exception
  {
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());
    ProcessInstanceExt proc = (ProcessInstanceExt)procDef.newInstance();

    MessageService msgService = MessageService.locateMessageService();
    try
    {
      msgService.sendMessage(proc.getKey(), "TaskA", getMessage());
      fail("Send to an unregistered process is expected to fail");
    }
    catch (RuntimeException ex)
    {
      // expected
    }
  }

  public void testSuspendedMessage() throws Exception
  {
    ProcessDefinitionService procDefService = getProcessEngine().getService(ProcessDefinitionService.class);
    ProcessInstanceService procService = getProcessEngine().getService(ProcessInstanceService.class);
    
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());
    ProcessInstance proc = procDef.newInstance();
    
    proc = procService.registerInstance(proc);
    try
    {
      // Send the message before the process is started
      MessageService msgService = MessageService.locateMessageService();
      msgService.sendMessage(proc.getKey(), "TaskA", getMessage());

      ProcessInstanceExt procExt = (ProcessInstanceExt)proc;
      procExt.startProcessAsync();
      procExt.waitForEnd(5000);
    }
    finally
    {
      procDefService.unregisterProcessDefinition(procDef.getKey());
    }

    Message endMsg = getMessages().get(0);
    assertNotNull("End message expected", endMsg);
    assertEquals("bar", endMsg.getProperty("foo").getValue());
  }

  public void testSuspendedToken() throws Exception
  {
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());
    final ProcessInstanceExt proc = (ProcessInstanceExt)procDef.newInstance();
    SignalListener sigListener = new SignalListener()
    {
      private boolean sendMessage = true;

      public boolean acceptSignal(Signal signal)
      {
        return signal.getSignalType() == SignalType.SYSTEM_TASK_EXIT;
      }

      public void catchSignal(Signal signal)
      {
        // Send the message after the process reached the receive task
        if (sendMessage == true)
        {
          sendMessage = false;
          MessageService msgService = MessageService.locateMessageService();
          msgService.sendMessage(proc.getKey(), "TaskA", getMessage());
        }
      }
    };
    SignalService sigManager = getProcessEngine().getService(SignalService.class);
    sigManager.addSignalListener(sigListener);

    try
    {
      proc.startProcessAsync();
      proc.waitForEnd(5000);
    }
    finally
    {
      sigManager.removeSignalListener(sigListener);
    }

    Message endMsg = getMessages().get(0);
    assertNotNull("End message expected", endMsg);
    assertEquals("bar", endMsg.getProperty("foo").getValue());
  }

  private Message getMessage()
  {
    MessageBuilder msgBuilder = MessageBuilderService.locateMessageBuilder();
    Message msg = msgBuilder.newMessage("ReceiveTaskMessage").addProperty("foo", "bar", true).getMessage();
    return msg;
  }

  protected ProcessDefinition getProcessDefinition() throws IOException
  {
    ProcessBuilderService pbService = getProcessEngine().getService(ProcessBuilderService.class);
    ProcessBuilderExt procBuilder = (ProcessBuilderExt)pbService.getProcessBuilder();
    procBuilder.addProcess("ReceiveTaskTest");
    procBuilder.addProcessMessage("ReceiveTaskMessage").addProperty("foo", null, true);
    procBuilder.addProcessMessage("EndEventMessage").addToRef(getTestID()).addProperty("foo", null, true);
    procBuilder.addStartEvent("Start").addSequenceFlow("TaskA");
    procBuilder.addTaskExt("TaskA", TaskType.Receive).addInMessageRef("ReceiveTaskMessage");
    procBuilder.addSequenceFlow("End");
    EventBuilder eventBuilder = procBuilder.addEndEventExt("End", EventDetailType.Message);
    eventBuilder.addMessageRef("EndEventMessage");
    return procBuilder.getProcessDefinition();
  }
}
