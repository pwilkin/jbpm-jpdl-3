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
package org.jbpm.integration.spec.model;

// $Id: ProcessInstanceImpl.java 3479 2008-12-20 12:55:32Z thomas.diesler@jboss.com $

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.ObjectName;

import org.jboss.bpm.api.Constants;
import org.jboss.bpm.api.model.Node;
import org.jboss.bpm.api.model.ObjectNameFactory;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.runtime.Attachments;
import org.jboss.bpm.api.runtime.Context;
import org.jboss.bpm.api.runtime.Token;
import org.jboss.bpm.api.runtime.Attachments.Key;
import org.jboss.bpm.api.runtime.Token.TokenStatus;
import org.jboss.bpm.api.service.ContextService;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.api.service.ProcessInstanceService;
import org.jbpm.JbpmContext;
import org.jbpm.integration.spec.runtime.InvocationProxy;
import org.jbpm.integration.spec.runtime.TokenImpl;
import org.jbpm.util.Clock;

/**
 * An integration wrapper
 * 
 * @author thomas.diesler@jboss.com
 * @since 15-Nov-2008
 */
public class ProcessInstanceImpl extends AbstractElementImpl<org.jbpm.graph.exe.ProcessInstance> implements ProcessInstance
{
  private static final long serialVersionUID = 1L;

  private ObjectName keyCache;

  public static ProcessInstance newInstance(ProcessEngine engine, org.jbpm.graph.exe.ProcessInstance tmpProc, boolean proxy)
  {
    ProcessInstance proc = new ProcessInstanceImpl(engine, tmpProc);
    if (proxy == true)
    {
      proc = InvocationProxy.newInstance((ProcessInstanceImpl)proc, ProcessInstance.class);
    }
    return proc;
  }

  private ProcessInstanceImpl(ProcessEngine engine, org.jbpm.graph.exe.ProcessInstance tmpProc)
  {
    super(engine, tmpProc, org.jbpm.graph.exe.ProcessInstance.class);

    if (tmpProc.getId() > 0)
      keyCache = getKey(tmpProc);
  }

  public ObjectName getKey()
  {
    ObjectName objKey = keyCache;
    if (objKey == null)
    {
      org.jbpm.graph.exe.ProcessInstance delegate = getDelegate();
      objKey = getKey(delegate);
      if (delegate.getId() > 0)
        keyCache = objKey;
    }
    return objKey;
  }

  public static ObjectName getKey(org.jbpm.graph.exe.ProcessInstance procInst)
  {
    org.jbpm.graph.def.ProcessDefinition oldProcDef = procInst.getProcessDefinition();
    return ObjectNameFactory.create(Constants.ID_DOMAIN + ":procInst=" + oldProcDef.getName() + ",id=" + procInst.getId());
  }

  public ProcessDefinition getProcessDefinition()
  {
    org.jbpm.graph.def.ProcessDefinition oldProcDef = getDelegate().getProcessDefinition();
    return ProcessDefinitionImpl.newInstance(getProcessEngine(), oldProcDef, true);
  }

  public ProcessStatus getProcessStatus()
  {
    Token rootToken = getRootToken();
    TokenStatus tokenStatus = rootToken != null ? rootToken.getTokenStatus() : null;

    org.jbpm.graph.exe.ProcessInstance oldProcInst = getDelegate();

    long procID = oldProcInst.getId();
    ProcessStatus status = procID > 0 ? ProcessStatus.Ready : ProcessStatus.None;

    if (oldProcInst.getStart() != null)
      status = ProcessStatus.Active;

    if (oldProcInst.isSuspended())
      status = ProcessStatus.Suspended;

    if (oldProcInst.hasEnded() || tokenStatus == TokenStatus.Destroyed)
      status = ProcessStatus.Completed;

    return status;
  }

  public void setProcessStatus(ProcessStatus status)
  {
    // this.status = status;
  }

  public Token getRootToken()
  {
    Token token = null;
    if (getDelegate().getRootToken() != null)
    {
      token = TokenImpl.newInstance(getProcessEngine(), getDelegate().getRootToken());
    }
    return token;
  }

  public String getName()
  {
    return getProcessDefinition().getName();
  }

  public Date getEndDate()
  {
    return getDelegate().getEnd();
  }

  public Date getStartDate()
  {
    return getDelegate().getStart();
  }

  public <T extends Node> T getNode(Class<T> clazz, String name)
  {
    return getProcessDefinition().getNode(clazz, name);
  }

  public Node getNode(String name)
  {
    return getProcessDefinition().getNode(name);
  }

  public List<Node> getNodes()
  {
    return getProcessDefinition().getNodes();
  }

  public <T extends Node> List<T> getNodes(Class<T> clazz)
  {
    return getProcessDefinition().getNodes(clazz);
  }

  public Token startProcess()
  {
    return startProcess(null);
  }

  public Token startProcess(Attachments contextData)
  {
    ContextService ctxService = getProcessEngine().getService(ContextService.class);
    Context bpmContext = ctxService.getContext(true);
    try
    {
      // Register the Process implicitly
      ProcessInstanceService procService = getProcessEngine().getService(ProcessInstanceService.class);
      if (procService.getInstance(getKey()) == null)
        procService.registerInstance(this);

      org.jbpm.graph.exe.ProcessInstance procInst = getDelegate();

      // Initialize the members
      org.jbpm.graph.exe.Token rootToken = new org.jbpm.graph.exe.Token(procInst);
      procInst.setStart(Clock.getCurrentTime());
      procInst.setRootToken(rootToken);

      // Create the root token
      Token token = TokenImpl.newInstance(getProcessEngine(), rootToken);

      // Save the root token
      JbpmContext jbpmContext = bpmContext.getAttachment(JbpmContext.class);
      jbpmContext.getSession().save(rootToken);

      // Initialize the context data
      if (contextData != null)
      {
        Attachments tokenAtt = token.getAttachments();
        for (Key key : contextData.getAttachmentKeys())
        {
          Object val = contextData.getAttachment(key.getClassPart(), key.getNamePart());
          tokenAtt.addAttachment(key.getClassPart(), key.getNamePart(), val);
        }
      }

      // Set process to active
      setProcessStatus(ProcessStatus.Active);

      // Fire the start event
      procInst.fireStartEvent(rootToken.getNode());

      // Signal the root token
      token.signal();

      return token;
    }
    catch (RuntimeException rte)
    {
      setProcessStatus(ProcessStatus.Aborted);
      throw rte;
    }
    finally
    {
      bpmContext.close();
    }
  }

  public Set<Token> getTokens()
  {
    return getAllTokens(getRootToken());
  }

  public void suspend()
  {
    ProcessStatus status = getProcessStatus();
    if (status != ProcessStatus.Active)
      throw new IllegalStateException("Cannot suspend a process in state: " + status);

    getDelegate().suspend();
  }

  public void resume()
  {
    ProcessStatus status = getProcessStatus();
    if (status != ProcessStatus.Suspended)
      throw new IllegalStateException("Cannot resume a process in state: " + status);

    getDelegate().resume();
  }

  public void cancel()
  {
    ProcessStatus status = getProcessStatus();
    if (status != ProcessStatus.Active && status != ProcessStatus.Suspended)
      throw new IllegalStateException("Cannot cancel a process in state: " + status);

    org.jbpm.graph.exe.ProcessInstance delegate = getDelegate();
    delegate.setEnd(new Date());
    delegate.end();
  }

  private Set<Token> getAllTokens(Token token)
  {
    Set<Token> tokens = new HashSet<Token>();
    if (token != null)
    {
      tokens.add(token);
      for (Token childToken : token.getChildTokens())
        tokens.addAll(getAllTokens(childToken));
    }
    return tokens;
  }

  public String toString()
  {
    return "ProcessInstance[" + getKey() + ",status=" + getProcessStatus() + "]";
  }
}
