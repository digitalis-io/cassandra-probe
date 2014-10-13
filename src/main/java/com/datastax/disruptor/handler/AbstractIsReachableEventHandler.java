package com.datastax.disruptor.handler;

import com.datastax.disruptor.event.IsReachableEvent;
import com.lmax.disruptor.EventHandler;

public abstract class AbstractIsReachableEventHandler implements EventHandler<IsReachableEvent> {

    public abstract void onEvent(IsReachableEvent event, long sequence, boolean endOfBatch) throws Exception;

}
