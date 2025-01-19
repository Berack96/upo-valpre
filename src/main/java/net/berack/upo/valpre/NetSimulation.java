package net.berack.upo.valpre;

import java.util.ArrayList;
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.berack.upo.valpre.NetStatistics.SingleRun;
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
     * Runs the simulation with the given seed until a given criteria is met.
     * 
     * @param seed      The seed to use for the random number generator.
     * @param criterias The criteria to determine when to end the simulation. If
     *                  null then the simulation will run until there are no more
     *                  events.
     * @return The statistics the network.
     */
    public NetStatistics.SingleRun run(long seed, EndCriteria... criterias) {
        return this.run(new Rng(seed), criterias);
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
        var stats = new SingleRun[runs];

        for (int i = 0; i < runs; i++) {
            stats[i] = this.run(rng, criterias);
        }
        return new NetStatistics(stats);
    }

    /**
     * Runs the simulation multiple times with the given seed and number of runs.
     * The runs are calculated in parallel using the given number of threads.
     * 
     * @param seed       The seed to use for the random number generator.
     * @param runs       The number of runs to perform.
     * @param numThreads The number of threads to use for the simulation.
     * @param criterias  The criteria to determine when to end the simulation. If
     *                   null then the simulation will run until there are no more
     *                   events.
     * @return The statistics the network.
     * @throws InterruptedException If the threads are interrupted.
     * @throws ExecutionException   If the one of the threads has been aborted.
     */
    public NetStatistics runParallel(long seed, int runs, EndCriteria... criterias)
            throws InterruptedException, ExecutionException {
        var rngs = new Rngs(seed);
        var stats = new NetStatistics.SingleRun[runs];
        var futures = new Future[runs];

        var numThreads = Math.min(runs, Runtime.getRuntime().availableProcessors());
        var threads = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < runs; i++) {
            final var id = i;
            futures[i] = threads.submit(() -> {
                stats[id] = this.run(rngs.getRng(id), criterias);
            });
        }

        for (var i = 0; i < runs; i++) {
            futures[i].get();
        }

        threads.shutdownNow();
        return new NetStatistics(stats);
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
    public NetStatistics.SingleRun run(Rng rng, EndCriteria... criterias) {
        var run = new SimpleRun(this.servers, rng, criterias);
        while (!run.hasEnded()) {
            run.processNextEvent();
        }
        return run.endSimulation();
    }

    /**
     * Process an entire run of the simulation.
     */
    public static class SimpleRun {

        private final NetStatistics.SingleRun stats;
        private final PriorityQueue<Event> fel;
        private final EndCriteria[] criterias;

        /**
         * Creates a new run of the simulation with the given nodes and random number
         * generator.
         * 
         * @param nodes The nodes in the network.
         * @param rng   The random number generator to use.
         */
        private SimpleRun(Collection<ServerNode> nodes, Rng rng, EndCriteria... criterias) {
            this.fel = new PriorityQueue<>();
            this.stats = new NetStatistics.SingleRun(nodes, rng);
            this.criterias = criterias;

            // Initial arrivals (if spawned)
            for (var node : nodes) {
                if (node.shouldSpawnArrival(0))
                    this.addEvent(node, Event.Type.ARRIVAL);
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
            stats.simulationTime = event.time;

            switch (event.type) {
                case ARRIVAL -> this.processArrival(event);
                case DEPARTURE -> this.processDeparture(event);
            }
        }

        /**
         * Ends the simulation and returns the statistics of the network.
         * 
         * @return The statistics of the network.
         */
        public NetStatistics.SingleRun endSimulation() {
            this.stats.endSimulation();
            return this.stats;
        }

        /**
         * Processes an arrival event for the given node at the given time.
         * The event is processed by adding the arrival time to the queue, updating the
         * maximum queue length, and checking if a server is available to process the
         * arrival. If a server is available, a departure event is created and added to
         * the future event list.
         * 
         * @param stats The statistics of the network.
         * @param event The arrival event to process.
         * @param fel   The future event list to add new events to.
         */
        private void processArrival(Event event) {
            var nodeStats = stats.nodes.get(event.node.name);

            nodeStats.numArrivals++;
            nodeStats.enqueue(event.time);
            if (event.node.maxServers > nodeStats.numServerBusy) {
                nodeStats.numServerBusy++;
                this.addEvent(event.node, Event.Type.DEPARTURE);
            } else {
                nodeStats.busyTime += stats.simulationTime - nodeStats.lastEventTime;
            }

            nodeStats.lastEventTime = stats.simulationTime;
            if (event.node.shouldSpawnArrival(nodeStats.numArrivals)) {
                this.addEvent(event.node, Event.Type.ARRIVAL);
            }
        }

        /**
         * Processes a departure event for the given node at the given time.
         * The event is processed by removing the departure time from the queue,
         * updating the busy time, and checking if there are any arrivals in the queue.
         * If there are, a new departure event is created and added to the fel.
         * At the end it will add an arrival to the next node if the current node has a
         * child.
         * 
         * @param stats The statistics of the network.
         * @param event The departure event to process.
         * @param fel   The future event list to add new events to.
         */
        private void processDeparture(Event event) {
            var nodeStats = stats.nodes.get(event.node.name);
            var startService = nodeStats.dequeue();
            var response = stats.simulationTime - startService;

            if (nodeStats.getQueueSize() < nodeStats.numServerBusy) {
                nodeStats.numServerBusy--;
            } else {
                this.addEvent(event.node, Event.Type.DEPARTURE);
            }

            nodeStats.numDepartures++;
            nodeStats.responseTime += response;
            nodeStats.busyTime += stats.simulationTime - nodeStats.lastEventTime;
            nodeStats.lastEventTime = stats.simulationTime;

            if (!event.node.shouldSinkDeparture(nodeStats.numDepartures)) {
                var next = event.node.getChild(stats.rng);
                this.addEvent(next, Event.Type.ARRIVAL);
            }
        }

        /**
         * Adds an event to the future event list.
         * The event is created based on the given node and type, and the delay is
         * determined by the node's distribution.
         * 
         * @param node The node to create the event for.
         * @param type The type of event to create.
         */
        public void addEvent(ServerNode node, Event.Type type) {
            if (node != null) {
                var delay = node.getPositiveSample(stats.rng);
                var event = Event.newType(node, stats.simulationTime + delay, type);
                fel.add(event);
            }
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
                if (c.shouldEnd(stats)) {
                    return true;
                }
            }
            return false;
        }
    }
}
