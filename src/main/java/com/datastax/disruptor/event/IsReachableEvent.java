package com.datastax.disruptor.event;

import com.datastax.driver.core.Host;
import com.datastax.probe.model.IsReachable;
import com.lmax.disruptor.EventFactory;

public class IsReachableEvent {

    private Host host;
    private long bufferSeq = 0;

    public IsReachableEvent() {
    }

    public IsReachableEvent(Host host, long bufferSeq) {
	this.host = host;
	this.bufferSeq = bufferSeq;
    }

    public long getBufferSeq() {
	return bufferSeq;
    }

    public void setBufferSeq(long bufferSeq) {
	this.bufferSeq = bufferSeq;
    }

    public Host getHost() {
	return this.host;
    }

    public void setHost(Host host) {
	this.host = host;
    }

    /**
     * EventFactory is specified by the disruptor framework. This is how the
     * ring-buffer populates itself.
     * See init() in TransactionProcessor.
     */
    public final static EventFactory<IsReachableEvent> EVENT_FACTORY = new EventFactory<IsReachableEvent>() {
	public IsReachableEvent newInstance() {
	    return new IsReachableEvent();
	}
    };

    public String asJournalEntry() {
	return String.format("%s|%s|%s|%s|%s\n", this.getBufferSeq(), this.host);
    }

}
