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
package org.jboss.bpm.cts.pattern.control.sequence;

// $Id: SequenceTest.java 3480 2008-12-20 12:56:31Z thomas.diesler@jboss.com $

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.model.Signal;
import org.jboss.bpm.api.model.ProcessInstance.ProcessStatus;
import org.jboss.bpm.api.model.Signal.SignalType;
import org.jboss.bpm.api.runtime.Token;
import org.jboss.bpm.api.service.ProcessDefinitionService;
import org.jboss.bpm.api.test.CTSTestCase;

/**
 * Test the basic execution sequence
 * 
 * @author thomas.diesler@jboss.com
 * @since 03-Jul-2008
 */
public class SequenceTest extends CTSTestCase
{
  public void testSequence() throws Exception
  {
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());

    ProcessInstance proc = procDef.newInstance();

    Token token = proc.startProcess();
    assertEquals("Node name", "a", token.getNode().getName());
    
    token.signal();
    assertEquals("Node name", "end", token.getNode().getName());
    
    assertEquals(ProcessStatus.Completed, proc.getProcessStatus());
    
    // Validate received signals
    List<Signal> signals = getSignals();
    assertEquals(SignalType.SYSTEM_PROCESS_ENTER, signals.get(0).getSignalType());
    assertEquals(SignalType.SYSTEM_START_EVENT_ENTER, signals.get(1).getSignalType());
    assertEquals(SignalType.SYSTEM_START_EVENT_EXIT, signals.get(2).getSignalType());
    assertEquals(SignalType.SYSTEM_TASK_ENTER, signals.get(3).getSignalType());
    assertEquals(SignalType.SYSTEM_TASK_EXIT, signals.get(4).getSignalType());
    assertEquals(SignalType.SYSTEM_END_EVENT_ENTER, signals.get(5).getSignalType());
    
    // TODO: Discuss whether SYSTEM_END_EVENT_EXIT should be emmited
    //assertEquals(SignalType.SYSTEM_END_EVENT_EXIT, signals.get(6).getSignalType());
    
    assertEquals(SignalType.SYSTEM_PROCESS_EXIT, signals.get(6).getSignalType());
  }

  public ProcessDefinition getProcessDefinition() throws IOException
  {
    URL pdURL = getResourceURL("cts/common/simple-sequence-" + getDialect() + ".xml");
    ProcessDefinitionService pdService = getProcessEngine().getService(ProcessDefinitionService.class);
    ProcessDefinition procDef = pdService.parseProcessDefinition(pdURL);
    return procDef;
  }
}
