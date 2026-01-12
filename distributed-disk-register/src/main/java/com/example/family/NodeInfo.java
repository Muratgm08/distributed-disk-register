package com.example.family;

import java.util.Objects;

public class NodeInfo {
    private String host;
    private int port;

    public NodeInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }

    // Listelerde doğru silme/karşılaştırma yapabilmek için equals ve hashcode şart
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeInfo nodeInfo = (NodeInfo) o;
        return port == nodeInfo.port && Objects.equals(host, nodeInfo.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}