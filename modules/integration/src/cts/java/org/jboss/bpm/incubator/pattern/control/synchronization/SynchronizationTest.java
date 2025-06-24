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
package org.jboss.bpm.incubator.pattern.control.synchronization;

// $Id: SynchronizationTest.java 3473 2008-12-20 09:31:54Z thomas.diesler@jboss.com $

import java.io.IOException;

import org.jboss.bpm.api.model.Message;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.builder.MessageBuilder;
import org.jboss.bpm.api.test.CTSTestCase;
import org.jboss.bpm.incubator.client.ProcessInstanceExt;
import org.jboss.bpm.incubator.model.Assignment.AssignTime;
import org.jboss.bpm.incubator.model.Event.EventDetailType;
import org.jboss.bpm.incubator.model.Expression.ExpressionLanguage;
import org.jboss.bpm.incubator.model.Gateway.GatewayType;
import org.jboss.bpm.incubator.model.builder.ProcessBuilderExt;
import org.jboss.bpm.incubator.service.ProcessBuilderService;

/**
 * Parallel gateway that that has multiple incoming sequence flows. Each token arriving from an incoming sequence flow
 * is stored in the gateway until a token has arrived from each incoming sequence flow. The tokens are merged together
 * and leave the gateway as a one.
 * 
 * @author thomas.diesler@jboss.com
 * @since 06-Aug-2008
 */
public class SynchronizationTest extends CTSTestCase
{
  public void testParallelMerge() throws Exception
  {
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());
    ProcessInstanceExt proc = (ProcessInstanceExt)procDef.newInstance();
    
    // Start the process
    proc.startProcessAsync();

    // Wait for the process to end
    proc.waitForEnd();

    // Verify the result
    Message endMessage = getMessages().get(0);
    assertEquals("TaskA", endMessage.getProperty("taskValueA").getValue());
    assertEquals("TaskB", endMessage.getProperty("taskValueB").getValue());
  }

  public ProcessDefinition getProcessDefinition() throws IOException
  {
    ProcessBuilderService pbService = getProcessEngine().getService(ProcessBuilderService.class);
    ProcessBuilderExt procBuilder = (ProcessBuilderExt)pbService.getProcessBuilder();
    procBuilder.addProcess("ParallelGatewayMerge").addStartEvent("Start").addSequenceFlow("Split");
    procBuilder.addGateway("Split", GatewayType.Parallel).addSequenceFlow("TaskA").addSequenceFlow("TaskB");
    procBuilder.addTaskExt("TaskA").addNodeAssignment(AssignTime.Start, ExpressionLanguage.MVEL, "'TaskA'", "taskValueA").addSequenceFlow("Merge");
    procBuilder.addTaskExt("TaskB").addNodeAssignment(AssignTime.Start, ExpressionLanguage.MVEL, "'TaskB'", "taskValueB").addSequenceFlow("Merge");
    procBuilder.addGateway("Merge", GatewayType.Parallel).addSequenceFlow("End");
    procBuilder.addEndEventExt("End", EventDetailType.Message).addMessageRef("EndMessage");
    MessageBuilder msgBuilder = procBuilder.addProcessMessage("EndMessage");
    msgBuilder.addToRef(getTestID()).addProperty("taskValueA", null, true).addProperty("taskValueB", null, true);
    return procBuilder.getProcessDefinition();
  }
}
