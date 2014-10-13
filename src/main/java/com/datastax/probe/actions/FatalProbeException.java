package com.datastax.probe.actions;

public class FatalProbeException extends Exception {

    private static final long serialVersionUID = -8334177166575845682L;

    public FatalProbeException(String message, Exception cause) {
	super(message, cause);
    }
}
