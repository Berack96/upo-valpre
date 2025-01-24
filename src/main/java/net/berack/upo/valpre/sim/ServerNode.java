package net.berack.upo.valpre.sim;

import net.berack.upo.valpre.rand.Distribution;
import net.berack.upo.valpre.rand.Rng;

/**
 * Represents a node in the network. It can be a source, a queue, or a sink
 * based on the configuration passed as parameters.
 */
public class ServerNode {
    public final String name;
    public final int maxServers;
    public final int spawnArrivals;
    public final Distribution distribution;

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
        return new ServerNode(name, Integer.MAX_VALUE, distribution, Integer.MAX_VALUE);
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
        return new ServerNode(name, Integer.MAX_VALUE, distribution, spawnArrivals);
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
        return new ServerNode(name, maxServers, distribution, 0);
    }

    /**
     * Creates a generic node with the given name and distribution.
     * The servers number must be 1 or higher; if lower will be put to 1.
     * The spawn number must be 0 or higher; if lower will be put to 0.
     * The distribution can't be null, otherwise an exception is thrown.
     * 
     * @param name          The name of the node.
     * @param maxServers    The maximum number of servers in the queue.
     * @param distribution  The distribution of the service times.
     * @param spawnArrivals The number of arrivals to spawn.
     * @throws NullPointerException if the distribution is null
     */
    public ServerNode(String name, int maxServers, Distribution distribution, int spawnArrivals) {
        if (distribution == null)
            throw new NullPointerException("Distribution can't be null");
        if (maxServers <= 0)
            maxServers = 1;
        if (spawnArrivals < 0)
            spawnArrivals = 0;

        this.name = name;
        this.maxServers = maxServers;
        this.distribution = distribution;
        this.spawnArrivals = spawnArrivals;
    }

    /**
     * Gets a positive sample from the distribution.
     * This is useful if you need to generate a positive value from a distribution
     * that can generate negative values. For example, the normal distribution.
     * 
     * @param rng The random number generator to use.
     * @return A positive sample from the distribution.
     */
    public double getPositiveSample(Rng rng) {
        double sample;
        do {
            sample = this.distribution.sample(rng);
        } while (sample < 0);
        return sample;
    }

    /**
     * Determines if the node should spawn an arrival based on the number of
     * arrivals.
     * 
     * @param numArrivals The number of arrivals to check against.
     * @return True if the node should spawn an arrival, false otherwise.
     */
    public boolean shouldSpawnArrival(double numArrivals) {
        return this.spawnArrivals > Math.max(0, numArrivals);
    }
}
