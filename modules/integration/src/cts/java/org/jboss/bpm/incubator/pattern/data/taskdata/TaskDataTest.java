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
package org.jboss.bpm.incubator.pattern.data.taskdata;

// $Id: TaskDataTest.java 3473 2008-12-20 09:31:54Z thomas.diesler@jboss.com $

import java.io.IOException;

import org.jboss.bpm.api.model.Message;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.builder.MessageBuilder;
import org.jboss.bpm.api.test.CTSTestCase;
import org.jboss.bpm.incubator.client.ProcessInstanceExt;
import org.jboss.bpm.incubator.model.Assignment.AssignTime;
import org.jboss.bpm.incubator.model.Event.EventDetailType;
import org.jboss.bpm.incubator.model.Expression.ExpressionLanguage;
import org.jboss.bpm.incubator.model.builder.ProcessBuilderExt;
import org.jboss.bpm.incubator.model.builder.TaskBuilder;
import org.jboss.bpm.incubator.service.ProcessBuilderService;

/**
 * Data elements can be defined by tasks which are accessible only within the context of individual execution instances
 * of that task. 
 * 
 * https://jira.jboss.org/jira/browse/JBPM-1515
 * 
 * @author thomas.diesler@jboss.com
 * @since 15-Aug-2008
 */
public class TaskDataTest extends CTSTestCase
{
  public void testTaskDataRead() throws Exception
  {
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());
    ProcessInstanceExt proc = (ProcessInstanceExt)procDef.newInstance();
    
    proc.startProcessAsync();
    proc.waitForEnd();
    
    Message endMessage = getMessages().get(0);
    assertNotNull("EndMessage expected", endMessage);
    assertEquals("bar", endMessage.getProperty("foo").getValue());
  }

  protected ProcessDefinition getProcessDefinition() throws IOException
  {
    ProcessBuilderService pbService = getProcessEngine().getService(ProcessBuilderService.class);
    ProcessBuilderExt procBuilder = (ProcessBuilderExt)pbService.getProcessBuilder();
    procBuilder.addProcess("TaskData").addStartEvent("Start").addSequenceFlow("TaskA");
    TaskBuilder taskBuilder = procBuilder.addTaskExt("TaskA");
    taskBuilder.addNodeProperty("foo", "bar").addSequenceFlow("End");
    taskBuilder.addNodeAssignment(AssignTime.Start, ExpressionLanguage.MVEL, "TaskData_TaskA_foo", "foo");
    procBuilder.addEndEventExt("End", EventDetailType.Message).addMessageRef("EndMessage");
    MessageBuilder msgBuilder = procBuilder.addProcessMessage("EndMessage");
    msgBuilder.addToRef(getTestID()).addProperty("foo", null, true);
    return procBuilder.getProcessDefinition();
  }
}
