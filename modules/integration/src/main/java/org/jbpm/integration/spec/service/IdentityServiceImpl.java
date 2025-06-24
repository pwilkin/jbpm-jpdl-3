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

// $Id: IdentityServiceImpl.java 3485 2008-12-20 14:33:15Z thomas.diesler@jboss.com $

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jboss.bpm.api.NotImplementedException;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.api.service.internal.AbstractService;
import org.jboss.bpm.incubator.service.IdentityService;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.identity.Group;
import org.jbpm.identity.User;
import org.jbpm.identity.hibernate.IdentitySession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The IdentityService manages identities
 * 
 * @author thomas.diesler@jboss.com
 * @since 28-Nov-2008
 */
public class IdentityServiceImpl extends AbstractService implements IdentityService, MutableService
{
  // Provide logging
  final Logger log = LoggerFactory.getLogger(IdentityServiceImpl.class);

  public void setProcessEngine(ProcessEngine engine)
  {
    super.setProcessEngine(engine);
  }

  @SuppressWarnings("unchecked")
  public List<String> getActors()
  {
    List<String> actors = new ArrayList<String>();

    IdentitySession identSession = getIdentitySession();
    try
    {
      actors.addAll(identSession.getUsers());
    }
    catch (RuntimeException rte)
    {
      throw rte;
    }
    finally
    {
      identSession.close();
    }

    return actors;
  }

  @SuppressWarnings("unchecked")
  public List<String> getActorsByGroup(String group)
  {
    List<String> actors = new ArrayList<String>();

    IdentitySession identSession = getIdentitySession();
    try
    {
      Group identGroup = identSession.getGroupByName(group);
      for (User user : (Set<User>)identGroup.getUsers())
      {
        actors.add(user.getName());
      }
    }
    catch (RuntimeException rte)
    {
      throw rte;
    }
    finally
    {
      identSession.close();
    }

    return actors;
  }

  public List<String> getGroups()
  {
    throw new NotImplementedException();
  }

  @SuppressWarnings("unchecked")
  public List<String> getGroupsByActor(String actor)
  {
    List<String> groups = new ArrayList<String>();

    IdentitySession identSession = getIdentitySession();
    try
    {
      User identUser = identSession.getUserByName(actor);
      for (Group group : (Set<Group>)identUser.getGroupsForGroupType("organisation"))
      {
        groups.add(group.getName());
      }
    }
    catch (RuntimeException rte)
    {
      throw rte;
    }
    finally
    {
      identSession.close();
    }

    return groups;
  }

  private IdentitySession getIdentitySession()
  {
    ProcessEngineImpl engineImpl = (ProcessEngineImpl)getProcessEngine();
    JbpmConfiguration jbpmConfig = engineImpl.getJbpmConfiguration();
    JbpmContext jbpmContext = jbpmConfig.createJbpmContext();
    IdentitySession identSession = new IdentitySession(jbpmContext.getSession());
    return identSession;
  }
}
