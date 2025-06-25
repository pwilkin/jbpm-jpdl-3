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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
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
import org.hibernate.cfg.Configuration;
// import org.hibernate.cfg.Settings; // To be replaced by ServiceRegistry usage
import org.hibernate.service.ServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
// import org.hibernate.service.jdbc.connections.spi.ConnectionProviderInitiator; // Test import removed
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory; // Added import
// import org.hibernate.engine.Mapping; // To be replaced or accessed differently
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.JdbcServices; // Added for SqlExceptionHelper
// import org.hibernate.internal.util.JDBCExceptionReporter; // Ensured removed
import org.jbpm.JbpmException;

/**
 * utilities for the jBPM database schema.
 */
public class JbpmSchema implements Serializable
{

  private static final long serialVersionUID = 1L;

  Configuration configuration = null;
  // Settings settings; // Replaced by serviceRegistry
  ServiceRegistry serviceRegistry;
  // Mapping mapping = null; // To be handled differently
  String[] createSql = null;
  String[] dropSql = null;
  String[] cleanSql = null;

  private Dialect dialect; // Added field declaration
  ConnectionProvider connectionProvider = null; // Corrected: Use short name, relies on correct import
  Connection connection = null;
  Statement statement = null;

  public JbpmSchema(Configuration configuration)
  {
    this.configuration = configuration;
    StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
            .applySettings(configuration.getProperties());
    this.serviceRegistry = registryBuilder.build();
    // Initialize dialect and connectionProvider using serviceRegistry
    // this.dialect = serviceRegistry.getService(org.hibernate.engine.jdbc.dialect.spi.DialectFactory.class).buildDialect(configuration.getProperties(), null); // More complex initialization might be needed
    this.dialect = serviceRegistry.getService(org.hibernate.engine.jdbc.dialect.spi.DialectFactory.class).buildDialect(configuration.getProperties(), null);
    this.connectionProvider = serviceRegistry.getService(org.hibernate.engine.jdbc.connections.spi.ConnectionProvider.class);
    // this.mapping = configuration.buildMapping(); // Commented out for now
  }

  public String[] getCreateSql()
  {
    // TODO: Hibernate 5 - configuration.generateSchemaCreationScript is removed.
    // Need to use MetadataSources -> Metadata -> SchemaExport
    log.warn("JbpmSchema.getCreateSql() is disabled for Hibernate 5 migration.");
    if (createSql == null) {
        createSql = new String[0]; // Return empty script
    }
    // if (createSql == null)
    // {
    //   createSql = configuration.generateSchemaCreationScript(this.dialect);
    // }
    return createSql;
  }

  public String[] getDropSql()
  {
    // TODO: Hibernate 5 - configuration.generateDropSchemaScript is removed.
    // Need to use MetadataSources -> Metadata -> SchemaExport
    log.warn("JbpmSchema.getDropSql() is disabled for Hibernate 5 migration.");
    if (dropSql == null) {
        dropSql = new String[0]; // Return empty script
    }
    // if (dropSql == null)
    // {
    //   dropSql = configuration.generateDropSchemaScript(this.dialect);
    // }
    return dropSql;
  }

  public String[] getCleanSql()
  {
    // TODO: Hibernate 5 - This method needs complete rewrite due to changes in Configuration API for table/foreign key iteration
    // and schema export logic.
    log.warn("JbpmSchema.getCleanSql() is disabled for Hibernate 5 migration and will return an empty script.");
    if (cleanSql == null) {
        cleanSql = new String[0]; // Return empty script
    }
    // if (cleanSql == null)
    // {
    //   // new SchemaExport(configuration); // SchemaExport might need ServiceRegistry too
    //
    //   Dialect currentDialect = this.dialect; // Use the initialized dialect
    //   String catalog = configuration.getProperty("hibernate.default_catalog");
    //   String schema = configuration.getProperty("hibernate.default_schema");
    //
    //   // loop over all foreign key constraints - THIS PART IS PROBLEMATIC and commented out for now
    //   List dropForeignKeysSql = new ArrayList();
    //   List createForeignKeysSql = new ArrayList();
    //   /*
    //   Iterator iter = configuration.getTableMappings();
    //   while (iter.hasNext())
    //   {
    //     Table table = (Table)iter.next();
    //     if (table.isPhysicalTable())
    //     {
    //       Iterator subIter = table.getForeignKeyIterator();
    //       while (subIter.hasNext())
    //       {
    //         ForeignKey fk = (ForeignKey)subIter.next();
    //
    //         if (fk.isPhysicalConstraint())
    //         {
    //           // collect the drop foreign key constraint sql
    //           String sqlDropString = fk.sqlDropString(currentDialect, catalog, schema);
    //           dropForeignKeysSql.add(sqlDropString);
    //
    //           // and collect the create foreign key constraint sql
    //           // String sqlCreateString = fk.sqlCreateString(currentDialect, mapping, catalog, schema); // this.mapping is an issue
    //           // createForeignKeysSql.add(sqlCreateString);
    //         }
    //       }
    //     }
    //   }
    //   */
    //
    //   List deleteSql = new ArrayList();
    //   Iterator iter = configuration.getTableMappings(); // This might still be an issue if table mappings changed structure
    //   while (iter.hasNext())
    //   {
    //     Table table = (Table)iter.next();
    //     deleteSql.add("delete from " + table.getQualifiedName(currentDialect, catalog, schema) );
    //   }
    //
    //   // glue
    //   // - drop foreign key constraints (commented out)
    //   // - delete contents of all tables
    //   // - create foreign key constraints (commented out)
    //   // together to form the clean script
    //   List cleanSqlList = new ArrayList();
    //   // cleanSqlList.addAll(dropForeignKeysSql);
    //   cleanSqlList.addAll(deleteSql);
    //   // cleanSqlList.addAll(createForeignKeysSql);
    //
    //   if (cleanSqlList.isEmpty()) {
    //     // If only foreign key logic was present and now commented, provide a placeholder or warning
    //     log.warn("Clean SQL script generation might be incomplete due to commented out foreign key logic.");
    //     cleanSql = new String[0]; // Empty script
    //   } else {
    //     cleanSql = (String[])cleanSqlList.toArray(new String[cleanSqlList.size()]);
    //   }
    // }
    return cleanSql;
  }

  public boolean hasJbpmTables()
  {
    return (getJbpmTables().size() > 0);
  }

  public List getJbpmTables()
  {
    // TODO: Hibernate 5 - configuration.getTableMappings() removed. Need to use Metadata.
    log.warn("JbpmSchema.getJbpmTables() is disabled for Hibernate 5 migration.");
    List jbpmTableNames = new ArrayList();
    // Iterator iter = configuration.getTableMappings();
    // while (iter.hasNext())
    // {
    //   Table table = (Table)iter.next();
    //   if (table.isPhysicalTable())
    //   {
    //     jbpmTableNames.add(table.getName());
    //   }
    // }
    return jbpmTableNames;
  }
  
  public Map getJbpmTablesRecordCount() {
    Map recordCounts = new HashMap();
    
    String sql = null;
    
    try
    {
      Iterator iter = getJbpmTables().iterator();

      createConnection();
      while (iter.hasNext()) {
        statement = connection.createStatement();
        String tableName = (String) iter.next();
        sql = "SELECT COUNT(*) FROM "+tableName;
        ResultSet resultSet = statement.executeQuery(sql);
        resultSet.next();
        int count = resultSet.getInt(1);
        resultSet.close();
        statement.close();
        recordCounts.put(tableName, count);
      }
    }
    catch (SQLException e)
    {
      throw new JbpmException("couldn't execute sql '" + sql + "'", e);
    }
    finally
    {
      closeConnection();
    }

    return recordCounts;
  }

  public void dropSchema()
  {
    execute(getDropSql());
  }

  public void createSchema()
  {
    execute(getCreateSql());
  }

  public void cleanSchema()
  {
    if (getJbpmTables().size() > 0)
      execute(getCleanSql());
  }

  public void saveSqlScripts(String dir, String prefix)
  {
    try
    {
      new File(dir).mkdirs();
      saveSqlScript(dir + "/" + prefix + ".drop.sql", getDropSql());
      saveSqlScript(dir + "/" + prefix + ".create.sql", getCreateSql());
      saveSqlScript(dir + "/" + prefix + ".clean.sql", getCleanSql());
      // TODO: Hibernate 5 - new SchemaExport(configuration) is removed. Needs rewrite using Metadata.
      log.warn("JbpmSchema.saveSqlScripts() - SchemaExport part is disabled for Hibernate 5 migration.");
      // new SchemaExport(configuration).setDelimiter(getSqlDelimiter()).setOutputFile(dir + "/" + prefix + ".drop.create.sql").create(true, false);
    }
    catch (IOException e)
    {
      throw new JbpmException("couldn't generate scripts", e);
    }
  }

  public static void main(String[] args)
  {
    if ((args == null) || (args.length == 0))
    {
      syntax();
    }
    else if ("create".equalsIgnoreCase(args[0]) && args.length <= 3)
    {
      Configuration configuration = createConfiguration(args, 1);
      new JbpmSchema(configuration).createSchema();
    }
    else if ("drop".equalsIgnoreCase(args[0]) && args.length <= 3)
    {
      Configuration configuration = createConfiguration(args, 1);
      new JbpmSchema(configuration).dropSchema();
    }
    else if ("clean".equalsIgnoreCase(args[0]) && args.length <= 3)
    {
      Configuration configuration = createConfiguration(args, 1);
      new JbpmSchema(configuration).cleanSchema();
    }
    else if ("scripts".equalsIgnoreCase(args[0]) && args.length >= 3 && args.length <= 5)
    {
      Configuration configuration = createConfiguration(args, 3);
      new JbpmSchema(configuration).saveSqlScripts(args[1], args[2]);
    }
    else
    {
      syntax();
    }
  }

  private static void syntax()
  {
    System.err.println("syntax:");
    System.err.println("JbpmSchema create [<hibernate.cfg.xml> [<hibernate.properties>]]");
    System.err.println("JbpmSchema drop [<hibernate.cfg.xml> [<hibernate.properties>]]");
    System.err.println("JbpmSchema clean [<hibernate.cfg.xml> [<hibernate.properties>]]");
    System.err.println("JbpmSchema scripts <dir> <prefix> [<hibernate.cfg.xml> [<hibernate.properties>]]");
  }

  static Configuration createConfiguration(String[] args, int index)
  {
    String hibernateCfgXml = (args.length > index ? args[index] : "hibernate.cfg.xml");
    String hibernateProperties = (args.length > (index + 1) ? args[index + 1] : null);

    Configuration configuration = new Configuration();
    configuration.configure(new File(hibernateCfgXml));
    if (hibernateProperties != null)
    {
      try
      {
        Properties properties = new Properties();
        InputStream inputStream = new FileInputStream(hibernateProperties);
        properties.load(inputStream);
        configuration.setProperties(properties);
      }
      catch (IOException e)
      {
        throw new JbpmException("couldn't load hibernate configuration", e);
      }
    }

    return configuration;
  }

  void saveSqlScript(String fileName, String[] sql) throws FileNotFoundException
  {
    FileOutputStream fileOutputStream = new FileOutputStream(fileName);
    try
    {
      PrintStream printStream = new PrintStream(fileOutputStream);
      for (int i = 0; i < sql.length; i++)
      {
        printStream.println(sql[i] + getSqlDelimiter());
      }
    }
    finally
    {
      try
      {
        fileOutputStream.close();
      }
      catch (IOException e)
      {
        log.debug("failed to close file", e);
      }
    }
  }

  public void execute(String[] sqls)
  {
    String sql = null;
    boolean showSql = false;

    try
    {
      createConnection();
      statement = connection.createStatement();

      for (int i = 0; i < sqls.length; i++)
      {
        sql = sqls[i];

        if (showSql)
          log.debug(sql);
        statement.executeUpdate(sql);
      }

    }
    catch (SQLException e)
    {
      throw new JbpmException("couldn't execute sql '" + sql + "'", e);
    }
    finally
    {
      closeConnection();
    }
  }

  void closeConnection()
  {
    if (statement != null)
    {
      try
      {
        statement.close();
      }
      catch (SQLException e)
      {
        log.debug("could not close jdbc statement", e);
      }
    }
    if (connection != null)
    {
      try
      {
        // In H5, SqlExceptionHelper is typically accessed via JdbcServices
        if (serviceRegistry != null) {
            serviceRegistry.getService(JdbcServices.class).getSqlExceptionHelper().logAndClearWarnings(connection);
        } else {
            // Fallback or log warning if serviceRegistry is unexpectedly null
            log.warn("ServiceRegistry is null in closeConnection, cannot log SQL warnings via SqlExceptionHelper optimally.");
            // As a last resort, direct clearWarnings, though not ideal.
            connection.clearWarnings();
        }
        // connection.clearWarnings(); // logAndClearWarnings should handle this
        connectionProvider.closeConnection(connection);
        // connectionProvider.close(); // Closing the provider itself here might be too aggressive if it's shared or managed by ServiceRegistry lifecycle.
        // The ServiceRegistry that created this provider should manage its lifecycle.
        // If this JbpmSchema instance's ServiceRegistry is self-managed and being destroyed, then provider.close() might be okay.
        // For now, let's rely on ServiceRegistry.destroy(this.serviceRegistry) to handle it.
      }
      catch (SQLException e)
      {
        log.debug("could not close jdbc connection", e);
      }
    }
  }

  void createConnection() throws SQLException
  {
    // connectionProvider is now initialized in the constructor via ServiceRegistry
    // connectionProvider = settings.getConnectionProvider();
    connection = this.connectionProvider.getConnection();
    if (!connection.getAutoCommit())
    {
      connection.commit();
      connection.setAutoCommit(true);
    }
  }

  public Properties getProperties()
  {
    return configuration.getProperties();
  }

  // sql delimiter ////////////////////////////////////////////////////////////

  static String sqlDelimiter = null;

  synchronized String getSqlDelimiter()
  {
    if (sqlDelimiter == null)
    {
      sqlDelimiter = getProperties().getProperty("jbpm.sql.delimiter", ";");
    }
    return sqlDelimiter;
  }

  // logger ///////////////////////////////////////////////////////////////////

  private static final Log log = LogFactory.getLog(JbpmSchema.class);
}
