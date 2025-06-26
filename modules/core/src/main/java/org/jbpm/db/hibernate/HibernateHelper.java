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
package org.jbpm.db.hibernate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.TargetType;
import org.jbpm.JbpmException;
import org.jbpm.db.ScriptTargetDescriptor;
import org.jbpm.db.StringWriterScriptTargetOutput;

public class HibernateHelper {

  public static String[] getCreateSchemaSql(Configuration configuration) {
    try {
      File tempFile = File.createTempFile("jbpm-create-", ".sql");
      tempFile.deleteOnExit();

      ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
        configuration.getProperties()).build();
      Metadata metadata = new org.hibernate.boot.MetadataSources(serviceRegistry).buildMetadata();
      SchemaManagementTool schemaManagementTool = serviceRegistry.getService(SchemaManagementTool.class);
      SchemaCreator schemaCreator = schemaManagementTool.getSchemaCreator(configuration.getProperties());

      StringWriterScriptTargetOutput scriptTargetOutput = new StringWriterScriptTargetOutput();
      schemaCreator.doCreation(metadata, false, new ScriptTargetDescriptor(scriptTargetOutput));

      List<String> lines = Files.readAllLines(tempFile.toPath());
      return lines.toArray(new String[0]);
    } catch (IOException e) {
      throw new JbpmException("couldn't generate create script", e);
    }
  }

  public static String[] getDropSchemaSql(Configuration configuration) {
    try {
      File tempFile = File.createTempFile("jbpm-drop-", ".sql");
      tempFile.deleteOnExit();

      ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
        configuration.getProperties()).build();
      Metadata metadata = new org.hibernate.boot.MetadataSources(serviceRegistry).buildMetadata();
      SchemaManagementTool schemaManagementTool = serviceRegistry.getService(SchemaManagementTool.class);
      SchemaDropper schemaDropper = schemaManagementTool.getSchemaDropper(configuration.getProperties());

      StringWriterScriptTargetOutput scriptTargetOutput = new StringWriterScriptTargetOutput();
      schemaDropper.doDrop(metadata, false, new ScriptTargetDescriptor(scriptTargetOutput));

      List<String> lines = Files.readAllLines(tempFile.toPath());
      return lines.toArray(new String[0]);
    } catch (IOException e) {
      throw new JbpmException("couldn't generate drop script", e);
    }
  }
}