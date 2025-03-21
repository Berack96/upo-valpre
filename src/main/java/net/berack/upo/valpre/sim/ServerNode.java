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
     * Creates a generic node with the given name and distribution.
     * The servers number must be 1 or higher; if lower will be put to 1.
     * The spawn number must be 0 or higher; if lower will be put to -1 (infinite).
     * The queue number must be equal or higher than the servers number; if lower
     * will be put to the servers number.
     * The service distribution can't be null, otherwise an exception is thrown.
     * 
     * @param name        The name of the node.
     * @param servers     The maximum number of servers in the queue.
     * @param spawn       The number of arrivals to spawn.
     * @param queue       The maximum number of requests in the queue.
     * @param service     The distribution of the service times.
     * @param unavailable The distribution of the unavailable times after service.
     * @throws NullPointerException if the distribution is null
     */
    private ServerNode(String name, int servers, int spawn, int queue, Distribution service, Distribution unavailable) {
        if (service == null)
            throw new NullPointerException("Service distribution can't be null");
        if (servers <= 0)
            servers = 1;
        if (spawn < 0)
            spawn = -1;
        if (queue < servers)
            queue = servers;

        this.name = name;
        this.maxQueue = queue;
        this.maxServers = servers;
        this.spawnArrivals = spawn;
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
    public String toString() {
        try {
            var build = new StringBuilder()
                    .append(this.name)
                    .append("[servers:")
                    .append(this.maxServers)
                    .append(", queue:")
                    .append(this.maxQueue)
                    .append(", spawn:")
                    .append(this.spawnArrivals)
                    .append(", ")
                    .append(Distribution.toString(this.service));
            if (this.unavailable != null)
                build.append(", u:").append(Distribution.toString(this.unavailable));
            return build.append(']').toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
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

    /**
     * Creates a new builder for the node.
     * It is useful to create a node with a more readable syntax and a in a more
     * flexible way.
     */
    public static class Builder {
        private String name;
        private int maxQueue;
        private int maxServers;
        private int spawnArrivals;
        private Distribution service;
        private Distribution unavailable;

        /**
         * Creates a new builder for the node with the given name and distribution.
         * The maximum number of servers is set to 1, the maximum number of requests in
         * the queue is set to 100, the number of arrivals to spawn is set to 0, and
         * the unavailable time is set to null.
         * 
         * @param name    The name of the node.
         * @param service The distribution of the service times.
         * @return The created sink node.
         */
        public Builder(String name, Distribution service) {
            this.reset(name, service);
        }

        /**
         * Reset the builder with the given name and distribution.
         * 
         * @param name    The name of the node.
         * @param service The distribution of the service times.
         * @return The builder itself.
         */
        public Builder reset(String name, Distribution service) {
            this.name = name;
            this.service = service;
            this.maxQueue = 100; // default value
            this.maxServers = 1;
            this.spawnArrivals = 0;
            this.unavailable = null;
            return this;
        }

        /**
         * Set the maximum number of clients in the queue.
         * 
         * @param maxQueue The maximum number of clients in the queue.
         * @return The builder itself.
         */
        public Builder queue(int maxQueue) {
            this.maxQueue = maxQueue;
            return this;
        }

        /**
         * Set the maximum number of servers.
         * 
         * @param maxServers The maximum number of servers.
         * @return The builder itself.
         */
        public Builder servers(int maxServers) {
            this.maxServers = maxServers;
            return this;
        }

        /**
         * Set the number of arrivals to spawn.
         * 
         * @param spawnArrivals The number of arrivals to spawn.
         * @return The builder itself.
         */
        public Builder spawn(int spawnArrivals) {
            this.spawnArrivals = spawnArrivals;
            return this;
        }

        /**
         * Set the distribution of the unavailable times after service.
         * 
         * @param unavailable The distribution of the unavailable times after service.
         * @return The builder itself.
         */
        public Builder unavailable(Distribution unavailable) {
            this.unavailable = unavailable;
            return this;
        }

        /**
         * Build the node with the given parameters.
         * 
         * @return The created node.
         */
        public ServerNode build() {
            return new ServerNode(name, maxServers, spawnArrivals, maxQueue, service, unavailable);
        }

        /**
         * Creates a source node with the given name and distribution.
         * It swpawns infinite arrivals (-1).
         * 
         * @param name         The name of the node.
         * @param distribution The distribution of the inter-arrival times.
         * @return The created source node.
         */
        public static ServerNode source(String name, Distribution distribution) {
            return new Builder(name, distribution).spawn(-1).build();
        }

        /**
         * Creates a terminal node with the given name, distribution, and number of
         * arrivals to spawn.
         * Once it has finished spawning the arrivals, it will not spawn anymore.
         * 
         * @param name          The name of the node.
         * @param service       The distribution of the inter-arrival times.
         * @param spawnArrivals The number of arrivals to spawn.
         * @return The created source node.
         */
        public static ServerNode terminal(String name, int spawnArrivals, Distribution service) {
            return new Builder(name, service).spawn(spawnArrivals).build();
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
        public static ServerNode queue(String name, int maxServers, Distribution service) {
            return new Builder(name, service).servers(maxServers).build();
        }

        /**
         * Creates a queue node with the given name, maximum number of servers, and
         * distribution. It also has a distribution for the unavailable times after
         * each service.
         * 
         * @param name        The name of the node.
         * @param maxServers  The maximum number of servers in the queue.
         * @param service     The distribution of the service times.
         * @param unavailable The distribution of the unavailable times after service.
         * @return The created queue node.
         */
        public static ServerNode queue(String name, int maxServers, Distribution service, Distribution unavailable) {
            return new Builder(name, service).unavailable(unavailable).servers(maxServers).build();
        }
    }
}
