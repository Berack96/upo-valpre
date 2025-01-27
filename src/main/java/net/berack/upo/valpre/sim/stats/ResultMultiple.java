package net.berack.upo.valpre.sim.stats;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * TODO
 */
public class ResultMultiple {
    public final Result[] runs;
    public final Result average;
    public final Result variance;
    public final Result error95;

    /**
     * TODO
     * 
     * @param runs
     */
    public ResultMultiple(Result... runs) {
        if (runs == null || runs.length <= 1)
            throw new IllegalArgumentException("Sample size must be > 1");

        this.runs = runs;
        this.average = ResultMultiple.calcAvg(runs);
        this.variance = ResultMultiple.calcVar(this.average, runs);
        this.error95 = calcError(this.average, this.variance, runs.length, 0.95);
    }

    /**
     * TODO
     * 
     * @param filename
     * @throws IOException
     */
    public void saveCSV(String filename) throws IOException {
        try (var file = new FileWriter(filename)) {
            var first = true;
            var builder = new StringBuilder();
            for (var run : this.runs) {
                builder.append(run.getSummaryCSV(first));
                first = false;
            }
            file.write(builder.toString());
        }
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
            avgElapsed += run.timeElapsedMS;

            for (var entry : run.nodes.entrySet()) {
                var stats = nodes.computeIfAbsent(entry.getKey(), _ -> new Statistics());
                stats.merge(entry.getValue(), (val1, val2) -> val1 + val2);
            }
        }

        avgTime /= runs.length;
        avgElapsed /= runs.length;
        for (var stat : nodes.values())
            stat.apply(val -> val / runs.length);
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
            varElapsed += Math.pow(run.timeElapsedMS - avg.simulationTime, 2);

            for (var entry : run.nodes.entrySet()) {
                var stat = nodes.computeIfAbsent(entry.getKey(), _ -> new Statistics());
                var average = avg.nodes.get(entry.getKey());
                var other = entry.getValue();
                var temp = new Statistics();
                Statistics.apply(temp, other, average, (o, a) -> Math.pow(o - a, 2));
                stat.merge(temp, (var1, var2) -> var1 + var2);
            }
        }

        varTime /= runs.length - 1;
        varElapsed /= runs.length - 1;
        for (var stat : nodes.values())
            stat.apply(val -> val / (runs.length - 1));

        return new Result(runs[0].seed, varTime, varElapsed, nodes);
    }

    /**
     * TODO
     * 
     * @param avg
     * @param stdDev
     * @param sampleSize
     * @param alpha
     * @return
     */
    public static Result calcError(Result avg, Result stdDev, int sampleSize, double alpha) {
        // Getting the correct values for the percentile
        var distr = new org.apache.commons.math3.distribution.TDistribution(sampleSize - 1);
        var percentile = distr.inverseCumulativeProbability(alpha);

        // Calculating the error
        var sqrtSample = Math.sqrt(sampleSize);
        var error = new Result(avg.seed,
                percentile * (stdDev.simulationTime / sqrtSample),
                percentile * (stdDev.timeElapsedMS / sqrtSample),
                new HashMap<>());
        for (var entry : stdDev.nodes.entrySet()) {
            var stat = new Statistics();
            stat.merge(entry.getValue(), (_, val) -> percentile * (val / sqrtSample));
            error.nodes.put(entry.getKey(), stat);
        }
        return error;
    }
}