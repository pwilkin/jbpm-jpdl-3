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
package org.jbpm.integration.spec.task;

// $Id: TaskInstanceImpl.java 3466 2008-12-19 22:53:18Z thomas.diesler@jboss.com $

import java.util.HashSet;
import java.util.Set;

import javax.management.ObjectName;

import org.jboss.bpm.api.model.ObjectNameFactory;
import org.jboss.bpm.api.runtime.Token;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.api.service.TokenService;
import org.jboss.bpm.incubator.task.TaskInstance;
import org.jbpm.integration.spec.model.AbstractElementImpl;
import org.jbpm.integration.spec.runtime.InvocationProxy;
import org.jbpm.integration.spec.runtime.TokenImpl;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The TaskService manages Tasks.
 * 
 * @author thomas.diesler@jboss.com
 * @since 28-Nov-2008
 */
public class TaskInstanceImpl extends AbstractElementImpl<org.jbpm.taskmgmt.exe.TaskInstance> implements TaskInstance
{
  private static final long serialVersionUID = 1L;

  // Provide logging
  final Logger log = LoggerFactory.getLogger(TaskInstanceImpl.class);

  private ObjectName keyCache;

  public static TaskInstance newInstance(ProcessEngine engine, org.jbpm.taskmgmt.exe.TaskInstance tmpTask)
  {
    return InvocationProxy.newInstance(new TaskInstanceImpl(engine, tmpTask), TaskInstance.class);
  }

  private TaskInstanceImpl(ProcessEngine engine, org.jbpm.taskmgmt.exe.TaskInstance tmpTask)
  {
    super(engine, tmpTask, org.jbpm.taskmgmt.exe.TaskInstance.class);

    if (tmpTask.getId() > 0)
      keyCache = getKey(tmpTask);
  }

  public ObjectName getKey()
  {
    ObjectName objKey = keyCache;
    if (objKey == null)
    {
      org.jbpm.taskmgmt.exe.TaskInstance delegate = getDelegate();
      objKey = getKey(delegate);
      if (delegate.getId() > 0)
        keyCache = objKey;
    }
    return objKey;
  }

  public static ObjectName getKey(org.jbpm.taskmgmt.exe.TaskInstance tmpTask)
  {
    return ObjectNameFactory.create("Task:id=" + tmpTask.getId());
  }

  public String getName()
  {
    return getDelegate().getName();
  }

  public void end(String targetName)
  {
    getDelegate().end(targetName);
  }

  public boolean hasEnded()
  {
    return getDelegate().hasEnded();
  }

  public boolean isCancelled()
  {
    return getDelegate().isCancelled();
  }

  public boolean isBlocking()
  {
    return getDelegate().isBlocking();
  }

  public boolean isSignalling()
  {
    return getDelegate().isSignalling();
  }

  public String getActor()
  {
    return getDelegate().getActorId();
  }

  public ObjectName getCorrelationKey()
  {
    Token token = null;

    if (getDelegate().getToken() != null)
    {
      long tokenID = getDelegate().getToken().getId();
      TokenService exService = getProcessEngine().getService(TokenService.class);
      for (Token auxTok : exService.getTokens())
      {
        TokenImpl tokenImpl = InvocationProxy.getUnderlying(auxTok, TokenImpl.class);
        if (tokenImpl.getDelegate().getId() == tokenID)
        {
          token = auxTok;
          break;
        }
      }
    }

    if (token == null)
      throw new IllegalStateException("Cannot obtain associated token");

    return token.getKey();
  }

  public Set<String> getPooledActors()
  {
    Set<String> actors = new HashSet<String>();
    for (PooledActor pa : getDelegate().getPooledActors())
    {
      actors.add(pa.getActorId());
    }
    return actors;
  }
}
