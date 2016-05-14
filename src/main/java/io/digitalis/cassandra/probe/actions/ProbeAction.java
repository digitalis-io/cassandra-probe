package io.digitalis.cassandra.probe.actions;


public interface ProbeAction {
    
    boolean execute() throws FatalProbeException;
}
