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
java -jar /some/remote/directory/cassandra-probe-jar-with-dependencies.jar <path_to_cassandra_yaml> <path_to_cqlshrc_file>
```

For example:
```
root@dse-cass2:/tmp# java -jar /tmp/cassandra-probe-jar-with-dependencies.jar /etc/dse/cassandra/cassandra.yaml /etc/dse/cassandra/cqlshrc.default
19:02:37.011 [main] INFO  com.datastax.probe.App - No login credentials required...
19:02:37.103 [main] INFO  com.datastax.probe.ClusterProbe - About to discover cluster 'dse-cass' details using seed contact points: [10.211.56.110, 10.211.56.111]
19:02:37.109 [main] WARN  c.d.driver.core.FrameCompressor - Cannot find Snappy class, you should make sure the Snappy library is in the classpath if you intend to use it. Snappy compression will not be available for the protocol.
19:02:37.109 [main] WARN  c.d.driver.core.FrameCompressor - Cannot find LZ4 class, you should make sure the LZ4 library is in the classpath if you intend to use it. LZ4 compression will not be available for the protocol.
19:02:37.462 [main] INFO  c.d.d.c.p.DCAwareRoundRobinPolicy - Using data-center name 'DC1' for DCAwareRoundRobinPolicy (if this is incorrect, please provide the correct datacenter name with DCAwareRoundRobinPolicy constructor)
19:02:37.465 [Cassandra Java Driver worker-0] INFO  com.datastax.driver.core.Cluster - New Cassandra host /10.211.56.112:9042 added
19:02:37.468 [Cassandra Java Driver worker-1] INFO  com.datastax.driver.core.Cluster - New Cassandra host dse-cass2/10.211.56.111:9042 added
19:02:37.468 [main] INFO  com.datastax.probe.ClusterProbe -
Discovered Cassandra Cluster 'dse-cass' details via native driver from host 'dse-cass2' :
	Host (dse-cass2) [
		HostAddress:		10.211.56.111
		HostName:		dse-cass2
		Socket Canonical:	dse-cass2
		Socket HostAddress:	10.211.56.111
		Socket HostName:	dse-cass2
		Socket Port:		9042
		DataCenter:		DC2
		Rack:			rack1
		Cassandra Version:	2.0.10
		DSE Patch:		71
	]
	Host (dse-cass1) [
		HostAddress:		10.211.56.110
		HostName:		dse-cass1
		Socket Canonical:	dse-cass1
		Socket HostAddress:	10.211.56.110
		Socket HostName:	dse-cass1
		Socket Port:		9042
		DataCenter:		DC1
		Rack:			rack1
		Cassandra Version:	2.0.10
		DSE Patch:		71
	]
	Host (dse-cass4) [
		HostAddress:		10.211.56.113
		HostName:		dse-cass4
		Socket Canonical:	dse-cass4
		Socket HostAddress:	10.211.56.113
		Socket HostName:	dse-cass4
		Socket Port:		9042
		DataCenter:		DC2
		Rack:			rack3
		Cassandra Version:	2.0.10
		DSE Patch:		71
	]
	Host (dse-cass3) [
		HostAddress:		10.211.56.112
		HostName:		dse-cass3
		Socket Canonical:	dse-cass3
		Socket HostAddress:	10.211.56.112
		Socket HostName:	dse-cass3
		Socket Port:		9042
		DataCenter:		DC1
		Rack:			rack2
		Cassandra Version:	2.0.10
		DSE Patch:		71
	]
19:02:37.468 [Cassandra Java Driver worker-1] INFO  com.datastax.driver.core.Cluster - New Cassandra host dse-cass1/10.211.56.110:9042 added
19:02:37.479 [Cassandra Java Driver worker-1] INFO  com.datastax.driver.core.Cluster - New Cassandra host dse-cass4/10.211.56.113:9042 added
19:02:37.494 [main] INFO  com.datastax.probe.App - Probing Host '10.211.56.111' : HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
19:02:37.497 [main] INFO  c.d.probe.actions.IsReachableProbe - Took 0 (ms) to check host is reachable: HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
19:02:37.501 [main] INFO  c.datastax.probe.actions.SocketProbe - Native - Took 3 (ms) to open Socket to host '10.211.56.111' on port '9042 : HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
19:02:37.502 [main] INFO  c.datastax.probe.actions.SocketProbe - Thrift - Took 0 (ms) to open Socket to host '10.211.56.111' on port '9160 : HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
19:02:37.503 [main] INFO  c.datastax.probe.actions.SocketProbe - Gossip - Took 0 (ms) to open Socket to host '10.211.56.111' on port '7000 : HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.111, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack1, cassandraVersion=2.0.10]
19:02:37.503 [main] INFO  com.datastax.probe.App - Probing Host '10.211.56.110' : HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
19:02:37.504 [main] INFO  c.d.probe.actions.IsReachableProbe - Took 0 (ms) to check host is reachable: HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
19:02:37.505 [main] INFO  c.datastax.probe.actions.SocketProbe - Native - Took 0 (ms) to open Socket to host '10.211.56.110' on port '9042 : HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
19:02:37.506 [main] INFO  c.datastax.probe.actions.SocketProbe - Thrift - Took 0 (ms) to open Socket to host '10.211.56.110' on port '9160 : HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
19:02:37.510 [main] INFO  c.datastax.probe.actions.SocketProbe - Gossip - Took 4 (ms) to open Socket to host '10.211.56.110' on port '7000 : HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.110, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack1, cassandraVersion=2.0.10]
19:02:37.510 [main] INFO  com.datastax.probe.App - Probing Host '10.211.56.113' : HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
19:02:37.511 [main] INFO  c.d.probe.actions.IsReachableProbe - Took 0 (ms) to check host is reachable: HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
19:02:37.511 [main] INFO  c.datastax.probe.actions.SocketProbe - Native - Took 0 (ms) to open Socket to host '10.211.56.113' on port '9042 : HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
19:02:37.512 [main] INFO  c.datastax.probe.actions.SocketProbe - Thrift - Took 0 (ms) to open Socket to host '10.211.56.113' on port '9160 : HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
19:02:37.513 [main] INFO  c.datastax.probe.actions.SocketProbe - Gossip - Took 0 (ms) to open Socket to host '10.211.56.113' on port '7000 : HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.113, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC2, rack=rack3, cassandraVersion=2.0.10]
19:02:37.513 [main] INFO  com.datastax.probe.App - Probing Host '10.211.56.112' : HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
19:02:37.514 [main] INFO  c.d.probe.actions.IsReachableProbe - Took 0 (ms) to check host is reachable: HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
19:02:37.515 [main] INFO  c.datastax.probe.actions.SocketProbe - Native - Took 0 (ms) to open Socket to host '10.211.56.112' on port '9042 : HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
19:02:37.516 [main] INFO  c.datastax.probe.actions.SocketProbe - Thrift - Took 0 (ms) to open Socket to host '10.211.56.112' on port '9160 : HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]
19:02:37.517 [main] INFO  c.datastax.probe.actions.SocketProbe - Gossip - Took 0 (ms) to open Socket to host '10.211.56.112' on port '7000 : HostProbe [fromAddress=dse-cass2, toAddress=10.211.56.112, nativePort=9042, storagePort=7000, rpcPort=9160, dc=DC1, rack=rack2, cassandraVersion=2.0.10]

```




