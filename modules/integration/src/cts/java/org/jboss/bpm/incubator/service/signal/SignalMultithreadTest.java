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

// $Id: SignalMultithreadTest.java 3480 2008-12-20 12:56:31Z thomas.diesler@jboss.com $

import org.jboss.bpm.api.model.Signal;
import org.jboss.bpm.api.model.SignalListener;
import org.jboss.bpm.api.model.Signal.SignalType;
import org.jboss.bpm.api.service.SignalService;
import org.jboss.bpm.api.test.CTSTestCase;

/**
 * Test the ProcessManager
 * 
 * @author thomas.diesler@jboss.com
 * @since 08-Jul-2008
 */
public class SignalMultithreadTest extends CTSTestCase
{
  final SignalService sigService = getProcessEngine().getService(SignalService.class);
  final Signal sigA = newSignal(getTestID(), SignalType.USER_SIGNAL, "A");
  final Signal sigB = newSignal(getTestID(), SignalType.USER_SIGNAL, "B");

  private Signal gotA;
  private Signal gotB;

  public void testReentrantSingleThread() throws Exception
  {
    SignalListener sigListener = new SignalListener()
    {
      public boolean acceptSignal(Signal signal)
      {
        return signal.getSignalType() == SignalType.USER_SIGNAL;
      }

      public void catchSignal(Signal signal)
      {
        String sigMsg = signal.getMessage();
        if ("A".equals(sigMsg))
        {
          gotA = signal;
          sigService.throwSignal(sigB);
        }
        if ("B".equals(sigMsg))
        {
          gotB = signal;
        }
      }
    };
    sigService.addSignalListener(sigListener);
    sigService.throwSignal(sigA);
    sigService.removeSignalListener(sigListener);

    assertEquals(sigA, gotA);
    assertEquals(sigB, gotB);
  }

  public void testReentrantMultipleThread() throws Exception
  {
    SignalListener sigListener = new SignalListener()
    {
      public boolean acceptSignal(Signal signal)
      {
        return signal.getSignalType() == SignalType.USER_SIGNAL;
      }

      public void catchSignal(Signal signal)
      {
        String sigMsg = signal.getMessage();
        if ("A".equals(sigMsg))
        {
          gotA = signal;
          sendThreadSignal(sigB);
        }
        if ("B".equals(sigMsg))
        {
          gotB = signal;
        }
      }
    };
    sigService.addSignalListener(sigListener);
    sendThreadSignal(sigA);
    sigService.removeSignalListener(sigListener);

    assertEquals(sigA, gotA);
    assertEquals(sigB, gotB);
  }

  private void sendThreadSignal(Signal sig)
  {
    SignalRunner sigRunner = new SignalRunner(sig);
    Thread thread = new Thread(sigRunner);
    thread.start();
    try
    {
      while (sigRunner.hasStarted() == false)
      {
        Thread.sleep(10);
      }
      thread.join();
    }
    catch (InterruptedException ex)
    {
      ex.printStackTrace();
    }
  }

  class SignalRunner implements Runnable
  {
    private Signal threadSig;
    private boolean hasStarted;

    public SignalRunner(Signal sig)
    {
      this.threadSig = sig;
    }

    public boolean hasStarted()
    {
      return hasStarted;
    }

    public void run()
    {
      hasStarted = true;
      sigService.throwSignal(threadSig);
    }
  }
}
