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

// $Id: ProcessDefinitionServiceTest.java 3484 2008-12-20 14:32:41Z thomas.diesler@jboss.com $

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import javax.management.ObjectName;

import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.service.ProcessDefinitionService;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.api.service.ProcessInstanceService;
import org.jboss.bpm.api.test.CTSTestCase;

/**
 * Test the ProcessDefinition
 * 
 * @author thomas.diesler@jboss.com
 * @since 24-Sep-2008
 */
public class ProcessDefinitionServiceTest extends CTSTestCase
{
  public void testBasicProcess() throws Exception
  {
    ProcessDefinition procDef = getProcessDefinition();

    ProcessEngine engine = procDef.getProcessEngine();
    assertNotNull("ProcessEngine not null", engine);

    ProcessDefinitionService procDefService = engine.getService(ProcessDefinitionService.class);
    assertNotNull("ProcessDefinitionService not null", procDefService);

    Set<ObjectName> procDefs = procDefService.getProcessDefinitions();
    assertEquals("ProcessDefinition not automatically registered", 0, procDefs.size());
  }

  public void testNewInstance() throws Exception
  {
    ProcessDefinition procDef = getProcessDefinition();

    ProcessEngine engine = procDef.getProcessEngine();
    ProcessDefinitionService procDefService = engine.getService(ProcessDefinitionService.class);
    ProcessInstanceService procService = engine.getService(ProcessInstanceService.class);

    ProcessInstance proc = procDef.newInstance();
    assertNotNull("Process not null", proc);
    assertNull("Process not automatically registered", procService.getInstance(proc.getKey()));
    assertNull("ProcessDefinition not automatically registered", procDefService.getProcessDefinition(procDef.getKey()));
  }

  public void testRegister() throws Exception
  {
    ProcessDefinition procDef = getProcessDefinition();

    ProcessEngine engine = procDef.getProcessEngine();
    ProcessDefinitionService procDefService = engine.getService(ProcessDefinitionService.class);
    
    procDef = procDefService.registerProcessDefinition(procDef);
    assertNotNull("Registered ProcessDefinition not null", procDef);
    
    boolean success = procDefService.unregisterProcessDefinition(procDef.getKey());
    assertTrue("ProcessDefinition unregistered", success);
  }
  
  protected ProcessDefinition getProcessDefinition() throws IOException
  {
    URL pdURL = getResourceURL("cts/common/simple-sequence-" + getDialect() + ".xml");
    ProcessDefinitionService pdService = getProcessEngine().getService(ProcessDefinitionService.class);
    ProcessDefinition procDef = pdService.parseProcessDefinition(pdURL);
    return procDef;
  }
}
