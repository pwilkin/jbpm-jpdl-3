package org.jbpm.db.compatibility;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.jbpm.db.hibernate.HibernateHelper;

/**
 * Schema update that can also save the update script to a file.
 * Originally a modified copy of the Hibernate 3 SchemaUpdate; since Hibernate 5 the stock
 * SchemaUpdate writes scripts itself, so this only adapts the old jBPM entry points to it.
 *
 * @author Christoph Sturm
 * @author Koen Aers
 */
public class JbpmSchemaUpdate {

	private static final Log log = LogFactory.getLog(JbpmSchemaUpdate.class);

	private Configuration configuration;
	private List exceptions = new ArrayList();

	public JbpmSchemaUpdate(Configuration cfg) throws HibernateException {
		this.configuration = cfg;
	}

	public JbpmSchemaUpdate(Configuration cfg, Properties connectionProperties) throws HibernateException {
		this.configuration = cfg;
		cfg.addProperties(connectionProperties);
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

			new JbpmSchemaUpdate(cfg).execute(script, doUpdate, out);
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
		try {
			EnumSet<TargetType> targets = EnumSet.noneOf(TargetType.class);
			if (script) {
				targets.add(TargetType.STDOUT);
			}
			if (doUpdate) {
				targets.add(TargetType.DATABASE);
			}
			SchemaUpdate schemaUpdate = new SchemaUpdate();
			if (script && out != null) {
				log.info("writing update script to file : " + out.getAbsolutePath());
				schemaUpdate.setOutputFile(out.getAbsolutePath());
				schemaUpdate.setDelimiter(";");
				targets.add(TargetType.SCRIPT);
			}
			schemaUpdate.execute(targets, HibernateHelper.buildMetadata(configuration));
			exceptions.addAll(schemaUpdate.getExceptions());
			log.info("schema update complete");
		}
		catch (Exception e) {
			exceptions.add(e);
			log.error("could not complete schema update", e);
		}
	}

	/**
	 * Returns a List of all Exceptions which occured during the export.
	 * @return A List containig the Exceptions occured during the export
	 */
	public List getExceptions() {
		return exceptions;
	}
}
