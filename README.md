cassandra-probe
===============

This project is just a simple java app that reads the [cassandra.yaml](http://www.datastax.com/documentation/cassandra/2.1/cassandra/configuration/configCassandra_yaml_r.html) file off the local file system, 
connects up to [Cassandra](http://www.datastax.com/documentation/cassandra/2.1/cassandra/gettingStartedCassandraIntro.html) using the seed nodes, discovers all the nodes in the cluster 
and then attempts to check each host is reachable (currently just on the rpc_address - if your running the gossip on a different network this wont work) and, if it is, attempts to open a Socket on each of the Cassandra ports that should have Cassandra listening on it.

Each probe logs out the time in ms it takes to complete - this is the main goal. 

I keep coming across situations where there is a lack of monitoring on the network and the ability to do any probing is extremely restrictive.

Additionally - why did I implement this in Java? Well, there are some situations where the only thing I can run is a java app. Obviously, there is nothing here that couldn't be implemented in bash or whatever tool you prefer.

Next steps are to start pushing the response times on each probe to Graphite etc.. 

There are a bunch of [Vagrant images](Vagrant) in the repo also if you want to fire up [DataStax Enterprise](http://www.datastax.com) quickly to play around with.


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

You will now have an executable jar (with all the dependencies included) located at `target/cassandra-probe-exec.jar` !

Usage:
```
usage: java -jar cassandra-probe-exec.jar
 -c,--cqlshrc <CQLSHRC>                   The path to the CQLSHRC containing security credentails for Cassandra. If this is specified the security credentials will be read
                                          from this file and NOT the username/password arguments
 -con,--consistency <CONSISTENCY LEVEL>   The consistency level to use for the test CQL statement
 -cp,--contact_points <CASSANDRA HOSTS>   The contact points to use for connecting to Cassandra via the Native protocol. Note - if the cassandra.yaml is provided, this value
                                          will be ignored
 -cql,--test_cql <TEST CQL QUERY>         Test CQL query to run against cluster
 -h,--help                                Display help information
 -i,--interval <INTERVAL>                 The interval in seconds between running probe jobs. If not specified or < 1, the probe will be run once only.
 -na,--native                             Probe the native port
 -np,--native_port <PORT NUMBER>          The native port. Defaults to 9042. Note - if the cassandra.yaml is provided, this value will be ignored
 -p,--password <PASSWORD>                 The password to connect to Cassandra
 -pi,--ping                               Execute ping/isReachable probe to Cassandra host
 -sp,--storage_port <PORT NUMBER>         The storage/gossip port. Defaults to 7000. Note - if the cassandra.yaml is provided, this value will be ignored
 -st,--storage                            Probe the storage/gossip port
 -th,--thrift                             Probe the thrift port
 -tp,--thrift_port <PORT NUMBER>          The thrift port. Defaults to 9160. Note - if the cassandra.yaml is provided, this value will be ignored
 -tr,--tracing                            Enable query tracing as part of the test query. WARNING - enabling tracing persists data to the system_traces keyspace in the
                                          cluster being probed.
 -u,--username <USERNAME>                 The username to connect to Cassandra
 -y,--yaml <YAML>                         The path to the cassandra.yaml to obtain the contact points (via the seeds) and ports Cassandra is listening on
```


Copy this jar on to your Cassandra server or whichever server you want to run the probe from e.g:
```
scp target/cassandra-probe-exec.jar your_username@someremotehost:/some/remote/directory

```

Then connect up to the Cassandra server and run execute the jar.

You can have the probe run continuously with an interval between probes passed in seconds. Note - overlapping probe jobs will not occur. 

If the Cassandra cluster has authentication enabled you need to pass in the username and password or the path to the [cqlshrc](http://www.datastax.com/documentation/cql/3.1/cql/cql_reference/cqlsh.html?scroll=refCqlsh__cqlshUsingCqlshrc) file on the local file system - this will contain the security credentials needed to connect.

If you want to run the probe once only and then exit then don't pass in an in interval.

For example:
```
root@dse-cass2:/tmp# java -jar /tmp/cassandra-probe-exec.jar -cql "select * from system_auth.users where name = 'cassandra';" -consistency QUORUM -tracing -u cassandra -p cassandra -contact_points 10.211.56.111 10.211.56.112 -interval 10
12:17:07.089 [main] INFO  com.datastax.probe.App - interval: 10
12:17:07.092 [main] INFO  com.datastax.probe.App - yaml: null, contact points: [10.211.56.111, 10.211.56.112]
12:17:07.096 [main] INFO  com.datastax.probe.App - Username/password authentication is provided
12:17:07.096 [main] INFO  com.datastax.probe.App - Test CQL query 'select * from system_auth.users where name = 'cassandra';' will be executed at Consistency level of 'QUORUM' with tracing enabled true
12:17:07.097 [main] INFO  com.datastax.probe.App - Running probe continuously with an interval of 10 seconds between probes
12:17:07.142 [main] INFO  org.quartz.impl.StdSchedulerFactory - Using default implementation for ThreadExecutor
12:17:07.145 [main] INFO  org.quartz.simpl.SimpleThreadPool - Job execution threads will use class loader of thread: main
12:17:07.155 [main] INFO  o.quartz.core.SchedulerSignalerImpl - Initialized Scheduler Signaller of type: class org.quartz.core.SchedulerSignalerImpl
12:17:07.156 [main] INFO  org.quartz.core.QuartzScheduler - Quartz Scheduler v.2.2.1 created.
12:17:07.157 [main] INFO  org.quartz.simpl.RAMJobStore - RAMJobStore initialized.
12:17:07.157 [main] INFO  org.quartz.core.QuartzScheduler - Scheduler meta-data: Quartz Scheduler (v2.2.1) 'DefaultQuartzScheduler' with instanceId 'NON_CLUSTERED'
  Scheduler class: 'org.quartz.core.QuartzScheduler' - running locally.
  NOT STARTED.
  Currently in standby mode.
  Number of jobs executed: 0
  Using thread pool 'org.quartz.simpl.SimpleThreadPool' - with 10 threads.
  Using job-store 'org.quartz.simpl.RAMJobStore' - which does not support persistence. and is not clustered.

12:17:07.157 [main] INFO  org.quartz.impl.StdSchedulerFactory - Quartz scheduler 'DefaultQuartzScheduler' initialized from default resource file in Quartz package: 'quartz.properties'
12:17:07.157 [main] INFO  org.quartz.impl.StdSchedulerFactory - Quartz scheduler version: 2.2.1
12:17:07.157 [main] INFO  org.quartz.core.QuartzScheduler - Scheduler DefaultQuartzScheduler_$_NON_CLUSTERED started.
12:17:07.173 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.job.ProbeJob - ProbeJob running...
12:17:07.175 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.job.ProbeJob - Instance cassandra-probe.ProbeJob of ProbeJob yamlPath: null, and cqlshrcPath is: null
12:17:07.177 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.Prober - No CQLSHRC file provided. Cassandra will be connected to without authentication
12:17:07.177 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.Prober - Username and password provided. Cassandra connection will be authenticated
12:17:07.177 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.Prober - 

New Probe Commencing....
12:17:07.177 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.ClusterProbe - About to discover cluster 'null' details using seed contact points: [10.211.56.111, 10.211.56.112]
12:17:07.181 [DefaultQuartzScheduler_Worker-1] WARN  c.d.driver.core.FrameCompressor - Cannot find Snappy class, you should make sure the Snappy library is in the classpath if you intend to use it. Snappy compression will not be available for the protocol.
12:17:07.181 [DefaultQuartzScheduler_Worker-1] WARN  c.d.driver.core.FrameCompressor - Cannot find LZ4 class, you should make sure the LZ4 library is in the classpath if you intend to use it. LZ4 compression will not be available for the protocol.
12:17:07.589 [DefaultQuartzScheduler_Worker-1] INFO  c.d.d.c.p.DCAwareRoundRobinPolicy - Using data-center name 'DC1' for DCAwareRoundRobinPolicy (if this is incorrect, please provide the correct datacenter name with DCAwareRoundRobinPolicy constructor)
12:17:07.591 [Cassandra Java Driver worker-0] INFO  com.datastax.driver.core.Cluster - New Cassandra host /10.211.56.112:9042 added
12:17:07.591 [Cassandra Java Driver worker-1] INFO  com.datastax.driver.core.Cluster - New Cassandra host /10.211.56.111:9042 added
12:17:07.591 [Cassandra Java Driver worker-2] INFO  com.datastax.driver.core.Cluster - New Cassandra host /10.211.56.110:9042 added
12:17:07.591 [Cassandra Java Driver worker-3] INFO  com.datastax.driver.core.Cluster - New Cassandra host /10.211.56.113:9042 added
12:17:07.596 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.ClusterProbe - 
Discovered Cassandra Cluster 'null' details via native driver from host 'jpm-macbook.local' :
	Host (10.211.56.111) [
		HostAddress:		10.211.56.111
		HostName:		10.211.56.111
		Socket Canonical:	10.211.56.111
		Socket HostAddress:	10.211.56.111
		Socket HostName:	10.211.56.111
		Socket Port:		9042
		DataCenter:		DC2
		Rack:			rack1
		Is Up:			true
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
		Is Up:			false
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
		Is Up:			true
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
		Is Up:			false
		Cassandra Version:	2.0.10
		DSE Patch:		71
	]
12:17:07.596 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.Prober - Probing Host '10.211.56.111' : HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
12:17:07.597 [DefaultQuartzScheduler_Worker-1] INFO  c.d.probe.actions.IsReachableProbe - Took 0 (ms) to check host is reachable: HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
12:17:07.602 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Native - Took 3 (ms) to open Socket to host '10.211.56.111' on port '9042 : HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
12:17:07.602 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Thrift - Took 0 (ms) to open Socket to host '10.211.56.111' on port '9160 : HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
12:17:07.602 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Gossip - Took 0 (ms) to open Socket to host '10.211.56.111' on port '7000 : HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
12:17:07.602 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.Prober - Probing Host '10.211.56.110' : HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
12:17:07.603 [DefaultQuartzScheduler_Worker-1] INFO  c.d.probe.actions.IsReachableProbe - Took 0 (ms) to check host is reachable: HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
12:17:07.603 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Native - Took 0 (ms) to open Socket to host '10.211.56.110' on port '9042 : HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
12:17:07.604 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Thrift - Took 0 (ms) to open Socket to host '10.211.56.110' on port '9160 : HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
12:17:07.604 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Gossip - Took 0 (ms) to open Socket to host '10.211.56.110' on port '7000 : HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
12:17:07.604 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.Prober - Probing Host '10.211.56.113' : HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
12:17:07.605 [DefaultQuartzScheduler_Worker-1] INFO  c.d.probe.actions.IsReachableProbe - Took 0 (ms) to check host is reachable: HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
12:17:07.605 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Native - Took 0 (ms) to open Socket to host '10.211.56.113' on port '9042 : HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
12:17:07.605 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Thrift - Took 0 (ms) to open Socket to host '10.211.56.113' on port '9160 : HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
12:17:07.606 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Gossip - Took 0 (ms) to open Socket to host '10.211.56.113' on port '7000 : HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
12:17:07.606 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.Prober - Probing Host '10.211.56.112' : HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
12:17:07.606 [DefaultQuartzScheduler_Worker-1] INFO  c.d.probe.actions.IsReachableProbe - Took 0 (ms) to check host is reachable: HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
12:17:07.607 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Native - Took 0 (ms) to open Socket to host '10.211.56.112' on port '9042 : HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
12:17:07.607 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Thrift - Took 0 (ms) to open Socket to host '10.211.56.112' on port '9160 : HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
12:17:07.608 [DefaultQuartzScheduler_Worker-1] INFO  c.datastax.probe.actions.SocketProbe - Gossip - Took 0 (ms) to open Socket to host '10.211.56.112' on port '7000 : HostProbe [fromAddress=jpm-macbook.local, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
12:17:07.608 [DefaultQuartzScheduler_Worker-1] INFO  com.datastax.probe.Prober - Cluster: false
12:17:07.966 [DefaultQuartzScheduler_Worker-1] INFO  c.d.probe.actions.TestCQLQueryProbe - Validating CQL select * from system_auth.users where name = 'cassandra';
12:17:07.966 [DefaultQuartzScheduler_Worker-1] INFO  c.d.probe.actions.TestCQLQueryProbe - CQL action is select
12:17:07.966 [DefaultQuartzScheduler_Worker-1] INFO  c.d.probe.actions.TestCQLQueryProbe - About to execute synchronous CQL statement 'select * from system_auth.users where name = 'cassandra';' against Cassandra with consistency 'QUORUM' and query tracing set to true
12:17:07.966 [DefaultQuartzScheduler_Worker-1] WARN  c.d.probe.actions.TestCQLQueryProbe - Query tracing has been enabled. Please note that this will increase query response time and will persist the query trace into the Cassandra system tables.
12:17:07.980 [DefaultQuartzScheduler_Worker-1] INFO  c.d.probe.actions.TestCQLQueryProbe - Took 13 (ms) to execute test query against Cassandra cluster with query tracing set to true
12:17:08.999 [DefaultQuartzScheduler_Worker-1] INFO  c.d.probe.actions.TestCQLQueryProbe - 
Query trace for 'select * from system_auth.users where name = 'cassandra';'
Host (queried): 10.211.56.112/10.211.56.112:9042
Host (tried): 10.211.56.112/10.211.56.112:9042
Trace id: afa80d60-6413-11e4-81ab-7557d54dde1e

activity                                                                         | timestamp    | source               | source_elapsed
---------------------------------------------------------------------------------+--------------+----------------------+--------------
                                            Message received from /10.211.56.112 | 12:14:03.583 |       /10.211.56.111 |           13
                                       Executing single-partition query on users | 12:14:03.583 |       /10.211.56.111 |          226
                                                    Acquiring sstable references | 12:14:03.583 |       /10.211.56.111 |          236
                                                     Merging memtable tombstones | 12:14:03.583 |       /10.211.56.111 |          270
                                                     Key cache hit for sstable 1 | 12:14:03.583 |       /10.211.56.111 |          340
                                     Seeking to partition beginning in data file | 12:14:03.583 |       /10.211.56.111 |          348
       Skipped 0/1 non-slice-intersecting sstables, included 0 due to tombstones | 12:14:03.583 |       /10.211.56.111 |          395
                                      Merging data from memtables and 1 sstables | 12:14:03.583 |       /10.211.56.111 |          402
                                              Read 1 live and 0 tombstoned cells | 12:14:03.583 |       /10.211.56.111 |          421
                                            Enqueuing response to /10.211.56.112 | 12:14:03.584 |       /10.211.56.111 |          582
                                               Sending message to /10.211.56.112 | 12:14:03.584 |       /10.211.56.111 |          743
                                            Message received from /10.211.56.112 | 12:14:03.586 |       /10.211.56.111 |            9
                                       Executing single-partition query on users | 12:14:03.586 |       /10.211.56.111 |          184
                                                    Acquiring sstable references | 12:14:03.586 |       /10.211.56.111 |          194
                                                     Merging memtable tombstones | 12:14:03.586 |       /10.211.56.111 |          226
                                                     Key cache hit for sstable 1 | 12:14:03.586 |       /10.211.56.111 |          301
                                     Seeking to partition beginning in data file | 12:14:03.586 |       /10.211.56.111 |          312
       Skipped 0/1 non-slice-intersecting sstables, included 0 due to tombstones | 12:14:03.586 |       /10.211.56.111 |          371
                                      Merging data from memtables and 1 sstables | 12:14:03.586 |       /10.211.56.111 |          382
                                              Read 1 live and 0 tombstoned cells | 12:14:03.586 |       /10.211.56.111 |          407
                                            Enqueuing response to /10.211.56.112 | 12:14:03.587 |       /10.211.56.111 |          538
                                               Sending message to /10.211.56.112 | 12:14:03.587 |       /10.211.56.111 |          618
               Parsing select * from system_auth.users where name = 'cassandra'; | 12:14:04.214 |       /10.211.56.112 |          103
                                                             Preparing statement | 12:14:04.214 |       /10.211.56.112 |          211
                                               Sending message to /10.211.56.111 | 12:14:04.215 |       /10.211.56.112 |          809
                                            Message received from /10.211.56.111 | 12:14:04.217 |       /10.211.56.112 |         3298
                                         Processing response from /10.211.56.111 | 12:14:04.217 |       /10.211.56.112 |         3365
                                               Sending message to /10.211.56.111 | 12:14:04.218 |       /10.211.56.112 |         3965
                                            Message received from /10.211.56.111 | 12:14:04.219 |       /10.211.56.112 |         5545
                                         Processing response from /10.211.56.111 | 12:14:04.220 |       /10.211.56.112 |         5756

12:17:17.109 [DefaultQuartzScheduler_Worker-2] INFO  com.datastax.probe.job.ProbeJob - ProbeJob running...
12:17:17.109 [DefaultQuartzScheduler_Worker-2] INFO  com.datastax.probe.job.ProbeJob - Instance cassandra-probe.ProbeJob of ProbeJob yamlPath: null, and cqlshrcPath is: null
12:17:17.109 [DefaultQuartzScheduler_Worker-2] INFO  com.datastax.probe.Prober - No CQLSHRC file provided. Cassandra will be connected to without authentication
12:17:17.109 [DefaultQuartzScheduler_Worker-2] INFO  com.datastax.probe.Prober - Username and password provided. Cassandra connection will be authenticated
12:17:17.109 [DefaultQuartzScheduler_Worker-2] INFO  com.datastax.probe.Prober - 

New Probe Commencing....
12:17:17.110 [DefaultQuartzScheduler_Worker-2] INFO  com.datastax.probe.ClusterProbe - About to discover cluster 'null' details using seed contact points: [10.211.56.111, 10.211.56.112]
12:17:17.303 [DefaultQuartzScheduler_Worker-2] INFO  c.d.d.c.p.DCAwareRoundRobinPolicy - Using data-center name 'DC1' for DCAwareRoundRobinPolicy (if this is incorrect, please provide the correct datacenter name with DCAwareRoundRobinPolicy constructor)

```




