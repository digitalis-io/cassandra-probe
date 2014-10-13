package com.datastax.probev1;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.TableMetadata;
import com.datastax.driver.core.VersionNumber;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

public class ClusterProbe {

    private static final String DEFAULT_CONTACT_POINT = "10.211.56.110";

    private static final Logger LOG = LoggerFactory.getLogger(ClusterProbe.class);

    private Cluster cluster;
    private ImmutableSet<Host> hosts;
    private String[] contactPoints;

    public static void main(String[] args) {
	ClusterProbe cl;
	if (args.length == 0) {
	    LOG.info("Attempting to connect using default contact points");
	    cl = new ClusterProbe(DEFAULT_CONTACT_POINT);
	} else {
	    LOG.info("Attempting to connect using contact points: " + args);
	    cl = new ClusterProbe(args);
	}

	cl.checkHosts();

	System.exit(0);
    }

    public ClusterProbe(final String... contactPoints) {
	this.contactPoints = contactPoints;
	this.connectToCluster();
	this.getClusterDetails();
    }

    public void connectToCluster() {
	this.cluster = Cluster.builder().addContactPoints(this.contactPoints).build();
    }

    public void checkHosts() {
	for (Host host : this.hosts) {
	    String hostInfo = this.prettyHost(host);
	    String ipAddress = host.getAddress().getHostAddress();
	    InetSocketAddress socketAddress = host.getSocketAddress();
	    int port = socketAddress.getPort();

	    LOG.info("Checking host: " + ipAddress + " is reachable");
	    InetAddress address = host.getAddress();

	    StopWatch stopWatch = new StopWatch();
	    try {
		stopWatch.start();
		address.isReachable(10000);
		stopWatch.stop();
		LOG.info("Took " + stopWatch.getTime() + " milliseconds OR " + stopWatch.getNanoTime() + " nanoseconds to check host is reachable " + hostInfo);
	    } catch (Exception e) {
		LOG.error("Problem encountered contacting host: " + e.getMessage() + " @ " + hostInfo, e);
		e.printStackTrace();
	    }

	    LOG.info("Checking host: " + ipAddress + " on native port " + port + " can be connected too via Telnet");

	    try {
		TelnetProbe telnet = new TelnetProbe(ipAddress, port);
		telnet.probe();
	    } catch (Exception e) {
		LOG.error("Problem encountered teleneting to host: " + e.getMessage() + " @ " + hostInfo, e);
		e.printStackTrace();
	    }

	}
    }

    public void getClusterDetails() {
	Preconditions.checkNotNull(this.cluster);

	Builder<Host> hostBuilder = ImmutableSet.<Host> builder();

	String clusterName = this.cluster.getClusterName();
	LOG.info("Client descriptive cluster Name: " + clusterName);

	Metadata metadata = this.cluster.getMetadata();
	Set<Host> allHosts = metadata.getAllHosts();
	StringBuilder b = new StringBuilder("Cluster " + clusterName + " details:");
	for (Host host : allHosts) {
	    b.append(this.prettyHost(host));
	    hostBuilder.add(host);
	}

	List<KeyspaceMetadata> keyspaces = metadata.getKeyspaces();
	for (KeyspaceMetadata keyspace : keyspaces) {
	    String name = keyspace.getName();
	    LOG.info("Keyspace Name: " + name);
	    Collection<TableMetadata> tables = keyspace.getTables();
	    for (TableMetadata table : tables) {
		table.getName();
	    }

	}

	LOG.info(b.toString());

	this.hosts = hostBuilder.build();

    }

    public String prettyHost(Host host) {
	Preconditions.checkNotNull(host);

	InetAddress address = host.getAddress();
	VersionNumber v = host.getCassandraVersion();
	String datacenter = host.getDatacenter();
	String rack = host.getRack();
	//	boolean up = host.isUp();
	InetSocketAddress socketAddress = host.getSocketAddress();
	int port = socketAddress.getPort();
	InetAddress sockAddress = socketAddress.getAddress();

	StringBuilder b = new StringBuilder();
	b.append("\n\tHost (" + address.getCanonicalHostName() + ") [");
	b.append("\n\t\tHostAddress:\t\t" + address.getHostAddress());
	b.append("\n\t\tHostName:\t\t" + address.getHostName());
	b.append("\n\t\tSocket Canonical:\t" + sockAddress.getCanonicalHostName());
	b.append("\n\t\tSocket HostAddress:\t" + sockAddress.getHostAddress());
	b.append("\n\t\tSocket HostName:\t" + sockAddress.getHostName());
	b.append("\n\t\tSocket Port:\t\t" + port);
	//	b.append("\n\t\tIs up:\t\t\t" + up); - not reliable, so dont use
	b.append("\n\t\tDataCenter:\t\t" + datacenter);
	b.append("\n\t\tRack:\t\t\t" + rack);
	b.append("\n\t\tCassandra Version:\t" + v.getMajor() + "." + v.getMinor() + "." + v.getPatch());
	b.append("\n\t\tDSE Patch:\t\t" + v.getDSEPatch());
	b.append("\n\t]");

	return b.toString();
    }

}
