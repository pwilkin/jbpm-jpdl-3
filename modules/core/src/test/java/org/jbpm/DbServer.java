package org.jbpm;

import org.hibernate.cfg.Configuration;
import org.hsqldb.Server;

/**
 * use this in combination with the HQL editor of the hibernate plugin.
 */
public class DbServer {

  public static void main(String[] args) {
    Configuration configuration = new Configuration();
    configuration.configure();

    Server server = new Server();
    server.setSilent(false);
    server.setDatabaseName(0, "jbpm");
    server.setDatabasePath(0, "mem:jbpm");
    server.setPort(9001);
    server.start();

    configuration.setProperty("hibernate.connection.url", "jdbc:hsqldb:hsql://localhost:9001/jbpm");
    configuration.buildSessionFactory().getSchemaManager().exportMappedObjects(true);
  }
}
