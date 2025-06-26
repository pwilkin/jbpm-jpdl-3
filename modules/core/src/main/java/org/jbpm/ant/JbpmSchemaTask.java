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
package org.jbpm.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaUpdate;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.jbpm.JbpmException;
import org.jbpm.db.ScriptTargetDescriptor;
import org.jbpm.db.StringWriterScriptTargetOutput;

public class JbpmSchemaTask extends Task {

  String action = null;
  File hibernateCfgXml = new File("hibernate.cfg.xml");
  File hibernateProperties = null;
  boolean drop = true;
  boolean create = true;
  boolean haltOnError = false;
  File outputFile = null;
  String delimiter = ";";

  public void execute() throws BuildException {
    try {
      // create the hibernate configuration
      Configuration configuration = new Configuration();
      if (hibernateCfgXml != null) {
        configuration.configure(hibernateCfgXml);
      }

      if (hibernateProperties != null) {
        Properties properties = new Properties();
        properties.load(new FileInputStream(hibernateProperties));
        configuration.setProperties(properties);
      }

      ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
        configuration.getProperties()).build();
      Metadata metadata = new org.hibernate.boot.MetadataSources(serviceRegistry).buildMetadata();
      SchemaManagementTool schemaManagementTool = serviceRegistry.getService(SchemaManagementTool.class);

      if ("update".equalsIgnoreCase(action)) {
        SchemaUpdate schemaUpdate = schemaManagementTool.getSchemaUpdater(configuration.getProperties());
        schemaUpdate.execute(new ScriptTargetDescriptor(new StringWriterScriptTargetOutput()), metadata, serviceRegistry, SchemaFilter.ALL);

      } else if ("export".equalsIgnoreCase(action)) {
        SchemaCreator schemaCreator = schemaManagementTool.getSchemaCreator(configuration.getProperties());
        schemaCreator.doCreation(metadata, false, new ScriptTargetDescriptor(new StringWriterScriptTargetOutput()));

      } else if ("drop".equalsIgnoreCase(action)) {
        SchemaDropper schemaDropper = schemaManagementTool.getSchemaDropper(configuration.getProperties());
        schemaDropper.doDrop(metadata, false, new ScriptTargetDescriptor(new StringWriterScriptTargetOutput()));

      } else if ("create".equalsIgnoreCase(action)) {
        SchemaCreator schemaCreator = schemaManagementTool.getSchemaCreator(configuration.getProperties());
        schemaCreator.doCreation(metadata, false, new ScriptTargetDescriptor(new StringWriterScriptTargetOutput()));
      }

    } catch (IOException e) {
      throw new BuildException(e);
    } catch (JbpmException e) {
      throw new BuildException(e);
    }
  }

  public void setAction(String action) {
    this.action = action;
  }
  public void setCreate(boolean create) {
    this.create = create;
  }
  public void setDelimiter(String delimiter) {
    this.delimiter = delimiter;
  }
  public void setDrop(boolean drop) {
    this.drop = drop;
  }
  public void setHaltOnError(boolean haltOnError) {
    this.haltOnError = haltOnError;
  }
  public void setHibernateCfgXml(File hibernateCfgXml) {
    this.hibernateCfgXml = hibernateCfgXml;
  }
  public void setHibernateProperties(File hibernateProperties) {
    this.hibernateProperties = hibernateProperties;
  }
  public void setOutputFile(File outputFile) {
    this.outputFile = outputFile;
  }
}