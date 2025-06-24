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

// $Id: SignalBuilderServiceImpl.java 3479 2008-12-20 12:55:32Z thomas.diesler@jboss.com $

import javax.management.ObjectName;

import org.jboss.bpm.api.model.Signal;
import org.jboss.bpm.api.model.Signal.SignalType;
import org.jboss.bpm.api.model.builder.SignalBuilder;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.api.service.SignalBuilderService;

/**
 * The SignalBuilder can be used to build a {@link Signal} dynamically.
 * 
 * @author thomas.diesler@jboss.com
 * @since 18-Jun-2008
 */
public class SignalBuilderServiceImpl extends SignalBuilderService implements MutableService
{
  public void setProcessEngine(ProcessEngine engine)
  {
    super.setProcessEngine(engine);
  }

  @Override
  public SignalBuilder getSignalBuilder()
  {
    SignalBuilder sigBuilder = new SignalBuilder()
    {
      public Signal newSignal(SignalType sigType, ObjectName fromRef, String message)
      {
        return new SignalImpl(sigType, fromRef, message);
      }
    };
    return sigBuilder;
  }
  
  class SignalImpl implements Signal
  {
    private static final long serialVersionUID = 1L;
    
    private ObjectName fromRef;
    private SignalType sigType;
    private String message;
    
    public SignalImpl(SignalType sigType, ObjectName fromRef, String message)
    {
      this.sigType = sigType;
      this.fromRef = fromRef;
      this.message = message;
    }

    public SignalType getSignalType()
    {
      return sigType;
    }
    
    public ObjectName getFromRef()
    {
      return fromRef;
    }

    public String getMessage()
    {
      return message;
    }

    public String toString()
    {
      return "[" + sigType + ",from=" + fromRef + ",msg=" + message + "]";
    }
  }
}