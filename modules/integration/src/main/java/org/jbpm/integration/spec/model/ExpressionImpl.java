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

//$Id: ExpressionImpl.java 3466 2008-12-19 22:53:18Z thomas.diesler@jboss.com $

import org.jboss.bpm.incubator.model.Expression;

/**
 * An Expression, which is used in the definition of attributes for @{link StartEvent},
 * 
 * @{link IntermediateEvent}, @{link Activity}, @{link ComplexGateway}, and @{link SequenceFlow}
 * 
 * @author thomas.diesler@jboss.com
 * @since 17-Nov-2008
 */
public class ExpressionImpl implements Expression
{
  // provide serial version UID
  private static final long serialVersionUID = 1L;

  private String body;
  private ExpressionLanguage lang;

  public ExpressionImpl(String body)
  {
    this.body = body;
    this.lang = ExpressionLanguage.MVEL;
  }

  public ExpressionImpl(ExpressionLanguage lang, String body)
  {
    this.body = body;
    this.lang = lang;
  }

  // @Override
  public ExpressionLanguage getExpressionLanguage()
  {
    return lang;
  }

  // @Override
  public String getExpressionBody()
  {
    return body;
  }

  public static Expression valueOf(String exprStr)
  {
    Expression expr = null;
    if (exprStr != null && exprStr.startsWith("[") && exprStr.endsWith("]") && exprStr.indexOf(":") > 0)
    {
      int index = exprStr.indexOf(":");
      String langPart = exprStr.substring(1, index);
      String bodyPart = exprStr.substring(index + 1, exprStr.length() - 1);
      expr = new ExpressionImpl(ExpressionLanguage.valueOf(langPart), bodyPart);
    }
    return expr;
  }

  public String toString()
  {
    return "[" + lang + ":" + body + "]";
  }
}
