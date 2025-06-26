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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.spi.SchemaUpdate;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import java.util.Collections;

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

		SchemaUpdate schemaUpdate = new SchemaUpdate();

		if (doUpdate) {
			schemaUpdate.execute(new TargetDescriptor() {
				@Override
				public void accept(String command) {
					if (script) {
						log.info(command);
					}
					Connection connection = null;
					Statement statement = null;
					try {
						connection = connectionProvider.getConnection();
						statement = connection.createStatement();
						statement.executeUpdate(command);
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
			}, metadata, serviceRegistry, ContributableMatcher.ALL);
		}

		if (script) {
			try (FileWriter writer = new FileWriter(out)) {
				schemaUpdate.execute(new TargetDescriptor() {
					@Override
					public void accept(String command) {
						try {
							writer.write(command + ";\n");
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}, metadata, serviceRegistry, ContributableMatcher.ALL);
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