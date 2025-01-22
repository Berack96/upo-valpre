package net.berack.upo.valpre.sim;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import net.berack.upo.valpre.rand.Rng;
import net.berack.upo.valpre.sim.stats.Result;
import net.berack.upo.valpre.sim.stats.Statistics;

/**
 * Process an entire run of the simulation.
 */
public final class Simulation {
    private final Net net;
    private final Map<String, NodeBehavior> nodes;
    private final PriorityQueue<Event> fel;
    private final EndCriteria[] criterias;
    private final long timeStartedNano;
    private final long seed;
    private final Rng rng;
    private double time;

    /**
     * Creates a new run of the simulation with the given nodes and random number
     * generator.
     * 
     * @param nodes     The nodes in the network.
     * @param rng       The random number generator to use.
     * @param criterias when the simulation has to end.
     */
    public Simulation(Net net, Rng rng, EndCriteria... criterias) {
        this.net = net;
        this.nodes = new HashMap<>();
        this.fel = new PriorityQueue<>();
        this.criterias = criterias;
        this.timeStartedNano = System.nanoTime();
        this.seed = rng.getSeed();
        this.rng = rng;
        this.time = 0.0d;

        // Initial arrivals (if spawned)
        net.forEachNode(node -> {
            this.nodes.put(node.name, new NodeBehavior());
            if (node.shouldSpawnArrival(0))
                this.addArrival(node);
        });
    }

    /**
     * Runs the simulation until a given criteria is met.
     * 
     * @return The final statistics the network.
     */
    public Result run() {
        while (!this.hasEnded())
            this.processNextEvent();
        return this.endSimulation();
    }

    /**
     * Processes the next event in the future event list.
     * This method will throw NullPointerException if there are no more events.
     * You should check if the simulation has ended before calling this method.
     * 
     * @see #hasEnded()
     */
    public void processNextEvent() {
        var event = fel.poll();
        var node = event.node;
        var behaviour = this.nodes.get(node.name);
        this.time = event.time;

        switch (event.type) {
            case ARRIVAL -> {
                if (behaviour.updateArrival(event.time, node.maxServers))
                    this.addDeparture(node);
            }
            case DEPARTURE -> {
                if (behaviour.updateDeparture(event.time))
                    this.addDeparture(node);

                var next = this.net.getChildOf(node, this.rng);
                if (next != null) {
                    this.addArrival(next);
                }
                if (node.shouldSpawnArrival(behaviour.stats.numArrivals)) {
                    this.addArrival(node);
                }
            }
        }
    }

    /**
     * Ends the simulation and returns the statistics of the network.
     * 
     * @return The statistics of the network.
     */
    public Result endSimulation() {
        var elapsed = System.nanoTime() - this.timeStartedNano;
        var nodes = new HashMap<String, Statistics>();
        for (var entry : this.nodes.entrySet())
            nodes.put(entry.getKey(), entry.getValue().stats);

        return new Result(this.seed, this.time, elapsed, nodes);
    }

    /**
     * Get the current time.
     * 
     * @return a double representing the current time of the simulation.
     */
    public double getTime() {
        return this.time;
    }

    /**
     * Get the node requested by the name passed as a string.
     * 
     * @param node the name of the node
     * @return the node
     */
    public NodeBehavior getNode(String node) {
        return this.getNode(node);
    }

    /**
     * Adds an arrival event to the future event list. The event is created based
     * on the given node, and no delay is added.
     * 
     * @param node The node to create the event for.
     */
    public void addArrival(ServerNode node) {
        var event = Event.newArrival(node, this.time);
        fel.add(event);
    }

    /**
     * Adds a departure event to the future event list. The event is created based
     * on the given node, and the delay is determined by the node's distribution.
     * 
     * @param node The node to create the event for.
     */
    public void addDeparture(ServerNode node) {
        var delay = node.getPositiveSample(this.rng);
        var event = Event.newDeparture(node, this.time + delay);
        fel.add(event);
    }

    /**
     * Determines if the simulation has finshed based on the given criteria.
     * 
     * @return True if the simulation should end, false otherwise.
     */
    public boolean hasEnded() {
        if (fel.isEmpty()) {
            return true;
        }
        for (var c : this.criterias) {
            if (c.shouldEnd(this)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Represents a summary of the behavior of a server node in the network.
     * It is used by the simulation to track the number of arrivals and departures,
     * the maximum queue length, the busy time, and the response time.
     */
    public static class NodeBehavior {
        public int numServerBusy = 0;
        public final Statistics stats = new Statistics();
        private final ArrayDeque<Double> queue = new ArrayDeque<>();

        /**
         * TODO
         * 
         * @param time
         * @param maxServers
         * @return
         */
        public boolean updateArrival(double time, int maxServers) {
            var total = this.stats.averageQueueLength * this.stats.numArrivals;

            this.queue.add(time);
            this.stats.numArrivals++;
            this.stats.averageQueueLength = (total + this.queue.size()) / this.stats.numArrivals;
            this.stats.maxQueueLength = Math.max(this.stats.maxQueueLength, this.queue.size());

            var startDeparture = maxServers > this.numServerBusy;
            if (startDeparture) {
                this.numServerBusy++;
            } else {
                this.stats.busyTime += time - this.stats.lastEventTime;
            }

            this.stats.lastEventTime = time;
            return startDeparture;
        }

        /**
         * TODO
         * 
         * @param time
         * @return
         */
        public boolean updateDeparture(double time) {
            var startService = this.queue.poll();
            var response = time - startService;

            var startDeparture = this.queue.size() >= this.numServerBusy;
            if (!startDeparture) {
                this.numServerBusy--;
            }

            this.stats.numDepartures++;
            this.stats.responseTime += response;
            this.stats.busyTime += time - this.stats.lastEventTime;
            this.stats.lastEventTime = time;
            return startDeparture;
        }
    }
}