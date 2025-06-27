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
package org.jbpm.persistence.jta;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmException;
import org.jbpm.persistence.PersistenceService;

import org.jbpm.svc.Service;

public class JtaDbPersistenceServiceFactory extends org.jbpm.persistence.db.DbPersistenceServiceFactory {

  private static final long serialVersionUID = 1L;

  
  protected boolean isCurrentSessionEnabled = true;
  protected String userTransactionName = "java:comp/UserTransaction";

  public JtaDbPersistenceServiceFactory(JbpmConfiguration jbpmConfiguration) {
    super(jbpmConfiguration);
  }

  public PersistenceService openService() {
    return new JtaDbPersistenceService(this);
  }

  public SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      sessionFactory = JbpmConfiguration.getHibernateConfiguration().buildSessionFactory();
    }
    return sessionFactory;
  }

  public void close() {
    if (sessionFactory != null) {
      sessionFactory.close();
      sessionFactory = null;
    }
  }

  public UserTransaction getUserTransaction() {
    try {
      return (UserTransaction) new InitialContext().lookup(userTransactionName);
    } catch (NamingException e) {
      throw new JbpmException("couldn't find user transaction in jndi at '" + userTransactionName + "'", e);
    }
  }

  public boolean isCurrentSessionEnabled() {
    return isCurrentSessionEnabled;
  }

  public void setCurrentSessionEnabled(boolean isCurrentSessionEnabled) {
    this.isCurrentSessionEnabled = isCurrentSessionEnabled;
  }

  public String getUserTransactionName() {
    return userTransactionName;
  }

  public void setUserTransactionName(String userTransactionName) {
    this.userTransactionName = userTransactionName;
  }

  private static final Log log = LogFactory.getLog(JtaDbPersistenceServiceFactory.class);
}