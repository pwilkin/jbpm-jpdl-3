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
package org.jbpm.enterprise.ejbtimer;

import java.rmi.RemoteException;
import java.util.Collections;

import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.Test;

import org.apache.cactus.ServletTestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.EventCallback;
import org.jbpm.JbpmContext;
import org.jboss.bpm.api.test.IntegrationTestSetup;
import org.jbpm.command.Command;
import org.jbpm.command.DeployProcessCommand;
import org.jbpm.command.StartProcessInstanceCommand;
import org.jbpm.ejb.LocalCommandService;
import org.jbpm.ejb.LocalCommandServiceHome;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.scheduler.ejbtimer.EntitySchedulerService;

/**
 * Exercises for the {@linkplain EntitySchedulerService EJB scheduler service}.
 * 
 * @author Alejandro Guizar
 */
public class EjbSchedulerTest extends ServletTestCase {

  private LocalCommandService commandService;

  private static LocalCommandServiceHome commandServiceHome;

  private static final Log log = LogFactory.getLog(EjbSchedulerTest.class);

  public static Test suite() throws Exception {
    return new IntegrationTestSetup(EjbSchedulerTest.class, "enterprise-test.war");
  }

  protected void setUp() throws Exception {
    if (commandServiceHome == null) {
      Context initialContext = new InitialContext();
      try {
        commandServiceHome = (LocalCommandServiceHome) initialContext.lookup("java:comp/env/ejb/CommandServiceBean");
      }
      finally {
        initialContext.close();
      }
    }
    commandService = commandServiceHome.create();
    log.info("### " + getName() + " started ###");
  }

  protected void tearDown() throws Exception {
    log.info("### " + getName() + " done ###");
    commandService = null;
  }

  public void testScheduleFuture() throws Exception {
    deployProcess("<process-definition name='future'>"
        + "  <event type='process-end'>"
        + "    <action expression='#{eventCallback.processEnd}'/>"
        + "  </event>"
        + "  <start-state name='start'>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <state name='a'>"
        + "    <timer duedate='1 second' transition='timeout' />"
        + "    <transition name='timeout' to='end' />"
        + "  </state>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    long processId = launchProcess("future").getId();
    EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    assertTrue(isProcessFinished(processId));
  }

  public void testSchedulePast() throws Exception {
    deployProcess("<process-definition name='past'>"
        + "  <event type='process-end'>"
        + "    <action expression='#{eventCallback.processEnd}'/>"
        + "  </event>"
        + "  <start-state name='start'>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <state name='a'>"
        + "    <timer duedate='-1 second' transition='timeout' />"
        + "    <transition name='timeout' to='end' />"
        + "  </state>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    long processId = launchProcess("past").getId();
    EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    assertTrue(isProcessFinished(processId));
  }

  public void testScheduleRepeat() throws Exception {
    deployProcess("<process-definition name='repeat'>"
        + "  <event type='timer'>"
        + "    <action expression='#{eventCallback.timer}'/>"
        + "  </event>"
        + "  <start-state name='start'>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <state name='a'>"
        + "    <timer duedate='1 second' repeat='1 second' />"
        + "    <transition to='end' />"
        + "  </state>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    long processId = launchProcess("repeat").getId();
    for (int i = 0; i < 3; i++) {
      EventCallback.waitForEvent(Event.EVENTTYPE_TIMER);
      assertEquals("a", getProcessState(processId));
    }
    signalProcess(processId);
    assertTrue(isProcessFinished(processId));
  }

  public void testCancel() throws Exception {
    deployProcess("<process-definition name='cancel'>"
        + "  <event type='timer'>"
        + "    <action expression='#{eventCallback.timer}'/>"
        + "  </event>"
        + "  <start-state name='start'>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <state name='a'>"
        + "    <timer duedate='1 second' repeat='1 second' />"
        + "    <transition to='end' />"
        + "  </state>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    ProcessInstance process = launchProcess("cancel");
    long processId = process.getId();
    // first expiration
    EventCallback.waitForEvent(Event.EVENTTYPE_TIMER);
    assertEquals("a", getProcessState(processId));
    // repeated expiration
    EventCallback.waitForEvent(Event.EVENTTYPE_TIMER);
    assertEquals("a", getProcessState(processId));
    // no more expirations
    cancelTimer("a", process.getRootToken().getId());
    EventCallback.waitForEvent(Event.EVENTTYPE_TIMER, 2000);
    signalProcess(processId);
    assertTrue(isProcessFinished(processId));
  }

  public void testScheduleSequence() throws Exception {
    deployProcess("<process-definition name='sequence'>"
        + "  <event type='process-end'>"
        + "    <action expression='#{eventCallback.processEnd}'/>"
        + "  </event>"
        + "  <event type='timer'>"
        + "    <action expression='#{eventCallback.timer}'/>"
        + "  </event>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <state name='a'>"
        + "    <timer duedate='500 milliseconds' transition='timeout' />"
        + "    <transition name='timeout' to='b' />"
        + "  </state>"
        + "  <state name='b'>"
        + "    <timer duedate='500 milliseconds' transition='timeout' />"
        + "    <transition name='timeout' to='c' />"
        + "  </state>"
        + "  <state name='c'>"
        + "    <timer duedate='500 milliseconds' transition='timeout' />"
        + "    <transition name='timeout' to='d' />"
        + "  </state>"
        + "  <state name='d'>"
        + "    <timer duedate='500 milliseconds' transition='timeout' />"
        + "    <transition name='timeout' to='e' />"
        + "  </state>"
        + "  <state name='e'>"
        + "    <timer duedate='500 milliseconds' transition='timeout' />"
        + "    <transition name='timeout' to='end' />"
        + "  </state>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    long processId = launchProcess("sequence").getId();
    char state = 'b';
    for (int i = 0; i < 4; i++) {
      EventCallback.waitForEvent(Event.EVENTTYPE_TIMER);
      assertEquals(Character.toString(state++), getProcessState(processId));
    }
    EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    assertTrue(isProcessFinished(processId));
  }

  public void testScheduleFork() throws Exception {
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
        + "  <state name='a'>"
        + "    <timer duedate='0.25 seconds' transition='timeout' />"
        + "    <transition name='timeout' to='j' />"
        + "  </state>"
        + "  <state name='b'>"
        + "    <timer duedate='0.5 seconds' transition='timeout' />"
        + "    <transition name='timeout' to='j' />"
        + "  </state>"
        + "  <state name='c'>"
        + "    <timer duedate='0.75 seconds' transition='timeout' />"
        + "    <transition name='timeout' to='j' />"
        + "  </state>"
        + "  <state name='d'>"
        + "    <timer duedate='1 second' transition='timeout' />"
        + "    <transition name='timeout' to='j' />"
        + "  </state>"
        + "  <state name='e'>"
        + "    <timer duedate='1.25 second' transition='timeout' />"
        + "    <transition name='timeout' to='j' />"
        + "  </state>"
        + "  <join name='j' async='exclusive'>"
        + "    <transition to='end' />"
        + "  </join>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    long processId = launchProcess("fork").getId();
    EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    assertTrue(isProcessFinished(processId));
  }

  private ProcessDefinition deployProcess(String xml) throws RemoteException {
    return (ProcessDefinition) commandService.execute(new DeployProcessCommand(xml));
  }

  private ProcessInstance launchProcess(String processName) throws RemoteException {
    StartProcessInstanceCommand command = new StartProcessInstanceCommand();
    command.setProcessName(processName);
    command.setVariables(Collections.singletonMap("eventCallback", new EventCallback()));
    return (ProcessInstance) commandService.execute(command);
  }

  private void signalProcess(final long processId) throws RemoteException {
    commandService.execute(new Command() {

      private static final long serialVersionUID = 1L;

      public Object execute(JbpmContext jbpmContext) throws Exception {
        jbpmContext.loadProcessInstanceForUpdate(processId).signal();
        return null;
      }
    });
  }

  private String getProcessState(final long processId) throws RemoteException {
    return (String) commandService.execute(new Command() {

      private static final long serialVersionUID = 1L;

      public Object execute(JbpmContext jbpmContext) throws Exception {
        return jbpmContext.loadProcessInstance(processId).getRootToken().getNode().getName();
      }
    });
  }

  private boolean isProcessFinished(final long processId) throws RemoteException {
    Boolean isFinished = (Boolean) commandService.execute(new Command() {

      private static final long serialVersionUID = 1L;

      public Object execute(JbpmContext jbpmContext) throws Exception {
        return jbpmContext.loadProcessInstance(processId).hasEnded();
      }
    });
    return isFinished.booleanValue();
  }

  private void cancelTimer(final String timerName, final long tokenId) throws RemoteException {
    commandService.execute(new Command() {

      private static final long serialVersionUID = 1L;

      public Object execute(JbpmContext jbpmContext) throws Exception {
        Token token = jbpmContext.loadToken(tokenId);
        jbpmContext.getServices().getSchedulerService().deleteTimersByName(timerName, token);
        return null;
      }
    });
  }
}
