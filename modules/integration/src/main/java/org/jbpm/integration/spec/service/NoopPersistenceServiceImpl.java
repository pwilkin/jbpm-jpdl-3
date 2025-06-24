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

// $Id: NoopPersistenceServiceImpl.java 3466 2008-12-19 22:53:18Z thomas.diesler@jboss.com $

import java.util.HashMap;
import java.util.Map;

import javax.management.ObjectName;

import org.hibernate.Session;
import org.jboss.bpm.api.ProcessNotFoundException;
import org.jboss.bpm.api.model.Node;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.ProcessInstance;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.incubator.service.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An in-memory persistence service.
 * 
 * @author thomas.diesler@jboss.com
 * @since 17-Sep-2008
 */
public class NoopPersistenceServiceImpl extends PersistenceService implements MutableService
{
  // Provide logging
  final Logger log = LoggerFactory.getLogger(NoopPersistenceServiceImpl.class);

  private Map<ObjectName, ProcessDefinition> procDefs = new HashMap<ObjectName, ProcessDefinition>();
  private Map<ObjectName, ProcessInstance> procs = new HashMap<ObjectName, ProcessInstance>();
  private Map<ObjectName, Node> nodes = new HashMap<ObjectName, Node>();

  // @Override
  public void setProcessEngine(ProcessEngine engine)
  {
    super.setProcessEngine(engine);
  }

  // @Override
  public Session createSession()
  {
    return null;
  }

  // @Override
  public ObjectName saveProcessDefinition(ProcessDefinition procDef)
  {
    procDefs.put(procDef.getKey(), procDef);
    return procDef.getKey();
  }

  // @Override
  public ProcessDefinition loadProcessDefinition(ObjectName procDefID)
  {
    ProcessDefinition procDef = procDefs.get(procDefID);
    if (procDef == null)
      throw new ProcessNotFoundException("Cannot find process: " + procDefID);

    return procDef;
  }

  // @Override
  public void deleteProcessDefinition(ProcessDefinition procDef)
  {
    procDefs.remove(procDef.getKey());
  }

  // @Override
  public ObjectName saveProcess(ProcessInstance proc)
  {
    procs.put(proc.getKey(), proc);

    for (Node node : proc.getNodes())
      nodes.put(node.getKey(), node);

    return proc.getKey();
  }

  // @Override
  public ProcessInstance loadProcess(ObjectName procID)
  {
    ProcessInstance proc = procs.get(procID);
    if (proc == null)
      throw new ProcessNotFoundException("Cannot find process: " + procID);

    return proc;
  }

  // @Override
  public void deleteProcess(ProcessInstance proc)
  {
    procs.remove(proc.getKey());

    for (Node node : proc.getNodes())
      nodes.remove(node.getKey());
  }

  // @Override
  public ObjectName saveNode(Session session, Node node)
  {
    return node.getKey();
  }

  // @Override
  @SuppressWarnings("unchecked")
  public <T extends Node> T loadNode(Session session, Class<T> nodeImpl, ObjectName nodeID)
  {
    T node = (T)nodes.get(nodeID);
    if (node == null)
      throw new ProcessNotFoundException("Cannot find node: " + nodeID);

    return node;
  }
}
