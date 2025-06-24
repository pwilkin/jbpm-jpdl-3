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
import org.jboss.bpm.incubator.model.EndEvent;
import org.jboss.bpm.incubator.model.SequenceFlow;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.node.EndState;

/**
 * An integration wrapper
 * 
 * @author thomas.diesler@jboss.com
 * @since 15-Nov-2008
 */
public class EndEventImpl extends EventImpl<EndState> implements EndEvent
{
  private static final long serialVersionUID = 1L;

  public EndEventImpl(ProcessEngine engine, ProcessDefinition procDef, Node oldEnd)
  {
    super(engine, procDef, EndState.class, oldEnd);
  }

  // @Override
  public EventDetailType getResultType()
  {
    return EventDetailType.None;
  }

  // @Override
  public EventType getEventType()
  {
    return EventType.End;
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

  public String toString()
  {
    return getEventType() + "Event[" + getName() + "," + getResultType() + "]";
  }
}
