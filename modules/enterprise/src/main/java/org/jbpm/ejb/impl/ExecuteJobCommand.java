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
package org.jbpm.ejb.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.command.Command;
import org.jbpm.db.JobSession;
import org.jbpm.job.Job;
import org.jbpm.msg.jms.JmsMessageService;
import org.jbpm.msg.jms.JmsMessageServiceFactory;
import org.jbpm.svc.Services;

/**
 * Individual job processing command.
 * @author Alejandro Guizar
 */
public class ExecuteJobCommand implements Command {

  private final long jobId;

  private static final long serialVersionUID = 1L;

  public ExecuteJobCommand(long jobId) {
    this.jobId = jobId;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    JobSession jobSession = jbpmContext.getJobSession();
    Job job = jobSession.getJob(jobId);
    if (job == null) {
      log.debug("job " + jobId + " was deleted");
      return null;
    }
    String lockOwner = job.getLockOwner();
    if (lockOwner != null) {
      log.debug(job + " is locked by " + lockOwner);
      return null;
    }
    lockOwner = Long.toString(jobId);
    if (job.isExclusive()) {
      List exclusiveJobs = jobSession.findExclusiveJobs(lockOwner, job.getProcessInstance());
      // lock exclusive jobs
      int jobCount = exclusiveJobs.size();
      if (jobCount == 0) {
        // may happen if isolation level is below repeatable read
        log.debug(job + " was locked during attempt to lock other jobs");
        return null;
      }
      long[] exclusiveJobIds = new long[jobCount];
      for (int i = 0; i < jobCount; i++) {
        Job exclusiveJob = (Job) exclusiveJobs.get(i);
        exclusiveJob.setLockOwner(lockOwner);
        exclusiveJobIds[i] = exclusiveJob.getId();
      }
      log.debug("locking jobs " + Arrays.toString(exclusiveJobIds));
      // execute exclusive jobs in separate transaction
      postJobsExecution(jbpmContext, exclusiveJobIds);        
    }
    else {
      // lock job to prevent others from deleting it
      job.setLockOwner(lockOwner);
      log.debug("executing " + job);
      executeJob(job, jbpmContext);
    }
    return null;
  }

  static void executeJob(Job job, JbpmContext jbpmContext) {
    try {
      if (job.execute(jbpmContext)) {
        jbpmContext.getJobSession().deleteJob(job);
      }
    }
    catch (RuntimeException e) {
      // nothing to do but clean up and exit
      throw e;
    }
    catch (Exception e) {
      // save data about recoverable error condition
      log.error("exception while executing " + job, e);
      StringWriter memoryWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(memoryWriter));
      job.setException(memoryWriter.toString());
      job.setRetries(job.getRetries() - 1);
    }
  }

  private static void postJobsExecution(JbpmContext jbpmContext, long[] exclusiveJobIds)
      throws NamingException, JMSException {
    Services services = jbpmContext.getServices();
    JmsMessageServiceFactory messageServiceFactory = (JmsMessageServiceFactory) services.getServiceFactory(Services.SERVICENAME_MESSAGE);
    Destination destination = messageServiceFactory.getCommandDestination();

    JmsMessageService messageService = (JmsMessageService) services.getMessageService();
    Session session = messageService.getSession();
    MessageProducer producer = session.createProducer(destination);
    try {
      Command command = new ExecuteJobsCommand(exclusiveJobIds);
      Message message = session.createObjectMessage(command);
      producer.send(message);
    }
    finally {
      producer.close();
    }
  }

  private static Log log = LogFactory.getLog(ExecuteJobCommand.class);
}
