package org.jbpm.db.hibernate;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.jbpm.JbpmException;

public class StringMax implements UserType, org.hibernate.usertype.ParameterizedType {

  private static final long serialVersionUID = 1L;

  int length = 4000;

  private static final int[] SQL_TYPES = { Types.VARCHAR };

  public int getSqlType() {
    return Types.VARCHAR;
  }

  public Class returnedClass() {
    return String.class;
  }

  public boolean equals(Object x, Object y) throws HibernateException {
    if (x == y) {
      return true;
    }
    if (x == null || y == null) {
      return false;
    }
    return x.equals(y);
  }

  public int hashCode(Object x) throws HibernateException {
    return x.hashCode();
  }

  public Object nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
    String result = rs.getString(position);
    return result;
  }

  public void nullSafeSet(PreparedStatement st, Object value, int position, SharedSessionContractImplementor session) throws HibernateException, SQLException {
    String stringValue = (String) value;
    if ( (stringValue!=null)
         && (stringValue.length()>length)
       ) {
      stringValue = stringValue.substring(0, length);
    }
    st.setString(position, stringValue);
  }

  public Object deepCopy(Object value) throws HibernateException {
    return value;
  }

  public boolean isMutable() {
    return false;
  }

  public Serializable disassemble(Object value) throws HibernateException {
    return (Serializable) value;
  }

  public Object assemble(Serializable cached, Object owner) throws HibernateException {
    return cached;
  }

  public Object replace(Object original, Object target, Object owner) throws HibernateException {
    return original;
  }

  public void setParameterValues(Properties parameters) {
    if ( (parameters!=null)
         && (parameters.containsKey("length"))
       ){
      try {
        length = Integer.parseInt(parameters.getProperty("length"));
      } catch (NumberFormatException e) {
        throw new JbpmException("hibernate column type 'string_max' can't parse value '"+parameters.getProperty("length")+"' as a max length.  default is 4000.", e);
      }
    }
  }
}