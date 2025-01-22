package net.berack.upo.valpre.sim.stats;

import java.util.Map;

/**
 * Represents the statistics of a network simulation.
 * It is used by the simulation to track the behavior of the network and its
 * nodes, including the number of arrivals and departures, the maximum queue
 * length, the busy time, and the response time.
 */
public class Result {
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
