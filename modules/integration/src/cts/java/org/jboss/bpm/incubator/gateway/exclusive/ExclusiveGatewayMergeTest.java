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
package org.jboss.bpm.incubator.gateway.exclusive;

// $Id: ExclusiveGatewayMergeTest.java 3473 2008-12-20 09:31:54Z thomas.diesler@jboss.com $

import java.io.IOException;
import java.util.List;

import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.Signal;
import org.jboss.bpm.api.test.CTSTestCase;
import org.jboss.bpm.incubator.client.ProcessInstanceExt;
import org.jboss.bpm.incubator.model.Gateway.GatewayType;
import org.jboss.bpm.incubator.model.builder.ProcessBuilderExt;
import org.jboss.bpm.incubator.service.ProcessBuilderService;

/**
 * Exclusive data-based gateway that has multiple incoming sequence flows. The first token arriving from one of the
 * incoming sequence flows proceeds along the outgoing sequence flow. All other token are ignored until a token has
 * arrived from each incoming sequence flow.
 * 
 * @author thomas.diesler@jboss.com
 * @since 06-Aug-2008
 */
public class ExclusiveGatewayMergeTest extends CTSTestCase
{
  public void testMerge() throws Exception
  {
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());
    ProcessInstanceExt proc = (ProcessInstanceExt)procDef.newInstance();

    // Start the process
    proc.startProcessAsync();

    // Wait for the process to end
    proc.waitForEnd(5000);

    // Verify the result
    List<Signal> endSignals = getSignals(Signal.SignalType.SYSTEM_END_EVENT_EXIT);
    assertEquals(1, endSignals.size());
  }

  public ProcessDefinition getProcessDefinition() throws IOException
  {
    ProcessBuilderService pbService = getProcessEngine().getService(ProcessBuilderService.class);
    ProcessBuilderExt procBuilder = (ProcessBuilderExt)pbService.getProcessBuilder();
    procBuilder.addProcess("ExclusiveGatewayMerge").addStartEvent("Start").addSequenceFlow("Split");
    procBuilder.addGateway("Split", GatewayType.Inclusive).addSequenceFlow("TaskA").addSequenceFlow("TaskB");
    procBuilder.addTaskExt("TaskA").addSequenceFlow("Merge");
    procBuilder.addTaskExt("TaskB").addSequenceFlow("Merge");
    procBuilder.addGateway("Merge", GatewayType.Exclusive).addSequenceFlow("End");
    procBuilder.addEndEvent("End");
    return procBuilder.getProcessDefinition();
  }
}
