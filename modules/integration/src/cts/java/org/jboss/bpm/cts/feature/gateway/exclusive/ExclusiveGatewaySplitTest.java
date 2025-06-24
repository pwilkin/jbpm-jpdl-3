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
package org.jboss.bpm.cts.feature.gateway.exclusive;

// $Id: ExclusiveGatewaySplitTest.java 3486 2008-12-20 15:46:33Z thomas.diesler@jboss.com $

import java.io.IOException;
import java.net.URL;

import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.model.ProcessInstance.ProcessStatus;
import org.jboss.bpm.api.runtime.BasicAttachments;
import org.jboss.bpm.api.runtime.Token;
import org.jboss.bpm.api.runtime.Token.TokenStatus;
import org.jboss.bpm.api.service.ProcessDefinitionService;
import org.jboss.bpm.api.test.CTSTestCase;

/**
 * Exclusive data-based gateway that has conditional outgoing sequence flows. Only one of the gates is taken. It is an
 * error if no gate is applicable.
 * 
 * @author thomas.diesler@jboss.com
 * @since 06-Aug-2008
 */
public class ExclusiveGatewaySplitTest extends CTSTestCase
{
  public void testGateA() throws Exception
  {
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());
    ProcessInstance proc = procDef.newInstance();

    BasicAttachments att = new BasicAttachments();
    att.addAttachment("foo", "5");

    Token tok = proc.startProcess(att);

    String nodeName = tok.getNode().getName();
    assertEquals("endA", nodeName);
    
    assertEquals(TokenStatus.Destroyed, tok.getTokenStatus());
    assertEquals(ProcessStatus.Completed, proc.getProcessStatus());
  }

  public void testGateB() throws Exception
  {
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());
    ProcessInstance proc = procDef.newInstance();

    BasicAttachments att = new BasicAttachments();
    att.addAttachment("foo", "15");
    
    Token tok = proc.startProcess(att);

    String nodeName = tok.getNode().getName();
    assertEquals("endB", nodeName);
    
    assertEquals(TokenStatus.Destroyed, tok.getTokenStatus());
    assertEquals(ProcessStatus.Completed, proc.getProcessStatus());
  }

  public void testInvalidGate() throws Exception
  {
    ProcessDefinition procDef = unregisterOnTearDown(getProcessDefinition());
    ProcessInstance proc = procDef.newInstance();

    BasicAttachments att = new BasicAttachments();
    att.addAttachment("foo", "10");
    
    try
    {
      proc.startProcess(att);
      fail("No gate defined for foo==10");
    }
    catch (RuntimeException rte)
    {
      // expected
    }
  }

  protected ProcessDefinition getProcessDefinition() throws IOException
  {
    URL pdURL = getResourceURL("cts/feature/gateway/exclusive/exclusive-split-" + getDialect() + ".xml");
    ProcessDefinitionService pdService = getProcessEngine().getService(ProcessDefinitionService.class);
    ProcessDefinition procDef = pdService.parseProcessDefinition(pdURL);
    return procDef;
  }
}
