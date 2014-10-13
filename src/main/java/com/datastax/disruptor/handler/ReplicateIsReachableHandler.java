package com.datastax.disruptor.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.disruptor.event.IsReachableEvent;

public class ReplicateIsReachableHandler extends AbstractIsReachableEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReplicateIsReachableHandler.class);

    @Override
    public void onEvent(IsReachableEvent event, long sequence, boolean endOfBatch) throws Exception {
	logger.warn("TODO: REPLICATE -> {}", event.getHost().toString());
    }
}
