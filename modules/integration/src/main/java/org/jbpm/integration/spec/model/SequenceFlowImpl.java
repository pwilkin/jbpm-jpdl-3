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

import org.jboss.bpm.incubator.model.Expression;
import org.jboss.bpm.incubator.model.SequenceFlow;
import org.jboss.bpm.incubator.model.Expression.ExpressionLanguage;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.Transition;

/**
 * An integration wrapper
 * 
 * @author thomas.diesler@jboss.com
 * @since 15-Nov-2008
 */
public class SequenceFlowImpl implements SequenceFlow
{
  private static final long serialVersionUID = 1L;

  private Transition oldTrans;
  private ConditionType condType = ConditionType.None;
  private Expression expr;

  public SequenceFlowImpl(Transition oldTrans)
  {
    this.oldTrans = oldTrans;
  }

  public SequenceFlowImpl(Transition oldTrans, ExpressionLanguage exprLang, String exprBody)
  {
    this.oldTrans = oldTrans;

    if (exprLang != null && exprBody != null)
    {
      this.condType = ConditionType.Expression;
      this.expr = new ExpressionImpl(exprLang, exprBody);
    }
  }

  public SequenceFlowImpl(Transition oldTrans, boolean isDefault)
  {
    this.oldTrans = oldTrans;
    this.condType = ConditionType.Default;
  }

  public Transition getOldTransition()
  {
    return oldTrans;
  }

  // @Override
  public String getName()
  {
    return oldTrans.getName();
  }

  // @Override
  public Expression getConditionExpression()
  {
    return expr;
  }

  // @Override
  public ConditionType getConditionType()
  {
    return condType;
  }

  // @Override
  public String getSourceRef()
  {
    Node from = oldTrans.getFrom();
    return from != null ? from.getName() : null;
  }

  // @Override
  public String getTargetRef()
  {
    Node to = oldTrans.getTo();
    return to != null ? to.getName() : null;
  }

  public String toString()
  {
    return "[" + getSourceRef() + "->" + getTargetRef() + ",cond=" + condType + "]";
  }
}
