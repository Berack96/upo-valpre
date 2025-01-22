package net.berack.upo.valpre;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.berack.upo.valpre.NetStatistics.RunResult;
import net.berack.upo.valpre.NetStatistics.Statistics;
import net.berack.upo.valpre.rand.Rng;
import net.berack.upo.valpre.rand.Rngs;

/**
 * A network simulation that uses a discrete event simulation to model the
 * behavior of a network of servers.
 */
public class NetSimulation {
    private final Collection<ServerNode> servers = new ArrayList<>();

    /**
     * Adds a new server node to the network.
     * 
     * @param node The server node to add.
     */
    public void addNode(ServerNode node) {
        this.servers.add(node);
    }

    /**
     * Run the simualtion multiple times with the given seed and number of runs.
     * The runs are calculated one after the other. For a parallel run see
     * {@link #runParallel(long, int, EndCriteria...)}.
     * 
     * @param seed      The seed to use for the random number generator.
     * @param runs      The number of runs to perform.
     * @param criterias The criteria to determine when to end the simulation. If
     *                  null then the simulation will run until there are no more
     *                  events.
     * @return The statistics the network.
     */
    public NetStatistics run(long seed, int runs, EndCriteria... criterias) {
        var rng = new Rng(seed);
        var stats = new RunResult[runs];

        for (int i = 0; i < runs; i++) {
            stats[i] = this.run(rng, criterias);
        }
        return new NetStatistics(stats);
    }

    /**
     * Runs the simulation multiple times with the given seed and number of runs.
     * The runs are calculated in parallel using the given number of threads.
     * The maximum number of threads are determined by the available processors
     * and the number of runs.
     * 
     * @param seed      The seed to use for the random number generator.
     * @param runs      The number of runs to perform.
     * @param criterias The criteria to determine when to end the simulation. If
     *                  null then the simulation will run until there are no more
     *                  events.
     * @return The statistics the network.
     * @throws InterruptedException If the threads are interrupted.
     * @throws ExecutionException   If the one of the threads has been aborted.
     */
    public NetStatistics runParallel(long seed, int runs, EndCriteria... criterias)
            throws InterruptedException, ExecutionException {
        var rngs = new Rngs(seed);
        var results = new NetStatistics.RunResult[runs];
        var futures = new Future[runs];

        var numThreads = Math.min(runs, Runtime.getRuntime().availableProcessors());
        try (var threads = Executors.newFixedThreadPool(numThreads)) {
            for (int i = 0; i < runs; i++) {
                final var id = i;
                futures[i] = threads.submit(() -> {
                    results[id] = this.run(rngs.getRng(id), criterias);
                });
            }

            for (var i = 0; i < runs; i++) {
                futures[i].get();
            }

            return new NetStatistics(results);
        }
    }

    /**
     * Runs the simulation until a given criteria is met.
     * 
     * @param rng       The random number generator to use.
     * @param criterias The criteria to determine when to end the simulation. If
     *                  null then the simulation will run until there are no more
     *                  events.
     * @return The statistics the network.
     */
    public NetStatistics.RunResult run(Rng rng, EndCriteria... criterias) {
        var run = new SimulationRun(this.servers, rng, criterias);
        while (!run.hasEnded())
            run.processNextEvent();
        return run.endSimulation();
    }

    /**
     * Process an entire run of the simulation.
     */
    public static class SimulationRun {
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
        private SimulationRun(Collection<ServerNode> nodes, Rng rng, EndCriteria... criterias) {
            this.nodes = new HashMap<>();
            this.fel = new PriorityQueue<>();
            this.criterias = criterias;
            this.timeStartedNano = System.nanoTime();
            this.seed = rng.getSeed();
            this.rng = rng;
            this.time = 0.0d;

            // Initial arrivals (if spawned)
            for (var node : nodes) {
                this.nodes.put(node.name, new NodeBehavior());
                if (node.shouldSpawnArrival(0))
                    this.addArrival(node);
            }
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
            var node = this.nodes.get(event.node.name);
            this.time = event.time;

            switch (event.type) {
                case ARRIVAL -> {
                    if (node.updateArrival(event.time, event.node.maxServers))
                        this.addDeparture(event.node);
                }
                case DEPARTURE -> {
                    if (node.updateDeparture(event.time))
                        this.addDeparture(event.node);

                    var next = event.node.getChild(this.rng);
                    if (next != null) {
                        this.addArrival(next);
                    }
                    if (event.node.shouldSpawnArrival(node.stats.numArrivals)) {
                        this.addArrival(event.node);
                    }
                }
            }
        }

        /**
         * Ends the simulation and returns the statistics of the network.
         * 
         * @return The statistics of the network.
         */
        private NetStatistics.RunResult endSimulation() {
            var elapsed = System.nanoTime() - this.timeStartedNano;
            var nodes = new HashMap<String, Statistics>();
            for (var entry : this.nodes.entrySet())
                nodes.put(entry.getKey(), entry.getValue().stats);

            return new RunResult(this.seed, this.time, elapsed, nodes);
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
