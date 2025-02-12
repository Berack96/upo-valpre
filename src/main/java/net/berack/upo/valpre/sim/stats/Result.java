package net.berack.upo.valpre.sim.stats;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents the statistics of a network simulation.
 * It is used by the simulation to save the final results of the network and its
 * nodes, including the number of arrivals and departures, the maximum queue
 * length, the busy time, and the response time.
 */
public class Result implements Iterable<Entry<String, NodeStats>> {
    public final String[] nodes;
    public final NodeStats[] stats;
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
    private Result(long seed, double time, double elapsed, String[] nodes, NodeStats[] stats) {
        this.seed = seed;
        this.simulationTime = time;
        this.timeElapsedMS = elapsed;
        this.nodes = nodes;
        this.stats = stats;
    }

    public NodeStats getStat(String node) {
        for (var i = 0; i < this.nodes.length; i++)
            if (this.nodes[i].equals(node))
                return this.stats[i];
        throw new IllegalArgumentException("Node not found");
    }

    @Override
    public String toString() {
        return buildPrintable(this.seed, this.simulationTime, this.timeElapsedMS, this.nodes, this.stats);
    }

    @Override
    public Iterator<Entry<String, NodeStats>> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return this.index < Result.this.nodes.length;
            }

            @Override
            public Entry<String, NodeStats> next() {
                var node = Result.this.nodes[this.index];
                var stat = Result.this.stats[this.index];
                this.index++;
                return Map.entry(node, stat);
            }
        };
    }

    /**
     * Create a string representation of the result. It includes the seed, the final
     * time of the simulation, the real time elapsed while running the simulation in
     * ms, and the stats of each node.
     * 
     * @param seed    the initial seed used by the simulation
     * @param simTime the final time of the simulation
     * @param timeMS  the real time elapsed while running the simulation in ms
     * @param nodes   the names of the nodes
     * @param stats   the stats of each node
     * @return a string representation of the result
     */
    private static String buildPrintable(long seed, double simTime, double timeMS, String[] nodes, NodeStats[] stats) {
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

        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            var stat = stats[i];
            table.addRow(
                    node,
                    iFormat.formatted(stat.numDepartures),
                    fFormat.formatted(stat.avgQueueLength),
                    fFormat.formatted(stat.avgWaitTime),
                    fFormat.formatted(stat.avgResponse),
                    fFormat.formatted(stat.throughput),
                    fFormat.formatted(stat.utilization * 100),
                    fFormat.formatted(stat.unavailable * 100),
                    fFormat.formatted(stat.lastEventTime));
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
        private String[] nodes;
        private NodeStats.Summary[] stats;
        private List<Result> runs = new ArrayList<>();

        /**
         * Creates a new summary object for the given seed.
         * 
         * @param seed the initial seed used by the simulation
         */
        public Summary(long seed, String[] nodes) {
            this.seed = seed;
            this.setup(nodes);
        }

        /**
         * Creates a new summary object for the given results.
         * 
         * @param results the results to summarize
         */
        public Summary(List<Result> results) {
            var first = results.get(0);
            this.seed = first.seed;
            this.setup(first.nodes);
            for (var result : results)
                this.add(result);
        }

        /**
         * Sets up the summary with the nodes. It initializes the statistics of the
         * nodes to 0. It is used by the constructors.
         */
        private void setup(String[] nodes) {
            this.nodes = nodes;
            this.stats = new NodeStats.Summary[this.nodes.length];
            for (var i = 0; i < this.nodes.length; i++)
                this.stats[i] = new NodeStats.Summary();
        }

        /**
         * Adds the result to the summary. It updates the average simulation time and
         * the average time elapsed. It also updates the statistics of the nodes.
         * 
         * @param result the result to add
         * @throws IllegalArgumentException if the result is null or the nodes do not
         *                                  match
         */
        public void add(Result result) {
            if (result == null)
                throw new IllegalArgumentException("Result cannot be null");
            if (result.nodes.length != this.nodes.length)
                throw new IllegalArgumentException("Nodes do not match");

            var n = this.runs.size() + 1;
            this.runs.add(result);
            this.avgSimulationTime += (result.simulationTime - this.avgSimulationTime) / n;
            this.avgTimeElapsedMS += (result.timeElapsedMS - this.avgTimeElapsedMS) / n;

            for (var i = 0; i < this.nodes.length; i++) {
                var stats = this.stats[i];
                var summary = result.stats[i];
                stats.update(summary);
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
        public List<String> getNodes() {
            return List.of(this.nodes);
        }

        /**
         * Gets the summary of the statistics of a node.
         * 
         * @param node the node to get the summary
         * @return the summary of the statistics of the node
         * @throws IllegalArgumentException if the node is not found
         */
        public NodeStats.Summary getSummaryOf(String node) {
            for (var i = 0; i < this.nodes.length; i++)
                if (this.nodes[i].equals(node))
                    return this.stats[i];
            throw new IllegalArgumentException("Node not found");
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
            var stats = new NodeStats[this.nodes.length];
            for (var i = 0; i < this.nodes.length; i++)
                stats[i] = this.stats[i].average;

            return buildPrintable(this.seed, this.avgSimulationTime, this.avgTimeElapsedMS, this.nodes, stats);
        }
    }

    /**
     * A builder class to create a new result object. It allows to set the seed, the
     * simulation time, the time elapsed, the nodes, and the stats.
     */
    public static class Builder {
        public long seed;
        public double simulationTime;
        public double timeElapsedMS;
        private List<String> nodes = new ArrayList<>();
        private List<NodeStats> stats = new ArrayList<>();

        /**
         * Resets the builder to its initial state.
         */
        public Builder reset() {
            this.seed = 0;
            this.simulationTime = 0.0d;
            this.timeElapsedMS = 0.0d;
            this.nodes.clear();
            this.stats.clear();
            return this;
        }

        /**
         * Sets the seed of the result.
         * 
         * @param seed the seed to set
         * @return the builder
         */
        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        /**
         * Sets the simulation time and the time elapsed of the result.
         * 
         * @param simulationTime the simulation time to set
         * @param timeElapsedMS  the time elapsed to set
         * @return the builder
         */
        public Builder times(double simulationTime, double timeElapsedMS) {
            this.simulationTime = simulationTime;
            this.timeElapsedMS = timeElapsedMS;
            return this;
        }

        /**
         * Adds a node and its stats to the result.
         * 
         * @param node the node to add
         * @param stat the stats of the node
         * @return the builder
         */
        public Builder addNode(String node, NodeStats stat) {
            this.nodes.add(node);
            this.stats.add(stat);
            return this;
        }

        public Result build() {
            var nodes = this.nodes.toArray(String[]::new);
            var stats = this.stats.toArray(NodeStats[]::new);
            return new Result(this.seed, this.simulationTime, this.timeElapsedMS, nodes, stats);
        }
    }
}
