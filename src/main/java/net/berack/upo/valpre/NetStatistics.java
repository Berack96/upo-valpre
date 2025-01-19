package net.berack.upo.valpre;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.berack.upo.valpre.rand.Rng;

public class NetStatistics {
    public final SingleRun[] runs;

    public NetStatistics(SingleRun... runs) {
        this.runs = runs;
    }

    /**
     * Represents the statistics of a network simulation.
     * It is used by the simulation to track the behavior of the network and its
     * nodes, including the number of arrivals and departures, the maximum queue
     * length, the busy time, and the response time.
     */
    public static class SingleRun {
        public final Map<String, Node> nodes;
        public final long seed;
        public final Rng rng;
        public double simulationTime;
        public long timeElapsedNano;

        /**
         * Creates a new statistics object for the given collection of server nodes and
         * random number generator.
         * 
         * @param nodes The collection of server nodes to track.
         * @param rng   The random number generator to use.
         */
        public SingleRun(Collection<ServerNode> nodes, Rng rng) {
            this.rng = rng;
            this.seed = rng.getSeed();

            this.simulationTime = 0.0d;
            this.timeElapsedNano = System.nanoTime();
            this.nodes = new HashMap<String, Node>();
            for (var node : nodes) {
                var s = new Node();
                this.nodes.put(node.name, s);
            }

        }

        /**
         * Ends the simulation and calculates the elapsed time.
         */
        public void endSimulation() {
            this.timeElapsedNano = System.nanoTime() - this.timeElapsedNano;
        }

        /**
         * Print a summary of the statistics to the console.
         * The summary includes the seed, the simulation time, the elapsed time, and
         * the statistics for each node in the network.
         */
        public void printSummary() {
            var size = (int) Math.ceil(Math.log10(this.simulationTime));
            var format = "%" + (size + 4) + ".3f";

            System.out.println("===== Net Stats =====");
            System.out.println("Seed:       \t" + this.seed);
            System.out.printf("Simulation: \t" + format + "\n", this.simulationTime);
            System.out.printf("Elapsed:    \t" + format + "ms\n", this.timeElapsedNano / 1e6);

            for (var entry : this.nodes.entrySet()) {
                var stats = entry.getValue();
                var entrySize = (int) Math.max(size, (int) Math.ceil((Math.log10(stats.numArrivals))));
                var iFormat = "%" + entrySize + "d";
                var fFormat = "%" + (entrySize + 4) + ".3f";

                System.out.println("===== " + entry.getKey() + " =====");
                System.out.printf("  Arrivals:  \t" + iFormat + "\n", stats.numArrivals);
                System.out.printf("  Departures:\t" + iFormat + "\n", stats.numDepartures);
                System.out.printf("  Max Queue: \t" + iFormat + "\n", stats.maxQueueLength);
                System.out.printf("  Response:  \t" + fFormat + "\n", stats.responseTime / stats.numDepartures);
                System.out.printf("  Busy %%:   \t" + fFormat + "\n", stats.busyTime * 100 / stats.lastEventTime);
                System.out.printf("  Last Event:\t" + fFormat + "\n", stats.lastEventTime);
            }
        }
    }

    /**
     * Represents a statistical summary of the behavior of a server node in the
     * network.
     * It is used by the simulation to track the number of arrivals and departures,
     * the maximum queue length, the busy time, and the response time.
     */
    public static class Node {
        public int numArrivals = 0;
        public int numDepartures = 0;
        public int maxQueueLength = 0;
        public double averageQueueLength = 0.0d;
        public double busyTime = 0.0d;
        public double responseTime = 0.0d;
        public double lastEventTime = 0.0d;

        public int numServerBusy = 0;
        private ArrayDeque<Double> queue = new ArrayDeque<>();

        /**
         * Resets the statistics to their initial values.
         */
        public void reset() {
            this.numArrivals = 0;
            this.numDepartures = 0;
            this.maxQueueLength = 0;
            this.averageQueueLength = 0.0;
            this.busyTime = 0.0;
            this.responseTime = 0.0;
            this.lastEventTime = 0.0;
            this.numServerBusy = 0;
            this.queue.clear();
        }

        public double dequeue() {
            return this.queue.poll();
        }

        public void enqueue(double time) {
            var total = this.averageQueueLength * (this.numArrivals - 1);

            this.queue.add(time);
            this.averageQueueLength = (total + this.queue.size()) / this.numArrivals;
            this.maxQueueLength = Math.max(this.maxQueueLength, this.queue.size());
        }

        public int getQueueSize() {
            return this.queue.size();
        }
    }

}