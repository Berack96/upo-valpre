package net.berack.upo.valpre;

import java.util.ArrayList;
import java.util.List;
import net.berack.upo.valpre.rand.Distribution;
import net.berack.upo.valpre.rand.Rng;

public class ServerNode {
    public final String name;
    public final int maxServers;
    public final Distribution distribution;
    public final boolean isSink;
    public final boolean isSource;
    private final List<NodeChild> children = new ArrayList<>();
    private double sumProbabilities = 0.0;

    public static ServerNode createSource(String name, Distribution distribution) {
        return new ServerNode(name, Integer.MAX_VALUE, distribution, false, true);
    }

    public static ServerNode createQueue(String name, int maxServers, Distribution distribution) {
        return new ServerNode(name, maxServers, distribution, false, false);
    }

    public ServerNode(String name, int maxServers, Distribution distribution, boolean isSink, boolean isSource) {
        this.name = name;
        this.maxServers = maxServers;
        this.distribution = distribution;
        this.isSink = isSink;
        this.isSource = isSource;
    }

    public void addChild(ServerNode node, double probability) {
        this.children.add(new NodeChild(node, probability));
        this.sumProbabilities += probability;
    }

    public ServerNode getChild(Rng rng) {
        var random = rng.random();
        for (var child : this.children) {
            random -= child.probability / this.sumProbabilities;
            if (random <= 0) {
                return child.node;
            }
        }
        return null;
    }

    public static class NodeChild {
        public final ServerNode node;
        public final double probability;

        public NodeChild(ServerNode node, double probability) {
            this.node = node;
            this.probability = probability;
        }
    }
}
