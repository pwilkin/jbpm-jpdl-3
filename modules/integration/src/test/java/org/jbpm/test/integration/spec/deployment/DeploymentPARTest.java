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
package org.jbpm.test.integration.spec.deployment;

// $Id: DeploymentPARTest.java 3467 2008-12-19 23:15:33Z thomas.diesler@jboss.com $

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jboss.bpm.api.deployment.Deployment;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.runtime.Token;
import org.jboss.bpm.api.service.DeploymentService;
import org.jboss.bpm.api.test.APITestCase;
import org.jboss.bpm.incubator.service.TaskInstanceService;

/**
 * Test simple PAR deployment
 * 
 * @author thomas.diesler@jboss.com
 * @since 24-Nov-2008
 */
public class DeploymentPARTest extends APITestCase
{
  
  public void testSimplePARDeploy() throws Exception
  {
    URL parURL = getTestArchiveURL("fork-join-example.par");

    DeploymentService depService = getProcessEngine().getService(DeploymentService.class);
    Deployment dep = depService.createDeployment(parURL);

    ProcessDefinition procDef = depService.deploy(dep);
    assertNotNull("ProcDef not null", procDef);

    ProcessInstance proc = procDef.newInstance();
    
    Token token = proc.startProcess();

    assertEquals("fork1", token.getNode().getName());
    assertEquals(2, token.getChildTokens().size());
    
    // Get the child tokens
    Token childOne = getChildToken(token, "Review Order");
    assertNotNull("Review Order", childOne);
    
    Token childTwo = getChildToken(token, "Prepare shipping");
    assertNotNull("Review Order", childTwo);
    
    TaskInstanceService taskService = getProcessEngine().getService(TaskInstanceService.class);
    assertEquals(2, taskService.getTasksByProcess(proc.getKey()).size());
    
    // Undeploy the process
    assertTrue("Undeploy successful", depService.undeploy(dep));
  }

  private Token getChildToken(Token token, String nodeName)
  {
    Map<String, Token> childToks = new HashMap<String, Token>();
    Iterator<Token> itTok = token.getChildTokens().iterator();
    while (itTok.hasNext())
    {
      Token child = itTok.next();
      childToks.put(child.getNode().getName(), child);
    }
    return childToks.get(nodeName);
  }
}
