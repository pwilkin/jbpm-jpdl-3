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

// $Id: DeploymentServiceImpl.java 3485 2008-12-20 14:33:15Z thomas.diesler@jboss.com $

import java.io.IOException;
import java.net.URL;

import javax.management.ObjectName;

import org.jboss.bpm.api.deployment.Deployment;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.service.DeploymentService;
import org.jboss.bpm.api.service.ProcessDefinitionService;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.api.service.internal.AbstractService;
import org.jbpm.integration.spec.deployment.PARDeployment;
import org.jbpm.integration.spec.deployment.XMLDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The DeploymentService is the entry point to deploy and undeploy ProcessDefinitions.
 * 
 * @author thomas.diesler@jboss.com
 * @since 28-Nov-2008
 */
public class DeploymentServiceImpl extends AbstractService implements DeploymentService, MutableService
{
  // Provide logging
  final Logger log = LoggerFactory.getLogger(DeploymentServiceImpl.class);

  // @Override
  public void setProcessEngine(ProcessEngine engine)
  {
    super.setProcessEngine(engine);
  }

  public Deployment createDeployment(URL depURL)
  {
    Deployment dep;
    if (depURL.toExternalForm().endsWith("xml"))
    {
      dep = new XMLDeployment(depURL);
    }
    else if (depURL.toExternalForm().endsWith("par"))
    {
      dep = new PARDeployment(depURL);
    }
    else
    {
      throw new IllegalArgumentException("Unsupported deployment URL: " + depURL);
    }
    return dep;
  }

  public ProcessDefinition deploy(Deployment dep)
  {
    try
    {
      String pdXML = dep.getProcessDefinitionXML();

      // Parse and register the ProcessDefinition
      ProcessDefinitionService pdService = getProcessEngine().getService(ProcessDefinitionService.class);
      ProcessDefinition procDef = pdService.parseProcessDefinition(pdXML);
      procDef = pdService.registerProcessDefinition(procDef);

      dep.addAttachment(ObjectName.class, "procDefKey", procDef.getKey());

      return procDef;
    }
    catch (IOException ex)
    {
      throw new IllegalStateException("Cannot deploy: dep");
    }
  }

  public boolean undeploy(Deployment dep)
  {
    ObjectName key = dep.getAttachment(ObjectName.class, "procDefKey");
    ProcessDefinitionService pdService = getProcessEngine().getService(ProcessDefinitionService.class);
    return pdService.unregisterProcessDefinition(key);
  }
}
