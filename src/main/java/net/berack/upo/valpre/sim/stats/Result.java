package net.berack.upo.valpre.sim.stats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the statistics of a network simulation.
 * It is used by the simulation to save the final results of the network and its
 * nodes, including the number of arrivals and departures, the maximum queue
 * length, the busy time, and the response time.
 */
public class Result {
    public final Map<String, NodeStats> nodes;
    public final long seed;
    public final double simulationTime;
    public final double timeElapsedMS;

    /**
     * Creates a new result object for the given parameters obtained by the
     * simulation.
     * 
     * @param seed    the initial seed used by the simulation
     * @param time    the final time of the simulation
     * @param elapsed the real time elapsed while running the simulation in ms
     * @param nodes   all the stats collected by the simulation saved per node
     */
    public Result(long seed, double time, double elapsed, Map<String, NodeStats> nodes) {
        this.seed = seed;
        this.simulationTime = time;
        this.timeElapsedMS = elapsed;
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return buildPrintable(this.seed, this.simulationTime, this.timeElapsedMS, this.nodes);
    }

    private static String buildPrintable(long seed, double simTime, double timeMS, Map<String, NodeStats> nodes) {
        var size = (int) Math.ceil(Math.max(Math.log10(simTime), 1));
        var iFormat = "%" + size + ".0f";
        var fFormat = "%" + (size + 4) + ".3f";

        var builder = new StringBuilder();
        builder.append("===== Net Stats =====\n");
        builder.append(String.format("Seed:       \t%d\n", seed));
        builder.append(String.format("Simulation: \t" + fFormat + "\n", simTime));
        builder.append(String.format("Elapsed:    \t" + fFormat + "ms\n", timeMS));

        var table = new ConsoleTable("Node", "Departures", "Avg Queue", "Avg Wait", "Avg Response", "Throughput",
                "Utilization %", "Unavailable %", "Last Event");

        for (var entry : nodes.entrySet()) {
            var stats = entry.getValue();
            table.addRow(
                    entry.getKey(),
                    iFormat.formatted(stats.numDepartures),
                    fFormat.formatted(stats.avgQueueLength),
                    fFormat.formatted(stats.avgWaitTime),
                    fFormat.formatted(stats.avgResponse),
                    fFormat.formatted(stats.throughput),
                    fFormat.formatted(stats.utilization * 100),
                    fFormat.formatted(stats.unavailable * 100),
                    fFormat.formatted(stats.lastEventTime));
        }

        builder.append(table);
        return builder.toString();
    }

    /**
     * Represents the summary of the statistics of a network simulation.
     * It is used to save the final results of the network and its nodes, including
     * the number of arrivals and departures, the maximum queue length, the busy
     * time, and the response time.
     */
    public static class Summary {
        public final long seed;
        private double avgSimulationTime = 0.0d;
        private double avgTimeElapsedMS = 0.0d;
        private Map<String, NodeStats.Summary> stats = new HashMap<>();
        private List<Result> runs = new ArrayList<>();

        /**
         * Creates a new summary object for the given seed.
         * 
         * @param seed the initial seed used by the simulation
         */
        public Summary(long seed) {
            this.seed = seed;
        }

        /**
         * Creates a new summary object for the given results.
         * 
         * @param results the results to summarize
         */
        public Summary(List<Result> results) {
            this(results.get(0).seed);
            for (var result : results)
                this.add(result);
        }

        /**
         * Adds the result to the summary. It updates the average simulation time and
         * the average time elapsed. It also updates the statistics of the nodes.
         * 
         * @param result the result to add
         */
        public void add(Result result) {
            if (result == null)
                throw new IllegalArgumentException("Result cannot be null");

            var n = this.runs.size() + 1;
            this.runs.add(result);
            this.avgSimulationTime += (result.simulationTime - this.avgSimulationTime) / n;
            this.avgTimeElapsedMS += (result.timeElapsedMS - this.avgTimeElapsedMS) / n;

            for (var entry : result.nodes.entrySet()) {
                var node = entry.getKey();
                var stats = entry.getValue();
                var summary = this.stats.computeIfAbsent(node, _ -> new NodeStats.Summary());
                summary.update(stats);
            }
        }

        /**
         * Gets the average simulation time of the summary.
         * 
         * @return the average simulation time
         */
        public double getAvgSimulationTime() {
            return this.avgSimulationTime;
        }

        /**
         * Gets the average time elapsed of the summary.
         * 
         * @return the average time elapsed
         */
        public double getAvgTimeElapsedMS() {
            return this.avgTimeElapsedMS;
        }

        /**
         * Gets the nodes of the summary.
         * 
         * @return the nodes of the summary
         */
        public Collection<String> getNodes() {
            return this.stats.keySet();
        }

        /**
         * Gets the summary of the statistics of a node.
         * 
         * @param node the node to get the summary
         * @return the summary of the statistics of the node
         * @throws IllegalArgumentException if the node is not found
         */
        public NodeStats.Summary getSummaryOf(String node) {
            var stat = this.stats.get(node);
            if (stat == null)
                throw new IllegalArgumentException("Node not found");
            return stat;
        }

        /**
         * Gets all the runs of the summary.
         * 
         * @return the runs of the summary
         */
        public List<Result> getRuns() {
            return List.copyOf(this.runs);
        }

        @Override
        public String toString() {
            var stats = new HashMap<String, NodeStats>();
            for (var entry : this.stats.entrySet())
                stats.put(entry.getKey(), entry.getValue().average);

            return buildPrintable(this.seed, this.avgSimulationTime, this.avgTimeElapsedMS, stats);
        }
    }
}
