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
package org.jbpm.integration.spec.service;

// $Id: ContextServiceImpl.java 3485 2008-12-20 14:33:15Z thomas.diesler@jboss.com $

import java.util.Map;

import org.jboss.bpm.api.runtime.BasicAttachments;
import org.jboss.bpm.api.runtime.Context;
import org.jboss.bpm.api.service.ContextService;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.api.service.internal.AbstractService;
import org.jbpm.JbpmContext;
import org.jbpm.signal.EventService;
import org.jbpm.svc.ServiceFactory;
import org.jbpm.svc.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ContextService is the entry point to create or get an Context.
 * 
 * @author thomas.diesler@jboss.com
 * @since 02-Dec-2008
 */
public class ContextServiceImpl extends AbstractService implements ContextService, MutableService
{
  // Provide logging
  final static Logger log = LoggerFactory.getLogger(ContextServiceImpl.class);

  private static ThreadLocal<ContextImpl> contextAssociation = new ThreadLocal<ContextImpl>();

  @Override
  public void setProcessEngine(ProcessEngine engine)
  {
    super.setProcessEngine(engine);
  }

  public Context createContext()
  {
    ProcessEngineImpl engineImpl = (ProcessEngineImpl)getProcessEngine();
    JbpmContext jbpmContext = engineImpl.getJbpmConfiguration().createJbpmContext();
    ContextImpl currContext = new ContextImpl(jbpmContext);
    contextAssociation.set(currContext);
    return currContext;
  }

  public Context getContext(boolean create)
  {
    ContextImpl currContext = contextAssociation.get();

    if (currContext == null && create == true)
      currContext = (ContextImpl)createContext();

    if (currContext != null)
      currContext.clientCount++;

    return currContext;
  }

  class ContextImpl extends BasicAttachments implements Context
  {
    private JbpmContext jbpmContext;
    private int clientCount;

    ContextImpl(JbpmContext jbpmContext)
    {
      this.jbpmContext = jbpmContext;
      addAttachment(JbpmContext.class, jbpmContext);
      
      // Add the event service
      Services services = jbpmContext.getServices();
      ServiceFactory serviceFactory = services.getServiceFactory(EventService.SERVICE_NAME);
      if (serviceFactory == null)
      {
        Map<String, ServiceFactory> factories = services.getServiceFactories();
        factories.put(EventService.SERVICE_NAME, new EventServiceFactory(getProcessEngine()));
      }
    }

    public void close()
    {
      clientCount--;

      if (clientCount < 0)
        throw new IllegalStateException("PersistenceContext already closed");

      if (clientCount == 0)
      {
        contextAssociation.set(null);
        jbpmContext.close();
      }
    }
  }
}