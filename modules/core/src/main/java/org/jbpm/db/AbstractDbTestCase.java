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
package org.jbpm.db;

import java.util.EnumSet;
import java.util.Map;
import java.util.HashMap;

import junit.framework.TestCase;

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

public abstract class AbstractDbTestCase extends TestCase {

  protected JbpmConfiguration jbpmConfiguration = null;
  protected JbpmContext jbpmContext = null;

  public void setUp() throws Exception {
    super.setUp();
    if (jbpmConfiguration == null) {
      jbpmConfiguration = JbpmConfiguration.getInstance();
      dropSchema();
      createSchema();
    }
    jbpmContext = jbpmConfiguration.createJbpmContext();
  }

  public void tearDown() throws Exception {
    jbpmContext.close();
    if (jbpmContext.getSessionFactory().isClosed()) {
      jbpmConfiguration = null;
      jbpmContext = null;
    }
    super.tearDown();
  }

  public void createSchema() {
    Configuration hibernateConfiguration = JbpmConfiguration.getHibernateConfiguration();
    ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
      hibernateConfiguration.getProperties()).build();
    Metadata metadata = new org.hibernate.boot.MetadataSources(serviceRegistry).buildMetadata();
    SchemaManagementTool schemaManagementTool = serviceRegistry.getService(SchemaManagementTool.class);

    Map<String, Object> configValues = new HashMap<>();
    for (Map.Entry<Object, Object> entry : hibernateConfiguration.getProperties().entrySet()) {
        configValues.put((String) entry.getKey(), entry.getValue());
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

    SchemaCreator schemaCreator = schemaManagementTool.getSchemaCreator(configValues);
    schemaCreator.doCreation(metadata, executionOptions, ContributableMatcher.ALL, new MetadataSourceDescriptor(), new ScriptTargetDescriptor(new StringWriterScriptTargetOutput()));
  }

  public void dropSchema() {
    Configuration hibernateConfiguration = JbpmConfiguration.getHibernateConfiguration();
    ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
      hibernateConfiguration.getProperties()).build();
    Metadata metadata = new org.hibernate.boot.MetadataSources(serviceRegistry).buildMetadata();
    SchemaManagementTool schemaManagementTool = serviceRegistry.getService(SchemaManagementTool.class);

    Map<String, Object> configValues = new HashMap<>();
    for (Map.Entry<Object, Object> entry : hibernateConfiguration.getProperties().entrySet()) {
        configValues.put((String) entry.getKey(), entry.getValue());
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

    SchemaDropper schemaDropper = schemaManagementTool.getSchemaDropper(configValues);
    schemaDropper.doDrop(metadata, executionOptions, ContributableMatcher.ALL, new MetadataSourceDescriptor(), new ScriptTargetDescriptor(new StringWriterScriptTargetOutput()));
  }

  protected void newTransaction() {
    jbpmContext.close();
    jbpmContext = jbpmConfiguration.createJbpmContext();
  }
}