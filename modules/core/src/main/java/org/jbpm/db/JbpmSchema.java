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

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.config.spi.ConfigurationService;
import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.EnumSet;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToWriter;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.hibernate.tool.schema.TargetType;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.StringWriter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;

import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;

import org.jbpm.JbpmException;

/**
 * utilities for the jBPM database schema.
 */
public class JbpmSchema implements Serializable {

  private static final long serialVersionUID = 1L;

  private final ServiceRegistry serviceRegistry;
  private final org.hibernate.boot.Metadata metadata;
  private final Dialect dialect;
  private final ConnectionProvider connectionProvider;

  private String[] createSql = null;
  private String[] dropSql = null;
  private String[] cleanSql = null;

  Connection connection = null;
  Statement statement = null;

  public JbpmSchema(org.hibernate.boot.Metadata metadata, ServiceRegistry serviceRegistry) {
    this.metadata = metadata;
    this.serviceRegistry = serviceRegistry;

    this.dialect = serviceRegistry.getService(JdbcEnvironment.class).getDialect();
    this.connectionProvider = serviceRegistry.getService(ConnectionProvider.class);
  }

  public String[] getCreateSql() {
    if (createSql == null) {
      final StringWriterScriptTargetOutput createScriptTarget = new StringWriterScriptTargetOutput();
      final TargetDescriptor createTargetDescriptor = new ScriptTargetDescriptor(createScriptTarget);
      Map<String, Object> configurationProperties = new HashMap<>();
      configurationProperties.put("jakarta.persistence.schema-generation.scripts.action", "create");
      configurationProperties.put("jakarta.persistence.schema-generation.scripts.create-target", createScriptTarget.getWriter());

      StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
          .applySettings(configurationProperties)
          .build();

      try {
          SchemaManagementTool schemaManagementTool = serviceRegistry.getService(SchemaManagementTool.class);
          SchemaCreator schemaCreator = schemaManagementTool.getSchemaCreator(configurationProperties);
          ExecutionOptions executionOptions = new ExecutionOptions() {
              @Override
              public boolean shouldManageNamespaces() {
                  return false;
              }

              @Override
              public Map<String, Object> getConfigurationValues() {
                  return serviceRegistry.getService(ConfigurationService.class).getSettings();
              }

              @Override
              public org.hibernate.tool.schema.spi.ExceptionHandler getExceptionHandler() {
                  return new org.hibernate.tool.schema.spi.ExceptionHandler() {
                      @Override
                      public void handleException(Exception exception) {
                          // no-op
                      }
                  };
              }

              @Override
              public org.hibernate.tool.schema.spi.SchemaFilter getSchemaFilter() {
                  return org.hibernate.tool.schema.spi.SchemaFilter.ALL;
              }
          };
          schemaCreator.doCreation(metadata, executionOptions, ContributableMatcher.ALL, new MetadataSourceDescriptor(), createTargetDescriptor);
      } catch (Exception e) {
          throw new JbpmException("couldn't create schema", e);
      } finally {
          StandardServiceRegistryBuilder.destroy(serviceRegistry);
      }
      createSql = new String[]{createScriptTarget.getWriter().toString()};
    }
    return createSql;
  }

  public String[] getDropSql() {
    if (dropSql == null) {
      final StringWriterScriptTargetOutput dropScriptTarget = new StringWriterScriptTargetOutput();
      final TargetDescriptor dropTargetDescriptor = new ScriptTargetDescriptor(dropScriptTarget);
      Map<String, Object> configurationProperties = new HashMap<>();
      configurationProperties.put("jakarta.persistence.schema-generation.scripts.action", "drop");
      configurationProperties.put("jakarta.persistence.schema-generation.scripts.drop-target", dropScriptTarget.getWriter());

      StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
          .applySettings(configurationProperties)
          .build();

      try {
          SchemaManagementTool schemaManagementTool = serviceRegistry.getService(SchemaManagementTool.class);
          SchemaDropper schemaDropper = schemaManagementTool.getSchemaDropper(configurationProperties);
          ExecutionOptions executionOptions = new ExecutionOptions() {
              @Override
              public boolean shouldManageNamespaces() {
                  return false;
              }

              @Override
              public Map<String, Object> getConfigurationValues() {
                  return serviceRegistry.getService(ConfigurationService.class).getSettings();
              }

              @Override
              public org.hibernate.tool.schema.spi.ExceptionHandler getExceptionHandler() {
                  return new org.hibernate.tool.schema.spi.ExceptionHandler() {
                      @Override
                      public void handleException(Exception exception) {
                          // no-op
                      }
                  };
              }

              @Override
              public org.hibernate.tool.schema.spi.SchemaFilter getSchemaFilter() {
                  return org.hibernate.tool.schema.spi.SchemaFilter.ALL;
              }
          };
          schemaDropper.doDrop(metadata, executionOptions, ContributableMatcher.ALL, new MetadataSourceDescriptor(), dropTargetDescriptor);
      } catch (Exception e) {
          throw new JbpmException("couldn't drop schema", e);
      } finally {
          StandardServiceRegistryBuilder.destroy(serviceRegistry);
      }
      dropSql = new String[]{dropScriptTarget.getWriter().toString()};
    }
    return dropSql;
  }

  public String[] getCleanSql() {
    if (cleanSql == null) {
      String catalog = (String) serviceRegistry.getService(ConfigurationService.class).getSettings().get("hibernate.default_catalog");
      String schema = (String) serviceRegistry.getService(ConfigurationService.class).getSettings().get("hibernate.default_schema");

      final StringWriterScriptTargetOutput dropScriptTarget = new StringWriterScriptTargetOutput();
      final TargetDescriptor dropTargetDescriptor = new ScriptTargetDescriptor(dropScriptTarget);
      Map<String, Object> dropConfigurationProperties = new HashMap<>();
      dropConfigurationProperties.put("jakarta.persistence.schema-generation.scripts.action", "drop");
      dropConfigurationProperties.put("jakarta.persistence.schema-generation.scripts.drop-target", dropScriptTarget.getWriter());

      StandardServiceRegistry dropServiceRegistry = new StandardServiceRegistryBuilder()
          .applySettings(dropConfigurationProperties)
          .build();

      try {
          SchemaManagementTool dropSchemaManagementTool = dropServiceRegistry.getService(SchemaManagementTool.class);
          SchemaDropper schemaDropper = dropSchemaManagementTool.getSchemaDropper(dropConfigurationProperties);
          ExecutionOptions dropExecutionOptions = new ExecutionOptions() {
              @Override
              public boolean shouldManageNamespaces() {
                  return false;
              }

              @Override
              public Map<String, Object> getConfigurationValues() {
                  return dropServiceRegistry.getService(ConfigurationService.class).getSettings();
              }

              @Override
              public org.hibernate.tool.schema.spi.ExceptionHandler getExceptionHandler() {
                  return new org.hibernate.tool.schema.spi.ExceptionHandler() {
                      @Override
                      public void handleException(Exception exception) {
                          // no-op
                      }
                  };
              }

              @Override
              public org.hibernate.tool.schema.spi.SchemaFilter getSchemaFilter() {
                  return org.hibernate.tool.schema.spi.SchemaFilter.ALL;
              }
          };
          schemaDropper.doDrop(metadata, dropExecutionOptions, ContributableMatcher.ALL, new MetadataSourceDescriptor(), dropTargetDescriptor);
      } catch (Exception e) {
          throw new JbpmException("couldn't drop schema", e);
      } finally {
          StandardServiceRegistryBuilder.destroy(dropServiceRegistry);
      }

      final StringWriterScriptTargetOutput createScriptTarget = new StringWriterScriptTargetOutput();
      final TargetDescriptor createTargetDescriptor = new ScriptTargetDescriptor(createScriptTarget);
      Map<String, Object> createConfigurationProperties = new HashMap<>();
      createConfigurationProperties.put("jakarta.persistence.schema-generation.scripts.action", "create");
      createConfigurationProperties.put("jakarta.persistence.schema-generation.scripts.create-target", createScriptTarget.getWriter());

      StandardServiceRegistry createServiceRegistry = new StandardServiceRegistryBuilder()
          .applySettings(createConfigurationProperties)
          .build();

      try {
          SchemaManagementTool createSchemaManagementTool = createServiceRegistry.getService(SchemaManagementTool.class);
          SchemaCreator schemaCreator = createSchemaManagementTool.getSchemaCreator(createConfigurationProperties);
          ExecutionOptions createExecutionOptions = new ExecutionOptions() {
              @Override
              public boolean shouldManageNamespaces() {
                  return false;
              }

              @Override
              public Map<String, Object> getConfigurationValues() {
                  return createServiceRegistry.getService(ConfigurationService.class).getSettings();
              }

              @Override
              public org.hibernate.tool.schema.spi.ExceptionHandler getExceptionHandler() {
                  return new org.hibernate.tool.schema.spi.ExceptionHandler() {
                      @Override
                      public void handleException(Exception exception) {
                          // no-op
                      }
                  };
              }

              @Override
              public org.hibernate.tool.schema.spi.SchemaFilter getSchemaFilter() {
                  return org.hibernate.tool.schema.spi.SchemaFilter.ALL;
              }
          };
          schemaCreator.doCreation(metadata, createExecutionOptions, ContributableMatcher.ALL, new MetadataSourceDescriptor(), createTargetDescriptor);
      } catch (Exception e) {
          throw new JbpmException("couldn't create schema", e);
      } finally {
          StandardServiceRegistryBuilder.destroy(createServiceRegistry);
      }

      List<String> deleteSql = new ArrayList<>();
      Iterator<Table> iterDelete = metadata.collectTableMappings().iterator();
      while (iterDelete.hasNext()) {
        Table table = iterDelete.next();
        if (table.isPhysicalTable()) {
          deleteSql.add("delete from " + table.getQualifiedTableName());
        }
      }

      List<String> cleanSqlList = new ArrayList<>();
      cleanSqlList.add(dropScriptTarget.getWriter().toString());
      cleanSqlList.addAll(deleteSql);
      cleanSqlList.add(createScriptTarget.getWriter().toString());

      cleanSql = cleanSqlList.toArray(new String[cleanSqlList.size()]);
    }
    return cleanSql;
  }

  public boolean hasJbpmTables() {
    return (getJbpmTables().size() > 0);
  }

  public List<String> getJbpmTables() {
    List<String> jbpmTableNames = new ArrayList<>();
    Iterator<Table> iter = metadata.collectTableMappings().iterator();
    while (iter.hasNext()) {
      Table table = iter.next();
      if (table.isPhysicalTable()) {
        jbpmTableNames.add(table.getName());
      }
    }
    return jbpmTableNames;
  }

  public Map<String, Integer> getJbpmTablesRecordCount() {
    Map<String, Integer> recordCounts = new HashMap<>();
    String sql = null;
    try {
      createConnection();
      String catalog = (String) serviceRegistry.getService(ConfigurationService.class).getSettings().get("hibernate.default_catalog");
      String schema = (String) serviceRegistry.getService(ConfigurationService.class).getSettings().get("hibernate.default_schema");
      
      for (String tableName : getJbpmTables()) {
        statement = connection.createStatement();
        sql = "SELECT COUNT(*) FROM " + tableName;
        ResultSet resultSet = statement.executeQuery(sql);
        resultSet.next();
        int count = resultSet.getInt(1);
        resultSet.close();
        statement.close();
        recordCounts.put(tableName, count);
      }
    } catch (SQLException e) {
      throw new JbpmException("couldn't execute sql '" + sql + "'", e);
    } finally {
      closeConnection();
    }
    return recordCounts;
  }

  public void dropSchema() {
    execute(getDropSql());
  }

  public void createSchema() {
    execute(getCreateSql());
  }

  public void cleanSchema() {
    if (getJbpmTables().size() > 0)
      execute(getCleanSql());
  }

  public void saveSqlScripts(String dir, String prefix) {
    try {
      new File(dir).mkdirs();
      saveSqlScript(dir + "/" + prefix + ".drop.sql", getDropSql());
      saveSqlScript(dir + "/" + prefix + ".create.sql", getCreateSql());
      saveSqlScript(dir + "/" + prefix + ".clean.sql", getCleanSql());

      String[] drop = getDropSql();
      String[] create = getCreateSql();
      String[] dropCreate = new String[drop.length + create.length];
      System.arraycopy(drop, 0, dropCreate, 0, drop.length);
      System.arraycopy(create, 0, dropCreate, drop.length, create.length);
      saveSqlScript(dir + "/" + prefix + ".drop.create.sql", dropCreate);
    } catch (IOException e) {
      throw new JbpmException("couldn't generate scripts", e);
    }
  }

  public static void main(String[] args) {
    if ((args == null) || (args.length == 0)) {
      syntax();
    } else if ("create".equalsIgnoreCase(args[0]) && args.length <= 3) {
      Object[] config = createConfiguration(args, 1);
      new JbpmSchema((Metadata) config[0], (ServiceRegistry) config[1]).createSchema();
    } else if ("drop".equalsIgnoreCase(args[0]) && args.length <= 3) {
      Object[] config = createConfiguration(args, 1);
      new JbpmSchema((Metadata) config[0], (ServiceRegistry) config[1]).dropSchema();
    } else if ("clean".equalsIgnoreCase(args[0]) && args.length <= 3) {
      Object[] config = createConfiguration(args, 1);
      new JbpmSchema((Metadata) config[0], (ServiceRegistry) config[1]).cleanSchema();
    } else if ("scripts".equalsIgnoreCase(args[0]) && args.length >= 3 && args.length <= 5) {
      Object[] config = createConfiguration(args, 3);
      new JbpmSchema((Metadata) config[0], (ServiceRegistry) config[1]).saveSqlScripts(args[1], args[2]);
    } else {
      syntax();
    }
  }

  private static void syntax() {
    System.err.println("syntax:");
    System.err.println("JbpmSchema create [<hibernate.cfg.xml> [<hibernate.properties>]]");
    System.err.println("JbpmSchema drop [<hibernate.cfg.xml> [<hibernate.properties>]]");
    System.err.println("JbpmSchema clean [<hibernate.cfg.xml> [<hibernate.properties>]]");
    System.err.println("JbpmSchema scripts <dir> <prefix> [<hibernate.cfg.xml> [<hibernate.properties>]]");
  }

  static Object[] createConfiguration(String[] args, int index) {
    String hibernateCfgXml = (args.length > index ? args[index] : "hibernate.cfg.xml");
    String hibernateProperties = (args.length > (index + 1) ? args[index + 1] : null);

    Properties properties = new Properties();
    try (InputStream inputStream = new FileInputStream(hibernateCfgXml)) {
      properties.loadFromXML(inputStream);
    } catch (IOException e) {
      throw new JbpmException("couldn't load hibernate configuration", e);
    }

    if (hibernateProperties != null) {
      try (InputStream inputStream = new FileInputStream(hibernateProperties)) {
        properties.load(inputStream);
      } catch (IOException e) {
        throw new JbpmException("couldn't load hibernate properties", e);
      }
    }

    ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
      .applySettings(properties)
      .build();

    Metadata metadata = new org.hibernate.boot.MetadataSources(serviceRegistry)
      .buildMetadata();

    return new Object[]{metadata, serviceRegistry};
  }

  void saveSqlScript(String fileName, String[] sql) throws FileNotFoundException {
    FileOutputStream fileOutputStream = new FileOutputStream(fileName);
    try {
      PrintStream printStream = new PrintStream(fileOutputStream);
      for (int i = 0; i < sql.length; i++) {
        printStream.println(sql[i] + getSqlDelimiter());
      }
    } finally {
      try {
        fileOutputStream.close();
      } catch (IOException e) {
        log.debug("failed to close file", e);
      }
    }
  }

  public void execute(String[] sqls) {
    String sql = null;
    boolean showSql = false;

    try {
      createConnection();
      statement = connection.createStatement();

      for (int i = 0; i < sqls.length; i++) {
        sql = sqls[i];

        if (showSql)
          log.debug(sql);
        statement.executeUpdate(sql);
      }

    } catch (SQLException e) {
      throw new JbpmException("couldn't execute sql '" + sql + "'", e);
    } finally {
      closeConnection();
    }
  }

  void closeConnection() {
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
        } else {
          log.warn("ServiceRegistry is null in closeConnection, cannot log SQL warnings via SqlExceptionHelper optimally.");
        }
        connectionProvider.closeConnection(connection);
      } catch (SQLException e) {
        log.debug("could not close jdbc connection", e);
      }
    }
  }

  void createConnection() throws SQLException {
    connection = this.connectionProvider.getConnection();
    if (!connection.getAutoCommit()) {
      connection.commit();
      connection.setAutoCommit(true);
    }
  }

  public Properties getProperties() {
    Properties properties = new Properties();
    properties.putAll(serviceRegistry.getService(ConfigurationService.class).getSettings());
    return properties;
  }

  static String sqlDelimiter = null;

  synchronized String getSqlDelimiter() {
    if (sqlDelimiter == null) {
      sqlDelimiter = getProperties().getProperty("jbpm.sql.delimiter", ";");
    }
    return sqlDelimiter;
  }

  private static final Log log = LogFactory.getLog(JbpmSchema.class);
}