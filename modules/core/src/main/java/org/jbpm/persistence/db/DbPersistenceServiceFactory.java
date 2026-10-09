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

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.jbpm.JbpmConfiguration;
import org.jbpm.db.JbpmSessionFactory;
import org.jbpm.db.hibernate.HibernateHelper;
import org.jbpm.persistence.PersistenceService;
import org.jbpm.util.JndiUtil;

public class DbPersistenceServiceFactory implements org.jbpm.persistence.PersistenceServiceFactory {

  private static final long serialVersionUID = 1L;

  protected JbpmConfiguration jbpmConfiguration = null;
  protected Configuration configuration = null;
  protected SessionFactory sessionFactory = null;
  protected String sessionFactoryJndiName = null;
  protected DataSource dataSource = null;
  protected String dataSourceJndiName = null;
  protected boolean isTransactionEnabled = true;
  protected boolean isCurrentSessionEnabled = false;

  /** used when the factory is configured as a bean in jbpm.cfg.xml */
  public DbPersistenceServiceFactory() {
  }

  public DbPersistenceServiceFactory(JbpmConfiguration jbpmConfiguration) {
    this.jbpmConfiguration = jbpmConfiguration;
  }

  public PersistenceService openService() {
    return new DbPersistenceService(this);
  }

  /**
   * the hibernate configuration, read from the resource configured as
   * <code>resource.hibernate.cfg.xml</code> (and optionally
   * <code>resource.hibernate.properties</code>) in jbpm.cfg.xml.
   */
  public synchronized Configuration getConfiguration() {
    if (configuration == null) {
      String hibernateCfgXmlResource = null;
      if (JbpmConfiguration.Configs.hasObject("resource.hibernate.cfg.xml")) {
        hibernateCfgXmlResource = JbpmConfiguration.Configs.getString("resource.hibernate.cfg.xml");
      }
      configuration = JbpmSessionFactory.createConfiguration(hibernateCfgXmlResource);
    }
    return configuration;
  }

  public synchronized SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      if (sessionFactoryJndiName != null) {
        log.debug("looking up hibernate session factory in jndi '" + sessionFactoryJndiName + "'");
        sessionFactory = (SessionFactory) JndiUtil.lookup(sessionFactoryJndiName, SessionFactory.class);
      } else {
        log.debug("building hibernate session factory");
        sessionFactory = getConfiguration().buildSessionFactory();
      }
    }
    return sessionFactory;
  }

  public void createSchema() {
    getSessionFactory().getSchemaManager().exportMappedObjects(false);
    HibernateHelper.clearHibernateCache(getSessionFactory());
  }

  public void dropSchema() {
    HibernateHelper.clearHibernateCache(getSessionFactory());
    getSessionFactory().getSchemaManager().dropMappedObjects(false);
  }

  /** deletes all records from the jbpm tables */
  public void cleanSchema() {
    getSessionFactory().getSchemaManager().truncateMappedObjects();
    HibernateHelper.clearHibernateCache(getSessionFactory());
  }

  public void close() {
    if (sessionFactory != null) {
      log.debug("closing hibernate session factory");
      sessionFactory.close();
      sessionFactory = null;
      // a hibernate 6 configuration cannot build a second session factory once the first
      // one is closed (its bootstrap registry is stopped), so start from a fresh one
      configuration = null;
    }
  }

  public DataSource getDataSource() {
    if (dataSource == null && dataSourceJndiName != null) {
      log.debug("looking up datasource from jndi location '" + dataSourceJndiName + "'");
      dataSource = (DataSource) JndiUtil.lookup(dataSourceJndiName, DataSource.class);
    }
    return dataSource;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public String getDataSourceJndiName() {
    return dataSourceJndiName;
  }

  public void setDataSourceJndiName(String dataSourceJndiName) {
    this.dataSourceJndiName = dataSourceJndiName;
  }

  public String getSessionFactoryJndiName() {
    return sessionFactoryJndiName;
  }

  public void setSessionFactoryJndiName(String sessionFactoryJndiName) {
    this.sessionFactoryJndiName = sessionFactoryJndiName;
  }

  public boolean isTransactionEnabled() {
    return isTransactionEnabled;
  }

  public void setTransactionEnabled(boolean isTransactionEnabled) {
    this.isTransactionEnabled = isTransactionEnabled;
  }

  public boolean isCurrentSessionEnabled() {
    return isCurrentSessionEnabled;
  }

  public void setCurrentSessionEnabled(boolean isCurrentSessionEnabled) {
    this.isCurrentSessionEnabled = isCurrentSessionEnabled;
  }

  private static final Log log = LogFactory.getLog(DbPersistenceServiceFactory.class);
}
