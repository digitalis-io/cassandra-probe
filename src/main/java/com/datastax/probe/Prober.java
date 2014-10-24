package com.datastax.probe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.probe.actions.FatalProbeException;
import com.datastax.probe.actions.IsReachableProbe;
import com.datastax.probe.actions.ProbeAction;
import com.datastax.probe.actions.SocketProbe;
import com.datastax.probe.model.HostProbe;
import com.google.common.base.Preconditions;

public class Prober {

    private static final int TIMEOUT_MS = 10000;

    private static final Logger LOG = LoggerFactory.getLogger(Prober.class);
    private final String yamlPath;
    private final String cqlshrcPath;

    private String user;
    private String pwd;

    public Prober(String yamlPath, String cqlshrcPath) {
	Preconditions.checkNotNull(yamlPath, "yaml path must be provided");
	this.yamlPath = yamlPath;
	if (StringUtils.isNotBlank(cqlshrcPath)) {
	    this.cqlshrcPath = cqlshrcPath;
	    parseCqlshRcFile();
	} else {
	    LOG.info("No CQLSHRC file provided. Cassandra will be connected to without authentication");
	    this.cqlshrcPath = null;
	}

    }

    public Prober(String yamlPath) {
	this(yamlPath, null);
    }

    public static void main(String[] args) {
	if (args == null || args.length < 1) {
	    String message = "Invalid usage. Path to cassandra.yaml should be passed in as arg[0]. Path to cqlshrc file should be (optionally) passed in as arg[1]";
	    LOG.error(message);
	    System.err.println(message);
	    System.exit(1);
	}


	try {
	    final Prober app = (args.length == 2) ? new Prober(args[0], args[1]) : new Prober(args[0]);
	    app.probe();
	} catch (Exception e) {
	    String message = "Problem encountered with probing cluster: " + e.getMessage();
	    System.err.println(message);
	    e.printStackTrace(System.err);
	    LOG.error(message, e);
	    System.exit(1);

	}

	System.exit(0);

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
	    throw new RuntimeException("The CQLSHRC '" + this.cqlshrcPath + "' file does not contain any user credentials. Expected property values for keys 'username' and 'password'");
	}

    }

    private void setUserDetails(String user, String pwd) {
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
	ClusterProbe cp = new ClusterProbe(this.detectLocalHostname(), this.getYamlPath(), this.user, this.pwd);
	cp.discoverCluster();
	Set<HostProbe> hosts = cp.getHosts();
	for (HostProbe h : hosts) {
	    LOG.info("Probing Host '" + h.getToAddress() + "' : " + h);

	    boolean hostReachable = false;
	    try {
		ProbeAction isReachable = new IsReachableProbe(h, TIMEOUT_MS);
		hostReachable = isReachable.execute();
	    } catch (Exception e) {
		LOG.warn(e.getMessage(), e);
		LOG.debug(e.getMessage(), e);
	    }

	    if (hostReachable) {
		try {
		    ProbeAction nativePort = new SocketProbe("Native", h, h.getNativePort(), TIMEOUT_MS);
		    nativePort.execute();
		} catch (Exception e) {
		    LOG.warn(e.getMessage(), e);
		    LOG.debug(e.getMessage(), e);
		}

		try {
		    ProbeAction rpcPort = new SocketProbe("Thrift", h, h.getRpcPort(), TIMEOUT_MS);
		    rpcPort.execute();
		} catch (Exception e) {
		    LOG.warn(e.getMessage(), e);
		    LOG.debug(e.getMessage(), e);
		}

		try {
		    ProbeAction storagePort = new SocketProbe("Gossip", h, h.getStoragePort(), TIMEOUT_MS);
		    storagePort.execute();
		} catch (Exception e) {
		    LOG.warn(e.getMessage(), e);
		    LOG.debug(e.getMessage(), e);
		}

	    } else {
		LOG.warn("Unable to reach host '" + h.getToAddress() + "' completely - Cassandra ports will not be probed");
	    }

	}

    }

}
