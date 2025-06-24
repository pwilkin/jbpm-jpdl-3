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
import org.jboss.bpm.incubator.model.Expression;
import org.jboss.bpm.incubator.model.SequenceFlow.ConditionType;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.def.Node.NodeType;
import org.jbpm.integration.spec.runtime.InvocationProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An integration wrapper
 * 
 * @author thomas.diesler@jboss.com
 * @since 15-Nov-2008
 */
public class ProcessDefinitionImpl extends AbstractElementImpl<org.jbpm.graph.def.ProcessDefinition> implements ProcessDefinition
{
  private static final long serialVersionUID = 1L;

  // provide logging
  private static final Logger log = LoggerFactory.getLogger(ProcessDefinitionImpl.class);

  private ObjectName keyCache;
  private List<Node> nodes;

  public static ProcessDefinition newInstance(ProcessEngine engine, org.jbpm.graph.def.ProcessDefinition tmpProcDef, boolean proxy)
  {
    ProcessDefinition procDef = new ProcessDefinitionImpl(engine, tmpProcDef);
    if (proxy == true)
    {
      procDef = InvocationProxy.newInstance((ProcessDefinitionImpl)procDef, ProcessDefinition.class);
    }
    return procDef;
  }

  private ProcessDefinitionImpl(ProcessEngine engine, org.jbpm.graph.def.ProcessDefinition tmpProcDef)
  {
    super(engine, tmpProcDef, org.jbpm.graph.def.ProcessDefinition.class);

    if (tmpProcDef.getName() == null)
      throw new InvalidProcessException("ProcessDefinition name cannot be null");

    if (tmpProcDef.getId() > 0)
      keyCache = getKey(tmpProcDef);
  }

  public ObjectName getKey()
  {
    ObjectName objKey = keyCache;
    if (objKey == null)
    {
      org.jbpm.graph.def.ProcessDefinition delegate = getDelegate();
      objKey = getKey(delegate);
      if (delegate.getId() > 0)
        keyCache = objKey;
    }
    return objKey;
  }

  public static ObjectName getKey(org.jbpm.graph.def.ProcessDefinition oldProcDef)
  {
    long id = oldProcDef.getId();
    int version = oldProcDef.getVersion();

    String keyStr = Constants.ID_DOMAIN + ":procdef=" + oldProcDef.getName() + ",id=" + id;
    if (version > 0)
      keyStr += ",version=" + version;

    return ObjectNameFactory.create(keyStr);
  }

  public String getName()
  {
    return getDelegate().getName();
  }

  public String getVersion()
  {
    int version = getDelegate().getVersion();
    return version > 0 ? String.valueOf(version) : null;
  }

  public ProcessInstance newInstance()
  {
    org.jbpm.graph.exe.ProcessInstance oldProcInst = new org.jbpm.graph.exe.ProcessInstance();
    oldProcInst.setProcessDefinition(getDelegate());
    oldProcInst.addInitialModuleDefinitions(getDelegate());
    return ProcessInstanceImpl.newInstance(getProcessEngine(), oldProcInst, true);
  }

  public void addNode(NodeImpl<?> nodeImpl)
  {
    org.jbpm.graph.def.Node oldNode = nodeImpl.getDelegate();
    getDelegate().addNode(oldNode);
  }

  public Node getNode(String name)
  {
    if (name == null)
      throw new IllegalArgumentException("Cannot find node with name: null");

    Node retNode = null;
    for (Node auxNode : getNodes())
    {
      if (name.equals(auxNode.getName()))
      {
        retNode = auxNode;
        break;
      }
    }
    return retNode;
  }

  public List<Node> getNodes()
  {
    if (nodes == null)
    {
      nodes = new ArrayList<Node>();
      ProcessEngine engine = getProcessEngine();
      for (org.jbpm.graph.def.Node oldNode : getDelegate().getNodes())
      {
        NodeImpl<?> nodeImpl;
        NodeType nodeType = oldNode.getNodeType();
        if (nodeType == NodeType.StartState)
        {
          nodeImpl = new StartEventImpl(engine, this, oldNode);
        }
        else if (nodeType == NodeType.State)
        {
          nodeImpl = new WaitStateImpl(engine, this, oldNode);
        }
        else if (nodeType == NodeType.Fork)
        {
          nodeImpl = new ParallelGatewayForkImpl(engine, this, oldNode);
        }
        else if (nodeType == NodeType.Join)
        {
          nodeImpl = new ParallelGatewayJoinImpl(engine, this, oldNode);
        }
        else if (nodeType == NodeType.Decision)
        {
          nodeImpl = new ExclusiveGatewayImpl(engine, this, oldNode);
        }
        else if (nodeType == NodeType.Task)
        {
          nodeImpl = new UserTaskImpl(engine, this, oldNode);
        }
        else if (nodeType == NodeType.EndState)
        {
          nodeImpl = new EndEventImpl(engine, this, oldNode);
        }
        else if (nodeType == NodeType.Node)
        {
          nodeImpl = new NoneTaskImpl(engine, this, oldNode);
        }
        else
        {
          throw new NotImplementedException("Unsupported node type: " + nodeType);
        }

        nodes.add(nodeImpl);
      }
      initializeSequenceFlows(nodes);
    }
    return Collections.unmodifiableList(nodes);
  }

  @SuppressWarnings("unchecked")
  public <T extends Node> T getNode(Class<T> clazz, String name)
  {
    for (Node node : getNodes(clazz))
    {
      if (node.getName().equals(name))
        return (T)node;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <T extends Node> List<T> getNodes(Class<T> clazz)
  {
    List<T> retNodes = new ArrayList<T>();
    for (Node node : getNodes())
    {
      if (clazz.isAssignableFrom(node.getClass()))
        retNodes.add((T)node);
    }
    return retNodes;
  }

  private void initializeSequenceFlows(List<Node> nodes)
  {
    for (Node node : nodes)
    {
      NodeImpl<?> nodeImpl = (NodeImpl<?>)node;
      org.jbpm.graph.def.Node oldNode = nodeImpl.getDelegate();
      if (oldNode.getLeavingTransitions() != null)
      {
        for (Transition trans : oldNode.getLeavingTransitions())
        {
          SequenceFlowImpl seqFlow = new SequenceFlowImpl(trans);

          String condition = trans.getCondition();
          if (condition != null)
          {
            Expression expr = ExpressionImpl.valueOf(condition);
            if (condition.equals("[" + ConditionType.Default + "]"))
            {
              seqFlow = new SequenceFlowImpl(trans, true);
            }
            else if (expr != null)
            {
              seqFlow = new SequenceFlowImpl(trans, expr.getExpressionLanguage(), expr.getExpressionBody());
            }
            else
            {
              log.warn("Cannot translate flow condition: " + condition);
            }
          }
          nodeImpl.addSequenceFlow(seqFlow);
        }
      }
    }
  }

  public String toString()
  {
    return "ProcessDefinition[" + getKey() + "]";
  }
}
