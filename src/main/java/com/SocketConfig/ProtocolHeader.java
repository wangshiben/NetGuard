package com.SocketConfig;

public class ProtocolHeader {
    private final int dataLength;
    private final int port;

    public ProtocolHeader(int dataLength, int port) {
        this.dataLength = dataLength;
        this.port = port;
    }

    public int getDataLength() {
        return dataLength;
    }

    public int getPort() {
        return port;
    }
}
