package com.datastax.probe;

import org.apache.commons.lang3.time.StopWatch;

public class ProbeException extends Exception {

    private static final long serialVersionUID = 3157483207943445386L;

    private StopWatch stopWatch;

    public ProbeException(final String message, final Throwable cause, final StopWatch stopWatch) {
	super(message, cause);
	this.stopWatch = stopWatch;
    }

    public StopWatch getStopWatch() {
	return stopWatch;
    }

}
