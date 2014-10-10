package com.datastax.demo;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.TableMetadata;
import com.datastax.driver.core.VersionNumber;
import com.google.common.base.Preconditions;

public class ClusterLogger {

    private static final String DEFAULT_CONTACT_POINT = "10.211.56.110";

    private static final Logger LOG = LoggerFactory.getLogger(ClusterLogger.class);

    private Cluster cluster;

    public static void main(String[] args) {
	ClusterLogger cl = new ClusterLogger();
	if (args.length == 0) {
	    LOG.info("Attempting to connect using default contact points");
	    cl.connectToCluster(DEFAULT_CONTACT_POINT);
	} else {
	    LOG.info("Attempting to connect using contact points: "+args);
	    cl.connectToCluster(args);
	}
	cl.logClusterDetails();
	System.exit(0);
    }

    public void connectToCluster(final String... contactPoints) {
	this.cluster = Cluster.builder().addContactPoints(contactPoints).build();
    }

    public void logClusterDetails() {
	Preconditions.checkNotNull(this.cluster);

	String clusterName = this.cluster.getClusterName();
	LOG.info("Cluster Name: " + clusterName);

	Metadata metadata = this.cluster.getMetadata();
	Set<Host> allHosts = metadata.getAllHosts();
	StringBuilder b = new StringBuilder("Cluster " + clusterName + " details:");
	for (Host host : allHosts) {
	    b.append(this.prettyHost(host));
	}
	
	List<KeyspaceMetadata> keyspaces = metadata.getKeyspaces();
	for (KeyspaceMetadata keyspace : keyspaces) {
	    String name = keyspace.getName();
	    LOG.info("Keyspace Name: "+name);
	    Collection<TableMetadata> tables = keyspace.getTables();
	    for (TableMetadata table: tables) {
		table.getName();
	    }
	    
	}
	
	
	LOG.info(b.toString());
    }

    public String prettyHost(Host host) {
	Preconditions.checkNotNull(host);

	InetAddress address = host.getAddress();
	VersionNumber v = host.getCassandraVersion();
	String datacenter = host.getDatacenter();
	String rack = host.getRack();
	boolean up = host.isUp();
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
	b.append("\n\t\tIs up:\t\t\t" + up);
	b.append("\n\t\tDataCenter:\t\t" + datacenter);
	b.append("\n\t\tRack:\t\t\t" + rack);
	b.append("\n\t\tCassandra Version:\t" + v.getMajor() + "." + v.getMinor() + "." + v.getPatch());
	b.append("\n\t\tDSE Patch:\t\t" + v.getDSEPatch());
	b.append("\n\t]");

	return b.toString();
    }

}
