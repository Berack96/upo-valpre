package net.berack.upo.valpre.sim.stats;

import java.util.Map;

/**
 * Represents the statistics of a network simulation.
 * It is used by the simulation to save the final results of the network and its
 * nodes, including the number of arrivals and departures, the maximum queue
 * length, the busy time, and the response time.
 */
public class Result {
    public final Map<String, Statistics> nodes;
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
    public Result(long seed, double time, double elapsed, Map<String, Statistics> nodes) {
        this.seed = seed;
        this.simulationTime = time;
        this.timeElapsedMS = elapsed;
        this.nodes = nodes;
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

        for (var entry : this.nodes.entrySet()) {
            var stats = entry.getValue();
            table.addRow(
                    entry.getKey(),
                    iFormat.formatted(stats.numDepartures),
                    fFormat.formatted(stats.avgQueueLength),
                    fFormat.formatted(stats.avgWaitTime),
                    fFormat.formatted(stats.avgResponse),
                    fFormat.formatted(stats.troughput),
                    fFormat.formatted(stats.utilization * 100),
                    fFormat.formatted(stats.unavailable * 100),
                    fFormat.formatted(stats.lastEventTime));
        }

        builder.append(table);
        return builder.toString();
    }
}
