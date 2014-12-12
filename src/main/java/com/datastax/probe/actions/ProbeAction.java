package com.datastax.probe.actions;


public interface ProbeAction {
    
    boolean execute() throws FatalProbeException;
}
