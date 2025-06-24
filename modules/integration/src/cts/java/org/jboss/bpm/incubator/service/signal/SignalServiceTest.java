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
package org.jboss.bpm.incubator.service.signal;

// $Id: SignalServiceTest.java 3480 2008-12-20 12:56:31Z thomas.diesler@jboss.com $

import java.util.List;

import org.jboss.bpm.api.model.Signal;
import org.jboss.bpm.api.model.Signal.SignalType;
import org.jboss.bpm.api.service.SignalService;
import org.jboss.bpm.api.test.CTSTestCase;

/**
 * Test the SignalService
 * 
 * @author thomas.diesler@jboss.com
 * @since 08-Jul-2008
 */
public class SignalServiceTest extends CTSTestCase
{
  /**
   * Throw a signal to the automatically registered signal listener
   */
  public void testSignalListener() throws Exception
  {
    SignalService sigService = getProcessEngine().getService(SignalService.class);
    Signal signal = newSignal(getTestID(), SignalType.USER_SIGNAL, "HelloWorld");
    sigService.throwSignal(signal);
    
    List<Signal> signals = getSignals();
    assertNotNull("Received signals not null", signals);
    assertEquals("One signal", 1, signals.size());
    assertEquals("HelloWorld", signals.get(0).getMessage());

    // test the the signal can be retrieved by type
    assertEquals("No signal", 0, getSignals(SignalType.SYSTEM_PROCESS_ENTER).size());
    assertEquals("One signal", 1, getSignals(SignalType.USER_SIGNAL).size());
  }
}
