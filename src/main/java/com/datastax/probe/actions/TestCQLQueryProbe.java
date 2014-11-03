package com.datastax.probe.actions;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ExecutionInfo;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.QueryTrace;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.exceptions.AuthenticationException;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.QueryValidationException;
import com.datastax.driver.core.exceptions.ReadTimeoutException;
import com.datastax.driver.core.exceptions.UnauthorizedException;
import com.datastax.driver.core.exceptions.UnavailableException;
import com.datastax.driver.core.exceptions.UnsupportedFeatureException;
import com.datastax.driver.core.exceptions.WriteTimeoutException;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public class TestCQLQueryProbe implements ProbeAction {
    private static final Logger LOG = LoggerFactory.getLogger(TestCQLQueryProbe.class);
    private static final ImmutableSet<String> PERMITTED_CQL_ACTIONS = ImmutableSet.of("select", "insert", "update");

    private Cluster cluster;
    private Session session;
    private ConsistencyLevel consistency;
    private String cqlQuery;
    private boolean tracingEnabled;

    public TestCQLQueryProbe(Cluster cluster, ConsistencyLevel consistency, String keySpace, String cqlQuery, boolean tracingEnabled) {
	this(cluster, ((StringUtils.isBlank(keySpace)) ? cluster.connect() : cluster.connect(keySpace)), consistency, cqlQuery, tracingEnabled);
    }

    public TestCQLQueryProbe(Cluster cluster, Session session, ConsistencyLevel consistency, String cqlQuery, boolean tracingEnabled) {
	Preconditions.checkNotNull(cluster);
	Preconditions.checkArgument(cluster.isClosed(), "Cluster must not be closed");
	Preconditions.checkNotNull(session);
	Preconditions.checkArgument(session.isClosed(), "Session must not be closed");
	Preconditions.checkNotNull(consistency);
	Preconditions.checkNotNull(cqlQuery);

	this.tracingEnabled = tracingEnabled;
	this.cluster = cluster;
	this.session = session;
	this.consistency = consistency;
	this.cqlQuery = cqlQuery;
	
	if (!validateCql(cqlQuery)) {
	    String msg = "Fatal error. Test CQL statement '" + cqlQuery + "' is not permitted. Only statements of types " + PERMITTED_CQL_ACTIONS.toArray().toString()
		    + " are permitted. Exiting program.";
	    System.err.println(msg);
	    LOG.error(msg);
	    System.exit(1);
	}

    }

    private boolean validateCql(final String cql) {
	LOG.info("Validating CQL " + cql);
	String[] cqlQuery = cql.toLowerCase().split(" ");
	String action = cqlQuery[0];
	LOG.info("CQL action is " + action);
	return (PERMITTED_CQL_ACTIONS.contains(action));
    }

    
    public void logExecutionInfo(String prefix, ExecutionInfo executionInfo) {
	StringBuilder msg = new StringBuilder(prefix);
	msg.append(String.format("\nHost (queried): %s\n", executionInfo.getQueriedHost().toString()));
	
	for (Host host : executionInfo.getTriedHosts()) {
	    msg.append(String.format("Host (tried): %s\n", host.toString()));
	}
	
	QueryTrace queryTrace = executionInfo.getQueryTrace();
	msg.append(String.format("Trace id: %s\n\n", queryTrace.getTraceId()));
	msg.append(String.format("%-38s | %-12s | %-10s | %-12s\n", "activity", "timestamp", "source", "source_elapsed"));
	msg.append(String.format("---------------------------------------+--------------+------------+--------------"));
	for (QueryTrace.Event event : queryTrace.getEvents()) {
	    msg.append(String.format("%38s | %12s | %10s | %12s\n", event.getDescription(), new DateTime(event.getTimestamp()), event.getSource(), event.getSourceElapsedMicros()));
	}
	
	LOG.info(msg.toString());
    }

    @Override
    public boolean execute() throws FatalProbeException {
	boolean result = false;
	StopWatch stopWatch = new StopWatch();
	try {
	    LOG.info("About to execute synchronous CQL statement '"+this.cqlQuery+"' against Cassandra with query tracing set to "+this.tracingEnabled);
	    stopWatch.start();
	    
	    SimpleStatement stmt = new SimpleStatement(this.cqlQuery);
	    stmt.setConsistencyLevel(this.consistency);
	    if (this.tracingEnabled) {
		LOG.warn("Query tracing has been enabled. Please note that this will increase query response time and persist the query trace into the Cassandra system tables.");
		stmt.enableTracing();
	    } else {
		stmt.disableTracing();
	    }
	    
	    ResultSet execute = this.session.execute(this.cqlQuery);
	    stopWatch.stop();
	    LOG.info("Took " + stopWatch.getTime() + " (ms) to execute test query against Cassandra cluster with query tracing set to "+this.tracingEnabled);
	    
	    if (this.tracingEnabled) {
		try {
		    Thread.sleep(10000);
		} catch (InterruptedException e) {
		}
		
		List<ExecutionInfo> allExecutionInfo = execute.getAllExecutionInfo();
		if (allExecutionInfo != null && allExecutionInfo.size() > 0) {
		    for (ExecutionInfo ei : allExecutionInfo) {
			logExecutionInfo("Execution Info for '" + this.cqlQuery + "'", ei);
		    }
		} else {
		    LOG.info("No Execution info found");
		}
	    }
	    
	} catch (UnauthorizedException e) {
	    e.printStackTrace(System.err);
	    String msg = "Fatal error. User is not authorized to perform CQL statement '"+this.cqlQuery+"': "+e.getMessage();
	    System.err.println(msg);
	    LOG.error(msg);
	    System.exit(1);
	    
	} catch (AuthenticationException e) {
	    InetSocketAddress errorHost = e.getAddress();
	    String msg = "Fatal error. Unable to authenticate Cassandra user against Cassandra node '"+errorHost.toString()+"': "+e.getMessage();
	    e.printStackTrace(System.err);
	    System.err.println(msg);
	    LOG.error(msg);
	    System.exit(1);
	    
	} catch (QueryValidationException e) {
	    e.printStackTrace(System.err);
	    String msg = "Fatal error. The test CQL statement '"+this.cqlQuery+"' is not valid: "+e.getMessage();
	    System.err.println(msg);
	    LOG.error(msg);
	    System.exit(1);
	    
	} catch (UnsupportedFeatureException e) {
	    e.printStackTrace(System.err);
	    String msg = "Fatal error. The test CQL statement '"+this.cqlQuery+"' is not compatable with the version of Cassandra being probed: "+e.getMessage();
	    System.err.println(msg);
	    LOG.error(msg);
	    System.exit(1);
	    
	} catch (NoHostAvailableException e) {
	    String msg = "Unable to establish a connection to any Cassandra node: "+e.getMessage();
	    Map<InetSocketAddress, Throwable> errors = e.getErrors();
	    StringBuilder errorMesages = new StringBuilder(msg);
	    if (errors != null && errors.size() > 0) {
		for (InetSocketAddress address : errors.keySet()) {
		    Throwable throwable = errors.get(address);
		    String stackTrace = ExceptionUtils.getStackTrace(throwable);
		    errorMesages.append("\n\t"+address.toString()+" : "+throwable.getMessage());
		    errorMesages.append("\n\t"+stackTrace);
		}
	    }
	    e.printStackTrace(System.err);
	    System.err.println(errorMesages.toString());
	    LOG.warn(errorMesages.toString());
	    
	    result = false;
	    
	} catch (UnavailableException e) {
	    int aliveReplicas = e.getAliveReplicas();
	    int requiredReplicas = e.getRequiredReplicas();
	    ConsistencyLevel cl = e.getConsistencyLevel();
	    e.printStackTrace(System.err);
	    String msg = aliveReplicas+" replicas are alive. "+requiredReplicas+" are requried. There is not enough replicas alive to achieve the requested consistency level of '"+cl+"' : "+e.getMessage();
	    System.err.println(msg);
	    LOG.warn(msg);
	    
	    result = false;
	    
	} catch (ReadTimeoutException e) {
	    int receivedAcknowledgements = e.getReceivedAcknowledgements();
	    int requiredAcknowledgements = e.getRequiredAcknowledgements();
	    ConsistencyLevel cl = e.getConsistencyLevel();
	    e.printStackTrace(System.err);
	    String msg = receivedAcknowledgements+" read acknowlegement(s) recieved. "+requiredAcknowledgements+" read acknowlegement(s) are required for read consistency level of '"+cl+"'. Not enough replicas responded in time : "+e.getMessage();
	    System.err.println(msg);
	    LOG.warn(msg);
	    
	    result = false;

	} catch (WriteTimeoutException e) {
	    int receivedAcknowledgements = e.getReceivedAcknowledgements();
	    int requiredAcknowledgements = e.getRequiredAcknowledgements();
	    ConsistencyLevel cl = e.getConsistencyLevel();
	    e.printStackTrace(System.err);
	    String msg = receivedAcknowledgements+" write acknowlegement(s) recieved. "+requiredAcknowledgements+" write acknowlegement(s) are required for write consistency level of '"+cl+"'. Not enough nodes responded in time : "+e.getMessage();
	    System.err.println(msg);
	    LOG.warn(msg);
	    
	    result = false;
	}
	
	return result;
    }
}
