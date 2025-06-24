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
package org.jboss.bpm.cts.feature.deployment;

// $Id: DeploymentXMLTest.java 3486 2008-12-20 15:46:33Z thomas.diesler@jboss.com $

import java.net.URL;

import org.jboss.bpm.api.deployment.Deployment;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.model.ProcessInstance.ProcessStatus;
import org.jboss.bpm.api.runtime.Token;
import org.jboss.bpm.api.service.DeploymentService;
import org.jboss.bpm.api.test.APITestCase;

/**
 * Test simple XML deployment
 * 
 * @author thomas.diesler@jboss.com
 * @since 24-Nov-2008
 */
public class DeploymentXMLTest extends APITestCase
{
  public void testSimpleXMLDeploy() throws Exception
  {
    URL pdURL = getResourceURL("cts/feature/deployment/simple-process-" + getDialect() + ".xml");

    DeploymentService depService = getProcessEngine().getService(DeploymentService.class);
    Deployment dep = depService.createDeployment(pdURL);

    ProcessDefinition procDef = depService.deploy(dep);
    assertNotNull("ProcDef not null", procDef);
    
    ProcessInstance proc = procDef.newInstance();
    
    Token token = proc.startProcess();
    assertEquals("Node name", "a", token.getNode().getName());
    
    token.signal();
    assertEquals("Node name", "b", token.getNode().getName());
    
    token.signal();
    assertEquals("Node name", "c", token.getNode().getName());
    
    token.signal();
    assertEquals("Node name", "end", token.getNode().getName());
    
    assertEquals(ProcessStatus.Completed, proc.getProcessStatus());

    // Undeploy the process
    assertTrue("Undeploy successful", depService.undeploy(dep));
  }
}
