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
import org.jboss.bpm.incubator.model.StartEvent;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.node.StartState;

/**
 * An integration wrapper
 * 
 * @author thomas.diesler@jboss.com
 * @since 15-Nov-2008
 */
public class StartEventImpl extends EventImpl<StartState> implements StartEvent
{
  private static final long serialVersionUID = 1L;

  public StartEventImpl(ProcessEngine engine, ProcessDefinition procDef, Node oldStart)
  {
    super(engine, procDef, StartState.class, oldStart);
  }

  public EventDetailType getTriggerType()
  {
    return EventDetailType.None;
  }

  public EventType getEventType()
  {
    return EventType.Start;
  }

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
    return getEventType() + "Event[" + getName() + "," + getTriggerType() + "]";
  }
}
