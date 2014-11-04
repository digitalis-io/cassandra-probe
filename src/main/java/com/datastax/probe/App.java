package com.datastax.probe;

import java.util.Arrays;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
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

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.probe.job.ProbeJob;

public class App {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    @SuppressWarnings("static-access")
    private static Options getCLiOption() {
	Options options = new Options();
	options.addOption(OptionBuilder.withLongOpt("help").withDescription("Display help information").create("h"));
	options.addOption(OptionBuilder.withLongOpt("interval")
		.withDescription("The interval in seconds between running probe jobs. If not specified or < 1, the probe will be run once only.").hasArg().withArgName("INTERVAL")
		.create("i"));
	options.addOption(OptionBuilder.withLongOpt("yaml")
		.withDescription("The path to the cassandra.yaml to obtain the contact points (via the seeds) and ports Cassandra is listening on").hasArg().withArgName("YAML")
		.create("y"));
	options.addOption(OptionBuilder.withLongOpt("username").withDescription("The username to connect to Cassandra").hasArg().withArgName("USERNAME").create("u"));
	options.addOption(OptionBuilder.withLongOpt("password").withDescription("The password to connect to Cassandra").hasArg().withArgName("PASSWORD").create("p"));
	options.addOption(OptionBuilder
		.withLongOpt("cqlshrc")
		.withDescription(
			"The path to the CQLSHRC containing security credentails for Cassandra. If this is specified the security credentials will be read from this file and NOT the username/password arguments")
		.hasArg().withArgName("CQLSHRC").create("c"));
	options.addOption(OptionBuilder.withLongOpt("ping").withDescription("Execute ping/isReachable probe to Cassandra host").create("pi"));
	options.addOption(OptionBuilder.withLongOpt("storage").withDescription("Probe the storage/gossip port").create("st"));
	options.addOption(OptionBuilder.withLongOpt("native").withDescription("Probe the native port").create("na"));
	options.addOption(OptionBuilder.withLongOpt("thrift").withDescription("Probe the thrift port").create("th"));
	
	options.addOption(OptionBuilder.withLongOpt("test_cql")
		.withDescription("Test CQL query to run against cluster").hasArg().withArgName("TEST CQL QUERY")
		.create("cql"));
	options.addOption(OptionBuilder.withLongOpt("consistency")
		.withDescription("The consistency level to use for the test CQL statement").hasArg().withArgName("CONSISTENCY LEVEL")
		.create("con"));
	options.addOption(OptionBuilder.withLongOpt("tracing").withDescription("Enable query tracing as part of the test query. WARNING - enabling tracing persists data to the system_traces keyspace in the cluster being probed.").create("tr"));
	
	
	
	return options;
    }

    private static void printHelp() {
	HelpFormatter help = new HelpFormatter();
	help.setWidth(HelpFormatter.DEFAULT_WIDTH + 100);
	help.printHelp("java -jar cassandra-probe-exec.jar", App.getCLiOption());
    }

    public static void main(String[] args) {
	if (args.length == 0 || Arrays.asList(args).contains("--help") || Arrays.asList(args).contains("-help") || Arrays.asList(args).contains("help")) {
	    App.printHelp();
	    System.exit(0);
	}

	CommandLineParser parser = new BasicParser();

	CommandLine cmd = null;
	try {
	    cmd = parser.parse(App.getCLiOption(), args);
	} catch (ParseException e) {
	    String msg = "Problem encountered parsing command line arguments : " + e.getMessage();
	    LOG.error(msg);
	    System.err.println(msg);
	    App.printHelp();
	    System.exit(1);
	}

	int interval = Integer.parseInt(cmd.getOptionValue("interval", "-1"));
	LOG.info("interval: " + interval);
	String yaml = cmd.getOptionValue("yaml");
	LOG.info("yaml: " + yaml);
	String cqlshrc = cmd.getOptionValue("cqlshrc");

	String userName = cmd.getOptionValue("username");
	String password = cmd.getOptionValue("password");

	boolean pingProbe = true;
	boolean storageProbe = true;
	boolean thriftProbe = true;
	boolean nativeProbe = true;

	if (cmd.hasOption("storage") || cmd.hasOption("thrift") || cmd.hasOption("native") || cmd.hasOption("ping")) {
	    storageProbe = cmd.hasOption("storage");
	    thriftProbe = cmd.hasOption("thrift");
	    nativeProbe = cmd.hasOption("native");
	    pingProbe = cmd.hasOption("ping");
	}

	if (StringUtils.isNotBlank(cqlshrc)) {
	    LOG.info("cqlshrc path provided as '" + cqlshrc + "'");
	    userName = null;
	    password = null;
	} else if (StringUtils.isNotBlank(userName)) {
	    if (StringUtils.isBlank(password)) {
		String msg = "Cassandra username is provided, but no password has been specified";
		LOG.error(msg);
		System.err.println(msg);
		App.printHelp();
		System.exit(1);
	    }
	    LOG.info("Username/password authentication is provided");

	} else {
	    LOG.info("No cqlshrc path or user credentials provided. Cassandra will be connected to without any authentication");
	}
	
	String testCql = cmd.getOptionValue("test_cql");
	String con = cmd.getOptionValue("consistency");
	boolean tracingEnabled = cmd.hasOption("tracing");

	ConsistencyLevel consistency = null;
	LOG.debug("Test CQL query '"+testCql+"' and consistency level of '"+con+"'");

	if (StringUtils.isNotBlank(testCql)) {
	    if (StringUtils.isBlank(con)) {
		String msg = "Consistency level must be specified if test cql query is provided";
		LOG.error(msg);
		System.err.println(msg);
		App.printHelp();
		System.exit(1);
	    }
	    LOG.info("Test CQL query '"+testCql+"' will be executed at Consistency level of '"+con+"' with tracing enabled "+tracingEnabled);
	    consistency = ConsistencyLevel.valueOf(con.toUpperCase().trim());
	    if (consistency == null) {
		String msg = "Unable to recognise inputted consistency '"+con+"'";
		LOG.error(msg);
		System.err.println(msg);
		App.printHelp();
		System.exit(1);
	    }
	} else {
	    LOG.info("No Test CQL query required");
	}
	
	
	try {
	    if (interval < 1) {
		LOG.info("Running probe once only");
		Prober app = null;
		if (cqlshrc != null) {
		    app = new Prober(yaml, cqlshrc, nativeProbe, thriftProbe, storageProbe, pingProbe, testCql, consistency, tracingEnabled);
		} else if (userName != null) {
		    app = new Prober(yaml, userName, password, nativeProbe, thriftProbe, storageProbe, pingProbe, testCql, consistency, tracingEnabled);
		} else {
		    app = new Prober(yaml, nativeProbe, thriftProbe, storageProbe, pingProbe, testCql, consistency, tracingEnabled);
		}
		app.probe();
		System.exit(0);
	    } else {
		LOG.info("Running probe continuously with an interval of " + interval + " seconds between probes");
		final App app = new App();
		app.startJob(interval, yaml, cqlshrc, userName, password, nativeProbe, thriftProbe, storageProbe, pingProbe, testCql, consistency, tracingEnabled);
	    }
	} catch (Exception e) {
	    String msg = "Problem encountered starting job: " + e.getMessage();
	    LOG.error(msg, e);
	    e.printStackTrace(System.err);
	    System.exit(1);
	}
    }

    public void startJob(final int intervalInSeconds, final String yamlPath, final String cqlshrcPath, final String userName, final String password, 
	    final boolean nativeProbe, final boolean thriftProbe, final boolean storageProbe, final boolean pingProbe, final String testCql,
	    final ConsistencyLevel consistency,  final boolean tracingEnabled) throws SchedulerException {
	final JobDataMap args = new JobDataMap();
	args.put("cqlshrcPath", cqlshrcPath);
	args.put("yamlPath", yamlPath);
	args.put("username", userName);
	args.put("password", password);
	args.put("nativeProbe", nativeProbe);
	args.put("thriftProbe", thriftProbe);
	args.put("storageProbe", storageProbe);
	args.put("pingProbe", pingProbe);
	args.put("testCql", testCql);
	args.put("consistency", consistency);
	args.put("tracingEnabled", tracingEnabled);	

	JobDetail job = JobBuilder.newJob(ProbeJob.class).withIdentity("ProbeJob", "cassandra-probe").usingJobData(args).build();

	Trigger trigger = TriggerBuilder.newTrigger().withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(intervalInSeconds).repeatForever()).build();

	SchedulerFactory schFactory = new StdSchedulerFactory();
	Scheduler sch = schFactory.getScheduler();
	sch.start();
	sch.scheduleJob(job, trigger);
    }

}