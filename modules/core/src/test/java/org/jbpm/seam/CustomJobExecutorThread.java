package org.jbpm.seam;

import org.jbpm.JbpmConfiguration;
import org.jbpm.job.Job;
import org.jbpm.job.executor.JobExecutor;
import org.jbpm.job.executor.JobExecutorThread;

public class CustomJobExecutorThread extends JobExecutorThread {

  public CustomJobExecutorThread(String name, JobExecutor jobExecutor, JbpmConfiguration jbpmConfiguration, int idleInterval, int maxIdleInterval, long maxLockTime, int maxHistory) {
    super(name, jobExecutor, jbpmConfiguration, idleInterval, maxIdleInterval, maxLockTime, maxHistory);
  }

  protected void executeJob(Job job) {
    // intercept before
    JobExecutorCustomizationTest.jobEvents.add("before");
    try {
      super.executeJob(job);
    } finally {
      // intercept after
      JobExecutorCustomizationTest.jobEvents.add("after");
    }
  }
}
