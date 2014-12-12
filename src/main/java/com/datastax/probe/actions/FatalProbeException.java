package com.datastax.probe.actions;

import ch.qos.logback.classic.Logger;

import com.datastax.probe.ProbeLoggerFactory;
import com.datastax.probe.model.HostProbe;

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
