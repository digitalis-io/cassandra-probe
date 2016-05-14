package io.digitalis.cassandra.probe.model;

public class HostProbe {
    
    private final String fromAddress;
    private final String toAddress;
    private final int nativePort;
    private final int storagePort;
    private final int rpcPort;
    private final String dc;
    private final String rack;
    private final String cassandraVersion;
    
    //listen_address=10.211.56.110
    //rpc_address=10.211.56.110
    public HostProbe(final String fromAddress, final String toAddress, final int nativePort, final int storagePort, final int rpcPort, final String dc, final String rack, final String cassandraVersion) {
	this.fromAddress = fromAddress;
	this.toAddress = toAddress;
	this.nativePort = nativePort;
	this.storagePort = storagePort;
	this.rpcPort = rpcPort;
	this.dc = dc;
	this.rack = rack;
	this.cassandraVersion = cassandraVersion;
    }


    public String getToAddress() {
        return toAddress;
    }

    public int getNativePort() {
        return nativePort;
    }

    public int getStoragePort() {
        return storagePort;
    }

    public int getRpcPort() {
        return rpcPort;
    }

    public String getDc() {
        return dc;
    }

    public String getRack() {
        return rack;
    }

    public String getCassandraVersion() {
        return cassandraVersion;
    }


    public String getFromAddress() {
        return fromAddress;
    }

    @Override
    public String toString() {
	return "HostProbe [fromAddress=" + fromAddress + ", toAddress=" + toAddress + ", nativePort=" + nativePort + ", storagePort=" + storagePort + ", rpcPort=" + rpcPort
		+ ", dc=" + dc + ", rack=" + rack + ", cassandraVersion=" + cassandraVersion + "]";
    }
}
