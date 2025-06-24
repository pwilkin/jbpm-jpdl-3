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

//$Id: ProcessEngineImpl.java 3238 2008-12-05 17:58:45Z thomas.diesler@jboss.com $

import java.util.Set;

import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.api.service.Service;
import org.jbpm.JbpmConfiguration;

/**
 * A process engine with public access
 * 
 * @author thomas.diesler@jboss.com
 * @since 18-Jun-2008
 */
public class ProcessEngineImpl extends ProcessEngine
{
  private JbpmConfiguration jbpmConfig;

  public void setServices(Set<Service> services)
  {
    this.services = services;

    // Set this engine on every mutable service
    for (Service service : services)
    {
      if (service instanceof MutableService)
      {
        MutableService mutService = (MutableService)service;
        mutService.setProcessEngine(this);
      }
    }
  }

  public JbpmConfiguration getJbpmConfiguration()
  {
    if (jbpmConfig == null)
    {
      jbpmConfig = JbpmConfiguration.getInstance();
    }
    return jbpmConfig;
  }
}
