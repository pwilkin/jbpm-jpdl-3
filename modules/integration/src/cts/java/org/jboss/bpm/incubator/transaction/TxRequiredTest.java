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
package org.jboss.bpm.incubator.transaction;

// $Id: TxRequiredTest.java 3473 2008-12-20 09:31:54Z thomas.diesler@jboss.com $

import java.io.IOException;
import java.util.List;

import org.jboss.bpm.api.Constants;
import org.jboss.bpm.api.Constants.TxType;
import org.jboss.bpm.api.model.Message;
import org.jboss.bpm.api.model.Node;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.runtime.BasicAttachments;
import org.jboss.bpm.api.runtime.Token;
import org.jboss.bpm.api.test.CTSTestCase;
import org.jboss.bpm.incubator.client.ProcessInstanceExt;
import org.jboss.bpm.incubator.model.Group;
import org.jboss.bpm.incubator.model.NodeExt;
import org.jboss.bpm.incubator.model.ProcessDefinitionExt;
import org.jboss.bpm.incubator.model.Group.GroupType;
import org.jboss.bpm.incubator.model.Task.TaskType;
import org.jboss.bpm.incubator.model.builder.ProcessBuilderExt;
import org.jboss.bpm.incubator.runtime.ExecutionHandler;
import org.jboss.bpm.incubator.service.ProcessBuilderService;

/**
 * Test two tasks with Tx attribute REQUIRED
 * 
 * @author thomas.diesler@jboss.com
 * @since 30-Oct-2008
 */
public class TxRequiredTest extends CTSTestCase
{
  public void testNoRollback() throws Exception
  {
    ProcessDefinitionExt procDef = (ProcessDefinitionExt)unregisterOnTearDown(getProcessDefinition());

    NodeExt taskA = (NodeExt)procDef.getNode("TaskA");
    NodeExt taskB = (NodeExt)procDef.getNode("TaskB");
    Group group = procDef.getGroup("TxRequired");
    Group groupA = taskA.getGroupRef();
    Group groupB = taskB.getGroupRef();

    assertNotNull("Group not null", group);
    assertSame("Group same", group, groupA);
    assertSame("Group same", group, groupB);

    ProcessInstanceExt proc = (ProcessInstanceExt)procDef.newInstance();

    proc.startProcessAsync();
    proc.waitForEnd();

    List<Message> messages = getMessages();
    assertEquals("Message expected", 1, messages.size());
  }

  public void testRollback() throws Exception
  {
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());
    ProcessInstanceExt proc = (ProcessInstanceExt)procDef.newInstance();

    BasicAttachments att = new BasicAttachments();
    att.addAttachment(Boolean.class, "rollback", Boolean.TRUE);

    proc.startProcessAsync(att);
    proc.waitForEnd(5000);

    List<Message> messages = getMessages();
    assertEquals("No messages expected", 0, messages.size());
  }

  protected ProcessDefinition getProcessDefinition() throws IOException
  {
    ProcessBuilderService pbService = getProcessEngine().getService(ProcessBuilderService.class);
    ProcessBuilderExt procBuilder = (ProcessBuilderExt)pbService.getProcessBuilder();
    procBuilder.addProcess("RequiresTxTest");
    procBuilder.addStartEvent("Start").addSequenceFlow("TaskA");
    procBuilder.addTaskExt("TaskA", TaskType.Send).addOutMessageRef("TaskAMessage").addGroupRef("TxRequired").addSequenceFlow("TaskB");
    procBuilder.addTaskExt("TaskB").addGroupRef("TxRequired").addSequenceFlow("End");
    procBuilder.addExecutionHandler(TextExecutionHandler.class);
    procBuilder.addEndEvent("End");
    procBuilder.addGroup(GroupType.Transaction, "TxRequired").addProperty(Constants.PROP_TX_TYPE, TxType.REQUIRED);
    procBuilder.addProcessMessage("TaskAMessage").addToRef(getTestID()).addProperty("msgProp", "msgA");
    return procBuilder.getProcessDefinition();
  }

  public static class TextExecutionHandler implements ExecutionHandler
  {
    private static final long serialVersionUID = 1L;

    private Node node;

    public void execute(Token token)
    {
//      Transaction tx = TransactionAssociation.getTransaction();
//      Boolean doRollback = token.getAttachments().getAttachment(Boolean.class, "rollback");
//      if (doRollback == Boolean.TRUE)
//        tx.rollback();
    }

    public Node getNode()
    {
      return node;
    }

    public void setNode(Node node)
    {
      this.node = node;
    }
  }
}
