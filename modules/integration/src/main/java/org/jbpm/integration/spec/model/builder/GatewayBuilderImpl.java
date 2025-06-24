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

//$Id: GatewayBuilderImpl.java 3466 2008-12-19 22:53:18Z thomas.diesler@jboss.com $

import org.jboss.bpm.api.NotImplementedException;
import org.jboss.bpm.incubator.model.Expression.ExpressionLanguage;
import org.jboss.bpm.incubator.model.builder.GatewayBuilder;

/**
 * The GatewayBuilder can be used to build a Gateway dynamically.
 * 
 * @author thomas.diesler@jboss.com
 * @since 15-Nov-2008
 */
public class GatewayBuilderImpl extends ProcessBuilderImpl implements GatewayBuilder
{
  protected GatewayBuilderImpl(ProcessBuilderImpl procBuilder)
  {
    super(procBuilder);
  }

  public GatewayBuilder addGate(String targetName)
  {
    addSequenceFlow(targetName);
    return this;
  }

  public GatewayBuilder addConditionalGate(String targetName, ExpressionLanguage exprLang, String exprBody)
  {
    addSequenceFlow(targetName, exprLang, exprBody);
    return this;
  }

  public GatewayBuilder addDefaultGate(String targetName)
  {
    throw new NotImplementedException();
  }
}