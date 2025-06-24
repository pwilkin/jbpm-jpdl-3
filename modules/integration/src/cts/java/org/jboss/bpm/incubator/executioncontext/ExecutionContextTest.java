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
package org.jboss.bpm.incubator.executioncontext;

// $Id: ExecutionContextTest.java 3473 2008-12-20 09:31:54Z thomas.diesler@jboss.com $

import org.jboss.bpm.api.runtime.Attachments;
import org.jboss.bpm.api.runtime.BasicAttachments;
import org.jboss.bpm.api.test.CTSTestCase;

/**
 * Test the execution context
 * 
 * @author thomas.diesler@jboss.com
 * @since 03-Jul-2008
 */
public class ExecutionContextTest extends CTSTestCase
{
  public void testStringKey() throws Exception 
  {
    Attachments att = new BasicAttachments();
    
    att.addAttachment("foo", "bar");
    assertEquals("bar", att.getAttachment("foo"));
    assertNull(att.getAttachment(String.class, "foo"));

    att.removeAttachment("foo");
    assertNull(att.getAttachment("foo"));
  }
  
  public void testClassKey() throws Exception 
  {
    Attachments att = new BasicAttachments();
    
    att.addAttachment(String.class, "bar");
    assertEquals("bar", att.getAttachment(String.class));
    
    att.removeAttachment(String.class);
    assertNull(att.getAttachment(String.class));
  }
  
  public void testStringClassKey() throws Exception 
  {
    Attachments att = new BasicAttachments();
    
    att.addAttachment(String.class, "foo", "bar");
    assertEquals("bar", att.getAttachment(String.class, "foo"));
    assertNull(att.getAttachment("foo"));
    
    att.removeAttachment(String.class, "foo");
    assertNull(att.getAttachment(String.class, "foo"));
  }
}
