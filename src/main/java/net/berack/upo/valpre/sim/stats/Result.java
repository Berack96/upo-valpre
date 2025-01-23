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
    public final long timeElapsedNano;

    /**
     * Creates a new result object for the given parameters obtained by the
     * simulation.
     * 
     * @param seed    the initial seed used by the simulation
     * @param time    the final time of the simulation
     * @param elapsed the real time elapsed while running the simulation in ns
     * @param nodes   all the stats collected by the simulation saved per node
     */
    public Result(long seed, double time, long elapsed, Map<String, Statistics> nodes) {
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

        String[] h = { "Node", "Departures", "Avg Queue", "Avg Response", "Throughput", "Utilization %", "Last Event" };
        var table = new ConsoleTable(h);

        for (var entry : this.nodes.entrySet()) {
            var stats = entry.getValue();
            table.addRow(
                    entry.getKey(),
                    String.format(iFormat, stats.numDepartures),
                    String.format(fFormat, stats.avgQueueLength),
                    String.format(fFormat, stats.avgResponse),
                    String.format(fFormat, stats.troughput),
                    String.format(fFormat, stats.utilization * 100),
                    String.format(fFormat, stats.lastEventTime));
        }
        return table.toString();
    }

    /**
     * Get the global information of the simulation. In particular this method build
     * a string that contains the seed and the time elapsed in the simulation and in
     * real time
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
