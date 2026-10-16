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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.hibernate.cfg.Configuration;
import org.jbpm.db.JbpmSchema;

/**
 * schema utilities for the identity tables. Since Hibernate 5 the schema generation lives in
 * {@link JbpmSchema}; this class only keeps the identity-specific entry points.
 */
public class IdentitySchema {

  private static final String IDENTITY_TABLE_PREFIX = "JBPM_ID_";

  Configuration configuration = null;
  Properties properties = null;
  JbpmSchema schema = null;

  public IdentitySchema(Configuration configuration) {
    this.configuration = configuration;
    this.properties = configuration.getProperties();
    this.schema = new JbpmSchema(configuration);
  }

  public String[] getCreateSql() {
    return schema.getCreateSql();
  }

  public String[] getDropSql() {
    return schema.getDropSql();
  }

  public String[] getCleanSql() {
    return schema.getCleanSql();
  }

  public boolean hasIdentityTables() {
    return (getIdentityTables().size()>0);
  }

  /** the mapped identity tables (Hibernate 3 version read them from the database metadata) */
  public List getIdentityTables() {
    List identityTableNames = new ArrayList();
    Iterator iter = schema.getJbpmTables().iterator();
    while (iter.hasNext()) {
      String tableName = (String) iter.next();
      if (tableName.toUpperCase().startsWith(IDENTITY_TABLE_PREFIX)) {
        identityTableNames.add(tableName);
      }
    }
    return identityTableNames;
  }

  public void dropSchema() {
    schema.dropSchema();
  }

  public void createSchema() {
    schema.createSchema();
  }

  public void cleanSchema() {
    schema.execute(getCleanSql());
  }

  public void saveSqlScripts(String dir, String prefix) {
    schema.saveSqlScripts(dir, prefix);
  }

  public static void main(String[] args) {
    if (args == null || args.length == 0) {
      syntax();
    } else if ("create".equalsIgnoreCase(args[0])) {
      new IdentitySchema(IdentitySessionFactory.createConfiguration()).createSchema();
    } else if ("drop".equalsIgnoreCase(args[0])) {
      new IdentitySchema(IdentitySessionFactory.createConfiguration()).dropSchema();
    } else if ("clean".equalsIgnoreCase(args[0])) {
      new IdentitySchema(IdentitySessionFactory.createConfiguration()).cleanSchema();
    } else if ("scripts".equalsIgnoreCase(args[0]) && args.length == 3) {
      new IdentitySchema(IdentitySessionFactory.createConfiguration()).saveSqlScripts(args[1], args[2]);
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

  public Properties getProperties() {
    return properties;
  }
}
