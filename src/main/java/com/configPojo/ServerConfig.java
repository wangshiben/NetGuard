package com.configPojo;

public class ServerConfig {
    private final String host;
    private final Integer port;

    public ServerConfig(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }
}
