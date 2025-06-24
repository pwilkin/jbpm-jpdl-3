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
package org.jboss.bpm.incubator.startevent;

// $Id: StartEventSignalTest.java 3488 2008-12-20 15:51:48Z thomas.diesler@jboss.com $

import java.io.IOException;
import java.util.Set;

import javax.management.ObjectName;

import org.jboss.bpm.api.model.Message;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.Signal;
import org.jboss.bpm.api.model.SignalListener;
import org.jboss.bpm.api.model.Signal.SignalType;
import org.jboss.bpm.api.model.builder.MessageBuilder;
import org.jboss.bpm.api.service.ProcessDefinitionService;
import org.jboss.bpm.api.service.ProcessInstanceService;
import org.jboss.bpm.api.service.SignalService;
import org.jboss.bpm.api.test.CTSTestCase;
import org.jboss.bpm.incubator.client.ProcessInstanceExt;
import org.jboss.bpm.incubator.model.Assignment.AssignTime;
import org.jboss.bpm.incubator.model.Event.EventDetailType;
import org.jboss.bpm.incubator.model.Expression.ExpressionLanguage;
import org.jboss.bpm.incubator.model.builder.EventBuilder;
import org.jboss.bpm.incubator.model.builder.ProcessBuilderExt;
import org.jboss.bpm.incubator.model.builder.TaskBuilder;
import org.jboss.bpm.incubator.service.ProcessBuilderService;

/**
 * Test start event with signal trigger
 * 
 * @author thomas.diesler@jboss.com
 * @since 06-Aug-2008
 */
public class StartEventSignalTest extends CTSTestCase
{
  public void testStart() throws Exception
  {
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());
    
    // You need to register the process definition to activate the start trigger
    ProcessDefinitionService procDefService = getProcessEngine().getService(ProcessDefinitionService.class);
    procDefService.registerProcessDefinition(procDef);
    ObjectName procDefID = procDef.getKey();

    ProcessInstanceExt proc = (ProcessInstanceExt)procDef.newInstance();
    try
    {
      proc.startProcessAsync();
      fail("Cannot obtain StartEvent.None to start the process explicitly");
    }
    catch (IllegalStateException ex)
    {
      // expected
    }

    // Send start trigger signal
    SignalService sigService = getProcessEngine().getService(SignalService.class);
    sigService.throwSignal(newSignal(getTestID(), SignalType.SYSTEM_START_TRIGGER, "A"));

    // Get the just started process 
    ProcessInstanceService procService = getProcessEngine().getService(ProcessInstanceService.class);
    Set<ObjectName> procIDs = procService.getInstance(procDefID, null);
    proc = (ProcessInstanceExt) procService.getInstance(procIDs.iterator().next());
    
    // Wait for the process to end
    if (proc != null)
      proc.waitForEnd(5000);

    // Verify the result
    Message endMessage = getMessages().get(0);
    assertEquals("TaskA", endMessage.getProperty("taskValue").getValue());
  }

  public ProcessDefinition getProcessDefinition() throws IOException
  {
    ProcessBuilderService pbService = getProcessEngine().getService(ProcessBuilderService.class);
    ProcessBuilderExt procBuilder = (ProcessBuilderExt)pbService.getProcessBuilder();
    procBuilder.addProcess("StartEventSignal");
    EventBuilder eventBuilder = procBuilder.addStartEventExt("StartA", EventDetailType.Signal);
    eventBuilder.addSignalRef(SignalType.SYSTEM_START_TRIGGER, "A");
    procBuilder.addSequenceFlow("TaskA");
    TaskBuilder taskBuilder = procBuilder.addTaskExt("TaskA");
    taskBuilder.addNodeAssignment(AssignTime.Start, ExpressionLanguage.MVEL, "'TaskA'", "taskValue");
    taskBuilder.addSequenceFlow("End");
    procBuilder.addEndEventExt("End", EventDetailType.Message).addMessageRef("EndMessage");
    MessageBuilder msgBuilder = procBuilder.addProcessMessage("EndMessage");
    msgBuilder.addToRef(getTestID()).addProperty("taskValue", null, true);
    return procBuilder.getProcessDefinition();
  }

  public static class TaskListener implements SignalListener
  {
    private Signal nextSignal;

    public TaskListener(Signal nextSignal)
    {
      this.nextSignal = nextSignal;
    }

    public boolean acceptSignal(Signal signal)
    {
      return signal.getSignalType() == SignalType.SYSTEM_TASK_ENTER;
    }

    public void catchSignal(Signal signal)
    {
      if (nextSignal != null)
      {
        //SignalService sigService = null; //getProcessEngine().getService(SignalService.class);
        //sigService.throwSignal(nextSignal);
        nextSignal = null;
      }
    }
  }
}
