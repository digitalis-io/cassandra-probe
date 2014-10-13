package com.datastax.disruptor.handler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.disruptor.event.IsReachableEvent;


public class JournalIsReachableHandler extends AbstractIsReachableEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(JournalIsReachableHandler.class);
    private FileWriter journal;
    
    public JournalIsReachableHandler(File journalFile) {
        try {
            this.journal = new FileWriter(journalFile, true);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    public void closeJournal() throws IOException {
        if (journal!=null) {
            journal.flush();
            journal.close();
        }
    }

    @Override
    public void onEvent(IsReachableEvent event, long sequence, boolean endOfBatch) throws Exception {
        journal.write(event.asJournalEntry());
        journal.flush();
        logger.debug("JOURNALED TRANSACTION -> {}", event.getHost().toString());
    }
}
