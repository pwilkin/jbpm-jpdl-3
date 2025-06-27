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
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.jbpm.db.MetadataSourceDescriptor;
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

      Map<String, Object> configValues = new HashMap<>();
      for (Map.Entry<Object, Object> entry : configuration.getProperties().entrySet()) {
          configValues.put(String.valueOf(entry.getKey()), entry.getValue());
      }

      ExecutionOptions executionOptions = new ExecutionOptions() {
          @Override
          public boolean shouldManageNamespaces() {
              return false;
          }

          @Override
          public Map<String, Object> getConfigurationValues() {
              return configValues;
          }

          @Override
          public org.hibernate.tool.schema.spi.ExceptionHandler getExceptionHandler() {
              return new org.hibernate.tool.schema.spi.ExceptionHandler() {
                  @Override
                  public void handleException(CommandAcceptanceException exception) {
                      // no-op
                  }
              };
          }

          @Override
          public org.hibernate.tool.schema.spi.SchemaFilter getSchemaFilter() {
              return org.hibernate.tool.schema.spi.SchemaFilter.ALL;
          }
      };

      if ("update".equalsIgnoreCase(action)) {
        // For update, we'll generate drop and create scripts and execute them
        StringWriterScriptTargetOutput dropScriptTarget = new StringWriterScriptTargetOutput();
        SchemaDropper schemaDropper = new org.hibernate.tool.schema.internal.SchemaDropperImpl(serviceRegistry);
        schemaDropper.doDrop(metadata, executionOptions, ContributableMatcher.ALL, new MetadataSourceDescriptor(), new ScriptTargetDescriptor(dropScriptTarget));

        StringWriterScriptTargetOutput createScriptTarget = new StringWriterScriptTargetOutput();
        SchemaCreator schemaCreator = new org.hibernate.tool.schema.internal.SchemaCreatorImpl(serviceRegistry);
        schemaCreator.doCreation(metadata, executionOptions, ContributableMatcher.ALL, new MetadataSourceDescriptor(), new ScriptTargetDescriptor(createScriptTarget));

        executeSql(dropScriptTarget.getWriter().toString(), serviceRegistry);
        executeSql(createScriptTarget.getWriter().toString(), serviceRegistry);

      } else if ("export".equalsIgnoreCase(action) || "create".equalsIgnoreCase(action)) {
        StringWriterScriptTargetOutput createScriptTarget = new StringWriterScriptTargetOutput();
        SchemaCreator schemaCreator = new org.hibernate.tool.schema.internal.SchemaCreatorImpl(serviceRegistry);
        schemaCreator.doCreation(metadata, executionOptions, ContributableMatcher.ALL, new MetadataSourceDescriptor(), new ScriptTargetDescriptor(createScriptTarget));
        if (outputFile != null) {
          try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(createScriptTarget.getWriter().toString());
          }
        }

      } else if ("drop".equalsIgnoreCase(action)) {
        StringWriterScriptTargetOutput dropScriptTarget = new StringWriterScriptTargetOutput();
        SchemaDropper schemaDropper = new org.hibernate.tool.schema.internal.SchemaDropperImpl(serviceRegistry);
        schemaDropper.doDrop(metadata, executionOptions, ContributableMatcher.ALL, new MetadataSourceDescriptor(), new ScriptTargetDescriptor(dropScriptTarget));
        if (outputFile != null) {
          try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(dropScriptTarget.getWriter().toString());
          }
        }
      }

    } catch (IOException e) {
      throw new BuildException(e);
    } catch (Exception e) { // Catch generic Exception for now, refine later if needed
      throw new BuildException(e);
    }
  }

  private void executeSql(String sql, ServiceRegistry serviceRegistry) {
    Connection connection = null;
    Statement statement = null;
    try {
      ConnectionProvider connectionProvider = serviceRegistry.getService(ConnectionProvider.class);
      connection = connectionProvider.getConnection();
      statement = connection.createStatement();
      for (String s : sql.split(delimiter)) {
        if (!s.trim().isEmpty()) {
          statement.executeUpdate(s);
        }
      }
    } catch (SQLException e) {
      throw new BuildException("Error executing SQL: " + sql, e);
    } finally {
      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          log.debug("could not close jdbc statement", e);
        }
      }
      if (connection != null) {
        try {
          serviceRegistry.getService(org.hibernate.engine.jdbc.spi.JdbcServices.class).getSqlExceptionHelper().logAndClearWarnings(connection);
          serviceRegistry.getService(ConnectionProvider.class).closeConnection(connection);
        } catch (SQLException e) {
          log.debug("could not close jdbc connection", e);
        }
      }
    }
  }

  private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(JbpmSchemaTask.class);

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