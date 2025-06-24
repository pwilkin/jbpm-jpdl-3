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
package org.jboss.bpm.incubator.pattern.control.parallelsplit;

// $Id: ParallelSplitTest.java 3473 2008-12-20 09:31:54Z thomas.diesler@jboss.com $

import java.io.IOException;
import java.util.List;

import javax.management.ObjectName;

import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.Signal;
import org.jboss.bpm.api.model.Signal.SignalType;
import org.jboss.bpm.api.test.CTSTestCase;
import org.jboss.bpm.incubator.client.ProcessInstanceExt;
import org.jboss.bpm.incubator.model.Gateway;
import org.jboss.bpm.incubator.model.builder.ProcessBuilderExt;
import org.jboss.bpm.incubator.service.ProcessBuilderService;

/**
 * Parallel gateway that has uncontrolled outgoing sequence flows. 
 * All of them are taken, which leads to parallel paths of execution. 
 * 
 * @author thomas.diesler@jboss.com
 * @since 06-Aug-2008
 */
public class ParallelSplitTest extends CTSTestCase
{
  public void testParallelSplit() throws Exception 
  {
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());
    ProcessInstanceExt proc = (ProcessInstanceExt)procDef.newInstance();
    
    proc.startProcessAsync();
    proc.waitForEnd();
    
    // Validate received signals
    List<Signal> signals = getSignals(SignalType.SYSTEM_END_EVENT_ENTER);
    assertEquals(2, signals.size());
    ObjectName fromRef0 = signals.get(0).getFromRef();
    ObjectName fromRef1 = signals.get(1).getFromRef();
    String fromRefs = fromRef0.getKeyProperty("name") + fromRef1.getKeyProperty("name");
    assertTrue("Unexpected from refs: " + fromRefs, fromRefs.contains("EndA"));
    assertTrue("Unexpected from refs: " + fromRefs, fromRefs.contains("EndB"));
  }

  public ProcessDefinition getProcessDefinition() throws IOException
  {
    ProcessBuilderService pbService = getProcessEngine().getService(ProcessBuilderService.class);
    ProcessBuilderExt procBuilder = (ProcessBuilderExt)pbService.getProcessBuilder();
    procBuilder.addProcess(getName()).addStartEvent("Start").addSequenceFlow("Split").addGateway("Split", Gateway.GatewayType.Parallel).
    addSequenceFlow("EndA").addSequenceFlow("EndB").addEndEvent("EndA").addEndEvent("EndB");
    return procBuilder.getProcessDefinition();
  }
}
