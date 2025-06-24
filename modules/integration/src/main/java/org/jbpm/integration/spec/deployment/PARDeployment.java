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
package org.jbpm.integration.spec.deployment;

// $Id: PARDeployment.java 3154 2008-11-28 16:41:56Z thomas.diesler@jboss.com $

import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jbpm.util.IoUtil;

/**
 * An abstraction of a process deployment
 * 
 * @author thomas.diesler@jboss.com
 * @since 17-Sep-2008
 */
public class PARDeployment extends AbstractDeployment
{
  private static final String PROCESSDEFINITION_ENTRY = "processdefinition.xml";
  private URL pdURL;

  public PARDeployment(URL pdURL)
  {
    this.pdURL = pdURL;
  }

  public String getProcessDefinitionXML() throws IOException
  {
    ZipInputStream zipInputStream = new ZipInputStream(pdURL.openStream());
    ZipEntry zipEntry = zipInputStream.getNextEntry();
    while (zipEntry != null)
    {
      String entryName = zipEntry.getName();
      byte[] bytes = IoUtil.readBytes(zipInputStream);
      if (bytes != null)
      {
        addAttachment(entryName, bytes);
      }
      zipEntry = zipInputStream.getNextEntry();
    }

    byte[] pdBytes = (byte[])getAttachment(PROCESSDEFINITION_ENTRY);
    if (pdBytes == null)
      throw new IllegalStateException("Cannot obtain '" + PROCESSDEFINITION_ENTRY + "' from: " + pdURL);

    String pdXML = new String(pdBytes);
    return pdXML;
  }
}
