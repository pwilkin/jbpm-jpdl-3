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

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.security.Permission;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.type.StandardBasicTypes;
import org.hibernate.HibernateException;


import org.hibernate.usertype.CompositeUserType;

public class PermissionUserType implements CompositeUserType {

  @Override
  public Object instantiate(org.hibernate.metamodel.spi.ValueAccess valueAccess, org.hibernate.engine.spi.SessionFactoryImplementor sessionFactory) {
    return new PermissionEmbeddable();
  }

  

  @Override
  public Class<?> embeddable() {
    return PermissionEmbeddable.class;
  }

  public static class PermissionEmbeddable implements Serializable {
    public String className;
    public String name;
    public String actions;
  }

  private static final String[] PROPERTY_NAMES = new String[]{"class", "name", "actions"};
  public String[] getPropertyNames() {
    return PROPERTY_NAMES;
  }

  

  

  public Object getPropertyValue(Object component, int property) throws HibernateException {
    Permission permission = (Permission) component;
    if (property==0) {
      return permission.getClass().getName();
    } else if (property==1) { 
      return permission.getName();
    } else if (property==2) { 
      return permission.getActions();
    } else {
      throw new IllegalArgumentException("illegal permission property '"+property+"'");
    }
  }

  public void setPropertyValue(Object arg0, int arg1, Object arg2) throws HibernateException {
    throw new UnsupportedOperationException("setting properties on a permission is not allowed");
  }

  public Class returnedClass() {
    return Permission.class;
  }

  public boolean equals(Object left, Object right) throws HibernateException {
    return left.equals(right);
  }

  public int hashCode(Object permission) throws HibernateException {
    return permission.hashCode();
  }
  
  private static final Class[] NAME_ACTIOS_CONSTRUCTOR_PARAMETER_TYPES = new Class[]{String.class, String.class};
  public Object nullSafeGet(ResultSet resultSet, String[] names, Object owner) throws SQLException {
    PermissionEmbeddable embeddable = new PermissionEmbeddable();
    embeddable.className = resultSet.getString(names[0]);
    embeddable.name = resultSet.getString(names[1]);
    embeddable.actions = resultSet.getString(names[2]);
    
    try {
      // TODO optimize performance by caching the constructors
      Class permissionClass = PermissionUserType.class.getClassLoader().loadClass(embeddable.className);
      Constructor constructor = permissionClass.getDeclaredConstructor(NAME_ACTIOS_CONSTRUCTOR_PARAMETER_TYPES);
      return constructor.newInstance(new Object[]{embeddable.name, embeddable.actions});
    } catch (Exception e) {
      throw new HibernateException("couldn't create permission from database record ["+embeddable.className+"|"+embeddable.name+"|"+embeddable.actions+"].  Does the permission class have a (String name,String actions) constructor ?", e);
    }
  }

  public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index) throws HibernateException, SQLException {
    Permission permission = (Permission) value;
    preparedStatement.setString(index, permission.getClass().getName());
    preparedStatement.setString(index+1, permission.getName());
    preparedStatement.setString(index+2, permission.getActions());
  }

  public Object deepCopy(Object permission) throws HibernateException {
    return permission;
  }

  public boolean isMutable() {
    return false;
  }

  public Serializable disassemble(Object value) throws HibernateException {
    Permission permission = (Permission) value;
    PermissionEmbeddable embeddable = new PermissionEmbeddable();
    embeddable.className = permission.getClass().getName();
    embeddable.name = permission.getName();
    embeddable.actions = permission.getActions();
    return embeddable;
  }

  public Object assemble(Serializable cached, Object owner) throws HibernateException {
    PermissionEmbeddable embeddable = (PermissionEmbeddable) cached;
    try {
      Class permissionClass = PermissionUserType.class.getClassLoader().loadClass(embeddable.className);
      Constructor constructor = permissionClass.getDeclaredConstructor(NAME_ACTIOS_CONSTRUCTOR_PARAMETER_TYPES);
      return constructor.newInstance(new Object[]{embeddable.name, embeddable.actions});
    } catch (Exception e) {
      throw new HibernateException("couldn't create permission from cached record ["+embeddable.className+"|"+embeddable.name+"|"+embeddable.actions+"].  Does the permission class have a (String name,String actions) constructor ?", e);
    }
  }

  public Object replace(Object original, Object target, Object owner) throws HibernateException {
    return deepCopy(original);
  }
}
