package com.datastax.probe.actions;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.probe.model.HostProbe;

public class TelnetProbe implements ProbeAction {

    private static final Logger LOG = LoggerFactory.getLogger(TelnetProbe.class);

    private int port;
    private final HostProbe host;
    private final StopWatch stopWatch;

    public TelnetProbe(final HostProbe host, final int port) {
	this.host = host;
	this.port = port;
	this.stopWatch = new StopWatch();
    }

    @Override
    public void execute() throws FatalProbeException {

	String toAddress = host.getToAddress();

	TelnetClient telnetClient = new TelnetClient();

	try {
	    this.stopWatch.start();
	    telnetClient.connect(host.getToAddress(), this.port);
	    this.stopWatch.stop();
	    LOG.info("Took " + this.stopWatch.getTime() + "(ms) to telnet to host '" + toAddress + "' on port '" + this.port);
	} catch (Exception e) {
	    this.stopWatch.stop();
	    String msg = "Problem ecountered attempting to telnet to Cassandra host '" + toAddress + "' on port '" + this.port + "' :" + e.getMessage();
	    throw new FatalProbeException(msg, e);
	}

    }

    @Override
    public StopWatch getTime() {
	return stopWatch;
    }

}
