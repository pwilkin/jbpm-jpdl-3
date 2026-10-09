package org.jbpm.persistence.db;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.hibernate.Transaction;

/** records what happens to a hibernate transaction; the transaction is a dynamic proxy. */
public class MockTransaction implements InvocationHandler {

  boolean wasCommitted = false;
  boolean wasRolledBack = false;

  private Transaction transaction;

  public Transaction createTransaction() {
    if (transaction == null) {
      transaction = (Transaction) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { Transaction.class }, this);
    }
    return transaction;
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String name = method.getName();
    if (name.equals("commit")) {
      wasCommitted = true;
      return null;
    } else if (name.equals("rollback")) {
      wasRolledBack = true;
      return null;
    } else if (name.equals("isActive")) {
      return !wasCommitted && !wasRolledBack;
    } else if (name.equals("getRollbackOnly")) {
      return false;
    } else if (name.equals("equals")) {
      return proxy == args[0];
    } else if (name.equals("hashCode")) {
      return System.identityHashCode(proxy);
    } else if (name.equals("toString")) {
      return "MockTransaction@" + Integer.toHexString(System.identityHashCode(proxy));
    }
    throw new UnsupportedOperationException(method.toString());
  }
}
