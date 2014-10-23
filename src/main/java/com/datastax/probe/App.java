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

public class App {

    private static final int TIMEOUT_MS = 10000;

    private static final Logger LOG = LoggerFactory.getLogger(App.class);
    private String yamlPath;
    private String user;
    private String pwd;

    public static void main(String[] args) {
	if (args == null || args.length < 1) {
	    String message = "Invalid usage. Path to cassandra.yaml should be passed in as arg[0]. Path to .cqlshrc should be (optionally) passed in as arg[1]";
	    LOG.error(message);
	    System.err.println(message);
	    System.exit(1);
	}

	String yamlPath = args[0];

	App app = new App();
	app.setYamlPath(yamlPath);

	try {
	    if (args.length == 2) {
		String cqlshRc = args[1];
		LOG.info("CQLSHRC file will be read from '" + cqlshRc + "'");
		if (StringUtils.isNotEmpty(cqlshRc)) {
		    LOG.info("Reading cassandra login credentials from CQLSHRC file located at " + cqlshRc);
		    app.parseCqlshRcFile(cqlshRc);
		} else {
		    LOG.info("No CQLSHRC passed in. No login credentials will be used...");
		}
	    } else {
		LOG.info("No CQLSHRC file provided. Cassandra will be connected to without authentication");
	    }

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

    private void parseCqlshRcFile(String path) throws IOException {
	Preconditions.checkNotNull(path, "The path to CQLSHRC file can not be null or empty");

	String userName = null;
	String password = null;

	File cqlshRcFile = new File(path);
	if (!cqlshRcFile.exists()) {
	    throw new RuntimeException("The CQLSHRC '" + path + "' file does not exist");
	}

	if (!cqlshRcFile.canRead()) {
	    throw new RuntimeException("The CQLSHRC '" + path + "' file can not be read");
	}

	Properties props = null;
	try {
	    props = new Properties();
	    props.load(new FileInputStream(cqlshRcFile));
	    userName = (String) props.get("username");
	    password = (String) props.get("password");
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    LOG.error("Problem encountered loading property CQLSHRC property file '" + path + "' : " + e.getMessage(), e);
	    throw new RuntimeException("Problem encountered loading property CQLSHRC property file '" + path + "' : " + e.getMessage(), e);
	} finally {
	    if (props != null) {
		props.clear();
		props = null;
	    }
	}

	if (StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(password)) {
	    this.setUserDetails(userName, password);
	} else {
	    throw new RuntimeException("The CQLSHRC '" + path + "' file does not contain user credentials");
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

    public void setYamlPath(String yamlPath) {
	this.yamlPath = yamlPath;
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
