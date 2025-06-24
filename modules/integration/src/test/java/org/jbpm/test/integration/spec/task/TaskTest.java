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
package org.jbpm.test.integration.spec.task;

// $Id: TaskTest.java 3253 2008-12-07 13:19:20Z thomas.diesler@jboss.com $

import org.jboss.bpm.api.deployment.Deployment;
import org.jboss.bpm.api.deployment.SimpleDeployment;
import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.runtime.Token;
import org.jboss.bpm.api.service.DeploymentService;
import org.jboss.bpm.api.test.APITestCase;
import org.jboss.bpm.incubator.service.TaskInstanceService;
import org.jboss.bpm.incubator.task.TaskInstance;

/**
 * This uses the API to do the same as the EndTasksDbTest
 * 
 * @author thomas.diesler@jboss.com
 * @since 03-Dec-2008
 */
public class TaskTest extends APITestCase
{
  public void testCancel() 
  {
    Deployment dep = new SimpleDeployment(
      "<process-definition name='endtasksprocess' xmlns='urn:jbpm.org:jpdl-3.2'>" +
      "  <start-state>" +
      "    <transition to='approval' />" +
      "  </start-state>" +
      "  <task-node name='approval' end-tasks='true'>" +
      "    <task name='approve' description='Review order'>" +
      "      <assignment pooled-actors='reviewers' />" +
      "    </task>" +
      "    <transition name='approve' to='process'/>" +
      "    <transition name='cancel'  to='cancelled'/>" +
      "  </task-node>" +
      "  <state name='process' />" +
      "  <state name='cancelled' />" +
      "</process-definition>"
    );
    
    DeploymentService depService = getProcessEngine().getService(DeploymentService.class);
    ProcessDefinition procDef = depService.deploy(dep);
    try
    {
      ProcessInstance proc = procDef.newInstance();
      
      Token token = proc.startProcess();
      
      assertEquals("approval", proc.getRootToken().getNode().getName());
      
      token.signal("cancel");
      
      assertEquals("cancelled", proc.getRootToken().getNode().getName());
    }
    finally
    {
      depService.undeploy(dep);
    }
  }

  public void testApprove() 
  {
    Deployment dep = new SimpleDeployment(
      "<process-definition name='endtasksprocess' xmlns='urn:jbpm.org:jpdl-3.2'>" +
      "  <start-state>" +
      "    <transition to='approval' />" +
      "  </start-state>" +
      "  <task-node name='approval' end-tasks='true'>" +
      "    <task name='approve' description='Review order'>" +
      "      <assignment pooled-actors='reviewers' />" +
      "    </task>" +
      "    <transition name='approve' to='process'/>" +
      "    <transition name='reject'  to='cancelled'/>" +
      "    <transition name='cancel'  to='cancelled'/>" +
      "  </task-node>" +
      "  <state name='process' />" +
      "  <state name='cancelled' />" +
      "</process-definition>"
    );
    
    DeploymentService depService = getProcessEngine().getService(DeploymentService.class);
    ProcessDefinition procDef = depService.deploy(dep);
    try
    {
      ProcessInstance proc = procDef.newInstance();
      
      Token token = proc.startProcess();

      assertEquals("approval", token.getNode().getName());
      
      TaskInstanceService taskService = getProcessEngine().getService(TaskInstanceService.class);
      TaskInstance task = taskService.getTasksByProcess(proc.getKey()).iterator().next();
      assertNotNull("One task", task);
      assertEquals("Task name", "approve", task.getName());

      task.end("approve");
      assertEquals("process", token.getNode().getName());
    }
    finally
    {
      depService.undeploy(dep);
    }
  }

  public void testReject() 
  {
    Deployment dep = new SimpleDeployment(
      "<process-definition name='endtasksprocess' xmlns='urn:jbpm.org:jpdl-3.2'>" +
      "  <start-state>" +
      "    <transition to='approval' />" +
      "  </start-state>" +
      "  <task-node name='approval' end-tasks='true'>" +
      "    <task name='approve' description='Review order'>" +
      "      <assignment pooled-actors='reviewers' />" +
      "    </task>" +
      "    <transition name='approve' to='process'/>" +
      "    <transition name='reject'  to='cancelled'/>" +
      "    <transition name='cancel'  to='cancelled'/>" +
      "  </task-node>" +
      "  <state name='process' />" +
      "  <state name='cancelled' />" +
      "</process-definition>"
    );
    
    DeploymentService depService = getProcessEngine().getService(DeploymentService.class);
    ProcessDefinition procDef = depService.deploy(dep);
    try
    {
      ProcessInstance proc = procDef.newInstance();
      
      Token token = proc.startProcess();

      assertEquals("approval", token.getNode().getName());
      
      TaskInstanceService taskService = getProcessEngine().getService(TaskInstanceService.class);
      TaskInstance task = taskService.getTasksByProcess(proc.getKey()).iterator().next();
      task.end("reject");
      
      assertEquals("cancelled", token.getNode().getName());
    }
    finally
    {
      depService.undeploy(dep);
    }
  }

  public void testTaskInstancesAfterCancellation() 
  {
    Deployment dep = new SimpleDeployment(
      "<process-definition name='endtasksprocess' xmlns='urn:jbpm.org:jpdl-3.2'>" +
      "  <start-state>" +
      "    <transition to='approval' />" +
      "  </start-state>" +
      "  <task-node name='approval' end-tasks='true'>" +
      "    <task name='approve' description='Review order'>" +
      "      <assignment pooled-actors='reviewers' />" +
      "    </task>" +
      "    <transition name='approve' to='process'/>" +
      "    <transition name='reject'  to='cancelled'/>" +
      "    <transition name='cancel'  to='cancelled'/>" +
      "  </task-node>" +
      "  <state name='process' />" +
      "  <state name='cancelled' />" +
      "</process-definition>"
    );
    
    DeploymentService depService = getProcessEngine().getService(DeploymentService.class);
    ProcessDefinition procDef = depService.deploy(dep);
    try
    {
      ProcessInstance proc = procDef.newInstance();
      
      Token token = proc.startProcess();
      token.signal("cancel");

      TaskInstanceService taskService = getProcessEngine().getService(TaskInstanceService.class);
      TaskInstance task = taskService.getTasksByProcess(proc.getKey()).iterator().next();
      assertTrue(task.getName() + " ended", task.hasEnded());
      assertFalse(task.getName() + " not cancelled", task.isCancelled());
      assertFalse(task.getName() + " not blocking", task.isBlocking());
      assertFalse(task.getName() + " not signalling", task.isSignalling());
    }
    finally
    {
      depService.undeploy(dep);
    }
  }
}
