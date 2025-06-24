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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.command.Command;
import org.jbpm.job.Job;

/**
 * Batch job processing command.
 * @author Alejandro Guizar
 */
public class ExecuteJobsCommand implements Command {

  private long[] jobIds;

  private static final long serialVersionUID = 1L;
  private static Log log = LogFactory.getLog(ExecuteJobsCommand.class);

  public ExecuteJobsCommand(long[] jobIds) {
    this.jobIds = jobIds;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    log.debug("executing jobs " + Arrays.toString(jobIds));
    List jobs = jbpmContext.getJobSession().loadJobs(jobIds);
    for (Iterator i = jobs.iterator(); i.hasNext();) {
      Job job = (Job) i.next();
      job.execute(jbpmContext);
    }
    return null;
  }

}
