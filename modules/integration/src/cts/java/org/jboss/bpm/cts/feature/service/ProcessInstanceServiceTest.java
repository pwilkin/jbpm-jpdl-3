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
package org.jboss.bpm.cts.feature.service;

// $Id: ProcessInstanceServiceTest.java 3484 2008-12-20 14:32:41Z thomas.diesler@jboss.com $

import java.io.IOException;
import java.net.URL;

import javax.management.ObjectName;

import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.runtime.Token;
import org.jboss.bpm.api.service.ProcessDefinitionService;
import org.jboss.bpm.api.service.ProcessInstanceService;
import org.jboss.bpm.api.test.CTSTestCase;

/**
 * Test the ProcessService
 * 
 * @author thomas.diesler@jboss.com
 * @since 08-Jul-2008
 */
public class ProcessInstanceServiceTest extends CTSTestCase
{
  public void testRegisterProcess() throws Exception
  {
    ProcessDefinitionService procDefService = getProcessEngine().getService(ProcessDefinitionService.class);
    ProcessInstanceService procService = getProcessEngine().getService(ProcessInstanceService.class);
    
    ProcessDefinition procDef = getProcessDefinition();
    ProcessInstance proc = procDef.newInstance();

    assertNull("ProcessDefinition not registered automatically", procDefService.getProcessDefinition(procDef.getKey()));
    assertNull("Process not registered automatically", procService.getInstance(proc.getKey()));

    // Register the process
    proc = procService.registerInstance(proc);
    assertNotNull("Proc not null", proc);
    assertNotNull("ProcessDefinition registered", procDefService.getProcessDefinition(procDef.getKey()));
    assertNotNull("Process registered", procService.getInstance(proc.getKey()));

    // Unregister the process
    procService.unregisterInstance(proc.getKey());
    assertNull("Process unregistered", procService.getInstance(proc.getKey()));
    assertNotNull("ProcessDefinition still registered", procDefService.getProcessDefinition(procDef.getKey()));
    
    procDefService.unregisterProcessDefinition(procDef.getKey());
    assertNull("ProcessDefinition unregistered", procDefService.getProcessDefinition(procDef.getKey()));
  }
  
  public void testStartProcess() throws Exception
  {
    ProcessDefinitionService procDefService = getProcessEngine().getService(ProcessDefinitionService.class);
    ProcessInstanceService procService = getProcessEngine().getService(ProcessInstanceService.class);
    
    ProcessDefinition procDef = getProcessDefinition();
    ProcessInstance proc = procDef.newInstance();
    
    Token token = proc.startProcess();
    assertNotNull("Token not null", token);

    ObjectName procID = proc.getKey();
    assertNotNull("Process registered", procService.getInstance(procID));

    // Unregister the process
    procService.unregisterInstance(procID);
    assertNull("Process unregistered", procService.getInstance(procID));
    assertNotNull("ProcessDefinition still registered", procDefService.getProcessDefinition(procDef.getKey()));
    
    procDefService.unregisterProcessDefinition(procDef.getKey());
    assertNull("ProcessDefinition unregistered", procDefService.getProcessDefinition(procDef.getKey()));
  }
  
  protected ProcessDefinition getProcessDefinition() throws IOException
  {
    URL pdURL = getResourceURL("cts/common/simple-sequence-" + getDialect() + ".xml");
    ProcessDefinitionService pdService = getProcessEngine().getService(ProcessDefinitionService.class);
    ProcessDefinition procDef = pdService.parseProcessDefinition(pdURL);
    return procDef;
  }
}
