package com.datastax.probe.job;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.probe.Prober;

@DisallowConcurrentExecution
public class ProbeJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(ProbeJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
	LOG.info("ProbeJob running...");
	StopWatch stopWatch = new StopWatch();

	JobKey key = context.getJobDetail().getKey();
	JobDataMap dataMap = context.getJobDetail().getJobDataMap();

	String yamlPath = dataMap.getString("yamlPath");
	String cqlshrcPath = dataMap.getString("cqlshrcPath");
	String username = dataMap.getString("username");
	String password = dataMap.getString("password");

	LOG.info("Instance " + key + " of ProbeJob yamlPath: " + yamlPath + ", and cqlshrcPath is: " + cqlshrcPath);

	try {
	    stopWatch.start();
	    Prober app = null;
	    if (StringUtils.isNotBlank(cqlshrcPath)) {
		app = new Prober(yamlPath, cqlshrcPath);
	    } else if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
		app = new Prober(yamlPath, username, password);
	    } else {
		app = new Prober(yamlPath);
	    }
	    app.probe();
	} catch (Exception e) {
	    String message = "Problem encountered with probing cluster: " + e.getMessage();
	    System.err.println(message);
	    e.printStackTrace(System.err);
	    LOG.error(message, e);
	    throw new RuntimeException(message, e);
	} finally {
	    stopWatch.stop();
	}

    }

}
