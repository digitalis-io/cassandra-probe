package com.datastax.disruptor.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.ExceptionHandler;

/**
 * Any un-handled or thrown exception in processing by an event handler will
 * be reported through an implementation of ExceptionHandler. Depending upon
 * which step in our "diamond configuration" has failed, we would take
 * action.
 */
public class GenericExceptionHandler implements ExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GenericExceptionHandler.class);

    public void handleEventException(Throwable ex, long sequence, Object event) {
	logger.error("Caught unhandled exception while processing: " + event.toString(), ex);
    }

    public void handleOnStartException(Throwable ex) {
	logger.error("Unexpected exception during startup.", ex);
    }

    public void handleOnShutdownException(Throwable ex) {
	logger.error("Unexpected exception during shutdown.", ex);
    }

}
