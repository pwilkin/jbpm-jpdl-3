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

import java.util.List;

import org.jboss.bpm.api.NotImplementedException;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.incubator.model.SequenceFlow;
import org.jboss.bpm.incubator.model.Task;
import org.jbpm.graph.def.Node;

/**
 * An integration wrapper
 * 
 * @author thomas.diesler@jboss.com
 * @since 15-Nov-2008
 */
public abstract class TaskImpl<T extends Node> extends NodeImpl<T> implements Task
{
  private static final long serialVersionUID = 1L;

  public TaskImpl(ProcessEngine engine, ProcessDefinition procDef, Class<T> clazz, Node oldNode)
  {
    super(engine, procDef, clazz, oldNode);
  }

  // @Override
  public SequenceFlow getInFlow()
  {
    List<SequenceFlow> inFlows = getInFlows();
    if (inFlows.size() == 0)
      throw new IllegalStateException("No incomming flow on: " + this);
    if (inFlows.size() > 1)
      throw new NotImplementedException("Multiple incomming flows on: " + this);

    return inFlows.get(0);
  }

  // @Override
  public SequenceFlow getOutFlow()
  {
    List<SequenceFlow> outFlows = getOutFlows();
    if (outFlows.size() == 0)
      throw new IllegalStateException("No outgoing flow on: " + this);
    if (outFlows.size() > 1)
      throw new NotImplementedException("Multiple outgoing flows on: " + this);

    return outFlows.get(0);
  }

  public String toString()
  {
    return getTaskType() + "Task[" + getName() + "]";
  }
}
