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
package org.jbpm.integration.spec.service;

// $Id: TokenServiceImpl.java 3466 2008-12-19 22:53:18Z thomas.diesler@jboss.com $

import java.util.HashSet;
import java.util.Set;

import javax.management.ObjectName;

import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.runtime.Token;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.api.service.ProcessInstanceService;
import org.jboss.bpm.api.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ExecutionService manages Tokens
 * 
 * @author thomas.diesler@jboss.com
 * @since 28-Nov-2008
 */
public class TokenServiceImpl extends TokenService implements MutableService
{
  // Provide logging
  final Logger log = LoggerFactory.getLogger(TokenServiceImpl.class);

  public void setProcessEngine(ProcessEngine engine)
  {
    super.setProcessEngine(engine);
  }

  @Override
  public Set<Token> getTokens()
  {
    Set<Token> tokens = new HashSet<Token>();
    ProcessInstanceService procService = getProcessEngine().getService(ProcessInstanceService.class);
    for (ObjectName procID : procService.getInstance())
    {
      ProcessInstance proc = procService.getInstance(procID);
      for (Token aux : proc.getTokens())
      {
        tokens.add(aux);
      }
    }
    return tokens;
  }

  @Override
  public Token getToken(ObjectName tokenID)
  {
    Token token = null;

    // [TODO] is there a better way than iterating over all processes and tokens?
    ProcessInstanceService procService = getProcessEngine().getService(ProcessInstanceService.class);
    for (ObjectName procID : procService.getInstance())
    {
      ProcessInstance proc = procService.getInstance(procID);
      for (Token aux : proc.getTokens())
      {
        if (aux.getKey().equals(tokenID))
        {
          token = aux;
          break;
        }
      }
    }
    return token;
  }
}
