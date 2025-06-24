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

// $Id: DialectHandlerServiceImpl.java 3485 2008-12-20 14:33:15Z thomas.diesler@jboss.com $

import java.net.URI;
import java.util.Map;

import org.jboss.bpm.api.service.DialectHandler;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.api.service.internal.AbstractDialectHandlerService;

/**
 * A registry that maps namespaceURI to a {@link DialectHandler}
 * 
 * @author thomas.diesler@jboss.com
 * @since 18-Jun-2008
 */
public class DialectHandlerServiceImpl extends AbstractDialectHandlerService implements MutableService
{
  // @Override
  public void setProcessEngine(ProcessEngine engine)
  {
    super.setProcessEngine(engine);
  }

  public void setDialectHandlers(Map<URI, DialectHandler> dialectHandlers)
  {
    this.dialectHandlers = dialectHandlers;
  }
}