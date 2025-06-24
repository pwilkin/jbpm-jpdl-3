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
package org.jbpm.integration.spec.model.builder;

//$Id: ProcessBuilderImpl.java 3466 2008-12-19 22:53:18Z thomas.diesler@jboss.com $

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jboss.bpm.api.InvalidProcessException;
import org.jboss.bpm.api.NotImplementedException;
import org.jboss.bpm.api.model.Node;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.incubator.model.EndEvent;
import org.jboss.bpm.incubator.model.StartEvent;
import org.jboss.bpm.incubator.model.Expression.ExpressionLanguage;
import org.jboss.bpm.incubator.model.Gateway.GatewayType;
import org.jboss.bpm.incubator.model.Task.TaskType;
import org.jboss.bpm.incubator.model.builder.GatewayBuilder;
import org.jboss.bpm.incubator.model.builder.ProcessBuilder;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.def.Node.NodeType;
import org.jbpm.graph.node.Decision;
import org.jbpm.graph.node.EndState;
import org.jbpm.graph.node.StartState;
import org.jbpm.graph.node.State;
import org.jbpm.integration.spec.model.EndEventImpl;
import org.jbpm.integration.spec.model.ExclusiveGatewayImpl;
import org.jbpm.integration.spec.model.NodeImpl;
import org.jbpm.integration.spec.model.NoneTaskImpl;
import org.jbpm.integration.spec.model.ProcessDefinitionImpl;
import org.jbpm.integration.spec.model.SequenceFlowImpl;
import org.jbpm.integration.spec.model.StartEventImpl;
import org.jbpm.integration.spec.model.WaitStateImpl;

/**
 * The ProcessBuilder can be used to dynamically build a {@link ProcessInstance}.
 * 
 * @author thomas.diesler@jboss.com
 * @since 15-Nov-2008
 */
public class ProcessBuilderImpl implements ProcessBuilder
{
  protected ProcessEngine engine;
  protected ProcessDefinitionImpl procDefImpl;
  protected NodeImpl<?> nodeImpl;

  private List<FlowSpec> flows = new ArrayList<FlowSpec>();

  public ProcessBuilderImpl(ProcessEngine engine)
  {
    this.engine = engine;
  }

  protected ProcessBuilderImpl(ProcessBuilderImpl procBuilder)
  {
    this.engine = procBuilder.engine;
    this.procDefImpl = procBuilder.procDefImpl;
    this.nodeImpl = procBuilder.nodeImpl;
    this.flows = procBuilder.flows;
  }

  // @Override
  public ProcessBuilder addProcess(String name)
  {
    org.jbpm.graph.def.ProcessDefinition oldProcDef = org.jbpm.graph.def.ProcessDefinition.createNewProcessDefinition();
    oldProcDef.setName(name);

    procDefImpl = (ProcessDefinitionImpl)ProcessDefinitionImpl.newInstance(engine, oldProcDef, false);
    return this;
  }

  // @Override
  public ProcessDefinition getProcessDefinition()
  {
    initProcessDefinition();
    return procDefImpl;
  }

  // @Override
  public ProcessBuilder addStartEvent(String name)
  {
    if (name == null)
      throw new InvalidProcessException("StartEvent name cannot be null");

    nodeImpl = new StartEventImpl(engine, procDefImpl, new StartState(name));
    procDefImpl.addNode(nodeImpl);
    return this;
  }

  // @Override
  public GatewayBuilder addGateway(String name, GatewayType type)
  {
    if (type == GatewayType.Exclusive)
    {
      nodeImpl = new ExclusiveGatewayImpl(engine, procDefImpl, new Decision(name));
      procDefImpl.addNode(nodeImpl);
    }
    else
    {
      throw new NotImplementedException("Unsupported gateway type: " + type);
    }
    return new GatewayBuilderImpl(this);
  }

  // @Override
  public ProcessBuilder addTask(String name)
  {
    return addTask(name, TaskType.None);
  }

  // @Override
  public ProcessBuilder addTask(String name, TaskType type)
  {
    if (type == TaskType.None)
    {
      nodeImpl = new NoneTaskImpl(engine, procDefImpl, new org.jbpm.graph.def.Node(name));
      procDefImpl.addNode(nodeImpl);
    }
    else if (type == TaskType.Wait)
    {
      nodeImpl = new WaitStateImpl(engine, procDefImpl, new State(name));
      procDefImpl.addNode(nodeImpl);
    }
    else
    {
      throw new NotImplementedException("Unsupported task type: " + type);
    }
    return this;
  }

  // @Override
  public ProcessBuilder addEndEvent(String name)
  {
    nodeImpl = new EndEventImpl(engine, procDefImpl, new EndState(name));
    procDefImpl.addNode(nodeImpl);
    return this;
  }

  // @Override
  public ProcessBuilder addSequenceFlow(String targetName)
  {
    return addSequenceFlow(targetName, null, null);
  }

  public ProcessBuilder addSequenceFlow(String targetName, ExpressionLanguage exprLang, String exprBody)
  {
    flows.add(new FlowSpec(nodeImpl.getName(), targetName, exprLang, exprBody));
    return this;
  }

  class FlowSpec
  {
    String source;
    String target;
    ExpressionLanguage exprLang;
    String exprBody;

    public FlowSpec(String source, String target, ExpressionLanguage exprLang, String exprBody)
    {
      this.source = source;
      this.target = target;
      this.exprLang = exprLang;
      this.exprBody = exprBody;
    }
  }

  private void initProcessDefinition()
  {
    // Initialize the flows
    for (FlowSpec flow : flows)
    {
      NodeImpl<?> srcNode = (NodeImpl<?>)procDefImpl.getNode(flow.source);
      if (srcNode == null)
        throw new InvalidProcessException("Cannot obtain source node: " + flow.source);

      NodeImpl<?> targetNode = (NodeImpl<?>)procDefImpl.getNode(flow.target);
      if (targetNode == null)
        throw new InvalidProcessException("Cannot obtain target node: " + flow.target);

      Transition trans = new Transition(flow.target);
      trans.setFrom(srcNode.getDelegate());
      trans.setTo(targetNode.getDelegate());

      SequenceFlowImpl seqFlow = new SequenceFlowImpl(trans, flow.exprLang, flow.exprBody);
      srcNode.addSequenceFlow(seqFlow);
    }

    // Verify that there is a start event
    List<StartEvent> startEvents = procDefImpl.getNodes(StartEvent.class);
    if (startEvents.size() == 0)
      throw new InvalidProcessException("Cannot obtain a start event");

    // Verify that there is an end event
    List<EndEvent> endEvents = procDefImpl.getNodes(EndEvent.class);
    if (endEvents.size() == 0)
      throw new InvalidProcessException("Cannot obtain an end event");

    // Detect unreachable nodes
    for (Node node : procDefImpl.getNodes())
    {
      NodeImpl<?> nodeImpl = (NodeImpl<?>)node;
      org.jbpm.graph.def.Node delegate = nodeImpl.getDelegate();
      Set<Transition> arriving = delegate.getArrivingTransitions();

      NodeType nodeType = delegate.getNodeType();
      if (nodeType != NodeType.StartState && arriving == null)
        throw new InvalidProcessException("Unreachable node: " + node);
    }

    // Detect dead end nodes
    for (Node node : procDefImpl.getNodes())
    {
      NodeImpl<?> nodeImpl = (NodeImpl<?>)node;
      org.jbpm.graph.def.Node delegate = nodeImpl.getDelegate();
      List<Transition> leaving = delegate.getLeavingTransitions();

      NodeType nodeType = delegate.getNodeType();
      if (nodeType != NodeType.EndState && leaving == null)
        throw new InvalidProcessException("Dead end node: " + node);
    }
  }
}