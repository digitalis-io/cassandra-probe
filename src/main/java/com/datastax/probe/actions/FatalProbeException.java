package com.datastax.probe.actions;

import com.datastax.probe.model.HostProbe;

public class FatalProbeException extends Exception {

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
