package org.jbpm.db.compatibility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.dialect.Dialect;

import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.engine.config.spi.ConfigurationService;
import java.util.Collections;
import java.util.EnumSet;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToWriter;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.jbpm.db.MetadataSourceDescriptor;
import org.jbpm.db.ScriptTargetDescriptor;
import org.jbpm.db.StringWriterScriptTargetOutput;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.SchemaFilter;

/**
 * This is a modified version of the hibernate tools schema update.
 * The modification is to support saving of the update script to a file.
 *
 * @author Christoph Sturm
 * @author Koen Aers
 */
public class JbpmSchemaUpdate {

	private static final Log log = LogFactory.getLog(JbpmSchemaUpdate.class);
	private ConnectionProvider connectionProvider; // Corrected: Use short name, relies on correct import
	private Configuration configuration;
	private Dialect dialect;
    private List exceptions;
    private ServiceRegistry serviceRegistry;
    private org.hibernate.boot.Metadata metadata;

    public JbpmSchemaUpdate(org.hibernate.boot.Metadata metadata, ServiceRegistry serviceRegistry) throws HibernateException {
        this.metadata = metadata;
        this.serviceRegistry = serviceRegistry;
        this.dialect = serviceRegistry.getService(org.hibernate.engine.jdbc.env.spi.JdbcEnvironment.class).getDialect();
        this.connectionProvider = this.serviceRegistry.getService(org.hibernate.engine.jdbc.connections.spi.ConnectionProvider.class);
        exceptions = new ArrayList();
    }


	
	public static void main(String[] args) {
		try {
			Configuration cfg = new Configuration();

			boolean script = true;
			// If true then execute db updates, otherwise just generate and display updates
			boolean doUpdate = true;
			String propFile = null;
			
			File out = null;

			for ( int i=0; i<args.length; i++ )  {
				if( args[i].startsWith("--") ) {
					if( args[i].equals("--quiet") ) {
						script = false;
					}
					else if( args[i].startsWith("--properties=") ) {
						propFile = args[i].substring(13);
					}
					else if ( args[i].startsWith("--config=") ) {
						cfg.configure( args[i].substring(9) );
					}
					else if ( args[i].startsWith("--text") ) {
						doUpdate = false;
					}
					
					else if (args[i].startsWith("--output=")) {
						out = new File(args[i].substring(9));
					}
				}
					else {
					cfg.addFile(args[i]);
				}

			}
			
			if (propFile!=null) {
				Properties props = new Properties();
				props.putAll( cfg.getProperties() );
				props.load( new FileInputStream(propFile) );
				cfg.setProperties(props);
			}

			            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(cfg.getProperties()).build();
            org.hibernate.boot.Metadata metadata = new org.hibernate.boot.MetadataSources(serviceRegistry).buildMetadata();
            new JbpmSchemaUpdate(metadata, serviceRegistry).execute(script, doUpdate, out);
		}
		catch (Exception e) {
			log.error( "Error running schema update", e );
		}
	}

	/**
	 * Execute the schema updates
	 * @param script print all DDL to the console
	 */
	public void execute(boolean script, boolean doUpdate, File out) {

		log.info("Running hbm2ddl schema update");

		exceptions.clear();

		SchemaManagementTool schemaManagementTool = serviceRegistry.getService(SchemaManagementTool.class);

		// Generate drop SQL
		final StringWriterScriptTargetOutput dropScriptTarget = new StringWriterScriptTargetOutput();
		final TargetDescriptor dropTargetDescriptor = new ScriptTargetDescriptor(dropScriptTarget);
		Map<String, Object> dropConfigurationProperties = new HashMap<>();
		dropConfigurationProperties.put("jakarta.persistence.schema-generation.scripts.action", "drop");
		dropConfigurationProperties.put("jakarta.persistence.schema-generation.scripts.drop-target", dropScriptTarget.getWriter());

		SchemaDropper schemaDropper = schemaManagementTool.getSchemaDropper(dropConfigurationProperties);
		ExecutionOptions dropExecutionOptions = new ExecutionOptions() {
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
		schemaDropper.doDrop(metadata, dropExecutionOptions, ContributableMatcher.ALL, new MetadataSourceDescriptor(), dropTargetDescriptor);
		String[] dropSql = new String[]{dropScriptTarget.getWriter().toString()};

		// Generate create SQL
		final StringWriterScriptTargetOutput createScriptTarget = new StringWriterScriptTargetOutput();
		final TargetDescriptor createTargetDescriptor = new ScriptTargetDescriptor(createScriptTarget);
		Map<String, Object> createConfigurationProperties = new HashMap<>();
		createConfigurationProperties.put("jakarta.persistence.schema-generation.scripts.action", "create");
		createConfigurationProperties.put("jakarta.persistence.schema-generation.scripts.create-target", createScriptTarget.getWriter());

		SchemaCreator schemaCreator = schemaManagementTool.getSchemaCreator(createConfigurationProperties);
		ExecutionOptions createExecutionOptions = new ExecutionOptions() {
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
		schemaCreator.doCreation(metadata, createExecutionOptions, ContributableMatcher.ALL, new MetadataSourceDescriptor(), createTargetDescriptor);
		String[] createSql = new String[]{createScriptTarget.getWriter().toString()};

		if (doUpdate) {
			Connection connection = null;
			Statement statement = null;
			try {
				connection = connectionProvider.getConnection();
				statement = connection.createStatement();
				for (String sql : dropSql) {
					if (script) log.info(sql);
					statement.executeUpdate(sql);
				}
				for (String sql : createSql) {
					if (script) log.info(sql);
					statement.executeUpdate(sql);
				}
			} catch (SQLException e) {
				exceptions.add(e);
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
						connectionProvider.closeConnection(connection);
					} catch (SQLException e) {
						log.debug("could not close jdbc connection", e);
					}
				}
			}
		}

		if (script && out != null) {
			try (FileWriter writer = new FileWriter(out)) {
				for (String sql : dropSql) {
					writer.write(sql + ";\n");
				}
				for (String sql : createSql) {
					writer.write(sql + ";\n");
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		log.info("schema update complete");
	}

    /**
     * Returns a List of all Exceptions which occured during the export.
     * @return A List containig the Exceptions occured during the export
     */
    public List getExceptions() {
        return exceptions;
    }
    
}