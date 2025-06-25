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
import org.hibernate.service.ServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.jbpm.JbpmConfiguration;
import org.jbpm.db.JbpmSchema;
import org.jbpm.db.hibernate.HibernateHelper;
import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;
import org.jbpm.util.JndiUtil;

public class DbPersistenceServiceFactory implements ServiceFactory
{

  private static final long serialVersionUID = 1L;

  Configuration configuration = null;

  String sessionFactoryJndiName = null;
  SessionFactory sessionFactory = null;
  ServiceRegistry serviceRegistry = null;

  String dataSourceJndiName = null;
  DataSource dataSource = null;

  boolean isTransactionEnabled = true;
  boolean isCurrentSessionEnabled = false;

  SchemaExport schemaExport = null;

  public Service openService()
  {
    log.debug("creating persistence service");
    return new DbPersistenceService(this);
  }

  public synchronized Configuration getConfiguration()
  {
    if (configuration == null)
    {
      String hibernateCfgXmlResource = null;
      if (JbpmConfiguration.Configs.hasObject("resource.hibernate.cfg.xml"))
      {
        hibernateCfgXmlResource = JbpmConfiguration.Configs.getString("resource.hibernate.cfg.xml");
      }
      String hibernatePropertiesResource = null;
      if (JbpmConfiguration.Configs.hasObject("resource.hibernate.properties"))
      {
        hibernatePropertiesResource = JbpmConfiguration.Configs.getString("resource.hibernate.properties");
      }
      configuration = HibernateHelper.createConfiguration(hibernateCfgXmlResource, hibernatePropertiesResource);
    }
    return configuration;
  }

  public synchronized SchemaExport getSchemaExport()
  {
    if (schemaExport == null)
    {
      log.debug("creating schema export");
      schemaExport = new SchemaExport(getConfiguration());
    }
    return schemaExport;
  }

  public synchronized SessionFactory getSessionFactory()
  {
    if (sessionFactory == null)
    {

      if (sessionFactoryJndiName != null)
      {
        log.debug("looking up hibernate session factory in jndi '" + sessionFactoryJndiName + "'");
        sessionFactory = (SessionFactory)JndiUtil.lookup(sessionFactoryJndiName, SessionFactory.class);

      }
      else
      {
        log.debug("building hibernate session factory");
        Configuration config = getConfiguration();
        StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
            .applySettings(config.getProperties());
        serviceRegistry = registryBuilder.build();
        sessionFactory = config.buildSessionFactory(serviceRegistry);
      }
    }
    return sessionFactory;
  }

  public synchronized ServiceRegistry getServiceRegistry() {
    if (serviceRegistry == null) {
      // Ensure SessionFactory (and thus ServiceRegistry) is initialized
      getSessionFactory();
      // If SessionFactory was obtained from JNDI, serviceRegistry might still be null.
      // In this case, we might need to build a new one or accept that it's not available.
      // For now, let's assume it should have been built if not from JNDI.
      // If it's still null and was from JNDI, this method might return null.
      // This could be an issue for JNDI-provided SessionFactories if the ServiceRegistry is strictly needed.
      // However, ShutDownHsqldb typically runs in an environment where JNDI lookup for SF is less common.
      if (sessionFactory != null && serviceRegistry == null && sessionFactory instanceof org.hibernate.engine.spi.SessionFactoryImplementor) {
        // If SF is available and is SFImpl, try to get its SR.
        // This handles cases where SF might be set externally or via JNDI but is a compatible type.
        serviceRegistry = ((org.hibernate.engine.spi.SessionFactoryImplementor) sessionFactory).getServiceRegistry();
      }
    }
    return serviceRegistry;
  }

  public DataSource getDataSource()
  {
    if ((dataSource == null) && (dataSourceJndiName != null))
    {
      log.debug("looking up datasource from jndi location '" + dataSourceJndiName + "'");
      dataSource = (DataSource)JndiUtil.lookup(dataSourceJndiName, DataSource.class);
    }
    return dataSource;
  }

  public void cleanSchema()
  {
    new JbpmSchema(getConfiguration()).cleanSchema();
    HibernateHelper.clearHibernateCache(getSessionFactory());
  }

  public void createSchema()
  {
    // TODO: Hibernate 5 - SchemaExport API changed. Needs rewrite.
    log.warn("DbPersistenceServiceFactory.createSchema() is disabled for Hibernate 5 migration.");
    // getSchemaExport().create(getScript(), true);
    // HibernateHelper.clearHibernateCache(getSessionFactory()); // clearHibernateCache is also disabled
  }

  public void dropSchema()
  {
    // TODO: Hibernate 5 - SchemaExport API changed. Needs rewrite.
    log.warn("DbPersistenceServiceFactory.dropSchema() is disabled for Hibernate 5 migration.");
    // HibernateHelper.clearHibernateCache(getSessionFactory()); // clearHibernateCache is also disabled
    // getSchemaExport().drop(getScript(), true);
  }

  boolean getScript()
  {
    boolean script = false;
    String showSql = getConfiguration().getProperty("hibernate.show_sql");
    if ("true".equalsIgnoreCase(showSql))
    {
      script = true;
    }
    return script;
  }

  public void close()
  {
    if (sessionFactory != null)
    {
      log.debug("closing hibernate session factory");
      sessionFactory.close();
    }
  }

  public void finalize() throws Throwable
  {
    close();
  }

  public String getDataSourceJndiName()
  {
    return dataSourceJndiName;
  }

  public void setDataSourceJndiName(String dataSourceJndiName)
  {
    this.dataSourceJndiName = dataSourceJndiName;
  }

  public String getSessionFactoryJndiName()
  {
    return sessionFactoryJndiName;
  }

  public void setSessionFactoryJndiName(String sessionFactoryJndiName)
  {
    this.sessionFactoryJndiName = sessionFactoryJndiName;
  }

  public void setConfiguration(Configuration configuration)
  {
    this.configuration = configuration;
  }

  public void setDataSource(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public void setSchemaExport(SchemaExport schemaExport)
  {
    this.schemaExport = schemaExport;
  }

  public void setSessionFactory(SessionFactory sessionFactory)
  {
    this.sessionFactory = sessionFactory;
  }

  public boolean isTransactionEnabled()
  {
    return isTransactionEnabled;
  }

  public void setTransactionEnabled(boolean isTransactionEnabled)
  {
    this.isTransactionEnabled = isTransactionEnabled;
  }

  public boolean isCurrentSessionEnabled()
  {
    return isCurrentSessionEnabled;
  }

  public void setCurrentSessionEnabled(boolean isCurrentSessionEnabled)
  {
    this.isCurrentSessionEnabled = isCurrentSessionEnabled;
  }

  private static Log log = LogFactory.getLog(DbPersistenceServiceFactory.class);
}
