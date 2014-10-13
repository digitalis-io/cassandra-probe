package com.datastax.probe;

import org.testng.annotations.Test;

public class TestClusterProbe {

    @Test()
    public void testDefaultInit() {
	ClusterProbe cp = new ClusterProbe(new String[] { "10.211.56.110" });
	cp.init();
    }

}
