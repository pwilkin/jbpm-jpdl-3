package org.jbpm.persistence.db;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;

/**
 * hands out {@link MockSession}s. The session factory and the session builder returned by
 * <code>withOptions()</code> are dynamic proxies; use {@link #createSessionFactory()}.
 */
public class MockSessionFactory implements InvocationHandler {

  private boolean failOnFlush;
  private boolean failOnClose;
  private SessionFactory sessionFactory;

  public void setFailOnFlush(boolean fail) {
    failOnFlush = fail;
  }

  public void setFailOnClose(boolean fail) {
    failOnClose = fail;
  }

  public SessionFactory createSessionFactory() {
    if (sessionFactory == null) {
      sessionFactory = (SessionFactory) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { SessionFactory.class }, this);
    }
    return sessionFactory;
  }

  Object openSession(Connection connection) {
    MockSession session = new MockSession(connection);
    session.setFailOnFlush(failOnFlush);
    session.setFailOnClose(failOnClose);
    session.sessionFactory = sessionFactory;
    return session.createSession();
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String name = method.getName();
    if (name.equals("openSession")) {
      return openSession(null);
    } else if (name.equals("withOptions")) {
      return new SessionBuilderHandler().createSessionBuilder();
    } else if (name.equals("getCurrentSession")) {
      return null;
    } else if (name.equals("isClosed")) {
      return false;
    } else if (name.equals("equals")) {
      return proxy == args[0];
    } else if (name.equals("hashCode")) {
      return System.identityHashCode(proxy);
    } else if (name.equals("toString")) {
      return "MockSessionFactory";
    }
    throw new UnsupportedOperationException(method.toString());
  }

  class SessionBuilderHandler implements InvocationHandler {
    Connection connection;

    SessionBuilder createSessionBuilder() {
      return (SessionBuilder) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { SessionBuilder.class }, this);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String name = method.getName();
      if (name.equals("connection")) {
        connection = (Connection) args[0];
        return proxy;
      } else if (name.equals("openSession")) {
        return MockSessionFactory.this.openSession(connection);
      }
      throw new UnsupportedOperationException(method.toString());
    }
  }
}
