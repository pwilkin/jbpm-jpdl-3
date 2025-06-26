package org.jbpm.persistence;

import java.io.Serializable;

import org.hibernate.SessionFactory;
import org.jbpm.JbpmConfiguration;

public interface PersistenceServiceFactory extends Serializable {

  PersistenceService openService();

  SessionFactory getSessionFactory();

  void createSchema();

  void dropSchema();

  void cleanSchema();

  void close();

  boolean isCurrentSessionEnabled();

  void setCurrentSessionEnabled(boolean isCurrentSessionEnabled);

}
