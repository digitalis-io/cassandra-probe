package com.datastax.probe;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.probe.actions.FatalProbeException;
import com.datastax.probe.actions.IsReachableProbe;
import com.datastax.probe.actions.ProbeAction;
import com.datastax.probe.actions.PortProbe;
import com.datastax.probe.model.HostProbe;

public class App {
    
    private static final int TIMEOUT_MS = 10000;
    
    private static final Logger LOG = LoggerFactory.getLogger(App.class);
    private String yamlPath;
    private String user;
    private String pwd;

    public static void main(String[] args) {
	if (args == null || args.length < 1) {
	    String message = "Invalid usage. Path to cassandra.yaml should be passed in as arg[0]. Cassandra Username environment variable (optional) as arg[1] amd Cassandra Password environment variable (optional) as arg[2]";
	    LOG.error(message);
	    System.err.println(message);
	    System.exit(1);
	}

	String yamlPath = args[0];
	String user = null;
	String pwd = null;
	if (args.length > 1) {
	    LOG.info("Reading cassandra login credentials from environment variables...");
	    String cassandraUsernameEnv = args[1];
	    String cassandraPwdEnv = args[2];
	    user = System.getenv(cassandraUsernameEnv);
	    pwd = System.getenv(cassandraPwdEnv);
	} else {
	    LOG.info("No login credentials required...");
	}

	App app = new App();
	app.setYamlPath(yamlPath);
	if (user != null) {
	    app.setUserDetails(user, pwd);
	}

	try {
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
	    LOG.info("Probing Host: " + h);
	    try {
		ProbeAction isReachable = new IsReachableProbe(h, TIMEOUT_MS);
		isReachable.execute();
	    } catch (Exception e) {
		LOG.warn(e.getMessage());
		LOG.debug(e.getMessage(), e);
	    }

	    try {
		ProbeAction nativePort = new PortProbe("Native", h, h.getNativePort(), TIMEOUT_MS);
		nativePort.execute();
	    } catch (Exception e) {
		LOG.warn(e.getMessage());
		LOG.debug(e.getMessage(), e);
	    }

	    try {
		ProbeAction rpcPort = new PortProbe("Thrift", h, h.getRpcPort(), TIMEOUT_MS);
		rpcPort.execute();
	    } catch (Exception e) {
		LOG.warn(e.getMessage());
		LOG.debug(e.getMessage(), e);
	    }
	    
	    try {
		ProbeAction storagePort = new PortProbe("Gossip", h, h.getStoragePort(), TIMEOUT_MS);
		storagePort.execute();
	    } catch (Exception e) {
		LOG.warn(e.getMessage());
		LOG.debug(e.getMessage(), e);
	    }
	    
	}

    }

}
