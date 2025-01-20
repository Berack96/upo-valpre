package net.berack.upo.valpre;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 */
public class NetStatistics {
    public final RunResult[] runs;
    // public final Run average;
    // public final Run variance;

    /**
     * TODO
     * @param runs
     */
    public NetStatistics(RunResult... runs) {
        this.runs = runs;
    }

    /**
     * Represents the statistics of a network simulation.
     * It is used by the simulation to track the behavior of the network and its
     * nodes, including the number of arrivals and departures, the maximum queue
     * length, the busy time, and the response time.
     */
    public static class RunResult {
        public final Map<String, Statistics> nodes;
        public final long seed;
        public final double simulationTime;
        public final long timeElapsedNano;

        /**
         * Creates a new statistics object for the given collection of server nodes and
         * random number generator.
         * 
         * @param nodes The collection of server nodes to track.
         * @param rng   The random number generator to use.
         */
        public RunResult(long seed, double time, long elapsed, HashMap<String, Statistics> nodes) {
            this.seed = seed;
            this.simulationTime = time;
            this.timeElapsedNano = elapsed;
            this.nodes = nodes;
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
                var iFormat = "%" + entrySize + ".0f";
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
     * TODO
     */
    public static class Statistics {
        public double numArrivals = 0;
        public double numDepartures = 0;
        public double maxQueueLength = 0;
        public double averageQueueLength = 0.0d;
        public double busyTime = 0.0d;
        public double responseTime = 0.0d;
        public double lastEventTime = 0.0d;

        /**
         * Resets the statistics to their initial values.
         */
        public void reset() {
            this.numArrivals = 0;
            this.numDepartures = 0;
            this.maxQueueLength = 0;
            this.averageQueueLength = 0.0d;
            this.busyTime = 0.0d;
            this.responseTime = 0.0d;
            this.lastEventTime = 0.0d;
        }
    }
}