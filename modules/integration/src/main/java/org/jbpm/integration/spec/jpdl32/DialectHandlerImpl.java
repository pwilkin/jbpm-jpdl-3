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
package org.jbpm.integration.spec.jpdl32;

// $Id: DialectHandlerImpl.java 3485 2008-12-20 14:33:15Z thomas.diesler@jboss.com $

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URL;

import org.jboss.bpm.api.NotImplementedException;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.service.DialectHandler;
import org.jboss.bpm.api.service.internal.AbstractDialectHandler;
import org.jbpm.integration.spec.model.ProcessDefinitionImpl;

/**
 * A jPDL a {@link DialectHandler}
 * 
 * @author thomas.diesler@jboss.com
 * @since 24-Nov-2008
 */
public class DialectHandlerImpl extends AbstractDialectHandler
{
  public URI getNamespaceURI()
  {
    return URI.create("urn:jbpm.org:jpdl-3.2");
  }

  public ProcessDefinition parseProcessDefinition(URL pdXML) throws IOException
  {
    InputStream inStream = pdXML.openStream();
    org.jbpm.graph.def.ProcessDefinition oldProcDef = org.jbpm.graph.def.ProcessDefinition.parseXmlInputStream(inStream);
    ProcessDefinition procDef = ProcessDefinitionImpl.newInstance(getProcessEngine(), oldProcDef, false);
    return procDef;
  }

  public ProcessDefinition parseProcessDefinition(String pdXML)
  {
    org.jbpm.graph.def.ProcessDefinition oldProcDef = org.jbpm.graph.def.ProcessDefinition.parseXmlString(pdXML);
    ProcessDefinition procDef = ProcessDefinitionImpl.newInstance(getProcessEngine(), oldProcDef, false);
    return procDef;
  }

  public void marshallProcessDefinition(ProcessDefinition procDef, Writer out) throws IOException
  {
    throw new NotImplementedException("jPDL marshalling not implemented");
  }
}