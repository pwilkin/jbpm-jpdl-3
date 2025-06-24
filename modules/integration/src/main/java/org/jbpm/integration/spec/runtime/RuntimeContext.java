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

//$Id: RuntimeContext.java 3154 2008-11-28 16:41:56Z thomas.diesler@jboss.com $

import java.util.ArrayList;
import java.util.List;

import org.jboss.bpm.api.runtime.Token;

/**
 * A runtime context that passes through a chain of interceptors.
 * 
 * @author thomas.diesler@jboss.com
 * @since 07-Oct-2008
 */
public class RuntimeContext
{
  private List<NodeInterceptor> interceptors = new ArrayList<NodeInterceptor>();
  private int itorIndex;

  private Token token;

  public Token getToken()
  {
    return token;
  }

  public void setToken(Token token)
  {
    this.token = token;
    this.itorIndex = 0;
  }

  void addInterceptor(NodeInterceptor itor)
  {
    interceptors.add(itor);
  }

  protected void next()
  {
    if (itorIndex < interceptors.size())
    {
      NodeInterceptor itor = interceptors.get(itorIndex++);
      itor.execute(this);
    }
  }
}