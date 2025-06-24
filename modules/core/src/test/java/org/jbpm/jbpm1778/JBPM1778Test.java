package org.jbpm.jbpm1778;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

/**
 * Empty map variables on process creation is set as null
 * 
 * https://jira.jboss.org/jira/browse/JBPM-1778
 * 
 * @author Thomas.Diesler@jboss.com
 */
public class JBPM1778Test extends AbstractJbpmTestCase
{
  public void testEmptyMapVariables()
  {
    ProcessDefinition pd = getProcessDEfinition();
    
    ProcessInstance pi = pd.createProcessInstance(new HashMap<String, String>());
    TaskMgmtInstance tmi = pi.getTaskMgmtInstance();
    tmi.createStartTaskInstance();

    Map piVars = pi.getContextInstance().getVariables();
    assertNotNull("ProcessInstance vars not null", piVars);
    assertEquals("ProcessInstance vars empty", 0, piVars.size());
  }

  public void testNonEmptyMapVariables()
  {
    ProcessDefinition pd = getProcessDEfinition();
    
    HashMap<String, String> vars = new HashMap<String, String>();
    vars.put("uno", "dos");
    
    ProcessInstance pi = pd.createProcessInstance(vars);
    TaskMgmtInstance tmi = pi.getTaskMgmtInstance();
    tmi.createStartTaskInstance();

    Map piVars = pi.getContextInstance().getVariables();
    assertNotNull("ProcessInstance vars not null", piVars);
    assertEquals("ProcessInstance vars not empty", 1, piVars.size());
  }

  private ProcessDefinition getProcessDEfinition()
  {
    ProcessDefinition pd = ProcessDefinition.parseXmlString(
        "<process-definition>" + 
        " <start-state>" + 
        "  <transition to='s' />" + 
        " </start-state>" + 
        " <state name='s'>" + 
        "  <transition to='end' />" + 
        " </state>" + 
        " <end-state name='end' />" + 
        "</process-definition>");
    return pd;
  }
}
