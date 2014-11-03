package com.datastax.probe;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestClusterProbe {

    @Test()
    public void testParseCassandraYaml() {
	ClusterProbe cp;
	try {
	    cp = new ClusterProbe("localhost", "/apps/github/cassandra-probe/cassandra.yaml", null, null);

	    Assert.assertEquals("dse-cass", cp.getClusterName());
	    Assert.assertEquals(7000, cp.getStoragePort().intValue());
	    Assert.assertEquals(9160, cp.getRpcPort().intValue());
	    Assert.assertEquals(9042, cp.getNativePort().intValue());

	    String[] seeds = cp.getSeeds();
	    String[] expectedSeeds = new String[] { "10.211.56.110", "10.211.56.111" };
	    Assert.assertTrue(seeds.length == 2);
	    Assert.assertEquals(seeds, expectedSeeds);
	    
	    cp.discoverCluster(false);

	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    Assert.fail("Unexpected exception thrown: " + e.getMessage(), e);
	}

    }

}
