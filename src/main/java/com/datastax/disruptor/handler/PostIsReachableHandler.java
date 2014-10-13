package com.datastax.disruptor.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.disruptor.event.IsReachableEvent;
import com.datastax.probe.ClusterProbe;


public class PostIsReachableHandler extends AbstractIsReachableEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(PostIsReachableHandler.class);
    private ClusterProbe cluster;

    public PostIsReachableHandler(ClusterProbe cluster) {
	this.cluster = cluster;
    }
    
    @Override
    public void onEvent(IsReachableEvent event, long sequence, boolean endOfBatch) throws Exception {
//        Account act = accountStore.getAccount(event.getTransaction().getAccountnbr());
//        if (act==null) {
//            act = new Account(event.getTransaction().getAccountnbr());
//        }
//        act.post(event.getTransaction());
//        accountStore.saveAccount(act); 
//        logger.debug("POSTED TRANSACTION -> {}", event.getTransaction().toString());
    }
}
