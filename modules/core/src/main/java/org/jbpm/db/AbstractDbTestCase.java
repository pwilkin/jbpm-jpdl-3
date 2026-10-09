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
package org.jbpm.db;

// $Id: AbstractDbTestCase.java 3438 2008-12-19 09:06:06Z thomas.diesler@jboss.com $

import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.Job;
import org.jbpm.job.executor.JobExecutor;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.Services;
import org.jbpm.taskmgmt.exe.TaskInstance;

public abstract class AbstractDbTestCase extends AbstractJbpmTestCase
{
  private static final Log log = LogFactory.getLog(AbstractDbTestCase.class);

  protected JbpmConfiguration jbpmConfiguration;

  protected JbpmContext jbpmContext;

  protected Session session;
  protected GraphSession graphSession;
  protected TaskMgmtSession taskMgmtSession;
  protected ContextSession contextSession;
  protected JobSession jobSession;
  protected LoggingSession loggingSession;

  protected JobExecutor jobExecutor;

  protected void setUp() throws Exception
  {
    super.setUp();
    beginSessionTransaction();
  }

  protected void tearDown() throws Exception
  {
    commitAndCloseSession();

    super.tearDown();
  }

  protected String getHibernateDialect()
  {
    DbPersistenceServiceFactory factory = (DbPersistenceServiceFactory)jbpmContext.getServiceFactory(Services.SERVICENAME_PERSISTENCE);
    return factory.getConfiguration().getProperty(AvailableSettings.DIALECT);
  }

  protected void beginSessionTransaction()
  {
    createJbpmContext();
    initializeMembers();
  }

  protected void commitAndCloseSession()
  {
    closeJbpmContext();
    resetMembers();
  }

  protected void newTransaction()
  {
    commitAndCloseSession();
    beginSessionTransaction();
  }

  protected ProcessInstance saveAndReload(ProcessInstance pi)
  {
    jbpmContext.save(pi);
    newTransaction();
    return graphSession.loadProcessInstance(pi.getId());
  }

  protected TaskInstance saveAndReload(TaskInstance taskInstance)
  {
    jbpmContext.save(taskInstance);
    newTransaction();
    return session.get(TaskInstance.class, taskInstance.getId());
  }

  protected ProcessDefinition saveAndReload(ProcessDefinition pd)
  {
    graphSession.saveProcessDefinition(pd);
    newTransaction();
    return graphSession.loadProcessDefinition(pd.getId());
  }

  protected ProcessLog saveAndReload(ProcessLog processLog)
  {
    loggingSession.saveProcessLog(processLog);
    newTransaction();
    return loggingSession.loadProcessLog(processLog.getId());
  }

  protected void createSchema()
  {
    getJbpmConfiguration().createSchema();
  }

  protected void cleanSchema()
  {
    getJbpmConfiguration().cleanSchema();
  }

  protected void dropSchema()
  {
    getJbpmConfiguration().dropSchema();
  }

  protected String getJbpmTestConfig()
  {
    return "org/jbpm/db/jbpm.db.test.cfg.xml";
  }

  protected JbpmConfiguration getJbpmConfiguration()
  {
    if (jbpmConfiguration == null)
    {
      String jbpmTestConfiguration = getJbpmTestConfig();
      jbpmConfiguration = JbpmConfiguration.getInstance(jbpmTestConfiguration);
    }
    return jbpmConfiguration;
  }

  protected void createJbpmContext()
  {
    jbpmContext = getJbpmConfiguration().createJbpmContext();
  }

  protected void closeJbpmContext()
  {
    if (jbpmContext != null)
    {
      jbpmContext.close();
      jbpmContext = null;
    }
  }

  protected void startJobExecutor()
  {
    jobExecutor = getJbpmConfiguration().getJobExecutor();
    jobExecutor.start();
  }

  protected void waitForJobs(long timeout)
  {
    // install a timer that will interrupt if it takes too long
    // if that happens, it will lead to an interrupted exception and the test
    // will fail
    TimerTask interruptTask = new TimerTask()
    {
      Thread testThread = Thread.currentThread();

      public void run()
      {
        log.debug("test " + getName() + " took too long. going to interrupt...");
        testThread.interrupt();
      }
    };
    Timer timer = new Timer();
    timer.schedule(interruptTask, timeout);

    try
    {
      while (getNbrOfJobsAvailable() > 0)
      {
        log.debug("going to sleep for 200 millis, waiting for the job executor to process more jobs");
        Thread.sleep(200);
      }
    }
    catch (InterruptedException e)
    {
      fail("test execution exceeded treshold of " + timeout + " milliseconds");
    }
    finally
    {
      timer.cancel();
    }
  }

  protected int getNbrOfJobsAvailable()
  {
    if (session != null)
    {
      return getNbrOfJobsAvailable(session);
    }
    else
    {
      beginSessionTransaction();
      try
      {
        return getNbrOfJobsAvailable(session);
      }
      finally
      {
        commitAndCloseSession();
      }
    }
  }

  private int getNbrOfJobsAvailable(Session session)
  {
    int nbrOfJobsAvailable = 0;
    Number jobs = session.createQuery("select count(*) from org.jbpm.job.Job", Long.class).uniqueResult();
    log.debug("there are " + jobs + " jobs in the database");
    if (jobs != null)
    {
      nbrOfJobsAvailable = jobs.intValue();
    }
    return nbrOfJobsAvailable;
  }

  protected int getTimerCount()
  {
    Number timerCount = session.createQuery("select count(*) from org.jbpm.job.Timer", Long.class).uniqueResult();
    log.debug("there are " + timerCount + " timers in the database");
    return timerCount.intValue();
  }

  protected Job getJob()
  {
    return session.createQuery("from org.jbpm.job.Job", Job.class).uniqueResult();
  }

  protected void processJobs(long maxWait)
  {
    commitAndCloseSession();
    startJobExecutor();
    try
    {
      waitForJobs(maxWait);
    }
    finally
    {
      stopJobExecutor();
      beginSessionTransaction();
    }
  }

  protected void stopJobExecutor()
  {
    if (jobExecutor != null)
    {
      try
      {
        jobExecutor.stopAndJoin();
      }
      catch (InterruptedException e)
      {
        throw new RuntimeException("waiting for job executor to stop and join got interrupted", e);
      }
    }
  }

  protected void initializeMembers()
  {
    session = jbpmContext.getSession();
    graphSession = jbpmContext.getGraphSession();
    taskMgmtSession = jbpmContext.getTaskMgmtSession();
    loggingSession = jbpmContext.getLoggingSession();
    jobSession = jbpmContext.getJobSession();
    contextSession = jbpmContext.getContextSession();
  }

  protected void resetMembers()
  {
    session = null;
    graphSession = null;
    taskMgmtSession = null;
    loggingSession = null;
    jobSession = null;
    contextSession = null;
  }
}
