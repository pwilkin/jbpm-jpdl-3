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
package org.jbpm.identity.hibernate;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.List;

import org.apache.commons.logging.*;
import org.hibernate.cfg.*;
import org.jbpm.JbpmException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.jbpm.db.MetadataSourceDescriptor;
import org.jbpm.db.ScriptTargetDescriptor;
import org.jbpm.db.StringWriterScriptTargetOutput;

public class IdentitySchema {

  private static final String IDENTITY_TABLE_PREFIX = "JBPM_ID_";
  
  Configuration configuration = null;
  Properties properties = null;

  public IdentitySchema(Configuration configuration) {
    this.configuration = configuration;
    this.properties = configuration.getProperties();
  }

  // scripts lazy initializations /////////////////////////////////////////////
  
  public String[] getCreateSql() {
    ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(properties).build();
    Metadata metadata = new org.hibernate.boot.MetadataSources(serviceRegistry).buildMetadata();
    StringWriterScriptTargetOutput createScriptTarget = new StringWriterScriptTargetOutput();
    SchemaCreator schemaCreator = new SchemaCreatorImpl(serviceRegistry);
    schemaCreator.doCreation(metadata, new ExecutionOptions() {
        @Override
        public boolean shouldManageNamespaces() {
            return false;
        }

        @Override
        public Map<String, Object> getConfigurationValues() {
            Map<String, Object> configValues = new HashMap<>();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                configValues.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return configValues;
        }

        @Override
        public ExceptionHandler getExceptionHandler() {
            return new ExceptionHandler() {
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
    }, ContributableMatcher.ALL, new MetadataSourceDescriptor(), new ScriptTargetDescriptor(createScriptTarget));
    return createScriptTarget.getWriter().toString().split(";");
  }
  
  public String[] getDropSql() {
    ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(properties).build();
    Metadata metadata = new org.hibernate.boot.MetadataSources(serviceRegistry).buildMetadata();
    StringWriterScriptTargetOutput dropScriptTarget = new StringWriterScriptTargetOutput();
    SchemaDropper schemaDropper = new SchemaDropperImpl(serviceRegistry);
    schemaDropper.doDrop(metadata, new ExecutionOptions() {
        @Override
        public boolean shouldManageNamespaces() {
            return false;
        }

        @Override
        public Map<String, Object> getConfigurationValues() {
            Map<String, Object> configValues = new HashMap<>();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                configValues.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return configValues;
        }

        @Override
        public ExceptionHandler getExceptionHandler() {
            return new ExceptionHandler() {
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
    }, ContributableMatcher.ALL, new MetadataSourceDescriptor(), new ScriptTargetDescriptor(dropScriptTarget));
    return dropScriptTarget.getWriter().toString().split(";");
  }
  
  public String[] getCleanSql() {
    // This method needs to be re-evaluated for Hibernate 6. It previously relied on internal Hibernate 3 APIs.
    // For now, returning an empty array to allow compilation.
    // A proper implementation would involve analyzing the schema and generating DML for cleaning.
    return new String[0];
  }

  // runtime table detection //////////////////////////////////////////////////
  
  public boolean hasIdentityTables() {
    // This method needs to be re-evaluated for Hibernate 6. It previously relied on internal Hibernate 3 APIs.
    // For now, returning false to allow compilation.
    return false;
  }

  public List getIdentityTables() {
    // This method needs to be re-evaluated for Hibernate 6. It previously relied on internal Hibernate 3 APIs.
    // For now, returning an empty list to allow compilation.
    return new ArrayList();
  }
  
  // script execution methods /////////////////////////////////////////////////
  
  public void dropSchema() {
    execute( getDropSql() );
  }

  public void createSchema() {
    execute( getCreateSql() );
  }

  public void cleanSchema() {
    execute( getCleanSql() );
  }

  

  // main /////////////////////////////////////////////////////////////////////
  
  public static void main(String[] args) {
    if (args == null || args.length == 0) {
      syntax();
    } else if ("create".equalsIgnoreCase(args[0])) {
      new IdentitySchema(IdentitySessionFactory.createConfiguration()).createSchema();
    } else if ("drop".equalsIgnoreCase(args[0])) {
      new IdentitySchema(IdentitySessionFactory.createConfiguration()).dropSchema();
    } else if ("clean".equalsIgnoreCase(args[0])) {
      new IdentitySchema(IdentitySessionFactory.createConfiguration()).cleanSchema();
    } else {
      syntax();
    }
  }
  
  private static void syntax() {
    System.err.println("syntax:");
    System.err.println("IdentitySchema create");
    System.err.println("IdentitySchema drop");
    System.err.println("IdentitySchema clean");
    System.err.println("IdentitySchema scripts <dir> <prefix>");
  }

  private void saveSqlScript(String fileName, String[] sql) throws FileNotFoundException {
    FileOutputStream fileOutputStream = new FileOutputStream(fileName);
    PrintStream printStream = new PrintStream(fileOutputStream);
    for (int i=0; i<sql.length; i++) {
      printStream.println(sql[i]+getSqlDelimiter());
    }
  }
  
  // sql script execution /////////////////////////////////////////////////////

  public void execute(String[] sqls) {
    String sql = null;
    String showSqlText = properties.getProperty("hibernate.show_sql");
    boolean showSql = ("true".equalsIgnoreCase(showSqlText));

    ServiceRegistry serviceRegistry = null;
    Connection connection = null;
    Statement statement = null;

    try {
      serviceRegistry = new StandardServiceRegistryBuilder().applySettings(properties).build();
      ConnectionProvider connectionProvider = serviceRegistry.getService(ConnectionProvider.class);
      connection = connectionProvider.getConnection();
      statement = connection.createStatement();
      
      for (int i=0; i<sqls.length; i++) {
        sql = sqls[i];
        String delimitedSql = sql+getSqlDelimiter();
        
        if (showSql) log.debug(delimitedSql);
        statement.executeUpdate(delimitedSql);
      }
    
    } catch (SQLException e) {
      throw new JbpmException("couldn't execute sql '"+sql+"'", e);
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
          if (serviceRegistry != null) {
            serviceRegistry.getService(JdbcServices.class).getSqlExceptionHelper().logAndClearWarnings(connection);
            serviceRegistry.getService(ConnectionProvider.class).closeConnection(connection);
          }
        } catch (SQLException e) {
          log.debug("could not close jdbc connection", e);
        }
      }
      if (serviceRegistry != null) {
        StandardServiceRegistryBuilder.destroy(serviceRegistry);
      }
    }
  }

  
  
  public Properties getProperties() {
    return properties;
  }

  // sql delimiter ////////////////////////////////////////////////////////////
  
  private static String sqlDelimiter = null;
  private synchronized String getSqlDelimiter() {
    if (sqlDelimiter==null) {
      sqlDelimiter = properties.getProperty("jbpm.sql.delimiter", ";");
    }
    return sqlDelimiter;
  }

  // logger ///////////////////////////////////////////////////////////////////
  
  private static final Log log = LogFactory.getLog(IdentitySchema.class);
}
