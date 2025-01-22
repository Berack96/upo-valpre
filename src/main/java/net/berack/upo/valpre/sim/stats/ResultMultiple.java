package net.berack.upo.valpre.sim.stats;

import java.util.HashMap;

/**
 * TODO
 */
public class ResultMultiple {
    public final Result[] runs;
    public final Result average;
    public final Result variance;

    /**
     * TODO
     * 
     * @param runs
     */
    public ResultMultiple(Result... runs) {
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
    public static Result calcAvg(Result... runs) {
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
        return new Result(runs[0].seed, avgTime, avgElapsed, nodes);
    }

    /**
     * TODO
     * 
     * @param avg
     * @param runs
     * @return
     */
    public static Result calcVar(Result avg, Result... runs) {
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

        return new Result(runs[0].seed, varTime, varElapsed, nodes);
    }
}