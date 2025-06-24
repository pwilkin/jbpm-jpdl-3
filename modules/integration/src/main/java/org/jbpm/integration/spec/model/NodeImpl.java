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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.management.ObjectName;

import org.jboss.bpm.api.Constants;
import org.jboss.bpm.api.InvalidProcessException;
import org.jboss.bpm.api.NotImplementedException;
import org.jboss.bpm.api.model.Node;
import org.jboss.bpm.api.model.ObjectNameFactory;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.incubator.model.SequenceFlow;
import org.jboss.bpm.incubator.model.SequenceFlow.ConditionType;
import org.jbpm.graph.def.Transition;
import org.jbpm.integration.spec.runtime.InvocationProxy;

/**
 * An integration wrapper
 * 
 * @author thomas.diesler@jboss.com
 * @since 15-Nov-2008
 */
public abstract class NodeImpl<T extends org.jbpm.graph.def.Node> extends AbstractElementImpl<T> implements Node
{
  private static final long serialVersionUID = 1L;

  private ProcessDefinition procDef;

  private List<SequenceFlow> outFlows = new ArrayList<SequenceFlow>();
  private List<SequenceFlow> inFlows = new ArrayList<SequenceFlow>();

  public NodeImpl(ProcessEngine engine, ProcessDefinition procDef, Class<T> clazz, org.jbpm.graph.def.Node oldNode)
  {
    super(engine, oldNode, clazz);
    this.procDef = procDef;
  }

  public ObjectName getKey()
  {
    long id = getDelegate().getId();
    return ObjectNameFactory.create(Constants.ID_DOMAIN + ":node=" + getName() + ",id=" + id);
  }

  public String getName()
  {
    return getDelegate().getNameExt();
  }

  public ProcessDefinition getProcessDefinition()
  {
    return procDef;
  }

  public ProcessInstance getProcessInstance()
  {
    throw new NotImplementedException();
  }

  @SuppressWarnings("unchecked")
  public void addSequenceFlow(SequenceFlowImpl flow)
  {
    String targetRef = flow.getTargetRef();
    NodeImpl<T> targetNode = (NodeImpl<T>)procDef.getNode(targetRef);
    if (targetNode == null)
      throw new InvalidProcessException("Cannot obtain target node: " + targetRef);

    org.jbpm.graph.def.Node delegate = getDelegate();

    Transition trans = flow.getOldTransition();
    if (delegate.hasLeavingTransition(trans.getName()) == false)
    {
      delegate.addLeavingTransition(trans);
      targetNode.getDelegate().addArrivingTransition(trans);
    }

    if (flow.getConditionType() == ConditionType.Expression)
    {
      trans.setCondition(flow.getConditionExpression().toString());
    }
    else if (flow.getConditionType() == ConditionType.Default)
    {
      trans.setCondition("[" + ConditionType.Default + "]");
    }

    targetNode.inFlows.add(flow);
    outFlows.add(flow);
  }

  public SequenceFlow getOutFlowByTransition(Transition trans)
  {
    SequenceFlow outFlow = null;
    for (SequenceFlow auxFlow : outFlows)
    {
      SequenceFlowImpl flowImpl = InvocationProxy.getUnderlying(auxFlow, SequenceFlowImpl.class);
      if (flowImpl.getOldTransition() == trans)
      {
        outFlow = auxFlow;
        break;
      }
    }
    return outFlow;
  }

  public List<SequenceFlow> getInFlows()
  {
    return Collections.unmodifiableList(inFlows);
  }

  public List<SequenceFlow> getOutFlows()
  {
    return Collections.unmodifiableList(outFlows);
  }
}
