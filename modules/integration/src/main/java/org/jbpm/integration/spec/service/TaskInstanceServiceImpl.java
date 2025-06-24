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

// $Id: TaskInstanceServiceImpl.java 3485 2008-12-20 14:33:15Z thomas.diesler@jboss.com $

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.ObjectName;

import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.runtime.Context;
import org.jboss.bpm.api.service.ContextService;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.api.service.ProcessInstanceService;
import org.jboss.bpm.api.service.internal.AbstractService;
import org.jboss.bpm.incubator.service.TaskInstanceService;
import org.jboss.bpm.incubator.task.TaskInstance;
import org.jbpm.JbpmContext;
import org.jbpm.integration.spec.model.ProcessInstanceImpl;
import org.jbpm.integration.spec.runtime.InvocationProxy;
import org.jbpm.integration.spec.task.TaskInstanceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The TaskService manages Tasks.
 * 
 * @author thomas.diesler@jboss.com
 * @since 28-Nov-2008
 */
public class TaskInstanceServiceImpl extends AbstractService implements TaskInstanceService, MutableService
{
  // Provide logging
  final Logger log = LoggerFactory.getLogger(TaskInstanceServiceImpl.class);

  public void setProcessEngine(ProcessEngine engine)
  {
    super.setProcessEngine(engine);
  }

  public List<TaskInstance> getTasksByProcess(ObjectName procID)
  {
    ContextService ctxService = getProcessEngine().getService(ContextService.class);
    Context bpmContext = ctxService.getContext(true);
    try
    {
      ProcessInstanceService procService = getProcessEngine().getService(ProcessInstanceService.class);
      ProcessInstance proc = procService.getInstance(procID);
      if (proc == null)
        throw new IllegalStateException("Cannot obtain process: " + procID);

      List<TaskInstance> tasks = new ArrayList<TaskInstance>();
      ProcessInstanceImpl procImpl = InvocationProxy.getUnderlying(proc, ProcessInstanceImpl.class);
      Collection<org.jbpm.taskmgmt.exe.TaskInstance> oldTaskInstances = procImpl.getDelegate().getTaskMgmtInstance().getTaskInstances();
      for (org.jbpm.taskmgmt.exe.TaskInstance oldTaskInst : oldTaskInstances)
      {
        TaskInstance task = TaskInstanceImpl.newInstance(getProcessEngine(), oldTaskInst);
        tasks.add(task);
      }

      return tasks;
    }
    catch (RuntimeException rte)
    {
      throw rte;
    }
    finally
    {
      bpmContext.close();
    }
  }

  public TaskInstance getTask(ObjectName taskID)
  {
    ContextService ctxService = getProcessEngine().getService(ContextService.class);
    Context bpmContext = ctxService.getContext(true);
    try
    {
      JbpmContext jbpmContext = bpmContext.getAttachment(JbpmContext.class);
      org.jbpm.taskmgmt.exe.TaskInstance oldTaskInst = jbpmContext.getTaskInstance(adaptKey(taskID));
      return TaskInstanceImpl.newInstance(getProcessEngine(), oldTaskInst);
    }
    catch (RuntimeException rte)
    {
      throw rte;
    }
    finally
    {
      bpmContext.close();
    }
  }

  @SuppressWarnings("unchecked")
  public List<TaskInstance> getTasksByActor(String actor)
  {
    ContextService ctxService = getProcessEngine().getService(ContextService.class);
    Context bpmContext = ctxService.getContext(true);
    try
    {
      List<TaskInstance> tasks = new ArrayList<TaskInstance>();
      JbpmContext jbpmContext = bpmContext.getAttachment(JbpmContext.class);
      for (org.jbpm.taskmgmt.exe.TaskInstance oldTaskInst : (List<org.jbpm.taskmgmt.exe.TaskInstance>)jbpmContext.getTaskList(actor))
      {
        TaskInstance taskInst = TaskInstanceImpl.newInstance(getProcessEngine(), oldTaskInst);
        tasks.add(taskInst);
      }
      return tasks;
    }
    catch (RuntimeException rte)
    {
      throw rte;
    }
    finally
    {
      bpmContext.close();
    }
  }

  public void closeTask(ObjectName taskID, String signalName)
  {
    ContextService ctxService = getProcessEngine().getService(ContextService.class);
    Context bpmContext = ctxService.getContext(true);
    try
    {
      TaskInstanceImpl taskImpl = InvocationProxy.getUnderlying(getTask(taskID), TaskInstanceImpl.class);
      org.jbpm.taskmgmt.exe.TaskInstance oldTaskInst = taskImpl.getDelegate();

      if (signalName != null)
        oldTaskInst.end(signalName);
      else
        oldTaskInst.end();
    }
    catch (RuntimeException rte)
    {
      throw rte;
    }
    finally
    {
      bpmContext.close();
    }
  }

  public void reassignTask(ObjectName taskID, String actor)
  {
    ContextService ctxService = getProcessEngine().getService(ContextService.class);
    Context bpmContext = ctxService.getContext(true);
    try
    {
      TaskInstanceImpl taskImpl = InvocationProxy.getUnderlying(getTask(taskID), TaskInstanceImpl.class);
      org.jbpm.taskmgmt.exe.TaskInstance oldTaskInst = taskImpl.getDelegate();

      if (oldTaskInst.getStart() != null)
      {
        log.warn("Force stop on task " + oldTaskInst.getId() + ". Will be restarted.");
        oldTaskInst.setStart(null); // strange, but means isNotStarted()
      }

      oldTaskInst.start(actor, true);
    }
    catch (RuntimeException rte)
    {
      throw rte;
    }
    finally
    {
      bpmContext.close();
    }
  }

  private Long adaptKey(ObjectName key)
  {
    String id = key.getKeyProperty("id");
    if (id == null)
      throw new IllegalStateException("Cannot obtain id property from: " + key);

    return new Long(id);
  }
}
