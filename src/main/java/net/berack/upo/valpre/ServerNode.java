package net.berack.upo.valpre;

import java.util.ArrayList;
import java.util.List;
import net.berack.upo.valpre.rand.Distribution;
import net.berack.upo.valpre.rand.Rng;

/**
 * Represents a node in the network. It can be a source, a queue, or a sink
 * based on the configuration passed as parameters.
 */
public class ServerNode {
    public final String name;
    public final int maxServers;
    public final int sinkDepartures;
    public final int spawnArrivals;
    public final Distribution distribution;
    private final List<NodeChild> children = new ArrayList<>();
    private double sumProbabilities = 0.0;

    /**
     * Creates a source node with the given name and distribution.
     * It swpawns infinite arrivals (Integer.MAX_VALUE) that are served by infinite
     * servers (Integer.MAX_VALUE).
     * 
     * @param name         The name of the node.
     * @param distribution The distribution of the inter-arrival times.
     * @return The created source node.
     */
    public static ServerNode createSource(String name, Distribution distribution) {
        return new ServerNode(name, Integer.MAX_VALUE, distribution, Integer.MAX_VALUE, 0);
    }

    /**
     * Creates a source node with the given name, distribution, and number of
     * arrivals to spawn that are served by infinite servers (Integer.MAX_VALUE).
     * 
     * @param name          The name of the node.
     * @param distribution  The distribution of the inter-arrival times.
     * @param spawnArrivals The number of arrivals to spawn.
     * @return The created source node.
     */
    public static ServerNode createLimitedSource(String name, Distribution distribution, int spawnArrivals) {
        return new ServerNode(name, Integer.MAX_VALUE, distribution, spawnArrivals, 0);
    }

    /**
     * Creates a queue node with the given name, maximum number of servers, and
     * distribution.
     * 
     * @param name         The name of the node.
     * @param maxServers   The maximum number of servers in the queue.
     * @param distribution The distribution of the service times.
     * @return The created queue node.
     */
    public static ServerNode createQueue(String name, int maxServers, Distribution distribution) {
        return new ServerNode(name, maxServers, distribution, 0, 0);
    }

    /**
     * Creates a generic node with the given name and distribution.
     * 
     * @param name           The name of the node.
     * @param maxServers     The maximum number of servers in the queue.
     * @param distribution   The distribution of the service times.
     * @param spawnArrivals  The number of arrivals to spawn.
     * @param sinkDepartures The number of departures to sink.
     */
    public ServerNode(String name, int maxServers, Distribution distribution, int spawnArrivals, int sinkDepartures) {
        this.name = name;
        this.maxServers = maxServers;
        this.distribution = distribution;
        this.spawnArrivals = spawnArrivals;
        this.sinkDepartures = sinkDepartures;
    }

    /**
     * Adds a child node with the given probability to select it.
     * 
     * @param node        The child node to add.
     * @param probability The probability of the child node.
     */
    public void addChild(ServerNode node, double probability) {
        this.children.add(new NodeChild(node, probability));
        this.sumProbabilities += probability;
    }

    /**
     * Gets a child node based on the given random number generator.
     * 
     * @param rng The random number generator to use.
     * @return The child node selected based on the probabilities.
     */
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

    /**
     * Determines if the node should spawn an arrival based on the number of
     * arrivals.
     * 
     * @param numArrivals The number of arrivals to check against.
     * @return True if the node should spawn an arrival, false otherwise.
     */
    public boolean shouldSpawnArrival(int numArrivals) {
        return this.spawnArrivals > numArrivals;
    }

    /**
     * Determines if the node should sink a departure based on the number of
     * departures.
     * 
     * @param numDepartures The number of departures to check against.
     * @return True if the node should sink a departure, false otherwise.
     */
    public boolean shouldSinkDeparture(int numDepartures) {
        return this.sinkDepartures > numDepartures;
    }

    /**
     * Represents a child node with a probability to select it.
     */
    public static class NodeChild {
        public final ServerNode node;
        public final double probability;

        public NodeChild(ServerNode node, double probability) {
            this.node = node;
            this.probability = probability;
        }
    }
}
