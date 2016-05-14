package io.digitalis.cassandra.probe.actions;

import ch.qos.logback.classic.Logger;

import io.digitalis.cassandra.probe.ProbeLoggerFactory;
import io.digitalis.cassandra.probe.model.HostProbe;

public class FatalProbeException extends Exception {
    
    @SuppressWarnings("unused")
    private static final Logger LOG = ProbeLoggerFactory.getLogger(FatalProbeException.class);

    private static final long serialVersionUID = -8334177166575845682L;
    private final HostProbe host;

    public FatalProbeException(String message, Exception cause, HostProbe host) {
	super(message, cause);
	this.host = host;
    }

    public HostProbe getHost() {
	return host;
    }
}
