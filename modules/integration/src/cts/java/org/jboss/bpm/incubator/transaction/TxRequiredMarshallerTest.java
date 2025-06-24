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
package org.jboss.bpm.incubator.transaction;

// $Id: TxRequiredMarshallerTest.java 3473 2008-12-20 09:31:54Z thomas.diesler@jboss.com $

import java.io.IOException;

import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.service.ProcessDefinitionService;

/**
 * Test two tasks with Tx attribute REQUIRED
 * 
 * @author thomas.diesler@jboss.com
 * @since 30-Oct-2008
 */
public class TxRequiredMarshallerTest extends TxRequiredTest
{
  protected ProcessDefinition getProcessDefinition() throws IOException
  {
    // Marshall the process to a string
    ProcessDefinition procDef = super.getProcessDefinition();
    String procXML = marshallProcess(procDef);

    // System.out.println(procXML);

    // Recreate the ProcessDefinition from the marshalled ProcessDefinition
    ProcessDefinitionService pdService = getProcessEngine().getService(ProcessDefinitionService.class);
    return pdService.parseProcessDefinition(procXML);
  }
}
