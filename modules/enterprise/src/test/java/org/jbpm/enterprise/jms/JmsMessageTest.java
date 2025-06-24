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
package org.jbpm.enterprise.jms;

import java.util.Collections;

import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.Test;

import org.apache.cactus.ServletTestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.Environment;
import org.jboss.bpm.api.test.IntegrationTestSetup;
import org.jbpm.EventCallback;
import org.jbpm.JbpmContext;
import org.jbpm.command.Command;
import org.jbpm.command.DeployProcessCommand;
import org.jbpm.command.StartProcessInstanceCommand;
import org.jbpm.ejb.LocalCommandService;
import org.jbpm.ejb.LocalCommandServiceHome;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.msg.jms.JmsMessageService;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.Services;

/**
 * Exercises for the {@linkplain JmsMessageService JMS message service}.
 * 
 * @author Alejandro Guizar
 */
public class JmsMessageTest extends ServletTestCase 
{
  private static Log log = LogFactory.getLog(JmsMessageTest.class);
  
  private LocalCommandService commandService;

  private static LocalCommandServiceHome commandServiceHome;

  static final int processExecutionCount = 5;
  static final int maxWaitTime = 10 * 1000;

  public static Test suite() throws Exception
  {
    return new IntegrationTestSetup(JmsMessageTest.class, "enterprise-test.war");
  }

  protected void setUp() throws Exception
  {
    if (commandServiceHome == null)
    {
      Context initialContext = new InitialContext();
      try
      {
        commandServiceHome = (LocalCommandServiceHome)initialContext.lookup("java:comp/env/ejb/CommandServiceBean");
      }
      finally
      {
        initialContext.close();
      }
    }
    commandService = commandServiceHome.create();
    log.info("### " + getName() + " started ###");
  }

  protected void tearDown() throws Exception
  {
    log.info("### " + getName() + " done ###");
    commandService = null;
    EventCallback.clear();
  }

  public void testAsyncNode() 
  {
    deployProcess("<process-definition name='node'>"
        + "  <event type='process-end'>"
        + "    <action expression='#{eventCallback.processEnd}'/>"
        + "  </event>"
        + "  <start-state name='start'>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <node name='a' async='true'>"
        + "    <transition to='end' />"
        + "  </node>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    
    long processId = launchProcess("node").getId();
    EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    assertTrue("Process has ended", hasProcessEnded(processId));
  }

  public void testAsyncAction() 
  {
    deployProcess("<process-definition name='action'>"
        + "  <start-state name='start'>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <node name='a'>"
        + "    <event type='node-enter'>"
        + "      <action async='true' expression='#{eventCallback.nodeEnter}' />"
        + "    </event>"
        + "    <event type='node-leave'>"
        + "      <action async='true' expression='#{eventCallback.nodeLeave}' />"
        + "    </event>"
        + "    <transition to='end'>"
        + "      <action async='true' expression='#{eventCallback.transition}' />"
        + "    </transition>"
        + "  </node>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    
    long processId = launchProcess("action").getId();
    EventCallback.waitForEvent(Event.EVENTTYPE_NODE_ENTER);
    EventCallback.waitForEvent(Event.EVENTTYPE_NODE_LEAVE);
    EventCallback.waitForEvent(Event.EVENTTYPE_TRANSITION);
    assertTrue("Process has ended", hasProcessEnded(processId));
  }

  public void testAsyncSequence() 
  {
    deployProcess("<process-definition name='sequence'>"
        + "  <event type='process-end'>"
        + "    <action expression='#{eventCallback.processEnd}'/>"
        + "  </event>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <node name='a' async='true'>"
        + "    <transition to='b' />"
        + "  </node>"
        + "  <node name='b' async='true'>"
        + "    <transition to='c' />"
        + "  </node>"
        + "  <node name='c' async='true'>"
        + "    <transition to='d' />"
        + "  </node>"
        + "  <node name='d' async='true'>"
        + "    <transition to='e' />"
        + "  </node>"
        + "  <node name='e' async='true'>"
        + "    <transition to='end' />"
        + "  </node>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    
    long processId = launchProcess("sequence").getId();
    EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    assertTrue("Process has ended", hasProcessEnded(processId));
  }

  public void testAsyncFork() throws Exception 
  {
    // [JBPM-1811] JmsMessageTest fails intermittently on HSQLDB
    if (getHibernateDialect().indexOf("HSQL") != -1) 
    {
      return;
    }

    deployProcess("<process-definition name='fork'>"
        + "  <event type='process-end'>"
        + "    <action expression='#{eventCallback.processEnd}'/>"
        + "  </event>"
        + "  <start-state>"
        + "    <transition to='f' />"
        + "  </start-state>"
        + "  <fork name='f'>"
        + "    <transition name='a' to='a' />"
        + "    <transition name='b' to='b' />"
        + "    <transition name='c' to='c' />"
        + "    <transition name='d' to='d' />"
        + "    <transition name='e' to='e' />"
        + "  </fork>"
        + "  <node name='a'>"
        + "    <transition to='j' />"
        + "  </node>"
        + "  <node name='b' async='true'>"
        + "    <transition to='j' />"
        + "  </node>"
        + "  <node name='c' async='true'>"
        + "    <transition to='j' />"
        + "  </node>"
        + "  <node name='d' async='true'>"
        + "    <transition to='j' />"
        + "  </node>"
        + "  <node name='e' async='true'>"
        + "    <transition to='j' />"
        + "  </node>"
        + "  <join name='j' async='exclusive'>"
        + "    <transition to='end' />"
        + "  </join>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    
    long processId = launchProcess("fork").getId();
    EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    Thread.sleep(1000);
    assertTrue("Process has ended", hasProcessEnded(processId));
  }

  private String getHibernateDialect()
  {
    return (String)commandService.execute(new Command()
    {
      private static final long serialVersionUID = 1L;

      public Object execute(JbpmContext jbpmContext) throws Exception
      {
        DbPersistenceServiceFactory factory = (DbPersistenceServiceFactory)jbpmContext.getServiceFactory(Services.SERVICENAME_PERSISTENCE);
        return factory.getConfiguration().getProperty(Environment.DIALECT);
      }
    });
  }

  public void testAsyncExecutions() 
  {
    deployProcess("<process-definition name='execution'>"
        + "  <event type='process-end'>"
        + "    <action expression='#{eventCallback.processEnd}' />"
        + "  </event>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <node name='a' async='true'>"
        + "    <transition to='b' />"
        + "  </node>"
        + "  <node name='b'>"
        + "    <event type='node-enter'>"
        + "      <action async='exclusive' expression='#{eventCallback.nodeEnter}' />"
        + "    </event>"
        + "    <transition to='c' />"
        + "  </node>"
        + "  <node name='c' async='exclusive'>"
        + "    <transition to='d' />"
        + "  </node>"
        + "  <node name='d'>"
        + "    <event type='node-leave'>"
        + "      <action async='exclusive' expression='#{eventCallback.nodeLeave}' />"
        + "    </event>"
        + "    <transition to='e' />"
        + "  </node>"
        + "  <node name='e' async='exclusive'>"
        + "    <transition to='end' />"
        + "  </node>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    
    long[] processIds = new long[processExecutionCount];
    for (int i = 0; i < processExecutionCount; i++)
    {
      processIds[i] = launchProcess("execution").getId();
      EventCallback.waitForEvent(Event.EVENTTYPE_NODE_ENTER);
    }
    for (int i = 0; i < processExecutionCount; i++)
    {
      EventCallback.waitForEvent(Event.EVENTTYPE_NODE_LEAVE);
    }
    for (int i = 0; i < processExecutionCount; i++)
    {
      waitForProcessEnd(processIds[i]);
      assertTrue(hasProcessEnded(processIds[i]));
    }
  }

  private ProcessDefinition deployProcess(String xml)
  {
    return (ProcessDefinition)commandService.execute(new DeployProcessCommand(xml));
  }

  private ProcessInstance launchProcess(String processName)
  {
    StartProcessInstanceCommand command = new StartProcessInstanceCommand();
    command.setProcessDefinitionName(processName);
    command.setVariables(Collections.singletonMap("eventCallback", new EventCallback()));
    return (ProcessInstance)commandService.execute(command);
  }

  private boolean hasProcessEnded(final long processId)
  {
    Boolean isFinished = (Boolean)commandService.execute(new Command()
    {
      private static final long serialVersionUID = 1L;

      public Object execute(JbpmContext jbpmContext) throws Exception
      {
        return jbpmContext.loadProcessInstance(processId).hasEnded();
      }
    });
    return isFinished.booleanValue();
  }

  private void waitForProcessEnd(long processId)
  {
    long startTime = System.currentTimeMillis();
    do
    {
      EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END, 1000);
      if (System.currentTimeMillis() - startTime > maxWaitTime)
      {
        log.warn("process " + processId + " took too long");
        break;
      }
    }
    while (!hasProcessEnded(processId));
  }
}
