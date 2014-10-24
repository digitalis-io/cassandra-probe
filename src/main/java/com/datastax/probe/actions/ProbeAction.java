package com.datastax.probe.actions;

import org.apache.commons.lang3.time.StopWatch;

public interface ProbeAction {
    
    boolean execute() throws FatalProbeException;
}
