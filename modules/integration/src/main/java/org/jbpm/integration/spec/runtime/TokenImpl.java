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
package org.jbpm.integration.spec.runtime;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import org.jboss.bpm.api.Constants;
import org.jboss.bpm.api.model.Node;
import org.jboss.bpm.api.model.ObjectNameFactory;
import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.model.ProcessInstance.ProcessStatus;
import org.jboss.bpm.api.runtime.Attachments;
import org.jboss.bpm.api.runtime.Context;
import org.jboss.bpm.api.runtime.Token;
import org.jboss.bpm.api.service.ContextService;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.incubator.model.SequenceFlow;
import org.jbpm.JbpmContext;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.Transition;
import org.jbpm.integration.spec.model.AbstractElementImpl;
import org.jbpm.integration.spec.model.NodeImpl;
import org.jbpm.integration.spec.model.ProcessInstanceImpl;

/**
 * An integration wrapper
 * 
 * @author thomas.diesler@jboss.com
 * @since 15-Nov-2008
 */
public class TokenImpl extends AbstractElementImpl<org.jbpm.graph.exe.Token> implements Token
{
  private static final long serialVersionUID = 1L;

  private ObjectName keyCache;
  private SequenceFlow lastFlow;
  private Attachments att;

  public static Token newInstance(ProcessEngine engine, org.jbpm.graph.exe.Token tmpToken)
  {
    return InvocationProxy.newInstance(new TokenImpl(engine, tmpToken), Token.class);
  }

  private TokenImpl(ProcessEngine engine, org.jbpm.graph.exe.Token tmpToken)
  {
    super(engine, tmpToken, org.jbpm.graph.exe.Token.class);

    ContextInstance context = tmpToken.getProcessInstance().getContextInstance();
    this.att = new TokenAttachmentDelegate(this, context);

    if (tmpToken.getId() > 0)
      keyCache = getKey(tmpToken);
  }

  public ObjectName getKey()
  {
    ObjectName objKey = keyCache;
    if (objKey == null)
    {
      org.jbpm.graph.exe.Token delegate = getDelegate();
      objKey = getKey(delegate);
      if (delegate.getId() > 0)
        keyCache = objKey;
    }
    return objKey;
  }

  public static ObjectName getKey(org.jbpm.graph.exe.Token oldToken)
  {
    return ObjectNameFactory.create(Constants.ID_DOMAIN + ":id=" + oldToken.getId());
  }

  public ProcessInstance getProcess()
  {
    return ProcessInstanceImpl.newInstance(getProcessEngine(), getDelegate().getProcessInstance(), true);
  }

  public Attachments getAttachments()
  {
    return att;
  }

  public TokenStatus getTokenStatus()
  {
    TokenStatus status = TokenStatus.Suspended;
    if (getDelegate().hasEnded())
    {
      status = TokenStatus.Destroyed;
    }
    return status;
  }

  public Set<Token> getChildTokens()
  {
    Set<Token> childTokens = new HashSet<Token>();
    Map<String, org.jbpm.graph.exe.Token> oldChildMap = getDelegate().getChildren();
    if (oldChildMap != null)
    {
      for (org.jbpm.graph.exe.Token oldChildToken : oldChildMap.values())
      {
        if (oldChildToken.hasEnded() == false)
        {
          Token childToken = TokenImpl.newInstance(getProcessEngine(), oldChildToken);
          childTokens.add(childToken);
        }
      }
    }
    return childTokens;
  }

  public Node getNode()
  {
    String nodeName = getDelegate().getNode().getNameExt();
    return getProcess().getNode(nodeName);
  }

  public SequenceFlow getLastFlow()
  {
    return lastFlow;
  }

  public Token getParentToken()
  {
    Token token = null;
    if (getDelegate().getParent() != null)
    {
      token = TokenImpl.newInstance(getProcessEngine(), getDelegate().getParent());
    }
    return token;
  }

  public Token getRootToken()
  {
    org.jbpm.graph.exe.Token root = getDelegate();
    while (root.getParent() != null)
      root = root.getParent();

    Token token = TokenImpl.newInstance(getProcessEngine(), root);
    return token;
  }

  public void signal()
  {
    signalInternal(null);
  }

  public void signal(String name)
  {
    signalInternal(name);
  }

  private void signalInternal(String name)
  {
    ContextService ctxService = getProcessEngine().getService(ContextService.class);
    Context bpmContext = ctxService.getContext(true);
    try
    {
      NodeImpl<?> currNode = (NodeImpl<?>)getNode();
      if (currNode == null)
        throw new IllegalStateException("Cannot signal token with no current node: " + this);

      // Make sure we have no active children
      boolean hasActiveChild = false;
      for (Token child : getChildTokens())
      {
        if (child.getTokenStatus() != TokenStatus.Destroyed)
        {
          hasActiveChild = true;
          break;
        }
      }
      if (hasActiveChild)
        throw new IllegalStateException("Cannot signal token with active child tokens: " + this);

      // Signal the underlying jBPM node
      if (name != null)
      {
        Transition trans = currNode.getDelegate().getLeavingTransition(name);
        lastFlow = currNode.getOutFlowByTransition(trans);
        getDelegate().signal(name);
      }
      else
      {
        Transition trans = currNode.getDelegate().getDefaultLeavingTransition();
        lastFlow = currNode.getOutFlowByTransition(trans);
        getDelegate().signal();
      }

      // Save the token
      JbpmContext jbpmContext = bpmContext.getAttachment(JbpmContext.class);
      jbpmContext.save(getDelegate());
    }
    catch (RuntimeException rte)
    {
      ProcessInstanceImpl procImpl = InvocationProxy.getUnderlying(getProcess(), ProcessInstanceImpl.class);
      procImpl.setProcessStatus(ProcessStatus.Aborted);
      throw rte;
    }
    finally
    {
      bpmContext.close();
    }
  }

  public String toString()
  {
    Node currNode = getNode();
    int children = getChildTokens().size();
    boolean root = getParentToken() == null;
    TokenStatus status = getTokenStatus();
    return "[key=" + getKey() + ",root=" + root + ",status=" + status + ",node=" + currNode + ",flow=" + lastFlow + ",child=" + children + "]";
  }
}
