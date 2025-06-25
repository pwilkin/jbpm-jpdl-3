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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;
import org.hibernate.usertype.LoggableUserType; // For more detailed logging if needed, can remove if not used
import org.hibernate.type.IdentifierType; // For discriminator like functionality
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor; // For IdentifierType
import org.jbpm.context.def.Access;
import org.hibernate.engine.spi.Mapping; // Required for toColumnNullness


public class AccessType implements UserType, IdentifierType<Access>, Serializable {

  private static final long serialVersionUID = 1L;

  private static final int[] SQL_TYPES = { Types.VARCHAR };

  @Override
  public int[] sqlTypes() {
    return SQL_TYPES;
  }

  @Override
  public Class returnedClass() {
    return Access.class;
  }

  @Override
  public boolean equals(Object x, Object y) throws HibernateException {
    if (x == y) {
      return true;
    }
    if (x == null || y == null) {
      return false;
    }
    return x.equals(y);
  }

  // This is the Type.isEqual(Object, Object, SessionFactoryImplementor) method
  public boolean isEqual(Object x, Object y, org.hibernate.engine.spi.SessionFactoryImplementor factory) throws HibernateException {
    // The factory is not needed for this simple type's equality check.
    return equals(x, y); // Delegate to UserType's equals
  }

  // Method from Type interface
  public boolean isEqual(Object x, Object y) throws HibernateException {
    return equals(x, y); // Delegate to UserType's equals
  }

  // Method from Type interface
  public boolean isSame(Object x, Object y) throws HibernateException {
    // For simple immutable types, isSame can often delegate to equals.
    // isSame might be stricter in some contexts (e.g., checking if it's the exact same instance
    // in a managed session, but for a UserType like this, equality is usually sufficient).
    return equals(x, y);
  }

  @Override
  public int hashCode(Object x) throws HibernateException { // For UserType
    return x.hashCode();
  }

  // Method from Type interface (required by compiler if the above doesn't satisfy it due to UserType also having it)
  public int getHashCode(Object x, org.hibernate.engine.spi.SessionFactoryImplementor factory) throws HibernateException {
    // The factory is not needed for this simple type's hashcode.
    return hashCode(x); // Delegate to the UserType's hashCode
  }

  // Explicitly add Type's getHashCode(Object) to satisfy the compiler if UserType's hashCode isn't picked up.
  public int getHashCode(Object x) throws HibernateException {
     return x.hashCode();
  }

  @Override
  public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
      throws HibernateException, SQLException {
    String value = rs.getString(names[0]);
    if (rs.wasNull()) {
      return null;
    }
    return new Access(value);
  }

  // This is the nullSafeGet for the Type interface (which UserType and IdentifierType extend)
  // It expects a single column name. We'll delegate to the UserType version.
  public Object nullSafeGet(ResultSet rs, String name, SharedSessionContractImplementor session, Object owner)
      throws HibernateException, SQLException {
    return nullSafeGet(rs, new String[] { name }, session, owner);
  }

  @Override
  public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
      throws HibernateException, SQLException {
    if (value == null) {
      st.setNull(index, Types.VARCHAR);
    } else {
      st.setString(index, ((Access) value).toString());
    }
  }

  // Explicitly providing the 5-argument version for org.hibernate.type.Type interface
  public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session)
      throws HibernateException, SQLException {
    // Delegate to the 4-argument version, ignoring the 'settable' for this simple type.
    nullSafeSet(st, value, index, session);
  }

  @Override
  public Object deepCopy(Object value) throws HibernateException {
    // Access is immutable or effectively so (no setters that change its core value based on constructor)
    return value;
  }

  // This is the deepCopy method from Type interface, which UserType extends and the compiler is asking for.
  // For immutable types, it's common to return the value itself.
  // @Override // Not overriding from UserType directly, but from Type
  public Object deepCopy(Object value, org.hibernate.engine.spi.SessionFactoryImplementor factory) throws HibernateException {
    return value;
  }

  @Override
  public boolean isMutable() {
    // For an immutable type, this should always be false.
    return false;
  }

  // Method from Type interface (required by compiler)
  public boolean isModified(Object old, Object current, boolean[] settable, SharedSessionContractImplementor session) throws HibernateException {
    // For an immutable type, this should always be false.
    // The 'settable' array indicates which columns/properties are considered for modification checking.
    // Since AccessType is simple and immutable, we can just return false.
    return false;
  }

  // Method from Type interface (required by compiler)
  public boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) throws HibernateException {
      // For an immutable type, this should always be false.
      // The 'checkable' array (similar to 'settable' in isModified) indicates which columns/properties
      // are considered for dirty checking.
      return false;
  }

  // This version is also from the Type interface, and the one the compiler is currently asking for.
  @Override
  public boolean isDirty(Object old, Object current, SharedSessionContractImplementor session) throws HibernateException {
    // For an immutable type, this should always be false.
    return false;
  }

  // Method from Type interface (required by compiler)
  // @Override // Not overriding from UserType directly, but from Type
  public int compare(Object x, Object y) {
    if (x == y) {
      return 0;
    }
    if (x == null) {
      return -1; // nulls first
    }
    if (y == null) {
      return 1;  // nulls first
    }
    // Assuming Access objects have a meaningful toString() for comparison,
    // or a more specific method if available (e.g., getValue().compareTo())
    return ((Access) x).toString().compareTo(((Access) y).toString());
  }

  @Override
  public Serializable disassemble(Object value) throws HibernateException {
    return (Serializable) value;
  }

  // This is the assemble method from UserType.
  @Override
  public Object assemble(Serializable cached, Object owner) throws HibernateException {
    return cached;
  }

  // This is the assemble method from Type interface, which UserType extends and the compiler is asking for.
  @Override
  public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
      return cached; // For immutable types, cached is the already disassembled (and thus re-assemblable) form.
  }

  // This is the disassemble method from Type interface.
  @Override
  public Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
    return (Serializable) value;
  }

  // Method from UserType interface
  @Override
  public Object replace(Object original, Object target, Object owner) throws HibernateException {
    return original;
  }

  // Methods from Type interface (superclass of UserType)
  @Override
  public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, java.util.Map copyCache) throws HibernateException {
    return original; // For immutable types, returning original is generally safe.
  }

  @Override
  public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, java.util.Map copyCache, org.hibernate.type.ForeignKeyDirection foreignKeyDirection) throws HibernateException {
    return original; // For immutable types, returning original is generally safe.
  }

  // Methods from IdentifierType (and effectively replacing old DiscriminatorType methods)
  // Note: getName() is from UserType but often related to type registration
  public String getName() {
    return "access";
  }

  @Override
  public Access stringToObject(String xml) throws Exception {
    // In old DiscriminatorType, this returned Object. Now it's typed.
    // The old implementation returned the string itself, which is not an Access object.
    // Assuming the string 'xml' is the representation used in Access constructor.
    return new Access(xml);
  }

  // This method is NOT part of IdentifierType in Hibernate 5.4, so @Override was removed.
  // It might be a remnant from older Hibernate versions or specific internal usage.
  public String objectToSQLString(Access value, Dialect dialect) throws Exception {
    // This method implements IdentifierType.objectToSQLString
    if (value == null) {
        // As per IdentifierType contract (if it were part of it), it can throw Exception.
        // Or, handle null appropriately if dialect needs specific literal for null (e.g. "NULL").
        // For simplicity and to match old behavior of just quoting, throwing NPE if null.
        throw new NullPointerException("Value cannot be null for objectToSQLString, or dialect specific null handling is needed.");
    }
    return '\'' + value.toString() + '\'';
  }

  // fromStringValue was part of BasicType which ImmutableType extended. Not directly in UserType/IdentifierType.
  // stringToObject serves a similar purpose for IdentifierType.
  // public Object fromStringValue(String xml) {
  //   return new Access(xml); // Assuming this was the intent
  // }

  // toString(Object value) was from ImmutableType. Not directly in UserType.
  // Standard Object.toString() on Access object or null handling will be used.

  @Override
  public boolean[] toColumnNullness(Object value, Mapping mapping) {
    // AccessType maps to a single column.
    // If value is null, the column is null. Otherwise, it's not null.
    return new boolean[]{value == null};
  }

  @Override
  public org.hibernate.type.Type getSemiResolvedType(org.hibernate.engine.spi.SessionFactoryImplementor factory) {
    return this; // For simple types, the semi-resolved type is the type itself.
  }

  @Override
  public Object semiResolve(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
      // For simple types like this, the hydrated value is the fully resolved value.
      // No special semi-resolution step is needed.
      return value;
  }

  @Override
  public Object resolve(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
      // For simple types like this, the semi-resolved value is the fully resolved value.
      return value;
  }

  @Override
  public Object hydrate(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
    // For simple types, hydrate is often similar to nullSafeGet.
    // It extracts the value from the ResultSet without resolving entities/collections.
    String value = rs.getString(names[0]);
    if (rs.wasNull()) {
      return null;
    }
    return new Access(value);
  }

  @Override
  public void beforeAssemble(Serializable cached, SharedSessionContractImplementor session) {
      // For simple types, no specific action is needed before assembly.
  }

  // Method from Type interface
  // @Override // Not overriding from UserType directly, but from Type
  public String toLoggableString(Object value, org.hibernate.engine.spi.SessionFactoryImplementor factory) throws HibernateException {
    if (value == null) {
      return "null";
    }
    return value.toString();
  }
}