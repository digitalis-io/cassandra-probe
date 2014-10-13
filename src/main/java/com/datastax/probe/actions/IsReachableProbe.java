package com.datastax.probe.actions;

import java.net.InetAddress;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.probe.model.HostProbe;

public class IsReachableProbe implements ProbeAction {

    private static final Logger LOG = LoggerFactory.getLogger(IsReachableProbe.class);
    private static final int DEFAULT_TIMEOUT_MS = 2000;

    private final HostProbe host;
    private final StopWatch stopWatch;

    public IsReachableProbe(final HostProbe host) {
	this.host = host;
	this.stopWatch = new StopWatch();
    }

    @Override
    public void execute() throws FatalProbeException {
	String toAddress = host.getToAddress();
	try {
	    this.stopWatch.start();
	    InetAddress byName = InetAddress.getByName(toAddress);
	    byName.isReachable(DEFAULT_TIMEOUT_MS);
	    this.stopWatch.stop();
	    LOG.info("Took "+this.stopWatch.getTime()+"(ms) to reach host: "+toAddress);
	} catch (Exception e) {
	    this.stopWatch.stop();
	    String msg = "Fatal problem ecountered attempting to reach Cassandra host '" + toAddress + "' :" + e.getMessage();
	    throw new FatalProbeException(msg, e);
	}

    }

    @Override
    public StopWatch getTime() {
	return this.stopWatch;
    }

}
