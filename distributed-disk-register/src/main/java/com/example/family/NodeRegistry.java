package com.example.family;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NodeRegistry {
    private final List<NodeInfo> nodes = Collections.synchronizedList(new ArrayList<>());

    public void addNode(NodeInfo node) {
        // Zaten varsa ekleme
        for (NodeInfo n : nodes) {
            if (n.getPort() == node.getPort()) return;
        }
        nodes.add(node);
    }

    public void removeNode(NodeInfo node) {
        nodes.removeIf(n -> n.getPort() == node.getPort());
    }

    public List<NodeInfo> getNodes() {
        return new ArrayList<>(nodes); // Kopya döndür (Güvenlik için)
    }
}