package net.berack.upo.valpre.sim.stats;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * This class represent the result of multiple runs of simulation.
 */
public class ResultMultiple {
    public final Result[] runs;
    public final Result average;
    public final Result variance;
    public final Result error95;

    /**
     * This has all the result and give some statistics about the runs.
     * The object created has the average, the variance, and the error95.
     * The runs must be an array of at least 2 run result otherwise an exception is
     * thrown.
     * 
     * @param runs an array of run result
     * @throws IllegalArgumentException if the runs is null or if has a len <= 1
     */
    public ResultMultiple(Result... runs) {
        if (runs == null || runs.length <= 1)
            throw new IllegalArgumentException("Sample size must be > 1");

        this.runs = runs;
        this.average = ResultMultiple.calcAvg(runs);
        this.variance = ResultMultiple.calcStdDev(this.average, runs);
        this.error95 = calcError(this.average, this.variance, runs.length, 0.95);
    }

    /**
     * Save all the runs to a csv file.
     * 
     * @param filename the name of the file
     * @throws IOException if anything happens wile wriiting to the file
     */
    public void saveCSV(String filename) throws IOException {
        if (!filename.endsWith(".csv"))
            filename = filename + ".csv";

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
     * This method calculate the average of the runs result.
     * The average is calculated for each node.
     * 
     * @param runs the run to calculate
     * @return the average of the runs
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
     * This method calculate the standard deviation of the runs result.
     * The standard deviation is calculated for each node.
     * 
     * @param avg  the average of the runs. {@link #calcAvg(Result...)}
     * @param runs the run to calculate
     * @return the standard deviation of the runs
     */
    public static Result calcStdDev(Result avg, Result... runs) {
        var time = 0.0d;
        var elapsed = 0.0d;
        var nodes = new HashMap<String, Statistics>();

        for (var run : runs) {
            time += Math.pow(run.simulationTime - avg.simulationTime, 2);
            elapsed += Math.pow(run.timeElapsedMS - avg.simulationTime, 2);

            for (var entry : run.nodes.entrySet()) {
                var stat = nodes.computeIfAbsent(entry.getKey(), _ -> new Statistics());
                var average = avg.nodes.get(entry.getKey());
                var other = entry.getValue();
                var temp = new Statistics();
                Statistics.apply(temp, other, average, (o, a) -> Math.pow(o - a, 2));
                stat.merge(temp, (var1, var2) -> var1 + var2);
            }
        }

        time = Math.sqrt(time / runs.length - 1);
        elapsed = Math.sqrt(elapsed / runs.length - 1);
        for (var stat : nodes.values())
            stat.apply(val -> Math.sqrt(val / (runs.length - 1)));

        return new Result(runs[0].seed, time, elapsed, nodes);
    }

    /**
     * Calculates the error at the selected alpha level.
     * This method computes the error margin for the provided average and standard
     * deviation values,
     * considering the sample size and the confidence level (alpha).
     * The result is adjusted using a t-distribution to account for the variability
     * in smaller sample sizes.
     *
     * @param avg        The average of the results, typically computed using
     *                   {@link #calcAvg(Result...)}.
     * @param stdDev     The standard deviation of the results, typically computed
     *                   using {@link #calcVar(Result, Result...)}.
     * @param sampleSize The number of runs or samples used.
     * @param alpha      The significance level (probability) used for the
     *                   t-distribution. A value of 0.95 for a 95% confidence level.
     * @return The calculated error.
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