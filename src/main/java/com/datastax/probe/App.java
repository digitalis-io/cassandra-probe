package com.datastax.probe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
    }
    
    public App() {
    }

    public String getYamlPath() {
	return yamlPath;
    }

    public void setYamlPath(String yamlPath) {
	this.yamlPath = yamlPath;
    }

}
