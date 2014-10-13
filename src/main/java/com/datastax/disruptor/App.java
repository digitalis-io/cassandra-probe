package com.datastax.disruptor;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.datastax.driver.core.Host;
import com.datastax.probe.ClusterProbe;
import com.datastax.probe.model.IsReachable;
import com.google.common.base.Preconditions;

public class App {

    public static void main(String[] args) throws Exception {
	Preconditions.checkNotNull(args);
	String yamlPath = args[0];
	Preconditions.checkNotNull(yamlPath);
	
	
	App app = new App();
	app.multiThreadUpdatesTest(yamlPath);
    }

    public void multiThreadUpdatesTest(String yamlPath) throws InterruptedException, IOException {
	ClusterProbe cluster = new ClusterProbe(yamlPath);
	IsReachableProcessor processor = new IsReachableProcessor(cluster);
	processor.init();
	
	Set<Host> hosts = cluster.getHosts();
	
	Thread t1 = new Thread(new DemoWorker(hosts, processor));
	//Thread t2 = new Thread(new DemoWorker(hosts, processor));
	t1.start();
	//t2.start();

	// Wait for the transactions to filter through, of course you would
	// usually have the transaction processor lifecycle managed by a 
	// container or in some other more sophisticated way...
	try {
	    Thread.sleep(3000);
	} catch (Exception ignored) {
	}

	processor.destroy();

	cluster.dumpResults();
    }

    class DemoWorker implements Runnable {
	private IsReachableProcessor ip;
	private Set<Host> hosts;

	public DemoWorker(Set<Host> hosts, IsReachableProcessor ip) {
	    this.ip = ip;
	    this.hosts = hosts;
	}

	public void run() {
	    for (Host host : hosts) {
		ip.postIsReachable(host);
	    }
	}

    }

}
