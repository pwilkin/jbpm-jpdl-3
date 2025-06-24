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
package org.jbpm.integration.spec.model;

import org.jboss.bpm.api.model.AbstractElement;
import org.jboss.bpm.api.runtime.Context;
import org.jboss.bpm.api.service.ContextService;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.Identifiable;

/**
 * An integration wrapper
 * 
 * @author thomas.diesler@jboss.com
 * @since 15-Nov-2008
 */
public abstract class AbstractElementImpl<T extends Identifiable> implements AbstractElement
{
  private static final long serialVersionUID = 1L;

  private ProcessEngine engine;

  private T tempObj;
  private Class<T> objClass;
  private long objID;

  @SuppressWarnings("unchecked")
  public AbstractElementImpl(ProcessEngine engine, Identifiable tempObj, Class<T> objClass)
  {
    this.engine = engine;
    this.objClass = objClass;

    // Store the ID of the underlying jBPM3 object
    objID = tempObj.getId();
    if (objID == 0)
      this.tempObj = (T)tempObj;
  }

  public ProcessEngine getProcessEngine()
  {
    return engine;
  }

  @SuppressWarnings("unchecked")
  public T getDelegate()
  {
    T retObj = null;

    // Determine the current ID to use
    long currID = objID;
    if (currID == 0 && tempObj != null)
      currID = tempObj.getId();

    // If there is no ID, use the tmp object
    if (currID == 0 && tempObj != null)
      retObj = tempObj;

    // Get the delegate from the persistence session
    if (retObj == null)
    {
      if (objClass == null || currID == 0)
        throw new IllegalArgumentException("Cannot obtain delegate for '" + objClass + "' id: " + currID);

      // From now on use this object ID
      objID = currID;
      tempObj = null;

      ContextService execService = getProcessEngine().getService(ContextService.class);
      Context bpmContext = execService.getContext(true);
      try
      {
        JbpmContext jbpmContext = bpmContext.getAttachment(JbpmContext.class);
        Object obj = jbpmContext.getSession().get(objClass, objID);
        retObj = (T)obj;
      }
      finally
      {
        bpmContext.close();
      }
    }

    // Complain if the delegate cannot be obtained
    if (retObj == null)
      throw new IllegalStateException("Cannot obtain delegate for '" + objClass.getName() + "' id: " + objID);

    return retObj;
  }
}
