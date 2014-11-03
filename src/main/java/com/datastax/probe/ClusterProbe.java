package com.datastax.probe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.VersionNumber;
import com.datastax.probe.model.HostProbe;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

public class ClusterProbe {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterProbe.class);

    private com.datastax.driver.core.Cluster cassandraCluster;
    private ImmutableSet<HostProbe> hosts;

    private final File cassandraYaml;
    private final String localHostName;
    private final String user;
    private final String password;

    @SuppressWarnings("rawtypes")
    private Map allValues;

    private String clusterName;
    private Integer storagePort;
    private Integer nativePort;
    private Integer rpcPort;
    private String[] contactPoints;

    public ClusterProbe(String localHostName, String cassandraYamlPath, String user, String password) throws IOException {
	this.localHostName = localHostName;
	this.user = user;
	this.password = password;
	Preconditions.checkNotNull(cassandraYamlPath);
	this.cassandraYaml = new File(cassandraYamlPath);
	parseCassandraYaml();
    }

    @SuppressWarnings("rawtypes")
    private void parseCassandraYaml() throws FileNotFoundException {
	Preconditions.checkArgument(cassandraYaml.exists(), "Cassandra Yaml file '%s' does not exist", cassandraYaml.getAbsolutePath());

	final InputStream stream = new FileInputStream(this.cassandraYaml);

	this.allValues = (Map) new Yaml().load(stream);
	LOG.debug(allValues.toString());
	this.clusterName = (String) allValues.get("cluster_name");
	Preconditions.checkNotNull(clusterName, "cluster_name not found in inputted yaml '%s'", cassandraYaml.getAbsolutePath());

	this.storagePort = (Integer) allValues.get("storage_port");
	Preconditions.checkNotNull(storagePort, "storage_port not found in inputted yaml '%s'", cassandraYaml.getAbsolutePath());

	this.nativePort = (Integer) allValues.get("native_transport_port");
	Preconditions.checkNotNull(nativePort, "native_transport_port not found in inputted yaml '%s'", cassandraYaml.getAbsolutePath());

	this.rpcPort = (Integer) allValues.get("rpc_port");
	Preconditions.checkNotNull(rpcPort, "rpc_port not found in inputted yaml %s", cassandraYaml.getAbsolutePath());

	this.contactPoints = getSeeds();
	Preconditions.checkNotNull(contactPoints, "seed_provider contact points not found in inputted yaml %s", cassandraYaml.getAbsolutePath());
    }

    public void discoverCluster(boolean keepAlive) {
	try {
	    LOG.info("About to discover cluster '" + this.clusterName + "' details using seed contact points: " + Arrays.toString(this.contactPoints));

	    com.datastax.driver.core.Cluster.Builder clusterBulder = com.datastax.driver.core.Cluster.builder().addContactPoints(this.contactPoints)
		    .withClusterName(this.clusterName) //parsed from the yaml
		    .withPort(this.nativePort).withoutJMXReporting().withoutMetrics();

	    if (!Strings.isNullOrEmpty(this.user)) {
		clusterBulder.withCredentials(this.user, this.password);
	    }
	    this.cassandraCluster = clusterBulder.build();
	    this.cassandraCluster.init();

	    Builder<HostProbe> hostBuilder = ImmutableSet.<HostProbe> builder();

	    Metadata metadata = this.cassandraCluster.getMetadata();
	    Set<Host> allHosts = metadata.getAllHosts();
	    StringBuilder b = new StringBuilder("\nDiscovered Cassandra Cluster '" + this.clusterName + "' details via native driver from host '" + this.localHostName + "' :");
	    for (Host host : allHosts) {
		b.append(this.prettyHost(host));
		InetAddress sockAddress = host.getSocketAddress().getAddress();
		VersionNumber v = host.getCassandraVersion();
		String cassandraVersion = v.getMajor() + "." + v.getMinor() + "." + v.getPatch();

		HostProbe hp = new HostProbe(this.localHostName, sockAddress.getHostAddress(), this.nativePort, this.storagePort, this.rpcPort, host.getDatacenter(),
			host.getRack(), cassandraVersion);
		hostBuilder.add(hp);
	    }

	    LOG.info(b.toString());
	    this.hosts = hostBuilder.build();
	} finally {
	    if (!keepAlive) {
		this.cassandraCluster.close();
		this.cassandraCluster = null;
	    }
	}
    }

    /**
     * This is ugly as hell, raw and can use revisiting.
     * 
     * @return
     */
    @SuppressWarnings({ "rawtypes" })
    public String[] getSeeds() {
	ArrayList list = (ArrayList) allValues.get("seed_provider");
	Preconditions.checkNotNull(list, "seed_provider not found in inputted yaml '%s'", this.cassandraYaml.getAbsolutePath());

	LinkedHashMap hm = (LinkedHashMap) list.get(0);
	Set hmValues = hm.entrySet();
	for (Object o : hmValues) {
	    Entry entry = (Entry) o;
	    String key = (String) entry.getKey();
	    Object value = entry.getValue();
	    if ("parameters".equals(key)) {
		ArrayList l = (ArrayList) value;
		LinkedHashMap lm = (LinkedHashMap) l.get(0);
		Set entrySet = lm.entrySet();
		for (Object os : entrySet) {
		    Entry osEntry = (Entry) os;
		    String seeds = (String) osEntry.getValue();
		    return seeds.split(",");
		}
	    }
	}
	return null;
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

    public Set<HostProbe> getHosts() {
	return hosts;
    }

    public String getClusterName() {
	return clusterName;
    }

    public Integer getStoragePort() {
	return storagePort;
    }

    public Integer getNativePort() {
	return nativePort;
    }

    public Integer getRpcPort() {
	return rpcPort;
    }

    @SuppressWarnings("rawtypes")
    public Map getAllValues() {
	return allValues;
    }
}
