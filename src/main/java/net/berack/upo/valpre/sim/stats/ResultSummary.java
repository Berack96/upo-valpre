package net.berack.upo.valpre.sim.stats;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represent the summary of the result of multiple runs of
 * simulation. It has the average of the simulation time, the average of the
 * elapsed time, and the average of the statistics of the nodes.
 */
public class ResultSummary {

    public final long seed;
    public final double simulationTime;
    public final double timeElapsedMS;
    public final Result[] runs;

    private final Map<String, Map<String, StatisticsSummary>> stats;

    /**
     * This has all the result and give some statistics about the runs.
     * The object created has the average, the variance, and the error95.
     * The runs must be an array of at least 2 run result otherwise an exception is
     * thrown.
     * 
     * @param runs an array of run result
     * @throws IllegalArgumentException if the runs is null or if has a len <= 1
     */
    public ResultSummary(Result[] runs) {
        if (runs == null || runs.length <= 1)
            throw new IllegalArgumentException("Sample size must be > 1");

        // Get the seed, simulation time, and time elapsed
        var avgTime = 0.0d;
        var avgElapsed = 0L;
        for (var run : runs) {
            avgTime += run.simulationTime;
            avgElapsed += run.timeElapsedMS;
        }
        this.runs = runs;
        this.seed = runs[0].seed;
        this.simulationTime = avgTime / runs.length;
        this.timeElapsedMS = avgElapsed / runs.length;

        // Get the statistics of the nodes
        var nodeStats = new HashMap<String, Statistics[]>();
        for (var i = 0; i < runs.length; i++) {
            for (var entry : runs[i].nodes.entrySet()) {
                var node = entry.getKey();
                var stats = nodeStats.computeIfAbsent(node, _ -> new Statistics[runs.length]);
                stats[i] = entry.getValue();
            }
        }

        // Get the summary of the statistics of the nodes
        this.stats = new HashMap<>();
        for (var entry : nodeStats.entrySet()) {
            var node = entry.getKey();
            var summary = StatisticsSummary.getSummary(entry.getValue());
            this.stats.put(node, summary);
        }
    }

    /**
     * Get the summary of the statistics of a node.
     * 
     * @param node the node to get the summary
     * @param stat the statistic to get the summary
     * @return the summary of the statistics of the node
     */
    public StatisticsSummary getSummaryOf(String node, String stat) {
        return this.stats.get(node).get(stat);
    }

    /**
     * Get all the summary of the statistics of a node.
     * 
     * @param node the node to get the summary
     * @return the summary of the statistics of the node
     */
    public Map<String, StatisticsSummary> getSummaryOf(String node) {
        return this.stats.get(node);
    }

    /**
     * Get the nodes of the simulation.
     * 
     * @return the nodes of the simulation
     */
    public Collection<String> getNodes() {
        return this.stats.keySet();
    }

    @Override
    public String toString() {
        var size = (int) Math.ceil(Math.max(Math.log10(this.simulationTime), 1));
        var iFormat = "%" + size + ".0f";
        var fFormat = "%" + (size + 4) + ".3f";

        var builder = new StringBuilder();
        builder.append("===== Net Stats =====\n");
        builder.append(String.format("Seed:       \t%d\n", this.seed));
        builder.append(String.format("Simulation: \t" + fFormat + "\n", this.simulationTime));
        builder.append(String.format("Elapsed:    \t" + fFormat + "ms\n", this.timeElapsedMS / 1e6));
        // return builder.toString();

        var table = new ConsoleTable("Node", "Departures", "Avg Queue", "Avg Wait", "Avg Response", "Throughput",
                "Utilization %", "Unavailable %", "Last Event");

        for (var entry : this.stats.entrySet()) {
            var stats = entry.getValue();
            table.addRow(
                    entry.getKey(),
                    iFormat.formatted(stats.get("numDepartures").average),
                    fFormat.formatted(stats.get("avgQueueLength").average),
                    fFormat.formatted(stats.get("avgWaitTime").average),
                    fFormat.formatted(stats.get("avgResponse").average),
                    fFormat.formatted(stats.get("troughput").average),
                    fFormat.formatted(stats.get("utilization").average * 100),
                    fFormat.formatted(stats.get("unavailable").average * 100),
                    fFormat.formatted(stats.get("lastEventTime").average));
        }

        builder.append(table);
        return builder.toString();
    }
}
