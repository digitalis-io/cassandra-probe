package com.datastax.probe;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
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
    
    @SuppressWarnings("static-access")
    private static Options getCLiOption() {
	Options options = new Options();
	
	Option interval = OptionBuilder.withArgName("int").withLongOpt("interval")
		.isRequired(false)
		.hasArg()
		.withType(Integer.class)
		.withDescription("OPTIONAL: Interval in seconds between probe jobs. Overlapping probe jobs will not occur. If not specified the probe will only run once and then exit")
		.create();
	options.addOption(interval);
	
	Option cassandraYaml = OptionBuilder.withArgName("string").withLongOpt("yaml")
		.isRequired(true)
		.hasArg()
		.withType(String.class)
		.withDescription("The path to the cassandra.yaml file")
		.create();
	options.addOption(cassandraYaml);
	
	Option cqlshRc = OptionBuilder.withArgName("string").withLongOpt("cqlshrc")
		.isRequired(false)
		.hasArg()
		.withType(String.class)
		.withDescription("OPTIONAL: The path to the CQLSHRC file containing security user credentials to connect to Cassandra")
		.create();
	
	
	OptionGroup security = new OptionGroup();
	security.addOption(cqlshRc);
	security.setRequired(false);

	
	options.addOptionGroup(security);
	
	
	return options;
    }

    public static void main(String[] args) {
	
	 CommandLineParser parser = new BasicParser();
	 
	 CommandLine cmd = null;
	 try {
	    cmd = parser.parse(App.getCLiOption(), args);
	} catch (ParseException e) {
	    String msg = "Problem encountered parsing command line arguments : "+e.getMessage();
	    LOG.error(msg);
	    System.err.println(msg);
	    new HelpFormatter().printHelp( "java -jar cassandra-probe.jar", App.getCLiOption() );
	    System.exit(1);
	}
	 
	 
	int interval = Integer.parseInt(cmd.getOptionValue("interval", "-1"));
	LOG.info("interval: "+interval);
	String yaml = cmd.getOptionValue("yaml");
	LOG.info("yaml: "+yaml);
	String cqlshrc = cmd.getOptionValue("cqlshrc");

	if (StringUtils.isNotBlank(cqlshrc)) {
	    LOG.info("cqlshrc path provided as '" + cqlshrc + "'");
	} else {
	    LOG.info("No cqlshrc path provided. Cassandra will be connected to without authentication");
	}

	try {
	    if (interval < 1) {
		LOG.info("Running probe once only");
		final Prober app = (cqlshrc != null) ? new Prober(yaml, cqlshrc) : new Prober(yaml);
		app.probe();
		System.exit(0);
	    } else {
		LOG.info("Running probe continuously with an interval of "+interval+" seconds between probes");
		final App app = new App();
		app.startJob(interval, yaml, cqlshrc);
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