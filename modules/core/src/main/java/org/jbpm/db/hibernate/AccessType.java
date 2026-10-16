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
package org.jbpm.db.hibernate;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.jbpm.context.def.Access;

/**
 * stores {@link Access} as a varchar. Hibernate 4+ removed ImmutableType, so this is a UserType.
 */
public class AccessType implements UserType, Serializable {

  private static final long serialVersionUID = 1L;

  static final int[] SQLTYPES = new int[] { Types.VARCHAR };

  public int[] sqlTypes() { return SQLTYPES; }
  public Class returnedClass() { return Access.class; }
  public boolean equals(Object x, Object y) { return x == y || (x != null && x.equals(y)); }
  public int hashCode(Object x) { return x.hashCode(); }
  public Object deepCopy(Object value) { return value; }
  public boolean isMutable() { return false; }
  public Serializable disassemble(Object value) { return (Serializable) value; }
  public Object assemble(Serializable cached, Object owner) { return cached; }
  public Object replace(Object original, Object target, Object owner) { return original; }

  public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
      throws HibernateException, SQLException {
    // same as Hibernate 3 ImmutableType.get(): a null column still yields an Access (its default)
    return new Access(rs.getString(names[0]));
  }

  public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
      throws HibernateException, SQLException {
    if (value == null) {
      st.setNull(index, Types.VARCHAR);
    } else {
      st.setString(index, value.toString());
    }
  }
}
