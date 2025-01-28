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
    private int size;
    private String iFormat;
    private String fFormat;

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
        this.size = (int) Math.ceil(Math.log10(this.simulationTime));
        this.iFormat = "%" + this.size + ".0f";
        this.fFormat = "%" + (this.size + 4) + ".3f";
    }

    /**
     * Get the global information of the simulation. In particular this method build
     * a string that contains the seed and the time elapsed in the simulation and in
     * real time
     */
    public String getHeader() {
        var builder = new StringBuilder();
        builder.append("===== Net Stats =====\n");
        builder.append(String.format("Seed:       \t%d\n", this.seed));
        builder.append(String.format("Simulation: \t" + fFormat + "\n", this.simulationTime));
        builder.append(String.format("Elapsed:    \t" + fFormat + "ms\n", this.timeElapsedMS / 1e6));
        return builder.toString();
    }

    /**
     * Print a summary of the statistics to the console.
     * The summary includes the seed, the simulation time, the elapsed time, and
     * the statistics for each node in the network.
     */
    public String getSummary() {
        String[] h = { "Node", "Departures", "Avg Queue", "Avg Wait", "Avg Response", "Throughput", "Utilization %",
                "Last Event" };
        var table = new ConsoleTable(h);

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
                    fFormat.formatted(stats.lastEventTime));
        }
        return table.toString();
    }

    /**
     * TODO
     * 
     * @param tableHeader
     * @return
     */
    public String getSummaryCSV(boolean tableHeader) {
        var builder = new StringBuilder();

        if (tableHeader)
            builder.append(
                    "Seed,Node,Arrivals,Departures,MaxQueue,AvgQueue,AvgWait,AvgResponse,BusyTime,WaitTime,ResponseTime,LastEventTime,Throughput,Utilization\n");
        for (var entry : this.nodes.entrySet()) {
            var stats = entry.getValue();
            builder.append(this.seed);
            builder.append(',');
            builder.append(entry.getKey().replace(',', ';').replace('"', '\''));
            builder.append(',');
            builder.append(stats.numArrivals);
            builder.append(',');
            builder.append(stats.numDepartures);
            builder.append(',');
            builder.append(stats.maxQueueLength);
            builder.append(',');
            builder.append(stats.avgQueueLength);
            builder.append(',');
            builder.append(stats.avgWaitTime);
            builder.append(',');
            builder.append(stats.avgResponse);
            builder.append(',');
            builder.append(stats.busyTime);
            builder.append(',');
            builder.append(stats.waitTime);
            builder.append(',');
            builder.append(stats.responseTime);
            builder.append(',');
            builder.append(stats.lastEventTime);
            builder.append(',');
            builder.append(stats.troughput);
            builder.append(',');
            builder.append(stats.utilization);
            builder.append('\n');
        }
        return builder.toString();
    }
}
