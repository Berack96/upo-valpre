package net.berack.upo.valpre.sim;

import net.berack.upo.valpre.rand.Distribution;
import net.berack.upo.valpre.rand.Rng;

/**
 * Represents a node in the network. It can be a source, a queue, or a sink
 * based on the configuration passed as parameters.
 */
public class ServerNode {
    public final String name;
    public final int maxQueue;
    public final int maxServers;
    public final int spawnArrivals;
    public final Distribution service;
    public final Distribution unavailable;

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
        return new ServerNode(name, Integer.MAX_VALUE, distribution, null, Integer.MAX_VALUE);
    }

    /**
     * Creates a source node with the given name, distribution, and number of
     * arrivals to spawn that are served by infinite servers (Integer.MAX_VALUE).
     * 
     * @param name          The name of the node.
     * @param service       The distribution of the inter-arrival times.
     * @param spawnArrivals The number of arrivals to spawn.
     * @return The created source node.
     */
    public static ServerNode createLimitedSource(String name, Distribution service, int spawnArrivals) {
        return new ServerNode(name, Integer.MAX_VALUE, service, null, spawnArrivals);
    }

    /**
     * Creates a queue node with the given name, maximum number of servers, and
     * distribution.
     * 
     * @param name       The name of the node.
     * @param maxServers The maximum number of servers in the queue.
     * @param service    The distribution of the service times.
     * @return The created queue node.
     */
    public static ServerNode createQueue(String name, int maxServers, Distribution service) {
        return new ServerNode(name, maxServers, service, null, 0);
    }

    /**
     * Creates a queue node with the given name, maximum number of servers, and
     * distribution.
     * 
     * @param name        The name of the node.
     * @param maxServers  The maximum number of servers in the queue.
     * @param service     The distribution of the service times.
     * @param unavailable The distribution of the unavailable times after service.
     * @return The created queue node.
     */
    public static ServerNode createQueue(String name, int maxServers, Distribution service, Distribution unavailable) {
        return new ServerNode(name, maxServers, service, unavailable, 0);
    }

    /**
     * Creates a generic node with the given name and distribution.
     * The servers number must be 1 or higher; if lower will be put to 1.
     * The spawn number must be 0 or higher; if lower will be put to 0.
     * The distribution can't be null, otherwise an exception is thrown.
     * 
     * @param name          The name of the node.
     * @param maxServers    The maximum number of servers in the queue.
     * @param service       The distribution of the service times.
     * @param unavailable   The distribution of the unavailable times after service.
     * @param spawnArrivals The number of arrivals to spawn.
     * @throws NullPointerException if the distribution is null
     */
    private ServerNode(String name, int maxServers, Distribution service, Distribution unavailable, int spawnArrivals) {
        if (service == null)
            throw new NullPointerException("Service distribution can't be null");
        if (maxServers <= 0)
            maxServers = 1;
        if (spawnArrivals < 0)
            spawnArrivals = 0;

        this.name = name;
        this.maxQueue = 100; // TODO change to runtime
        this.maxServers = maxServers;
        this.spawnArrivals = spawnArrivals;
        this.service = service;
        this.unavailable = unavailable;
    }

    /**
     * Gets a positive sample from the distribution.
     * This is useful if you need to generate a positive value from a distribution
     * that can generate negative values. For example, the normal distribution.
     * 
     * @param rng The random number generator to use.
     * @return A positive sample from the distribution.
     */
    public double getServiceTime(Rng rng) {
        return Distribution.getPositiveSample(this.service, rng);
    }

    /**
     * Return the unavailable time after a service
     * 
     * @param rng The random number generator to use.
     * @return A positive or 0 value from the distribution.
     */
    public double getUnavailableTime(Rng rng) {
        return Distribution.getPositiveSample(this.unavailable, rng);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ServerNode))
            return false;
        var other = (ServerNode) obj;
        return obj.hashCode() == other.hashCode();
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
