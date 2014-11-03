package com.datastax.probe.actions;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ExecutionInfo;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
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
import com.datastax.probe.ClusterProbe;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public class TestCQLQueryProbe implements ProbeAction {
    
    private SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
    
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
	Preconditions.checkArgument(!cluster.isClosed(), "Cluster must not be closed");
	Preconditions.checkNotNull(session);
	Preconditions.checkArgument(!session.isClosed(), "Session must not be closed");
	Preconditions.checkNotNull(consistency);
	Preconditions.checkNotNull(cqlQuery);

	this.tracingEnabled = tracingEnabled;
	this.cluster = cluster;
	this.session = session;
	this.consistency = consistency;
	this.cqlQuery = cqlQuery;

	if (!validateCql(cqlQuery)) {
	    String msg = "Fatal error. Test CQL statement '" + cqlQuery + "' is not permitted. Only statements of types " + PERMITTED_CQL_ACTIONS
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
	if (executionInfo != null) {
	    StringBuilder msg = new StringBuilder("\n"+prefix);
	    msg.append(String.format("\nHost (queried): %s\n", executionInfo.getQueriedHost().toString()));

	    for (Host host : executionInfo.getTriedHosts()) {
		msg.append(String.format("Host (tried): %s\n", host.toString()));
	    }
	    
	    QueryTrace queryTrace = executionInfo.getQueryTrace();
	    if (queryTrace != null) {
		msg.append(String.format("Trace id: %s\n\n", queryTrace.getTraceId()));
		msg.append(String.format("%-80s | %-12s | %-20s | %-12s\n", "activity", "timestamp", "source", "source_elapsed"));
		msg.append(String.format("---------------------------------------------------------------------------------+--------------+----------------------+--------------\n"));
		for (QueryTrace.Event event : queryTrace.getEvents()) {
		    msg.append(String.format("%80s | %12s | %20s | %12s\n", event.getDescription(), format.format(event.getTimestamp()), event.getSource(),
			    event.getSourceElapsedMicros()));
		}
		LOG.info(msg.toString());
	    } else {
		LOG.warn("Query Trace is null\n"+msg);
	    }
	} else {
	    LOG.warn("Null execution info");
	}
    }

    public void logCluster(Cluster cluster) {
	try {
	    if (cluster != null && !cluster.isClosed()) {
		String clusterName = cluster.getClusterName();
		Metadata metadata = cluster.getMetadata();
		Set<Host> allHosts = metadata.getAllHosts();
		StringBuilder b = new StringBuilder("\nCassandra Cluster '" + clusterName + "' details (via native client driver) are :");
		for (Host host : allHosts) {
		    b.append(ClusterProbe.prettyHost(host));
		}
		LOG.info(b.toString());
	    } else {
		LOG.warn("Null or closed cluster");
	    }
	} catch (Throwable t) {

	}
    }

    @Override
    public boolean execute() throws FatalProbeException {
	boolean result = false;
	StopWatch stopWatch = new StopWatch();
	try {
	    LOG.info("About to execute synchronous CQL statement '" + this.cqlQuery + "' against Cassandra with consistency '"+this.consistency+"' and query tracing set to " + this.tracingEnabled);

	    SimpleStatement stmt = new SimpleStatement(this.cqlQuery);
	    stmt.setConsistencyLevel(this.consistency);
	    if (this.tracingEnabled) {
		LOG.warn("Query tracing has been enabled. Please note that this will increase query response time and will persist the query trace into the Cassandra system tables.");
		stmt.enableTracing();
	    } else {
		stmt.disableTracing();
	    }

	    stopWatch.start();
	    ResultSet rs = this.session.execute(stmt);
	    stopWatch.stop();
	    result = true;
	    LOG.info("Took " + stopWatch.getTime() + " (ms) to execute test query against Cassandra cluster with query tracing set to " + this.tracingEnabled);
	    
	    if (this.tracingEnabled) {
		try {
		    Thread.sleep(10000); //sleep a bit to allow tracing info to propagate
		} catch (InterruptedException e) {
		}
		logExecutionInfo("Query trace for '" + this.cqlQuery + "'", rs.getExecutionInfo());
	    }

	} catch (UnauthorizedException e) {
	    logCluster(this.cluster);
	    e.printStackTrace(System.err);
	    String msg = "Fatal error. User is not authorized to perform CQL statement '" + this.cqlQuery + "': " + e.getMessage();
	    System.err.println(msg);
	    LOG.error(msg);
	    System.exit(1);

	} catch (AuthenticationException e) {
	    logCluster(this.cluster);
	    InetSocketAddress errorHost = e.getAddress();
	    String msg = "Fatal error. Unable to authenticate Cassandra user against Cassandra node '" + errorHost.toString() + "': " + e.getMessage();
	    e.printStackTrace(System.err);
	    System.err.println(msg);
	    LOG.error(msg);
	    System.exit(1);

	} catch (QueryValidationException e) {
	    logCluster(this.cluster);
	    e.printStackTrace(System.err);
	    String msg = "Fatal error. The test CQL statement '" + this.cqlQuery + "' is not valid: " + e.getMessage();
	    System.err.println(msg);
	    LOG.error(msg);
	    System.exit(1);

	} catch (UnsupportedFeatureException e) {
	    logCluster(this.cluster);
	    e.printStackTrace(System.err);
	    String msg = "Fatal error. The test CQL statement '" + this.cqlQuery + "' is not compatable with the version of Cassandra being probed: " + e.getMessage();
	    System.err.println(msg);
	    LOG.error(msg);
	    System.exit(1);

	} catch (NoHostAvailableException e) {
	    logCluster(this.cluster);
	    Map<InetSocketAddress, Throwable> errors = e.getErrors();
	    StringBuilder errorMesages = new StringBuilder("Unable to establish a client connection to any Cassandra node: " + e.getMessage() + ". Tried: ");
	    if (errors != null && errors.size() > 0) {
		for (InetSocketAddress address : errors.keySet()) {
		    Throwable throwable = errors.get(address);
		    String stackTrace = ExceptionUtils.getStackTrace(throwable);
		    errorMesages.append("\n\t" + address.toString() + " : " + throwable.getMessage());
		    errorMesages.append("\n\t" + stackTrace);
		}
	    }
	    e.printStackTrace(System.err);
	    System.err.println(errorMesages.toString());
	    LOG.error(errorMesages.toString());

	} catch (UnavailableException e) {
	    logCluster(this.cluster);
	    int aliveReplicas = e.getAliveReplicas();
	    int requiredReplicas = e.getRequiredReplicas();
	    ConsistencyLevel cl = e.getConsistencyLevel();
	    e.printStackTrace(System.err);
	    String msg = aliveReplicas + " replicas are alive. " + requiredReplicas
		    + " are requried. There is not enough replicas alive to achieve the requested consistency level of '" + cl + "' : " + e.getMessage();
	    System.err.println(msg);
	    LOG.error(msg);

	} catch (ReadTimeoutException e) {
	    logCluster(this.cluster);
	    int receivedAcknowledgements = e.getReceivedAcknowledgements();
	    int requiredAcknowledgements = e.getRequiredAcknowledgements();
	    ConsistencyLevel cl = e.getConsistencyLevel();
	    e.printStackTrace(System.err);
	    String msg = receivedAcknowledgements + " read acknowlegement(s) recieved. " + requiredAcknowledgements
		    + " read acknowlegement(s) are required for read consistency level of '" + cl + "'. Not enough replicas responded in time : " + e.getMessage();
	    System.err.println(msg);
	    LOG.warn(msg);

	} catch (WriteTimeoutException e) {
	    logCluster(this.cluster);
	    int receivedAcknowledgements = e.getReceivedAcknowledgements();
	    int requiredAcknowledgements = e.getRequiredAcknowledgements();
	    ConsistencyLevel cl = e.getConsistencyLevel();
	    e.printStackTrace(System.err);
	    String msg = receivedAcknowledgements + " write acknowlegement(s) recieved. " + requiredAcknowledgements
		    + " write acknowlegement(s) are required for write consistency level of '" + cl + "'. Not enough nodes responded in time : " + e.getMessage();
	    System.err.println(msg);
	    LOG.warn(msg);

	} catch (Throwable t) {
	    logCluster(this.cluster);
	    t.printStackTrace(System.err);
	    String msg = "Unexpected error encountered: " + t.getMessage();
	    System.err.println(msg);
	    LOG.warn(msg);
	}

	return result;
    }

    public void closeSession() {
	if (this.session != null && !this.session.isClosed()) {
	    this.session.close();
	    this.session = null;
	}
    }
}
