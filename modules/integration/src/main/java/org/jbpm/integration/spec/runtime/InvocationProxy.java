/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import org.jboss.bpm.api.runtime.Context;
import org.jboss.bpm.api.service.ContextService;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.api.service.ProcessEngineSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorates management invocations with common system aspects.
 * 
 * @author Thomas.Diesler@jboss.com
 * @since 05-Dec-2008
 */
public class InvocationProxy implements InvocationProxySupport, InvocationHandler
{
  // provide logging
  private static final Logger log = LoggerFactory.getLogger(InvocationProxy.class);

  private static List<Method> proxyMethods = Arrays.asList(InvocationProxySupport.class.getMethods());

  private ProcessEngineSupport obj;
  private ProcessEngine engine;

  @SuppressWarnings("unchecked")
  public static <T> T newInstance(ProcessEngineSupport obj, Class<T> interf)
  {
    Class[] interfaces = new Class[] { interf, InvocationProxySupport.class };
    ClassLoader classLoader = obj.getClass().getClassLoader();
    return (T)Proxy.newProxyInstance(classLoader, interfaces, new InvocationProxy(obj));
  }

  @SuppressWarnings("unchecked")
  public static <T> T getUnderlying(Object implObj, Class<T> implClazz)
  {
    T underlyingImpl;
    if (implClazz.isInstance(implObj))
    {
      underlyingImpl = (T)implObj;
    }
    else if (implObj instanceof InvocationProxySupport)
    {
      InvocationProxySupport proxy = (InvocationProxySupport)implObj;
      underlyingImpl = (T)proxy.getUnderlying();
    }
    else
    {
      throw new IllegalArgumentException("Cannot obtain underlying implementation from: " + implObj);
    }
    return underlyingImpl;
  }

  private InvocationProxy(ProcessEngineSupport obj)
  {
    this.engine = obj.getProcessEngine();
    this.obj = obj;
  }

  public ProcessEngineSupport getUnderlying()
  {
    return obj;
  }

  public Object invoke(Object proxy, Method m, Object[] args) throws Throwable
  {
    if (proxyMethods.contains(m))
    {
      return m.invoke(this, args);
    }
    else
    {
      Throwable targetException = null;

      ContextService ctxService = engine.getService(ContextService.class);
      Context bpmContext = ctxService.getContext(true);
      try
      {
        return m.invoke(obj, args);
      }
      catch (InvocationTargetException ex)
      {
        targetException = ex.getTargetException();
        throw targetException;
      }
      finally
      {
        try
        {
          bpmContext.close();
        }
        catch (Throwable th)
        {
          if (targetException == null)
            throw th;

          log.error("Cannot close the execution context", th);
          throw targetException;
        }
      }
    }
  }
}