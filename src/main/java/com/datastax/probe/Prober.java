package com.datastax.probe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ch.qos.logback.classic.Logger;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.probe.actions.FatalProbeException;
import com.datastax.probe.actions.IsReachableProbe;
import com.datastax.probe.actions.ProbeAction;
import com.datastax.probe.actions.SocketProbe;
import com.datastax.probe.actions.TestCQLQueryProbe;
import com.datastax.probe.model.HostProbe;
import com.google.common.base.Preconditions;

public class Prober {

    private static final int TIMEOUT_MS = 10000;

    private static final Logger LOG = ProbeLoggerFactory.getLogger(Prober.class);
    private final String yamlPath;
    private String cqlshrcPath;

    private String user;
    private String pwd;

    private boolean nativeProbe = false;
    private boolean thriftProbe = false;
    private boolean storageProbe = false;
    private boolean pingProbe = false;

    private String testCql;
    private ConsistencyLevel consistency;

    private boolean tracingEnabled;
    private int storagePort;
    private int nativePort;
    private int thriftPort;

    private String[] contactPoints;

    public Prober(int storagePort, int nativePort, int thriftPort, String[] contactPoints, String yamlPath, String cqlshrcPath, boolean nativeProbe, boolean thriftProbe, boolean storageProbe, boolean pingProbe, String testCql,
	    ConsistencyLevel consistency,  boolean tracingEnabled) {
	Preconditions.checkArgument(((contactPoints != null && contactPoints.length > 0) || StringUtils.isBlank(yamlPath)), "contact points or yaml path must be provided");
	this.contactPoints = contactPoints;
	this.nativeProbe = nativeProbe;
	this.thriftProbe = thriftProbe;
	this.storageProbe = storageProbe;
	this.pingProbe = pingProbe;
	this.yamlPath = yamlPath;
	if (StringUtils.isNotBlank(cqlshrcPath)) {
	    this.cqlshrcPath = cqlshrcPath;
	    parseCqlshRcFile();
	} else {
	    LOG.info("No CQLSHRC file provided. Cassandra will be connected to without authentication");
	    this.cqlshrcPath = null;
	}
	this.testCql = testCql;
	this.consistency = consistency;
	this.tracingEnabled = tracingEnabled;
	this.storagePort = storagePort;
	this.nativePort = nativePort;
	this.thriftPort = thriftPort;
    }

    public Prober(int storagePort, int nativePort, int thriftPort, String[] contactPoints, String yamlPath, String userName, String password, boolean nativeProbe, boolean thriftProbe, boolean storageProbe, boolean pingProbe, String testCql,
	    ConsistencyLevel consistency, boolean tracingEnabled) {
	this(storagePort, nativePort, thriftPort, contactPoints, yamlPath, null, nativeProbe, thriftProbe, storageProbe, pingProbe, testCql, consistency, tracingEnabled);
	Preconditions.checkNotNull(userName, "username must be provided");
	Preconditions.checkNotNull(password, "password must be provided");
	this.setUserDetails(userName, password);
    }

    public Prober(int storagePort, int nativePort, int thriftPort, String[] contactPoints, String yamlPath, boolean nativeProbe, boolean thriftProbe, boolean storageProbe, boolean pingProbe, String testCql, ConsistencyLevel consistency, boolean tracingEnabled) {
	this(storagePort, nativePort, thriftPort, contactPoints, yamlPath, null, nativeProbe, thriftProbe, storageProbe, pingProbe, testCql, consistency, tracingEnabled);
    }

    public void parseCqlshRcFile() {
	LOG.info("CQLSHRC file will be read from '" + this.cqlshrcPath + "'");

	Preconditions.checkNotNull(this.cqlshrcPath, "The path to CQLSHRC file can not be null or empty");

	String userName = null;
	String password = null;

	File cqlshRcFile = new File(this.cqlshrcPath);
	if (!cqlshRcFile.exists()) {
	    throw new RuntimeException("The CQLSHRC '" + this.cqlshrcPath + "' file does not exist");
	}

	if (!cqlshRcFile.canRead()) {
	    throw new RuntimeException("The CQLSHRC '" + this.cqlshrcPath + "' file can not be read");
	}

	Properties props = null;
	try {
	    props = new Properties();
	    props.load(new FileInputStream(cqlshRcFile));
	    userName = (String) props.get("username");
	    password = (String) props.get("password");
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    LOG.error("Problem encountered loading property CQLSHRC property file '" + this.cqlshrcPath + "' : " + e.getMessage(), e);
	    throw new RuntimeException("Problem encountered loading property CQLSHRC property file '" + this.cqlshrcPath + "' : " + e.getMessage(), e);
	} finally {
	    if (props != null) {
		props.clear();
		props = null;
	    }
	}

	if (StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(password)) {
	    this.setUserDetails(userName, password);
	} else {
	    throw new RuntimeException("The CQLSHRC '" + this.cqlshrcPath
		    + "' file does not contain any user credentials. Expected property values for keys 'username' and 'password'");
	}

    }

    private void setUserDetails(String user, String pwd) {
	LOG.info("Username and password provided. Cassandra connection will be authenticated");
	this.user = user;
	this.pwd = pwd;
    }

    private String detectLocalHostname() {
	String hostname = "hostname-could-not-be-detected";
	try {
	    hostname = InetAddress.getLocalHost().getHostName();
	} catch (UnknownHostException e) {
	    LOG.error("Could not determine local hostname");
	}
	return hostname;
    }

    public String getYamlPath() {
	return yamlPath;
    }

    public void probe() throws FatalProbeException, IOException {
	LOG.info("\n\nNew Probe Commencing....");
	
	ClusterProbe cp = new ClusterProbe(this.detectLocalHostname(), this.getYamlPath(), this.user, this.pwd, this.storagePort, this.nativePort, this.thriftPort, this.contactPoints);
	cp.discoverCluster(true);
	Set<HostProbe> hosts = cp.getHosts();
	for (HostProbe h : hosts) {
	    LOG.info("Probing Host '" + h.getToAddress() + "' : " + h);

	    boolean hostReachable = false;
	    if (this.pingProbe) {
		try {
		    ProbeAction isReachable = new IsReachableProbe(h, TIMEOUT_MS);
		    hostReachable = isReachable.execute();
		} catch (Exception e) {
		    LOG.warn(e.getMessage(), e);
		    LOG.debug(e.getMessage(), e);
		}
	    } else {
		LOG.warn("IsReachable/Ping probe has been disabled. Will assume host is reachable for subsequent probes. Ideally this should always be enabled.");
		hostReachable = true;
	    }

	    if (hostReachable) {
		if (this.nativeProbe) {
		    try {
			ProbeAction nativePort = new SocketProbe("Native", h, h.getNativePort(), TIMEOUT_MS);
			nativePort.execute();
		    } catch (Exception e) {
			LOG.warn(e.getMessage(), e);
			LOG.debug(e.getMessage(), e);
		    }
		} else {
		    LOG.info("Native probe disabled");
		}

		if (this.thriftProbe) {
		    try {
			ProbeAction rpcPort = new SocketProbe("Thrift", h, h.getRpcPort(), TIMEOUT_MS);
			rpcPort.execute();
		    } catch (Exception e) {
			LOG.warn(e.getMessage(), e);
			LOG.debug(e.getMessage(), e);
		    }
		} else {
		    LOG.info("Thrift probe disabled");
		}

		if (this.storageProbe) {
		    try {
			ProbeAction storagePort = new SocketProbe("Gossip", h, h.getStoragePort(), TIMEOUT_MS);
			storagePort.execute();
		    } catch (Exception e) {
			LOG.warn(e.getMessage(), e);
			LOG.debug(e.getMessage(), e);
		    }
		} else {
		    LOG.info("Storage/Gossip probe disabled");
		}

	    } else {
		LOG.warn("Unable to reach host '" + h.getToAddress() + "' completely - Cassandra ports will not be probed");
	    }

	}

	if (StringUtils.isNotBlank(this.testCql) && this.consistency != null) {
	    TestCQLQueryProbe cqlProbe = null;
	    try {
		Cluster cluster = cp.getCassandraCluster();
		LOG.info("Cluster: "+cluster.isClosed());
		
		cqlProbe = new TestCQLQueryProbe(cluster, this.consistency, null, this.testCql, this.tracingEnabled);
		cqlProbe.execute();
	    } finally {
		if (cqlProbe != null) {
		    cqlProbe.closeSession();
		}
		if (cp != null) {
		    cp.shutDownCluster();
		}
	    }
	}

    }

}
