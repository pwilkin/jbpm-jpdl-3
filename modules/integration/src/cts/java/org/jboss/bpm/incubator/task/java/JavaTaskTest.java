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
package org.jboss.bpm.incubator.task.java;

// $Id: JavaTaskTest.java 3473 2008-12-20 09:31:54Z thomas.diesler@jboss.com $

import java.io.IOException;

import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.runtime.Attachments;
import org.jboss.bpm.api.runtime.Token;
import org.jboss.bpm.api.test.CTSTestCase;
import org.jboss.bpm.incubator.client.ProcessInstanceExt;
import org.jboss.bpm.incubator.model.Assignment.AssignTime;
import org.jboss.bpm.incubator.model.Expression.ExpressionLanguage;
import org.jboss.bpm.incubator.model.builder.ProcessBuilderExt;
import org.jboss.bpm.incubator.model.builder.TaskBuilder;
import org.jboss.bpm.incubator.runtime.BasicNodeHandler;
import org.jboss.bpm.incubator.runtime.ExecutionHandler;
import org.jboss.bpm.incubator.service.ProcessBuilderService;

/**
 * Test ExecutionHandler attached to Task
 * 
 * @author thomas.diesler@jboss.com
 * @since 03-Jul-2008
 */
public class JavaTaskTest extends CTSTestCase
{
  public void testExecutionHandler() throws Exception
  {
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());
    ProcessInstanceExt proc = (ProcessInstanceExt)procDef.newInstance();
    
    proc.startProcessAsync();
    proc.waitForEnd(5000);
    
    assertEquals("kermit", TaskExecutionHandler.procProp);
    assertEquals("piggy", TaskExecutionHandler.taskProp);
    assertEquals(Boolean.TRUE, TaskExecutionHandler.procAssign);
    assertEquals(Boolean.TRUE, TaskExecutionHandler.taskAssign);
  }

  protected ProcessDefinition getProcessDefinition() throws IOException
  {
    ProcessBuilderService pbService = getProcessEngine().getService(ProcessBuilderService.class);
    ProcessBuilderExt procBuilder = (ProcessBuilderExt)pbService.getProcessBuilder();
    procBuilder.addProcess("TaskExecutionHandlerTest");
    procBuilder.addProcessProperty("procProp", "kermit");
    procBuilder.addProcessAssignment(AssignTime.Start, ExpressionLanguage.MVEL, "TaskExecutionHandlerTest_procProp == 'kermit'", "procAssign");
    procBuilder.addStartEvent("Start").addSequenceFlow("TaskA");
    TaskBuilder taskBuilder = procBuilder.addTaskExt("TaskA");
    taskBuilder.addNodeProperty("taskProp", "piggy").addExecutionHandler(TaskExecutionHandler.class).addSequenceFlow("End");
    taskBuilder.addNodeAssignment(AssignTime.Start, ExpressionLanguage.MVEL, "TaskExecutionHandlerTest_TaskA_taskProp == 'piggy'", "taskAssign");
    procBuilder.addEndEvent("End");
    return procBuilder.getProcessDefinition();
  }

  @SuppressWarnings("serial")
  public static class TaskExecutionHandler extends BasicNodeHandler implements ExecutionHandler
  {
    static String procProp;
    static String taskProp;
    static Object procAssign;
    static Object taskAssign;
    
    /**
     * This ExecutionHandler is supposed to see
     * - Process properties
     * - The result of start time process assignments
     * - Activity properties
     * - The result of start time activity assignments
     */
    public void execute(Token token)
    {
      Attachments atts = token.getAttachments();
      procProp = (String)atts.getAttachment("TaskExecutionHandlerTest.procProp");
      taskProp = (String)atts.getAttachment("TaskExecutionHandlerTest.TaskA.taskProp");
      procAssign = atts.getAttachment("procAssign");
      taskAssign = atts.getAttachment("taskAssign");
    }
  }
}
