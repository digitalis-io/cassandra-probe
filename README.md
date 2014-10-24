cassandra-probe
===============

This project is just a simple java app that reads the cassandra.yaml file off the local file system, connects up to Cassandra using the seed nodes, discovers all the nodes in the cluster and then attempts to check each host is reachable and, if it is, attempts to open a Socket on each of the Cassandra ports that should have Cassandra listening on it.

Each probe logs out the time in ms it takes to complete - this is the main goal. 

I keep coming across situations where there is a lack of monitoring on the network
and the ability to do any probing is extremely restrictive.

Additionally - why did I implement this in Java? Well, there are some situations where the only thing I can run is a java app. Obviously, there is nothing here that couldn't be implemented in bash or whatever tool you prefer.

Next steps are to start pushing the response times on each probe to Graphite etc.. 

There are a bunch of Vagrant images in the repo also if you want to fire up DataStax Enterprise quickly to play around with.



Getting started
---------------

Clone the repo:
```
git clone https://github.com/millerjp/cassandra-probe.git
cd cassandra-probe
```

Build the executable jar:
```
mvn clean compile assembly:single

```

You will now have an executable jar with all the dependencies in there. It is located in `target/cassandra-probe-jar-with-dependencies.jar` !

Copy this jar on to your Cassandra server e.g:
```
scp target/cassandra-probe-jar-with-dependencies.jar your_username@someremotehost:/some/remote/directory

```

Then connect up to the Cassandra server and run the command
```
java -jar /some/remote/directory/cassandra-probe-jar-with-dependencies.jar <interval_in_seconds_between probes> <path_to_cassandra_yaml> <path_to_cqlshrc_file>
```

You can have the probe run continuously with an interval between probes passed in seconds. Note - overlapping probe jobs will not occur. 

Alternatively, if you want to run the probe once only and then exit then pass in an in interval < 1.

For example:
```
root@dse-cass2:/tmp# java -jar 5 /tmp/cassandra-probe-jar-with-dependencies.jar /etc/dse/cassandra/cassandra.yaml /etc/dse/cassandra/cqlshrc.default
08:23:34.288 [main] INFO  com.datastax.probe.App - interval provided as '5'
08:23:34.291 [main] INFO  com.datastax.probe.App - yamlPath provided as '/apps/github/cassandra-probe/cassandra.yaml'
08:23:34.291 [main] INFO  com.datastax.probe.App - No cqlshrc path provided. Cassandra will be connected to without authentication
08:23:34.291 [main] INFO  com.datastax.probe.App - Running probe continuously with an interval of 5 seconds between probes
08:23:34.334 [main] INFO  org.quartz.impl.StdSchedulerFactory - Using default implementation for ThreadExecutor
08:23:34.337 [main] INFO  org.quartz.simpl.SimpleThreadPool - Job execution threads will use class loader of thread: main
08:23:34.350 [main] INFO  o.quartz.core.SchedulerSignalerImpl - Initialized Scheduler Signaller of type: class org.quartz.core.SchedulerSignalerImpl
08:23:34.351 [main] INFO  org.quartz.core.QuartzScheduler - Quartz Scheduler v.2.2.1 created.
08:23:34.352 [main] INFO  org.quartz.simpl.RAMJobStore - RAMJobStore initialized.
08:23:34.353 [main] INFO  org.quartz.core.QuartzScheduler - Scheduler meta-data: Quartz Scheduler (v2.2.1) 'DefaultQuartzScheduler' with instanceId 'NON_CLUSTERED'
  Scheduler class: 'org.quartz.core.QuartzScheduler' - running locally.
  NOT STARTED.
  Currently in standby mode.
  Number of jobs executed: 0
  Using thread pool 'org.quartz.simpl.SimpleThreadPool' - with 10 threads.
  Using job-store 'org.quartz.simpl.RAMJobStore' - which does not support persistence. and is not clustered.

08:23:34.353 [main] INFO  org.quartz.impl.StdSchedulerFactory - Quartz scheduler 'DefaultQuartzScheduler' initialized from default resource file in Quartz package: 'quartz.properties'
08:23:34.353 [main] INFO  org.quartz.impl.StdSchedulerFactory - Quartz scheduler version: 2.2.1
08:23:34.353 [main] INFO  org.quartz.core.QuartzScheduler - Scheduler DefaultQuartzScheduler_$_NON_CLUSTERED started.
08:23:34.372 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.job.ProbeJob - ProbeJob running...
08:23:34.374 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.job.ProbeJob - Instance cassandra-probe.ProbeJob of ProbeJob yamlPath: /apps/github/cassandra-probe/cassandra.yaml, and cqlshrcPath is: null
08:23:34.378 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.Prober - No CQLSHRC file provided. Cassandra will be connected to without authentication
08:23:34.445 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.ClusterProbe - About to discover cluster 'dse-cass' details using seed contact points: [10.211.56.110, 10.211.56.111]
08:23:34.452 [DefaultQuartzScheduler_Worker-1] WARN  c.d.driver.core.FrameCompressor - Cannot find Snappy class, you should make sure the Snappy library is in the classpath if you intend to use it. Snappy compression will not be available for the protocol.
08:23:34.453 [DefaultQuartzScheduler_Worker-1] WARN  c.d.driver.core.FrameCompressor - Cannot find LZ4 class, you should make sure the LZ4 library is in the classpath if you intend to use it. LZ4 compression will not be available for the protocol.
08:23:34.748 [DefaultQuartzScheduler_Worker-1] INFO  c.d.d.c.p.DCAwareRoundRobinPolicy - Using data-center name 'DC1' for DCAwareRoundRobinPolicy (if this is incorrect, please provide the correct datacenter name with DCAwareRoundRobinPolicy constructor)
08:23:34.757 [Cassandra Java Driver worker-0] INFO  com.datastax.driver.core.Cluster - New Cassandra host /10.211.56.112:9042 added
08:23:34.757 [Cassandra Java Driver worker-1] INFO  com.datastax.driver.core.Cluster - New Cassandra host /10.211.56.111:9042 added
08:23:34.758 [Cassandra Java Driver worker-2] INFO  com.datastax.driver.core.Cluster - New Cassandra host /10.211.56.110:9042 added
08:23:34.758 [Cassandra Java Driver worker-3] INFO  com.datastax.driver.core.Cluster - New Cassandra host /10.211.56.113:9042 added
08:23:34.761 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.ClusterProbe - 
Discovered Cassandra Cluster 'dse-cass' details via native driver from host 'jpm-macbook' :
	Host (10.211.56.111) [
		HostAddress:		10.211.56.111
		HostName:		10.211.56.111
		Socket Canonical:	10.211.56.111
		Socket HostAddress:	10.211.56.111
		Socket HostName:	10.211.56.111
		Socket Port:		9042
		DataCenter:		DC2
		Rack:			rack1
		Cassandra Version:	2.0.10
		DSE Patch:		71
	]
	Host (10.211.56.110) [
		HostAddress:		10.211.56.110
		HostName:		10.211.56.110
		Socket Canonical:	10.211.56.110
		Socket HostAddress:	10.211.56.110
		Socket HostName:	10.211.56.110
		Socket Port:		9042
		DataCenter:		DC1
		Rack:			rack1
		Cassandra Version:	2.0.10
		DSE Patch:		71
	]
	Host (10.211.56.113) [
		HostAddress:		10.211.56.113
		HostName:		10.211.56.113
		Socket Canonical:	10.211.56.113
		Socket HostAddress:	10.211.56.113
		Socket HostName:	10.211.56.113
		Socket Port:		9042
		DataCenter:		DC2
		Rack:			rack3
		Cassandra Version:	2.0.10
		DSE Patch:		71
	]
	Host (10.211.56.112) [
		HostAddress:		10.211.56.112
		HostName:		10.211.56.112
		Socket Canonical:	10.211.56.112
		Socket HostAddress:	10.211.56.112
		Socket HostName:	10.211.56.112
		Socket Port:		9042
		DataCenter:		DC1
		Rack:			rack2
		Cassandra Version:	2.0.10
		DSE Patch:		71
	]
08:23:34.772 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.Prober - Probing Host '10.211.56.111' : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
08:23:34.773 [DefaultQuartzScheduler_Worker-1] INFO  c.d.probe.actions.IsReachableProbe - Took 0 (ms) to check host is reachable: HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
08:23:34.777 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Native - Took 3 (ms) to open Socket to host '10.211.56.111' on port '9042 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
08:23:34.777 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Thrift - Took 0 (ms) to open Socket to host '10.211.56.111' on port '9160 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
08:23:34.778 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Gossip - Took 0 (ms) to open Socket to host '10.211.56.111' on port '7000 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
08:23:34.778 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.Prober - Probing Host '10.211.56.110' : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
08:23:34.778 [DefaultQuartzScheduler_Worker-1] INFO  c.d.probe.actions.IsReachableProbe - Took 0 (ms) to check host is reachable: HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
08:23:34.779 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Native - Took 0 (ms) to open Socket to host '10.211.56.110' on port '9042 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
08:23:34.779 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Thrift - Took 0 (ms) to open Socket to host '10.211.56.110' on port '9160 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
08:23:34.779 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Gossip - Took 0 (ms) to open Socket to host '10.211.56.110' on port '7000 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
08:23:34.780 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.Prober - Probing Host '10.211.56.113' : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
08:23:34.780 [DefaultQuartzScheduler_Worker-1] INFO  c.d.probe.actions.IsReachableProbe - Took 0 (ms) to check host is reachable: HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
08:23:34.780 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Native - Took 0 (ms) to open Socket to host '10.211.56.113' on port '9042 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
08:23:34.781 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Thrift - Took 0 (ms) to open Socket to host '10.211.56.113' on port '9160 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
08:23:34.782 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Gossip - Took 0 (ms) to open Socket to host '10.211.56.113' on port '7000 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
08:23:34.782 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.Prober - Probing Host '10.211.56.112' : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
08:23:34.783 [DefaultQuartzScheduler_Worker-1] INFO  c.d.probe.actions.IsReachableProbe - Took 0 (ms) to check host is reachable: HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
08:23:34.784 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Native - Took 0 (ms) to open Socket to host '10.211.56.112' on port '9042 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
08:23:34.785 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Thrift - Took 0 (ms) to open Socket to host '10.211.56.112' on port '9160 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
08:23:34.785 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Gossip - Took 0 (ms) to open Socket to host '10.211.56.112' on port '7000 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
08:23:34.785 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.job.ProbeJob - ProbeJob ran - took 410 ms to run complete job
08:23:39.297 [DefaultQuartzScheduler_Worker-2] INFO  com.datastax.probe.job.ProbeJob - ProbeJob running...
08:23:39.297 [DefaultQuartzScheduler_Worker-2] INFO  com.datastax.probe.job.ProbeJob - Instance cassandra-probe.ProbeJob of ProbeJob yamlPath: /apps/github/cassandra-probe/cassandra.yaml, and cqlshrcPath is: null
08:23:39.297 [DefaultQuartzScheduler_Worker-2] INFO  com.datastax.probe.Prober - No CQLSHRC file provided. Cassandra will be connected to without authentication
08:23:39.307 [DefaultQuartzScheduler_Worker-2] INFO  com.datastax.probe.ClusterProbe - About to discover cluster 'dse-cass' details using seed contact points: [10.211.56.110, 10.211.56.111]
08:23:39.403 [DefaultQuartzScheduler_Worker-2] INFO  c.d.d.c.p.DCAwareRoundRobinPolicy - Using data-center name 'DC1' for DCAwareRoundRobinPolicy (if this is incorrect, please provide the correct datacenter name with DCAwareRoundRobinPolicy constructor)
08:23:39.403 [Cassandra Java Driver worker-0] INFO  com.datastax.driver.core.Cluster - New Cassandra host /10.211.56.112:9042 added
08:23:39.403 [Cassandra Java Driver worker-1] INFO  com.datastax.driver.core.Cluster - New Cassandra host /10.211.56.111:9042 added
08:23:39.403 [Cassandra Java Driver worker-2] INFO  com.datastax.driver.core.Cluster - New Cassandra host /10.211.56.110:9042 added
08:23:39.404 [Cassandra Java Driver worker-3] INFO  com.datastax.driver.core.Cluster - New Cassandra host /10.211.56.113:9042 added
08:23:39.405 [DefaultQuartzScheduler_Worker-2] INFO  com.datastax.probe.ClusterProbe - 
Discovered Cassandra Cluster 'dse-cass' details via native driver from host 'jpm-macbook' :
	Host (10.211.56.111) [
		HostAddress:		10.211.56.111
		HostName:		10.211.56.111
		Socket Canonical:	10.211.56.111
		Socket HostAddress:	10.211.56.111
		Socket HostName:	10.211.56.111
		Socket Port:		9042
		DataCenter:		DC2
		Rack:			rack1
		Cassandra Version:	2.0.10
		DSE Patch:		71
	]
	Host (10.211.56.110) [
		HostAddress:		10.211.56.110
		HostName:		10.211.56.110
		Socket Canonical:	10.211.56.110
		Socket HostAddress:	10.211.56.110
		Socket HostName:	10.211.56.110
		Socket Port:		9042
		DataCenter:		DC1
		Rack:			rack1
		Cassandra Version:	2.0.10
		DSE Patch:		71
	]
	Host (10.211.56.113) [
		HostAddress:		10.211.56.113
		HostName:		10.211.56.113
		Socket Canonical:	10.211.56.113
		Socket HostAddress:	10.211.56.113
		Socket HostName:	10.211.56.113
		Socket Port:		9042
		DataCenter:		DC2
		Rack:			rack3
		Cassandra Version:	2.0.10
		DSE Patch:		71
	]
	Host (10.211.56.112) [
		HostAddress:		10.211.56.112
		HostName:		10.211.56.112
		Socket Canonical:	10.211.56.112
		Socket HostAddress:	10.211.56.112
		Socket HostName:	10.211.56.112
		Socket Port:		9042
		DataCenter:		DC1
		Rack:			rack2
		Cassandra Version:	2.0.10
		DSE Patch:		71
	]
08:23:39.411 [DefaultQuartzScheduler_Worker-2] INFO  com.datastax.probe.Prober - Probing Host '10.211.56.111' : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
08:23:39.412 [DefaultQuartzScheduler_Worker-2] INFO  c.d.probe.actions.IsReachableProbe - Took 0 (ms) to check host is reachable: HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
08:23:39.412 [DefaultQuartzScheduler_Worker-2] INFO  c.datastax.probe.actions.SocketProbe - Native - Took 0 (ms) to open Socket to host '10.211.56.111' on port '9042 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
08:23:39.412 [DefaultQuartzScheduler_Worker-2] INFO  c.datastax.probe.actions.SocketProbe - Thrift - Took 0 (ms) to open Socket to host '10.211.56.111' on port '9160 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
08:23:39.413 [DefaultQuartzScheduler_Worker-2] INFO  c.datastax.probe.actions.SocketProbe - Gossip - Took 0 (ms) to open Socket to host '10.211.56.111' on port '7000 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
08:23:39.413 [DefaultQuartzScheduler_Worker-2] INFO  com.datastax.probe.Prober - Probing Host '10.211.56.110' : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
08:23:39.413 [DefaultQuartzScheduler_Worker-2] INFO  c.d.probe.actions.IsReachableProbe - Took 0 (ms) to check host is reachable: HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
08:23:39.413 [DefaultQuartzScheduler_Worker-2] INFO  c.datastax.probe.actions.SocketProbe - Native - Took 0 (ms) to open Socket to host '10.211.56.110' on port '9042 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
08:23:39.414 [DefaultQuartzScheduler_Worker-2] INFO  c.datastax.probe.actions.SocketProbe - Thrift - Took 0 (ms) to open Socket to host '10.211.56.110' on port '9160 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
08:23:39.414 [DefaultQuartzScheduler_Worker-2] INFO  c.datastax.probe.actions.SocketProbe - Gossip - Took 0 (ms) to open Socket to host '10.211.56.110' on port '7000 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
08:23:39.414 [DefaultQuartzScheduler_Worker-2] INFO  com.datastax.probe.Prober - Probing Host '10.211.56.113' : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
08:23:39.415 [DefaultQuartzScheduler_Worker-2] INFO  c.d.probe.actions.IsReachableProbe - Took 0 (ms) to check host is reachable: HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
08:23:39.415 [DefaultQuartzScheduler_Worker-2] INFO  c.datastax.probe.actions.SocketProbe - Native - Took 0 (ms) to open Socket to host '10.211.56.113' on port '9042 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
08:23:39.416 [DefaultQuartzScheduler_Worker-2] INFO  c.datastax.probe.actions.SocketProbe - Thrift - Took 0 (ms) to open Socket to host '10.211.56.113' on port '9160 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
08:23:39.416 [DefaultQuartzScheduler_Worker-2] INFO  c.datastax.probe.actions.SocketProbe - Gossip - Took 0 (ms) to open Socket to host '10.211.56.113' on port '7000 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
08:23:39.416 [DefaultQuartzScheduler_Worker-2] INFO  com.datastax.probe.Prober - Probing Host '10.211.56.112' : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
08:23:39.417 [DefaultQuartzScheduler_Worker-2] INFO  c.d.probe.actions.IsReachableProbe - Took 0 (ms) to check host is reachable: HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
08:23:39.417 [DefaultQuartzScheduler_Worker-2] INFO  c.datastax.probe.actions.SocketProbe - Native - Took 0 (ms) to open Socket to host '10.211.56.112' on port '9042 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
08:23:39.418 [DefaultQuartzScheduler_Worker-2] INFO  c.datastax.probe.actions.SocketProbe - Thrift - Took 0 (ms) to open Socket to host '10.211.56.112' on port '9160 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
08:23:39.419 [DefaultQuartzScheduler_Worker-2] INFO  c.datastax.probe.actions.SocketProbe - Gossip - Took 0 (ms) to open Socket to host '10.211.56.112' on port '7000 : HostProbe [fromAddress=jpm-macbook, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
08:23:39.419 [DefaultQuartzScheduler_Worker-2] INFO  com.datastax.probe.job.ProbeJob - ProbeJob ran - took 121 ms to run complete job

```




