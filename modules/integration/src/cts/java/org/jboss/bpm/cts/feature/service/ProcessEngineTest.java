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
package org.jboss.bpm.cts.feature.service;

// $Id: ProcessEngineTest.java 3474 2008-12-20 10:00:20Z thomas.diesler@jboss.com $

import org.jboss.bpm.api.config.Configuration;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.api.test.CTSTestCase;

/**
 * Test the ProcessEngine
 * 
 * @author thomas.diesler@jboss.com
 * @since 24-Sep-2008
 */
public class ProcessEngineTest extends CTSTestCase
{
  public void testDefaultProcessEngine() throws Exception
  {
    ProcessEngine engine = Configuration.getProcessEngine();
    assertNotNull("ProcessEngine not null", engine);
  }
}
