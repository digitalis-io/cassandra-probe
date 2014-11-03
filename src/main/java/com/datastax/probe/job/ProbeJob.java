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

import com.datastax.driver.core.ConsistencyLevel;
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
	boolean nativeProbe = dataMap.getBoolean("nativeProbe");
	boolean thriftProbe = dataMap.getBoolean("thriftProbe");
	boolean storageProbe = dataMap.getBoolean("storageProbe");
	boolean pingProbe = dataMap.getBoolean("pingProbe");
	
	String testCql = dataMap.getString("testCql");
	ConsistencyLevel consistency = (ConsistencyLevel) dataMap.get("consistency");
	boolean tracingEnabled = dataMap.getBoolean("tracingEnabled");


	LOG.info("Instance " + key + " of ProbeJob yamlPath: " + yamlPath + ", and cqlshrcPath is: " + cqlshrcPath);

	try {
	    stopWatch.start();
	    Prober app = null;
	    if (StringUtils.isNotBlank(cqlshrcPath)) {
		app = new Prober(yamlPath, cqlshrcPath, nativeProbe, thriftProbe, storageProbe, pingProbe, testCql, consistency, tracingEnabled);
	    } else if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
		app = new Prober(yamlPath, username, password, nativeProbe, thriftProbe, storageProbe, pingProbe, testCql, consistency, tracingEnabled);
	    } else {
		app = new Prober(yamlPath, nativeProbe, thriftProbe, storageProbe, pingProbe, testCql, consistency, tracingEnabled);
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
