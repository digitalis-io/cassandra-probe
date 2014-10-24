package com.datastax.probe.actions;

import java.net.InetAddress;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.probe.model.HostProbe;

public class IsReachableProbe implements ProbeAction {

    private static final Logger LOG = LoggerFactory.getLogger(IsReachableProbe.class);

    private final HostProbe host;
    private final int timeOutMs;

    public IsReachableProbe(final HostProbe host, int timeOutMs) {
	this.host = host;
	this.timeOutMs = timeOutMs;
    }

    @Override
    public boolean execute() throws FatalProbeException {
	String toAddress = host.getToAddress();
	boolean result = false;
	StopWatch stopWatch = new StopWatch();
	try {
	    stopWatch.start();
	    InetAddress byName = InetAddress.getByName(toAddress);
	    result = byName.isReachable(this.timeOutMs);
	    stopWatch.stop();
	    if (result) {
		LOG.info("Took " + stopWatch.getTime() + " (ms) to check host is reachable: " + this.host);
	    } else {
		LOG.warn("Could not reach host '"+toAddress+"' after "+this.timeOutMs+" (ms) : "+this.host); 
	    }
	} catch (Exception e) {
	    stopWatch.stop();
	    String msg = "Fatal problem ecountered attempting to reach Cassandra host '" + toAddress + "' :" + e.getMessage();
	    throw new FatalProbeException(msg, e, this.host);
	}
	return result;

    }
}
