package com.datastax.probev1;

import java.io.IOException;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelnetProbe {

    private static final Logger LOG = LoggerFactory.getLogger(TelnetProbe.class);

    private String host;
    private int port;
    private TelnetClient tc;

    public TelnetProbe(String host, int port) {
	this.host = host;
	this.port = port;

    }

    public void probe() throws ProbeException {
	this.tc = new TelnetClient();
	StopWatch stopWatch = new StopWatch();
	try {
	    stopWatch.start();
	    tc.connect(this.host, this.port);
	    stopWatch.stop();
	    LOG.info("Took: " + stopWatch.getTime() + " millis OR " + stopWatch.getNanoTime() + " nanoseconds. Connected to:" + this.host + " " + this.port);
	} catch (Throwable e) {
	    stopWatch.stop();
	    String msg = "Took: " + stopWatch.getTime() + " millis. Exception while connecting:" + e.getMessage();
	    LOG.error(msg, e);
	    e.printStackTrace(System.err);
	    throw new ProbeException(msg, e, stopWatch);
	} finally {
	    try {
		tc.disconnect();
		LOG.info("Took: " + stopWatch.getTime() + " millis. Disconnected from:" + this.host + " " + this.port);
	    } catch (IOException e) {
		LOG.error("Took: " + stopWatch.getTime() + " millis. Exception while disconnecting:" + e.getMessage(), e);
		e.printStackTrace(System.err);
	    }
	}
    }
}
