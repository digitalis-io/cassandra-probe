package com.datastax.probe.actions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.probe.model.HostProbe;

public class SocketProbe implements ProbeAction {

    private static final Logger LOG = LoggerFactory.getLogger(SocketProbe.class);

    private int port;
    private final HostProbe host;
    private final int timeoutMs;
    private final String description;

    public SocketProbe(final String description, final HostProbe host, final int port, final int timeoutMs) {
	this.description = description;
	this.host = host;
	this.port = port;
	this.timeoutMs = timeoutMs;
    }

    @Override
    public boolean execute() throws FatalProbeException {
	boolean result = false;

	String toAddress = host.getToAddress();
	
	Socket socket = null;
	StopWatch stopWatch = new StopWatch();
	try {
	    stopWatch.start();
	    socket = new Socket();
	    socket.setReuseAddress(false);
	    socket.connect(new InetSocketAddress(toAddress, this.port), this.timeoutMs);
	    result = true;
	} catch (UnknownHostException uhe) {
	    result = false;
	    String msg = "Could not resolve host while attempting to open Socket to Cassandra host '" + toAddress + "' on port '" + this.port + "' :" + uhe.getMessage() + " : " + this.host;
	    throw new FatalProbeException(msg, uhe, this.host);
	} catch (SocketException se) {
	    result = false;
	    String msg = "Got SocketException while attempting to open Socket to Cassandra host '" + toAddress + "' on port '" + this.port + "' :" + se.getMessage() + " : " + this.host;
	    throw new FatalProbeException(msg, se, this.host);
	} catch (IOException ie) {
	    result = false;
	    String msg = "Got IOException while attempting to open Socket to Cassandra host '" + toAddress + "' on port '" + this.port + "' :" + ie.getMessage() + " : " + this.host;
	    throw new FatalProbeException(msg, ie, this.host);
	} catch (Exception e) {
	    result = false;
	    String msg = "Problem ecountered attempting to open Socket to Cassandra host '" + toAddress + "' on port '" + this.port + "' :" + e.getMessage() + " : " + this.host;
	    throw new FatalProbeException(msg, e, this.host);
	} finally {
	    stopWatch.stop();
	    if (socket != null && socket.isConnected()) {
		try {
		    socket.close();
		    socket.shutdownInput();
		    socket.shutdownOutput();
		} catch (IOException e) {
		}
	    }
	    
	    LOG.info(description + " - Took " + stopWatch.getTime() + " (ms) to open Socket to host '" + toAddress + "' on port '" + this.port + " : " + this.host);
	}

	return result;

    }

}
