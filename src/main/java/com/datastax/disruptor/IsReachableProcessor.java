package com.datastax.disruptor;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.datastax.disruptor.event.IsReachableEvent;
import com.datastax.disruptor.handler.GenericExceptionHandler;
import com.datastax.disruptor.handler.JournalIsReachableHandler;
import com.datastax.disruptor.handler.PostIsReachableHandler;
import com.datastax.disruptor.handler.ReplicateIsReachableHandler;
import com.datastax.driver.core.Host;
import com.datastax.probe.ClusterProbe;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class IsReachableProcessor {

    private final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    private Disruptor disruptor;
    private RingBuffer ringBuffer;
    private ClusterProbe cluster;

    JournalIsReachableHandler journal;
    ReplicateIsReachableHandler replicate;
    PostIsReachableHandler post;

    public IsReachableProcessor(ClusterProbe cluster) {
	this.cluster = cluster;
    }

    public void postIsReachable(Host host) {
	disruptor.publishEvent(new IsReachableEventPublisher(host));
    }

    public void init() {
	disruptor = new Disruptor<IsReachableEvent>(IsReachableEvent.EVENT_FACTORY, 1024, EXECUTOR, ProducerType.SINGLE, new YieldingWaitStrategy());

	// Pretend that we have real journaling, just to demo it...
	File journalDir = new File("target/test");
	journalDir.mkdirs();
	File journalFile = new File(journalDir, "test-journal.txt");

	// In this example start fresh each time - though a real implementation
	// might roll over the journal or the like.
	if (journalFile.exists()) {
	    journalFile.delete();
	}

	journal = new JournalIsReachableHandler(journalFile);

	replicate = new ReplicateIsReachableHandler();

	post = new PostIsReachableHandler(this.cluster);

	// This is where the magic happens 
	// (see "diamond configuration" in javadoc above)
	disruptor.handleEventsWith(journal, replicate).then(post);

	// We don't do any fancy exception handling in this demo, but if we
	// did, one way to set it up for each handler is like this:
	ExceptionHandler exh = new GenericExceptionHandler();
	disruptor.handleExceptionsFor(journal).with(exh);
	disruptor.handleExceptionsFor(replicate).with(exh);
	disruptor.handleExceptionsFor(post).with(exh);

	ringBuffer = disruptor.start();

    }

    public void destroy() {
	try {
	    journal.closeJournal();
	} catch (Exception ignored) {
	}

	try {
	    disruptor.shutdown();
	} catch (Exception ignored) {
	}

	EXECUTOR.shutdownNow();
    }

    /**
     * This is the way events get into the system - the ring buffer is full of
     * pre-allocated events
     * and this is how a specific event's state is input to the buffer.
     * Pre-allocation of the buffer
     * is a key component of the Disruptor pattern.
     */
    class IsReachableEventPublisher implements EventTranslator<IsReachableEvent> {
	private Host host;

	public IsReachableEventPublisher(Host host) {
	    this.host = host;
	}

	public void translateTo(IsReachableEvent event, long sequence) {
	    event.setHost(host);
	    event.setBufferSeq(sequence);
	}
    }

}
