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
package org.jbpm.integration.spec.runtime;

//$Id: TokenAttachmentDelegate.java 3487 2008-12-20 15:47:24Z thomas.diesler@jboss.com $

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.bpm.api.NotImplementedException;
import org.jboss.bpm.api.runtime.Attachments;
import org.jbpm.context.exe.ContextInstance;

/**
 * An Attachment implementation that delegates to to the ContextInstance
 * 
 * @author thomas.diesler@jboss.com
 * @since 26-Nov-2008
 */
public class TokenAttachmentDelegate implements Attachments
{
  private ContextInstance context;
  private TokenImpl token;

  public TokenAttachmentDelegate(TokenImpl token, ContextInstance context)
  {
    this.token = token;
    this.context = context;
  }

  public <T> T addAttachment(Class<T> clazz, Object value)
  {
    return addAttachment(clazz, null, value);
  }

  public Object addAttachment(String name, Object value)
  {
    return addAttachment(null, name, value);
  }

  @SuppressWarnings("unchecked")
  public <T> T addAttachment(Class<T> clazz, String name, Object value)
  {
    validateAttachmentKey(clazz, name);
    context.createVariable(name, value, token.getDelegate());
    return (T)value;
  }

  public <T> T getAttachment(Class<T> clazz)
  {
    return getAttachment(clazz, null);
  }

  public Object getAttachment(String name)
  {
    return getAttachment(null, name);
  }

  @SuppressWarnings("unchecked")
  public <T> T getAttachment(Class<T> clazz, String name)
  {
    validateAttachmentKey(clazz, name);
    return (T)context.getVariable(name, token.getDelegate());
  }

  @SuppressWarnings("unchecked")
  public Collection<Key> getAttachmentKeys()
  {
    Set<Key> keys = new HashSet<Key>();
    Set<String> strKeys = context.getVariables(token.getDelegate()).keySet();
    for (String strKey : strKeys)
    {
      Key key = new Key(null, strKey);
      keys.add(key);
    }
    return keys;
  }

  public <T> T removeAttachment(Class<T> clazz, String name)
  {
    T value = getAttachment(clazz, name);
    context.deleteVariable(name, token.getDelegate());
    return value;
  }

  public <T> T removeAttachment(Class<T> clazz)
  {
    return removeAttachment(clazz, null);
  }

  public Object removeAttachment(String name)
  {
    return removeAttachment(null, name);
  }

  private <T> void validateAttachmentKey(Class<T> clazz, String name)
  {
    if (clazz != null)
      throw new NotImplementedException("Attachments keyed by Class not supported");
    
    if (name == null)
      throw new NotImplementedException("Attachments with null name not supported");
  }
}