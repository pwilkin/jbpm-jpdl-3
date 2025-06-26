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
import java.util.Collections;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.persistence.PersistenceService;
import org.jbpm.persistence.PersistenceServiceFactory;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DbPersistenceServiceFactory implements org.jbpm.persistence.PersistenceServiceFactory {

  private static final long serialVersionUID = 1L;

  protected JbpmConfiguration jbpmConfiguration = null;
  protected SessionFactory sessionFactory = null;
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
      ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
        hibernateConfiguration.getProperties()).build();
      sessionFactory = new org.hibernate.boot.MetadataSources(serviceRegistry).buildMetadata().buildSessionFactory();
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
      SchemaManagementTool schemaManagementTool = serviceRegistry.getService(SchemaManagementTool.class);
      SchemaCreator schemaCreator = schemaManagementTool.getSchemaCreator(hibernateConfiguration.getProperties());
      schemaCreator.doCreation(metadata, false, new org.jbpm.db.ScriptTargetDescriptor(new org.jbpm.db.StringWriterScriptTargetOutput()));
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
      SchemaManagementTool schemaManagementTool = serviceRegistry.getService(SchemaManagementTool.class);
      SchemaDropper schemaDropper = schemaManagementTool.getSchemaDropper(hibernateConfiguration.getProperties());
      schemaDropper.doDrop(metadata, false, new org.jbpm.db.ScriptTargetDescriptor(new org.jbpm.db.StringWriterScriptTargetOutput()));
    } finally {
      jbpmContext.close();
    }
  }

  public void cleanSchema() {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      jbpmContext.getServices().getSchemaService().cleanSchema();
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

  private static final Log log = LogFactory.getLog(DbPersistenceServiceFactory.class);
}