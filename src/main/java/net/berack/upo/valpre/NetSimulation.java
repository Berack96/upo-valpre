package net.berack.upo.valpre;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import net.berack.upo.valpre.rand.Rng;

/**
 * A network simulation that uses a discrete event simulation to model the
 * behavior of a network of servers.
 */
public class NetSimulation {
    public final long seed;
    private final Map<String, ServerNode> servers = new HashMap<>();

    /**
     * Creates a new network simulation with the given seed.
     * 
     * @param seed The seed to use for the random number generator.
     */
    public NetSimulation(long seed) {
        this.seed = seed;
    }

    /**
     * Adds a new server node to the network.
     * 
     * @param node The server node to add.
     */
    public void addNode(ServerNode node) {
        this.servers.put(node.name, node);
    }

    /**
     * Runs the simulation for the given number of total arrivals, stopping when the
     * given node has reached the specified number of departures.
     * If needed the run method can be called by multiple threads.
     * 
     * @param criteria The criteria to determine when to end the simulation. If null
     *                 then the simulation will run until there are no more events.
     * @return The statistics of the nodes in the network.
     */
    public Map<String, Statistics> run(EndSimulationCriteria... criteria) {
        // Initialization
        var timeNow = 0.0d;
        var rng = new Rng(this.seed); // TODO change here for thread variance (use Rngs with ids)
        var fel = new PriorityQueue<Event>();
        var stats = new HashMap<String, Statistics>();
        for (var node : this.servers.values()) {
            var s = new Statistics(rng);
            s.addArrivalIf(node.shouldSpawnArrival(s.numArrivals), node, timeNow, fel);
            stats.put(node.name, s);
        }

        // Main Simulation Loop
        while (!fel.isEmpty() && !this.shouldEnd(criteria, stats)) {
            var event = fel.poll();
            var statsNode = stats.get(event.node.name);
            timeNow = event.time;

            switch (event.type) {
                case ARRIVAL -> statsNode.processArrival(event, timeNow, fel);
                case DEPARTURE -> statsNode.processDeparture(event, timeNow, fel);
            }
        }
        return stats;
    }

    private boolean shouldEnd(EndSimulationCriteria[] criteria, Map<String, Statistics> stats) {
        for (var c : criteria) {
            if (c.shouldEnd(stats)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Represents a statistical summary of the behavior of a server node in the
     * network.
     * It is used by the simulation to track the number of arrivals and departures,
     * the maximum queue length, the busy time, and the response time.
     */
    public static class Statistics {
        public int numArrivals = 0;
        public int numDepartures = 0;
        public int maxQueueLength = 0;
        public double busyTime = 0.0;
        public double responseTime = 0.0;
        public double lastEventTime = 0.0;

        private int numServerBusy = 0;
        private ArrayDeque<Double> queue = new ArrayDeque<>();
        private final Rng rng;

        /**
         * Creates a new statistics object with the given random number generator.
         * 
         * @param rng The random number generator to use.
         */
        public Statistics(Rng rng) {
            this.rng = rng;
        }

        /**
         * Resets the statistics to their initial values.
         */
        public void reset() {
            this.numArrivals = 0;
            this.numDepartures = 0;
            this.numServerBusy = 0;
            this.busyTime = 0.0;
            this.responseTime = 0.0;
            this.queue.clear();
        }

        /**
         * Processes an arrival event for the given node at the given time.
         * The event is processed by adding the arrival time to the queue, updating the
         * maximum queue length, and checking if a server is available to process the
         * arrival. If a server is available, a departure event is created and added to
         * the future event list.
         * 
         * @param event   The arrival event to process.
         * @param timeNow The current time of the simulation.
         * @param fel     The future event list to add new events to.
         */
        private void processArrival(Event event, double timeNow, PriorityQueue<Event> fel) {
            this.numArrivals++;
            this.queue.add(event.time);
            this.maxQueueLength = Math.max(this.maxQueueLength, this.queue.size());

            if (event.node.maxServers > this.numServerBusy) {
                this.numServerBusy++;
                var time = event.node.distribution.sample(this.rng);
                var departure = Event.newDeparture(event.node, timeNow + time);
                fel.add(departure);
            } else {
                this.busyTime += timeNow - this.lastEventTime;
            }
            this.lastEventTime = timeNow;

            this.addArrivalIf(event.node.shouldSpawnArrival(this.numArrivals), event.node, timeNow, fel);
        }

        /**
         * Processes a departure event for the given node at the given time.
         * The event is processed by removing the departure time from the queue,
         * updating the busy time, and checking if there are any arrivals in the queue.
         * If there are, a new departure event is created and added to the fel.
         * At the end it will add an arrival to the next node if the current node has a
         * child.
         * 
         * @param event   The departure event to process.
         * @param timeNow The current time of the simulation.
         * @param fel     The future event list to add new events to.
         */
        private void processDeparture(Event event, double timeNow, PriorityQueue<Event> fel) {
            var startService = this.queue.poll();
            var response = timeNow - startService;

            if (this.queue.size() < this.numServerBusy) {
                this.numServerBusy--;
            } else {
                var time = event.node.distribution.sample(this.rng);
                var departure = Event.newDeparture(event.node, timeNow + time);
                fel.add(departure);
            }

            this.numDepartures++;
            this.responseTime += response;
            this.busyTime += timeNow - this.lastEventTime;
            this.lastEventTime = timeNow;

            var next = event.node.getChild(rng);
            this.addArrivalIf(!event.node.shouldSinkDeparture(this.numDepartures), next, timeNow, fel);
        }

        /**
         * Adds an arrival event to the future event list if the given condition is
         * true.
         * 
         * @param condition The condition to check.
         * @param node      The node to add the arrival event for.
         * @param timeNow   The current time of the simulation.
         * @param fel       The future event list to add the event to.
         */
        private void addArrivalIf(boolean condition, ServerNode node, double timeNow, PriorityQueue<Event> fel) {
            if (condition && node != null) {
                var delay = node.distribution.sample(this.rng);
                fel.add(Event.newArrival(node, timeNow + delay));
            }
        }
    }
}
