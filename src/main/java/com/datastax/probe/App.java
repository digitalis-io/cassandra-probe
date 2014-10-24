package com.datastax.probe;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.probe.job.ProbeJob;

public class App {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
	if (args == null || args.length < 2) {
	    String message = "Invalid usage. Interval in seconds should be passed in as arg[0]. If only required to run once, set interval to < 1 seconds. Path to cassandra.yaml should be passed in as arg[1]. "
		    + "Path to cqlshrc file should be (optionally) passed in as arg[2]";
	    LOG.error(message);
	    System.err.println(message);
	    System.exit(1);
	}

	int interval = -1;
	String yamlPath = null;
	try {
	    interval = Integer.parseInt(args[0].trim());
	    LOG.info("interval provided as '" + interval + "'");
	    yamlPath = args[1].trim();
	    LOG.info("yamlPath provided as '" + yamlPath + "'");
	} catch (Exception e) {
	    String msg = "Problem encountered parsing arguments: " + e.getMessage();
	    LOG.error(msg, e);
	    e.printStackTrace(System.err);
	    System.exit(1);
	}

	String cqlshrcPath = null;
	if (args.length == 3) {
	    cqlshrcPath = args[2];
	    LOG.info("cqlshrc path provided as '" + cqlshrcPath + "'");
	} else {
	    LOG.info("No cqlshrc path provided. Cassandra will be connected to without authentication");

	}

	try {
	    if (interval < 1) {
		LOG.info("Running probe once only");
		final Prober app = (cqlshrcPath != null) ? new Prober(yamlPath, cqlshrcPath) : new Prober(yamlPath);
		app.probe();
		System.exit(0);
	    } else {
		LOG.info("Running probe continuously with an interval of "+interval+" seconds between probes");
		final App app = new App();
		app.startJob(interval, yamlPath, cqlshrcPath);
	    }
	} catch (Exception e) {
	    String msg = "Problem encountered starting job: " + e.getMessage();
	    LOG.error(msg, e);
	    e.printStackTrace(System.err);
	    System.exit(1);
	}
    }

    public void startJob(int intervalInSeconds, String yamlPath, String cqlshrcPath) throws SchedulerException {
	JobDetail job = JobBuilder.newJob(ProbeJob.class).withIdentity("ProbeJob", "cassandra-probe").usingJobData("yamlPath", yamlPath).usingJobData("cqlshrcPath", cqlshrcPath)
		.build();

	Trigger trigger = TriggerBuilder.newTrigger().withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(intervalInSeconds).repeatForever()).build();

	SchedulerFactory schFactory = new StdSchedulerFactory();
	Scheduler sch = schFactory.getScheduler();
	sch.start();
	sch.scheduleJob(job, trigger);
    }

}
