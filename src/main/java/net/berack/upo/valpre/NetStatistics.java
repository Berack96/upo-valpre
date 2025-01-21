package net.berack.upo.valpre;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 */
public class NetStatistics {
    public final RunResult[] runs;
    public final RunResult average;
    public final RunResult variance;

    /**
     * TODO
     * 
     * @param runs
     */
    public NetStatistics(RunResult... runs) {
        this.runs = runs;
        this.average = calcAvg(runs);
        this.variance = calcVar(this.average, runs);
    }

    /**
     * TODO
     * 
     * @param runs
     * @return
     */
    public static RunResult calcAvg(RunResult... runs) {
        var avgTime = 0.0d;
        var avgElapsed = 0L;
        var nodes = new HashMap<String, Statistics>();

        for (var run : runs) {
            avgTime += run.simulationTime;
            avgElapsed += run.timeElapsedNano;

            for (var entry : run.nodes.entrySet()) {
                var stat = nodes.computeIfAbsent(entry.getKey(), _ -> new Statistics());
                var other = entry.getValue();
                stat.numDepartures += other.numDepartures;
                stat.numArrivals += other.numArrivals;
                stat.busyTime += other.busyTime;
                stat.responseTime += other.responseTime;
                stat.lastEventTime += other.lastEventTime;
                stat.averageQueueLength += other.averageQueueLength;
                stat.maxQueueLength = Math.max(stat.maxQueueLength, other.maxQueueLength);
            }
        }

        avgTime /= runs.length;
        avgElapsed /= runs.length;
        for (var stat : nodes.values()) {
            stat.numDepartures /= runs.length;
            stat.numArrivals /= runs.length;
            stat.busyTime /= runs.length;
            stat.responseTime /= runs.length;
            stat.lastEventTime /= runs.length;
            stat.averageQueueLength /= runs.length;
        }
        return new RunResult(runs[0].seed, avgTime, avgElapsed, nodes);
    }

    /**
     * TODO
     * 
     * @param avg
     * @param runs
     * @return
     */
    public static RunResult calcVar(RunResult avg, RunResult... runs) {
        var varTime = 0.0d;
        var varElapsed = 0L;
        var nodes = new HashMap<String, Statistics>();

        for (var run : runs) {
            varTime += Math.pow(run.simulationTime - avg.simulationTime, 2);
            varElapsed += Math.pow(run.timeElapsedNano - avg.simulationTime, 2);

            for (var entry : run.nodes.entrySet()) {
                var stat = nodes.computeIfAbsent(entry.getKey(), _ -> new Statistics());
                var average = avg.nodes.get(entry.getKey());
                var other = entry.getValue();
                stat.numDepartures += Math.pow(other.numDepartures - average.numDepartures, 2);
                stat.numArrivals += Math.pow(other.numArrivals - average.numArrivals, 2);
                stat.busyTime += Math.pow(other.busyTime - average.busyTime, 2);
                stat.responseTime += Math.pow(other.responseTime - average.responseTime, 2);
                stat.lastEventTime += Math.pow(other.lastEventTime - average.lastEventTime, 2);
                stat.averageQueueLength += Math.pow(other.averageQueueLength - average.averageQueueLength, 2);
            }
        }

        varTime /= runs.length - 1;
        varElapsed /= runs.length - 1;
        for (var stat : nodes.values()) {
            stat.numDepartures /= runs.length - 1;
            stat.numArrivals /= runs.length - 1;
            stat.busyTime /= runs.length - 1;
            stat.responseTime /= runs.length - 1;
            stat.lastEventTime /= runs.length - 1;
            stat.averageQueueLength /= runs.length - 1;
        }

        return new RunResult(runs[0].seed, varTime, varElapsed, nodes);
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
        public String getSummary() {
            var size = (int) Math.ceil(Math.log10(this.simulationTime));
            var iFormat = "%" + size + ".0f";
            var fFormat = "%" + (size + 4) + ".3f";
            var builder = new StringBuilder();

            for (var entry : this.nodes.entrySet()) {
                var stats = entry.getValue();
                var busy = stats.busyTime * 100 / stats.lastEventTime;
                var avgResp = stats.responseTime / stats.numDepartures;

                builder.append("===== " + entry.getKey() + " =====\n");
                builder.append(String.format("  Arrivals:  \t" + iFormat + "\n", stats.numArrivals));
                builder.append(String.format("  Departures:\t" + iFormat + "\n", stats.numDepartures));
                builder.append(String.format("  Max Queue: \t" + iFormat + "\n", stats.maxQueueLength));
                builder.append(String.format("  Avg Queue: \t" + fFormat + "\n", stats.averageQueueLength));
                builder.append(String.format("  Response:  \t" + fFormat + "\n", avgResp));
                builder.append(String.format("  Busy %%:   \t" + fFormat + "\n", busy));
                builder.append(String.format("  Last Event:\t" + fFormat + "\n", stats.lastEventTime));
            }
            return builder.toString();
        }

        /**
         * TODO
         */
        public String getSummaryAsTable() {
            var size = (int) Math.ceil(Math.log10(this.simulationTime));
            var iFormat = "%" + size + ".0f";
            var fFormat = "%" + (size + 4) + ".3f";

            String[] h = { "Node", "Arrivals", "Departures", "Max Queue", "Avg Queue", "Response", "Busy %",
                    "Last Event" };
            var table = new ConsoleTable(h);

            for (var entry : this.nodes.entrySet()) {
                var stats = entry.getValue();
                table.addRow(
                        entry.getKey(),
                        String.format(iFormat, stats.numArrivals),
                        String.format(iFormat, stats.numDepartures),
                        String.format(iFormat, stats.maxQueueLength),
                        String.format(fFormat, stats.averageQueueLength),
                        String.format(fFormat, stats.responseTime / stats.numDepartures),
                        String.format(fFormat, stats.busyTime * 100 / stats.lastEventTime),
                        String.format(fFormat, stats.lastEventTime));
            }
            return table.toString();
        }

        /**
         * TODO
         */
        public String getHeader() {
            var size = (int) Math.ceil(Math.log10(this.simulationTime));
            var format = "%" + (size + 4) + ".3f";
            var builder = new StringBuilder();
            builder.append("===== Net Stats =====\n");
            builder.append(String.format("Seed:       \t%d\n", this.seed));
            builder.append(String.format("Simulation: \t" + format + "\n", this.simulationTime));
            builder.append(String.format("Elapsed:    \t" + format + "ms\n", this.timeElapsedNano / 1e6));
            return builder.toString();
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

    /**
     * TODO
     */
    private static class ConsoleTable {
        private StringBuilder builder = new StringBuilder();
        private final int maxLen;
        private final String border;

        public ConsoleTable(String... header) {
            var max = 0;
            for (var name : header)
                max = Math.max(max, name.length());
            this.maxLen = max + 2;
            this.border = ("+" + "═".repeat(maxLen)).repeat(header.length) + "+\n";
            this.builder.append(border);
            this.addRow(header);
        }

        public void addRow(String... values) {
            for (var val : values) {
                var diff = maxLen - val.length();
                var first = (int) Math.ceil(diff / 2.0);
                builder.append('║');
                builder.append(" ".repeat(first));
                builder.append(val);
                builder.append(" ".repeat(diff - first));
            }

            builder.append("║\n");
            builder.append(border);
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }
}