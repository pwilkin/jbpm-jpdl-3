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

// $Id: ProcessDefinitionServiceImpl.java 3485 2008-12-20 14:33:15Z thomas.diesler@jboss.com $

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.ObjectName;

import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.runtime.Context;
import org.jboss.bpm.api.service.ContextService;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.api.service.ProcessInstanceService;
import org.jboss.bpm.api.service.internal.AbstractProcessDefinitionService;
import org.jbpm.JbpmContext;
import org.jbpm.db.GraphSession;
import org.jbpm.integration.spec.model.ProcessDefinitionImpl;
import org.jbpm.integration.spec.runtime.InvocationProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ProcessDefinitionService is the entry point to create, find and otherwise manage process definitions.
 * 
 * @author thomas.diesler@jboss.com
 * @since 18-Jun-2008
 */
public class ProcessDefinitionServiceImpl extends AbstractProcessDefinitionService implements MutableService
{
  // Provide logging
  final static Logger log = LoggerFactory.getLogger(ProcessDefinitionServiceImpl.class);

  @Override
  public void setProcessEngine(ProcessEngine engine)
  {
    super.setProcessEngine(engine);
  }

  public ProcessDefinition getProcessDefinition(ObjectName procDefID)
  {
    ContextService ctxService = getProcessEngine().getService(ContextService.class);
    Context bpmContext = ctxService.getContext(true);
    try
    {
      ProcessDefinition procDef = null;

      GraphSession graphSession = bpmContext.getAttachment(JbpmContext.class).getGraphSession();
      org.jbpm.graph.def.ProcessDefinition oldProcDef = graphSession.getProcessDefinition(adaptKey(procDefID));
      if (oldProcDef != null)
      {
        procDef = ProcessDefinitionImpl.newInstance(getProcessEngine(), oldProcDef, true);
      }

      return procDef;
    }
    finally
    {
      bpmContext.close();
    }
  }

  public Set<ObjectName> getProcessDefinitions()
  {
    ContextService ctxService = getProcessEngine().getService(ContextService.class);
    Context bpmContext = ctxService.getContext(true);
    try
    {
      Set<ObjectName> procDefs = new HashSet<ObjectName>();

      GraphSession graphSession = bpmContext.getAttachment(JbpmContext.class).getGraphSession();
      for (Object item : graphSession.findAllProcessDefinitions())
      {
        org.jbpm.graph.def.ProcessDefinition oldProcDef = (org.jbpm.graph.def.ProcessDefinition)item;
        procDefs.add(ProcessDefinitionImpl.getKey(oldProcDef));
      }

      return Collections.unmodifiableSet(procDefs);
    }
    finally
    {
      bpmContext.close();
    }
  }

  public ProcessDefinition registerProcessDefinition(ProcessDefinition procDef)
  {
    ContextService ctxService = getProcessEngine().getService(ContextService.class);
    Context bpmContext = ctxService.getContext(true);
    try
    {
      log.debug("registerProcessDefinition: " + procDef);

      if (getProcessDefinition(procDef.getKey()) != null)
        throw new IllegalStateException("Process definition already registered: " + procDef);

      JbpmContext jbpmContext = bpmContext.getAttachment(JbpmContext.class);
      ProcessDefinitionImpl procDefImpl = InvocationProxy.getUnderlying(procDef, ProcessDefinitionImpl.class);
      jbpmContext.deployProcessDefinition(procDefImpl.getDelegate());

      procDef = InvocationProxy.newInstance(procDefImpl, ProcessDefinition.class);
      return procDef;
    }
    finally
    {
      bpmContext.close();
    }
  }

  public boolean unregisterProcessDefinition(ObjectName procDefID)
  {
    boolean removed = false;

    ContextService ctxService = getProcessEngine().getService(ContextService.class);
    Context bpmContext = ctxService.getContext(true);
    try
    {
      ProcessDefinition procDef = getProcessDefinition(procDefID);
      if (procDef != null)
      {
        log.debug("unregisterProcessDefinition: " + procDef);

        // Unregister the associated process instances
        ProcessInstanceService procService = getProcessEngine().getService(ProcessInstanceService.class);
        for (ObjectName procID : procService.getInstance(procDefID, null))
          procService.unregisterInstance(procID);

        JbpmContext jbpmContext = bpmContext.getAttachment(JbpmContext.class);
        jbpmContext.getGraphSession().deleteProcessDefinition(adaptKey(procDefID));
        removed = true;
      }
    }
    finally
    {
      bpmContext.close();
    }

    return removed;
  }

  private Long adaptKey(ObjectName key)
  {
    String id = key.getKeyProperty("id");
    if (id == null)
      throw new IllegalStateException("Cannot obtain id property from: " + key);

    return new Long(id);
  }
}