package io.digitalis.cassandra.probe;

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

import io.digitalis.cassandra.probe.model.HostProbe;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.yaml.snakeyaml.Yaml;

import ch.qos.logback.classic.Logger;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.VersionNumber;
import com.datastax.driver.core.exceptions.AuthenticationException;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.ReadTimeoutException;
import com.datastax.driver.core.exceptions.UnauthorizedException;
import com.datastax.driver.core.exceptions.UnavailableException;
import com.datastax.driver.core.exceptions.UnsupportedFeatureException;
import com.datastax.driver.core.exceptions.WriteTimeoutException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

public class ClusterProbe {

	private static final Logger LOG = ProbeLoggerFactory.getLogger(ClusterProbe.class);

	private com.datastax.driver.core.Cluster cassandraCluster;
	private ImmutableSet<HostProbe> hosts;

	private File cassandraYaml;
	private final String localHostName;
	private final String user;
	private final String password;

	@SuppressWarnings("rawtypes")
	private Map allValues;

	private String clusterName;
	private Integer storagePort;
	private Integer nativePort;
	private Integer thriftPort;
	private String[] contactPoints;

	public ClusterProbe(String localHostName, String cassandraYamlPath, String user, String password, int storagePort, int nativePort, int thriftPort, String[] contactPoints) throws IOException {

		this.localHostName = localHostName;
		this.user = user;
		this.password = password;
		this.storagePort = storagePort;
		this.nativePort = nativePort;
		this.thriftPort = thriftPort;
		this.contactPoints = contactPoints;
		if (StringUtils.isNotBlank(cassandraYamlPath)) {
			this.cassandraYaml = new File(cassandraYamlPath);
			parseYaml();
		}
	}

	@SuppressWarnings("rawtypes")
	private void parseYaml() throws FileNotFoundException {
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

		this.thriftPort = (Integer) allValues.get("rpc_port");
		Preconditions.checkNotNull(this.thriftPort, "rpc_port not found in inputted yaml %s", cassandraYaml.getAbsolutePath());

		this.contactPoints = getSeeds();
		Preconditions.checkNotNull(contactPoints, "seed_provider contact points not found in inputted yaml %s", cassandraYaml.getAbsolutePath());
	}

	public void discoverCluster(boolean keepAlive) {
		try {
			LOG.info("About to discover cluster '" + this.clusterName + "' details using seed contact points: " + Arrays.toString(this.contactPoints));

			com.datastax.driver.core.Cluster.Builder clusterBulder = com.datastax.driver.core.Cluster.builder().addContactPoints(this.contactPoints).withSSL().withClusterName(this.clusterName)
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
				b.append(ClusterProbe.prettyHost(host));
				InetAddress sockAddress = host.getSocketAddress().getAddress();
				VersionNumber v = host.getCassandraVersion();

				String cassandraVersion = v.getMajor() + "." + v.getMinor() + "." + v.getPatch();

				HostProbe hp = new HostProbe(this.localHostName, sockAddress.getHostAddress(), this.nativePort, this.storagePort, this.thriftPort, host.getDatacenter(), host.getRack(),
						cassandraVersion);
				hostBuilder.add(hp);
			}

			LOG.info(b.toString());
			this.hosts = hostBuilder.build();
		} catch (UnauthorizedException e) {
			e.printStackTrace(System.err);
			String msg = "Fatal error. User is not authorized : " + e.getMessage();
			System.err.println(msg);
			LOG.error(msg, e);
			throw e;
		} catch (AuthenticationException e) {
			InetSocketAddress errorHost = e.getAddress();
			String msg = "Fatal error. Unable to authenticate Cassandra user against Cassandra node '" + errorHost.toString() + "': " + e.getMessage();
			e.printStackTrace(System.err);
			System.err.println(msg);
			LOG.error(msg, e);
			throw e;
		} catch (UnsupportedFeatureException e) {
			e.printStackTrace(System.err);
			String msg = "Fatal error. The feature being used is not compatable with the version of Cassandra being probed: " + e.getMessage();
			System.err.println(msg);
			LOG.error(msg, e);
			throw e;
		} catch (NoHostAvailableException e) {
			Map<InetSocketAddress, Throwable> errors = e.getErrors();
			StringBuilder errorMesages = new StringBuilder("Unable to establish a client connection to any Cassandra node: " + e.getMessage() + ". Tried: ");
			if (errors != null && errors.size() > 0) {
				for (InetSocketAddress address : errors.keySet()) {
					Throwable throwable = errors.get(address);
					String stackTrace = ExceptionUtils.getStackTrace(throwable);
					errorMesages.append("\n\t" + address.toString() + " : " + throwable.getMessage());
					errorMesages.append("\n\t" + stackTrace);
				}
			}
			e.printStackTrace(System.err);
			System.err.println(errorMesages.toString());
			LOG.error(errorMesages.toString(), e);
			throw e;

		} catch (UnavailableException e) {
			int aliveReplicas = e.getAliveReplicas();
			int requiredReplicas = e.getRequiredReplicas();
			ConsistencyLevel cl = e.getConsistencyLevel();
			e.printStackTrace(System.err);
			String msg = aliveReplicas + " replicas are alive. " + requiredReplicas + " are requried. There is not enough replicas alive to achieve the requested consistency level of '" + cl + "' : "
					+ e.getMessage();
			System.err.println(msg);
			LOG.error(msg, e);
			throw e;
		} catch (ReadTimeoutException e) {
			int receivedAcknowledgements = e.getReceivedAcknowledgements();
			int requiredAcknowledgements = e.getRequiredAcknowledgements();
			ConsistencyLevel cl = e.getConsistencyLevel();
			e.printStackTrace(System.err);
			String msg = receivedAcknowledgements + " read acknowlegement(s) recieved. " + requiredAcknowledgements + " read acknowlegement(s) are required for read consistency level of '" + cl
					+ "'. Not enough replicas responded in time : " + e.getMessage();
			System.err.println(msg);
			LOG.warn(msg, e);
			throw e;
		} catch (WriteTimeoutException e) {
			int receivedAcknowledgements = e.getReceivedAcknowledgements();
			int requiredAcknowledgements = e.getRequiredAcknowledgements();
			ConsistencyLevel cl = e.getConsistencyLevel();
			e.printStackTrace(System.err);
			String msg = receivedAcknowledgements + " write acknowlegement(s) recieved. " + requiredAcknowledgements + " write acknowlegement(s) are required for write consistency level of '" + cl
					+ "'. Not enough nodes responded in time : " + e.getMessage();
			System.err.println(msg);
			LOG.warn(msg, e);
			throw e;
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			String msg = "Unexpected error encountered: " + t.getMessage();
			System.err.println(msg);
			LOG.warn(msg, t);
			throw t;
		} finally {
			if (!keepAlive) {
				this.shutDownCluster();
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

	public static String prettyHost(final Host host) {
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
		b.append("\n\t\tIs Up:\t\t\t" + host.isUp());
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

	public Integer getThriftPort() {
		return this.thriftPort;
	}

	@SuppressWarnings("rawtypes")
	public Map getAllValues() {
		return allValues;
	}

	public com.datastax.driver.core.Cluster getCassandraCluster() {
		return this.cassandraCluster;
	}

	public void shutDownCluster() {
		if (this.cassandraCluster != null) {
			this.cassandraCluster.close();
			this.cassandraCluster = null;
		}
	}

}
