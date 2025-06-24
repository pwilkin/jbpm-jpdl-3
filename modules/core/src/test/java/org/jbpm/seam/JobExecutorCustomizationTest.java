package org.jbpm.seam;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.job.executor.JobExecutor;


public class JobExecutorCustomizationTest extends AbstractDbTestCase {
  
  public static List jobEvents = new ArrayList();
  
  protected String getJbpmTestConfig() {
    return "org/jbpm/seam/custom.job.executor.jbpm.cfg.xml";
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    jbpmConfiguration.close();
  }

  public void testCustomJobExecutor() {
    JobExecutor jobExecutor = getJbpmConfiguration().getJobExecutor();
    assertEquals(CustomJobExecutor.class, jobExecutor.getClass());
    
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition name='customjobexecution' initial='start'>" +
      "  <node name='start'>" +
      "    <transition to='end'>" +
      "      <action async='true' class='"+AsyncAction.class.getName()+"' />" +
      "    </transition>" +
      "  </node>" +
      "  <state name='end' />" +
      "</process-definition>"
    );
    jbpmContext.deployProcessDefinition(processDefinition);
    long processDefinitionId = processDefinition.getId();
    try {
      
      newTransaction();
      
      jbpmContext.newProcessInstanceForUpdate("customjobexecution");
      
      newTransaction();
      
      jobExecutor.start();
      try {
        waitForJobs(20000);
      } finally {
        jobExecutor.stop();
      }

    } finally {
      newTransaction();
      
      graphSession.deleteProcessDefinition(processDefinitionId);
    }
    
    List expectedJobEvents = new ArrayList();
    expectedJobEvents.add("before");
    expectedJobEvents.add("execute action");
    expectedJobEvents.add("after");
    
    assertEquals(expectedJobEvents, jobEvents);
  }
}
