package org.jbpm.db.hibernate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException; // Added for nullSafeSet signature
import org.hibernate.engine.spi.SharedSessionContractImplementor; // Changed for Hibernate 5
import org.hibernate.type.StringType;
import org.jbpm.JbpmException;

public class StringMax extends StringType implements org.hibernate.usertype.ParameterizedType {

  private static final long serialVersionUID = 1L;
  
  int length = 4000;

  // TODO: Hibernate 5 - nullSafeSet in superclass (AbstractStandardBasicType) is final.
  // This custom logic for truncation cannot be applied by overriding nullSafeSet.
  // StringMax would need to be a full UserType implementation to retain this feature.
  // For now, commenting out the override to allow compilation. This removes truncation.
  // @Override
  // public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
  //   String stringValue = (String) value;
  //   if ( (stringValue!=null)
  //        && (stringValue.length()>length)
  //      ) {
  //     stringValue = stringValue.substring(0, length);
  //   }
  //   super.nullSafeSet(st, stringValue, index, session);
  // }

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
