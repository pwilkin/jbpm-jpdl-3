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

// $Id: ExclusiveGatewayImpl.java 3479 2008-12-20 12:55:32Z thomas.diesler@jboss.com $

import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.runtime.Token;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.incubator.model.ExclusiveGateway;
import org.jboss.bpm.incubator.model.Expression;
import org.jboss.bpm.incubator.model.SequenceFlow;
import org.jboss.bpm.incubator.model.SequenceFlow.ConditionType;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.Decision;
import org.jbpm.graph.node.DecisionHandler;
import org.jbpm.integration.spec.runtime.ExpressionEvaluator;
import org.jbpm.integration.spec.runtime.TokenImpl;

/**
 * An integration wrapper
 * 
 * @author thomas.diesler@jboss.com
 * @since 15-Nov-2008
 */
public class ExclusiveGatewayImpl extends GatewayImpl<Decision> implements ExclusiveGateway
{
  private static final long serialVersionUID = 1L;

  public ExclusiveGatewayImpl(ProcessEngine engine, ProcessDefinition procDef, Node oldDecision)
  {
    super(engine, procDef, Decision.class, oldDecision);
    //ExclusiveGatewayDecisionHandler decisionHandler = new ExclusiveGatewayDecisionHandler();
    //getDelegate().setDecisionDelegation(new Delegation(decisionHandler));
  }

  // @Override
  public GatewayType getGatewayType()
  {
    return GatewayType.Exclusive;
  }

  // @Override
  public ExclusiveType getExclusiveType()
  {
    return ExclusiveType.Data;
  }

  class ExclusiveGatewayDecisionHandler implements DecisionHandler
  {
    private static final long serialVersionUID = 1L;

    // @Override
    public String decide(ExecutionContext execContext) throws Exception
    {
      ProcessEngine engine = getProcessDefinition().getProcessEngine();
      Token token = TokenImpl.newInstance(engine, execContext.getToken());

      SequenceFlow selectedGate = null;
      for (SequenceFlow auxGate : getGates())
      {
        SequenceFlow seqFlow = auxGate;
        if (seqFlow.getConditionType() == ConditionType.Expression)
        {
          Expression expr = seqFlow.getConditionExpression();
          ExpressionEvaluator exprEvaluator = new ExpressionEvaluator(expr);
          if ((Boolean)exprEvaluator.evaluateExpression(token))
          {
            selectedGate = auxGate;
            break;
          }
        }
      }

      // Use to the default gate if there is one
      if (selectedGate == null)
      {
        for (SequenceFlow auxGate : getGates())
        {
          SequenceFlow seqFlow = auxGate;
          if (seqFlow.getConditionType() == ConditionType.Default)
          {
            selectedGate = auxGate;
            break;
          }
        }
      }

      // Fallback to the single outgoing gate that is not conditional
      if (selectedGate == null && getGates().size() == 1)
      {
        SequenceFlow auxGate = getGates().get(0);
        SequenceFlow seqFlow = auxGate;
        if (seqFlow.getConditionType() == ConditionType.None)
        {
          selectedGate = auxGate;
        }
      }

      if (selectedGate == null)
        throw new IllegalStateException("Cannot select applicable gate in: " + this);

      return selectedGate.getTargetRef();
    }
  }
}
