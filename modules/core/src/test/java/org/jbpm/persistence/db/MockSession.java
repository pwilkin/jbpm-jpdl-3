package org.jbpm.persistence.db;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jdbc.Work;

/**
 * records what the persistence service does with its hibernate session. The session itself is a
 * dynamic proxy (see {@link #createSession()}); get back to the recorder with {@link #of(Session)}.
 */
public class MockSession implements InvocationHandler {

  MockTransaction transaction = null;
  Connection connection = null;
  SessionFactory sessionFactory = null;
  boolean isFlushed = false;
  boolean isClosed = false;

  boolean failOnFlush;
  boolean failOnClose;

  public MockSession() {
  }

  public MockSession(Connection connection) {
    this.connection = connection;
  }

  public static MockSession of(Session session) {
    return (MockSession) Proxy.getInvocationHandler(session);
  }

  public Session createSession() {
    return (Session) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { Session.class }, this);
  }

  public void setFailOnFlush(boolean fail) {
    failOnFlush = fail;
  }

  public void setFailOnClose(boolean fail) {
    failOnClose = fail;
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String name = method.getName();
    if (name.equals("beginTransaction")) {
      transaction = new MockTransaction();
      return transaction.createTransaction();
    } else if (name.equals("getTransaction")) {
      return transaction != null ? transaction.createTransaction() : null;
    } else if (name.equals("doWork")) {
      ((Work) args[0]).execute(connection);
      return null;
    } else if (name.equals("close")) {
      if (failOnClose) throw new HibernateException("simulated close exception");
      isClosed = true;
      return null;
    } else if (name.equals("flush")) {
      if (failOnFlush) throw new HibernateException("simulated flush exception");
      isFlushed = true;
      return null;
    } else if (name.equals("isOpen")) {
      return !isClosed;
    } else if (name.equals("getSessionFactory")) {
      return sessionFactory;
    } else if (name.equals("equals")) {
      return proxy == args[0];
    } else if (name.equals("hashCode")) {
      return System.identityHashCode(proxy);
    } else if (name.equals("toString")) {
      return "MockSession@" + Integer.toHexString(System.identityHashCode(proxy));
    }
    throw new UnsupportedOperationException(method.toString());
  }
}
