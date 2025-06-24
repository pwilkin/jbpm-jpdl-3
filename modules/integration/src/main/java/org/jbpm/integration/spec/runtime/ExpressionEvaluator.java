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

//$Id: ExpressionEvaluator.java 3466 2008-12-19 22:53:18Z thomas.diesler@jboss.com $

import java.util.HashMap;
import java.util.Map;

import org.jboss.bpm.api.runtime.Attachments;
import org.jboss.bpm.api.runtime.Token;
import org.jboss.bpm.api.runtime.Attachments.Key;
import org.jboss.bpm.incubator.model.Expression;
import org.jboss.bpm.incubator.model.Expression.ExpressionLanguage;
import org.mvel.MVEL;

/**
 * Evaluates an expression for a given token
 * 
 * @author thomas.diesler@jboss.com
 * @since 08-Jul-2008
 */
public class ExpressionEvaluator
{
  private Expression expr;

  public ExpressionEvaluator(Expression expr)
  {
    this.expr = expr;
  }

  /**
   * Evaluate an expression for a given token. <p/> Note that <code>propName.replace(".", "_")</code> applies to
   * property names for MVEL expressions, because the dot notation has special meaning in MVEL.
   */
  public Object evaluateExpression(Token token)
  {
    ExpressionLanguage exprLang = expr.getExpressionLanguage();
    if (exprLang == ExpressionLanguage.MVEL)
    {
      String mvel = expr.getExpressionBody();
      Attachments atts = token.getAttachments();
      Map<String, Object> vars = new HashMap<String, Object>();
      for (Key key : atts.getAttachmentKeys())
      {
        String name = key.getNamePart();
        Object value = atts.getAttachment(name);
        vars.put(name.replace(".", "_"), value);
      }
      return MVEL.eval(mvel, vars);
    }
    else
    {
      throw new IllegalStateException("Unsupported expression language: " + exprLang);
    }
  }
}