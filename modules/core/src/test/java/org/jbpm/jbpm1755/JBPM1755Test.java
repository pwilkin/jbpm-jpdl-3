package org.jbpm.jbpm1755;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.LockMode;
import org.jbpm.EventCallback;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.node.Join;

/**
 * Allow process author to set the parent token lock mode in the join token.
 * 
 * https://jira.jboss.org/jira/browse/JBPM-1755
 * 
 * @author Alejandro Guizar
 */
public class JBPM1755Test extends AbstractDbTestCase {

  private long processDefinitionId;

  private static final int processInstanceCount = 5;
  private static final long maxWaitTime = 10 * 1000;
  private static final Log log = LogFactory.getLog(JBPM1755Test.class);

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm1755/parallelprocess.xml");
    jbpmContext.deployProcessDefinition(processDefinition);
    processDefinitionId = processDefinition.getId();

    startJobExecutor();
  }

  @Override
  protected void tearDown() throws Exception {
    stopJobExecutor();
    graphSession.deleteProcessDefinition(processDefinitionId);
    super.tearDown();
    
    EventCallback.clear();
  }

  public void testReadLock() {
    launchProcessInstances(LockMode.READ);
  }

  public void testUpgradeLock() {
    launchProcessInstances(LockMode.UPGRADE);
  }

  public void testForceLock() {
    launchProcessInstances(LockMode.FORCE);
  }

  private void launchProcessInstances(LockMode lockMode) {
    ProcessDefinition processDefinition = graphSession.loadProcessDefinition(processDefinitionId);
    Join join = (Join) processDefinition.getNode("join1");
    join.setParentLockMode(lockMode.toString());

    long[] processInstanceIds = new long[processInstanceCount];
    for (int i = 0; i < processInstanceCount; i++) {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstanceIds[i] = processInstance.getId();
      processInstance.getContextInstance().setVariable("eventCallback", new EventCallback());
      processInstance.signal();
      jbpmContext.save(processInstance);
    }
    commitAndCloseSession();

    for (int i = 0; i < processInstanceCount; i++) {
      long processInstanceId = processInstanceIds[i];
      waitForProcessInstanceEnd(processInstanceId);
      assertTrue(hasProcessInstanceEnded(processInstanceId));
    }

    processJobs(maxWaitTime);
  }

  private void waitForProcessInstanceEnd(long processInstanceId) {
    long startTime = System.currentTimeMillis();
    do {
      EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END, 1000);
      if (System.currentTimeMillis() - startTime > maxWaitTime) {
        log.warn("process instance " + processInstanceId + " took too long");
        break;
      }
    } while (!hasProcessInstanceEnded(processInstanceId));
  }

  private boolean hasProcessInstanceEnded(long processInstanceId) {
    beginSessionTransaction();
    try {
      return jbpmContext.loadProcessInstance(processInstanceId).hasEnded();
    }
    finally {
      commitAndCloseSession();
    }
  }

}
