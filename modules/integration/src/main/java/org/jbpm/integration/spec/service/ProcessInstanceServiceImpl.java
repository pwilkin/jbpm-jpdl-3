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
package org.jbpm.integration.spec.service;

// $Id: ProcessInstanceServiceImpl.java 3485 2008-12-20 14:33:15Z thomas.diesler@jboss.com $

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.ObjectName;

import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.model.ProcessInstance.ProcessStatus;
import org.jboss.bpm.api.runtime.Context;
import org.jboss.bpm.api.service.ContextService;
import org.jboss.bpm.api.service.ProcessDefinitionService;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.api.service.ProcessInstanceService;
import org.jboss.bpm.api.service.internal.AbstractService;
import org.jbpm.JbpmContext;
import org.jbpm.db.GraphSession;
import org.jbpm.integration.spec.model.ProcessDefinitionImpl;
import org.jbpm.integration.spec.model.ProcessInstanceImpl;
import org.jbpm.integration.spec.runtime.InvocationProxy;
import org.jbpm.integration.spec.runtime.NodeInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ProcessService is the entry point to create, find and otherwise manage processes.
 * 
 * @author thomas.diesler@jboss.com
 * @since 18-Jun-2008
 */
public class ProcessInstanceServiceImpl extends AbstractService implements ProcessInstanceService, MutableService
{
  // Provide logging
  final static Logger log = LoggerFactory.getLogger(ProcessInstanceServiceImpl.class);

  private List<NodeInterceptor> nodeInterceptors = new ArrayList<NodeInterceptor>();

  public void setProcessEngine(ProcessEngine engine)
  {
    super.setProcessEngine(engine);
  }

  public void setInterceptors(List<String> itorClassNames)
  {
    for (String itorClass : itorClassNames)
    {
      NodeInterceptor itor = loadNodeInterceptor(itorClass);
      nodeInterceptors.add(itor);
    }
  }

  public List<NodeInterceptor> getNodeInterceptors()
  {
    return Collections.unmodifiableList(nodeInterceptors);
  }

  /**
   * Get a Process for a given id
   */
  public ProcessInstance getInstance(ObjectName procID)
  {
    ContextService ctxService = getProcessEngine().getService(ContextService.class);
    Context bpmContext = ctxService.getContext(true);
    try
    {
      ProcessInstance proc = null;

      GraphSession graphSession = bpmContext.getAttachment(JbpmContext.class).getGraphSession();
      org.jbpm.graph.exe.ProcessInstance oldProc = graphSession.getProcessInstance(adaptKey(procID));
      if (oldProc != null)
      {
        proc = ProcessInstanceImpl.newInstance(getProcessEngine(), oldProc, true);
      }

      return proc;
    }
    finally
    {
      bpmContext.close();
    }
  }

  /**
   * Get the set of registered Processes
   */
  public Set<ObjectName> getInstance()
  {
    Set<ObjectName> procs = new HashSet<ObjectName>();

    ContextService ctxService = getProcessEngine().getService(ContextService.class);
    Context bpmContext = ctxService.getContext(true);
    try
    {
      ProcessDefinitionService pdService = getProcessEngine().getService(ProcessDefinitionService.class);
      for (ObjectName procDefID : pdService.getProcessDefinitions())
      {
        Long id = adaptKey(procDefID);
        GraphSession graphSession = bpmContext.getAttachment(JbpmContext.class).getGraphSession();
        for (Object item : graphSession.findProcessInstances(id))
        {
          org.jbpm.graph.exe.ProcessInstance oldProc = (org.jbpm.graph.exe.ProcessInstance)item;
          procs.add(ProcessInstanceImpl.getKey(oldProc));
        }
      }
    }
    finally
    {
      bpmContext.close();
    }
    return Collections.unmodifiableSet(procs);
  }

  /**
   * Find the set of Processes for a given name
   * 
   * @param procDefID The process name
   * @param status The optional process status
   * @return An empty set if the process cannot be found
   */
  public Set<ObjectName> getInstance(ObjectName procDefID, ProcessStatus status)
  {
    Set<ObjectName> procs = new HashSet<ObjectName>();

    ContextService ctxService = getProcessEngine().getService(ContextService.class);
    Context bpmContext = ctxService.getContext(true);
    try
    {
      Long id = adaptKey(procDefID);
      GraphSession graphSession = bpmContext.getAttachment(JbpmContext.class).getGraphSession();
      for (Object item : graphSession.findProcessInstances(id))
      {
        org.jbpm.graph.exe.ProcessInstance oldProc = (org.jbpm.graph.exe.ProcessInstance)item;
        ProcessInstance auxProc = ProcessInstanceImpl.newInstance(getProcessEngine(), oldProc, true);
        if (status == null || auxProc.getProcessStatus() == status)
          procs.add(auxProc.getKey());
      }
    }
    finally
    {
      bpmContext.close();
    }
    return Collections.unmodifiableSet(procs);
  }

  /**
   * Register a Process.
   */
  public ProcessInstance registerInstance(ProcessInstance proc)
  {
    log.debug("registerProcess: " + proc);

    ContextService ctxService = getProcessEngine().getService(ContextService.class);
    Context bpmContext = ctxService.getContext(true);
    try
    {
      if (getInstance(proc.getKey()) != null)
        throw new IllegalStateException("Process already registered: " + proc);

      ProcessStatus procStatus = proc.getProcessStatus();
      if (procStatus != ProcessStatus.None)
        throw new IllegalStateException("Cannot register process in state: " + procStatus);

      ProcessDefinition procDef = proc.getProcessDefinition();

      // Register the process definition if needed
      ProcessDefinitionService procDefService = getProcessEngine().getService(ProcessDefinitionService.class);
      if (procDefService.getProcessDefinition(procDef.getKey()) == null)
        procDefService.registerProcessDefinition(procDef);

      ProcessInstanceImpl procImpl = InvocationProxy.getUnderlying(proc, ProcessInstanceImpl.class);
      org.jbpm.graph.exe.ProcessInstance oldProcInst = procImpl.getDelegate();

      // Make sure the process definition from this session is associated with the process instance
      procDef = procDefService.getProcessDefinition(procDef.getKey());
      ProcessDefinitionImpl procDefImpl = InvocationProxy.getUnderlying(procDef, ProcessDefinitionImpl.class);
      oldProcInst.setProcessDefinition(procDefImpl.getDelegate());

      // Save the ProcessInstance
      JbpmContext jbpmContext = bpmContext.getAttachment(JbpmContext.class);
      jbpmContext.save(oldProcInst);

      procImpl.setProcessStatus(ProcessStatus.Ready);
      proc = InvocationProxy.newInstance(procImpl, ProcessInstance.class);
      return proc;
    }
    finally
    {
      bpmContext.close();
    }
  }

  /**
   * Unregister a Process.
   */
  public boolean unregisterInstance(ObjectName procID)
  {
    boolean removed = false;

    ContextService ctxService = getProcessEngine().getService(ContextService.class);
    Context bpmContext = ctxService.getContext(true);
    try
    {
      ProcessInstance proc = getInstance(procID);
      if (proc != null)
      {
        log.debug("unregisterProcess: " + proc);

        ProcessInstanceImpl procImpl = InvocationProxy.getUnderlying(proc, ProcessInstanceImpl.class);
        GraphSession graphSession = bpmContext.getAttachment(JbpmContext.class).getGraphSession();
        graphSession.deleteProcessInstance(procImpl.getDelegate());
        removed = true;
      }
    }
    finally
    {
      bpmContext.close();
    }
    return removed;

  }

  private NodeInterceptor loadNodeInterceptor(String className)
  {
    NodeInterceptor itor = null;
    if (className != null)
    {
      try
      {
        ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
        itor = (NodeInterceptor)ctxLoader.loadClass(className).getDeclaredConstructor().newInstance();
      }
      catch (Exception ex)
      {
        log.error("Cannot load interceptor: " + className, ex);
      }
    }
    return itor;
  }

  private Long adaptKey(ObjectName key)
  {
    String id = key.getKeyProperty("id");
    if (id == null)
      throw new IllegalStateException("Cannot obtain id property from: " + key);

    return Long.valueOf(id);
  }
}