package com.datastax.probe;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.probe.actions.FatalProbeException;
import com.datastax.probe.actions.IsReachable;
import com.datastax.probe.actions.ProbeAction;
import com.datastax.probe.model.HostProbe;

public class App {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);
    private String yamlPath;

    public static void main(String[] args) {
	if (args == null || args.length != 1) {
	    String message = "Invalid usage. Path to cassandra.yaml should be passed in as arg[0]";
	    LOG.error(message);
	    System.err.println(message);
	    System.exit(1);
	}

	String yamlPath = args[0];
	App app = new App();
	app.setYamlPath(yamlPath);
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

    public App() {
    }

    public String getYamlPath() {
	return yamlPath;
    }

    public void setYamlPath(String yamlPath) {
	this.yamlPath = yamlPath;
    }

    public void probe() throws FatalProbeException, IOException {
	ClusterProbe cp = new ClusterProbe(this.getYamlPath());
	cp.discoverCluster();
	Set<HostProbe> hosts = cp.getHosts();
	for (HostProbe h : hosts) {
	    LOG.info("Probing Host: " + h);
	    ProbeAction isReachable = new IsReachable(h);
	    isReachable.execute();
	    
	}

    }

}
