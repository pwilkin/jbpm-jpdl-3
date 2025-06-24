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

// $Id: ProcessBuilderServiceImpl.java 3466 2008-12-19 22:53:18Z thomas.diesler@jboss.com $

import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.incubator.model.builder.ProcessBuilder;
import org.jboss.bpm.incubator.service.ProcessBuilderService;
import org.jbpm.integration.spec.model.builder.ProcessBuilderImpl;

/**
 * The ProcessBuilder can be used to build a {@link ProcessInstance} dynamically.
 * 
 * @author thomas.diesler@jboss.com
 * @since 18-Jun-2008
 */
public class ProcessBuilderServiceImpl extends ProcessBuilderService implements MutableService
{
  // @Override
  public void setProcessEngine(ProcessEngine engine)
  {
    super.setProcessEngine(engine);
  }

  // @Override
  public ProcessBuilder getProcessBuilder()
  {
    return new ProcessBuilderImpl(getProcessEngine());
  }
}