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
package org.jbpm.persistence.db;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.persistence.PersistenceService;
import org.jbpm.persistence.PersistenceServiceFactory;
import org.jbpm.db.MetadataSourceDescriptor;
import org.jbpm.db.ScriptTargetDescriptor;
import org.jbpm.db.StringWriterScriptTargetOutput;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

public class DbPersistenceServiceFactory implements org.jbpm.persistence.PersistenceServiceFactory {

  private static final long serialVersionUID = 1L;

  protected JbpmConfiguration jbpmConfiguration = null;
  protected SessionFactory sessionFactory = null;
  protected ServiceRegistry serviceRegistry = null;
  protected boolean isCurrentSessionEnabled = true;

  public DbPersistenceServiceFactory(JbpmConfiguration jbpmConfiguration) {
    this.jbpmConfiguration = jbpmConfiguration;
  }

  public PersistenceService openService() {
    return new DbPersistenceService(this);
  }

  public SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      // create a new hibernate configuration
      Configuration hibernateConfiguration = JbpmConfiguration.getHibernateConfiguration();
      this.serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
        hibernateConfiguration.getProperties()).build();
      sessionFactory = new org.hibernate.boot.MetadataSources(this.serviceRegistry).buildMetadata().buildSessionFactory();
    }
    return sessionFactory;
  }

  public void createSchema() {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      Configuration hibernateConfiguration = JbpmConfiguration.getHibernateConfiguration();
      ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
        hibernateConfiguration.getProperties()).build();
      Metadata metadata = new org.hibernate.boot.MetadataSources(serviceRegistry).buildMetadata();

      Map<String, Object> configValues = new HashMap<>();
      for (Map.Entry<Object, Object> entry : hibernateConfiguration.getProperties().entrySet()) {
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
                  public void handleException(org.hibernate.tool.schema.spi.CommandAcceptanceException exception) {
                      // no-op
                  }
              };
          }

          @Override
          public org.hibernate.tool.schema.spi.SchemaFilter getSchemaFilter() {
              return org.hibernate.tool.schema.spi.SchemaFilter.ALL;
          }
      };

      SchemaCreator schemaCreator = new org.hibernate.tool.schema.internal.SchemaCreatorImpl(serviceRegistry);
      schemaCreator.doCreation(metadata, executionOptions, ContributableMatcher.ALL, new MetadataSourceDescriptor(), new ScriptTargetDescriptor(new StringWriterScriptTargetOutput()));
    } finally {
      jbpmContext.close();
    }
  }

  public void dropSchema() {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      Configuration hibernateConfiguration = JbpmConfiguration.getHibernateConfiguration();
      ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
        hibernateConfiguration.getProperties()).build();
      Metadata metadata = new org.hibernate.boot.MetadataSources(serviceRegistry).buildMetadata();

      Map<String, Object> configValues = new HashMap<>();
      for (Map.Entry<Object, Object> entry : hibernateConfiguration.getProperties().entrySet()) {
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
                  public void handleException(org.hibernate.tool.schema.spi.CommandAcceptanceException exception) {
                      // no-op
                  }
              };
          }

          @Override
          public org.hibernate.tool.schema.spi.SchemaFilter getSchemaFilter() {
              return org.hibernate.tool.schema.spi.SchemaFilter.ALL;
          }
      };

      SchemaDropper schemaDropper = new org.hibernate.tool.schema.internal.SchemaDropperImpl(serviceRegistry);
      schemaDropper.doDrop(metadata, executionOptions, ContributableMatcher.ALL, new MetadataSourceDescriptor(), new ScriptTargetDescriptor(new StringWriterScriptTargetOutput()));
    } finally {
      jbpmContext.close();
    }
  }

  public void cleanSchema() {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      // jbpmContext.getServices().getSchemaService().cleanSchema();
    } finally {
      jbpmContext.close();
    }
  }

  public void close() {
    if (sessionFactory != null) {
      sessionFactory.close();
      sessionFactory = null;
    }
  }

  public boolean isCurrentSessionEnabled() {
    return isCurrentSessionEnabled;
  }

  public void setCurrentSessionEnabled(boolean isCurrentSessionEnabled) {
    this.isCurrentSessionEnabled = isCurrentSessionEnabled;
  }

  public DataSource getDataSource() {
    if (serviceRegistry != null) {
      return serviceRegistry.getService(org.hibernate.engine.jdbc.connections.spi.ConnectionProvider.class).unwrap(DataSource.class);
    }
    return null;
  }

  private static final Log log = LogFactory.getLog(DbPersistenceServiceFactory.class);
}