package org.jbpm.ant;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
// import org.hibernate.impl.SessionFactoryImpl; // Removed
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.Services;

public class ShutDownHsqldb extends Task {

  public void execute() throws BuildException {
    Connection connection = null;
    JbpmConfiguration jbpmConfiguration = AntHelper.getJbpmConfiguration(null);
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      DbPersistenceServiceFactory dbPersistenceServiceFactory = (DbPersistenceServiceFactory) jbpmContext.getServiceFactory(Services.SERVICENAME_PERSISTENCE);
      // Assumes DbPersistenceServiceFactory will have a getServiceRegistry() method after its refactoring for Hibernate 4
      Configuration hibernateConfiguration = JbpmConfiguration.getHibernateConfiguration();
      ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
        hibernateConfiguration.getProperties()).build();
      ConnectionProvider connectionProvider = serviceRegistry.getService(ConnectionProvider.class);
      connection = connectionProvider.getConnection();
      Statement statement = connection.createStatement();
      log("shutting down database");
      statement.executeUpdate("SHUTDOWN");
      connection.close();
      
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      jbpmContext.close();
    }
  }

}
