package com.datastax.probe;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.VersionNumber;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

public class ClusterProbe {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterProbe.class);

    private String[] contactPoints;

    private com.datastax.driver.core.Cluster cassandraCluster;
    private ImmutableSet<Host> hosts;

    public static void main(String[] args) {
	ClusterProbe cp = new ClusterProbe();
	cp.init();

	System.exit(0);
    }

    public ClusterProbe(String... contactPoints) {
	this.contactPoints = contactPoints;
    }

    private void buildCluster() {
	this.cassandraCluster = com.datastax.driver.core.Cluster.builder().addContactPoints(this.contactPoints).build();
    }

    public void dumpResults() {
	// TODO Auto-generated method stub
	throw new RuntimeException("Implement me!");
    }

    public void init() {
	this.buildCluster();
	Preconditions.checkNotNull(this.cassandraCluster);

	Builder<Host> hostBuilder = ImmutableSet.<Host> builder();

	Metadata metadata = this.cassandraCluster.getMetadata();
	Set<Host> allHosts = metadata.getAllHosts();
	StringBuilder b = new StringBuilder("Discovered Cluster details are:");
	for (Host host : allHosts) {
	    b.append(this.prettyHost(host));
	    hostBuilder.add(host);
	}

	LOG.info(b.toString());
	this.hosts = hostBuilder.build();
    }

    public String prettyHost(final Host host) {
	Preconditions.checkNotNull(host);

	InetAddress address = host.getAddress();
	VersionNumber v = host.getCassandraVersion();
	String datacenter = host.getDatacenter();
	String rack = host.getRack();
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
	b.append("\n\t\tDataCenter:\t\t" + datacenter);
	b.append("\n\t\tRack:\t\t\t" + rack);
	b.append("\n\t\tCassandra Version:\t" + v.getMajor() + "." + v.getMinor() + "." + v.getPatch());
	b.append("\n\t\tDSE Patch:\t\t" + v.getDSEPatch());
	b.append("\n\t]");

	return b.toString();
    }

    public Set<Host> getHosts() {
	return hosts;
    }

}
