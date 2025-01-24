package net.berack.upo.valpre.sim.stats;

import java.util.HashMap;

/**
 * TODO
 */
public class ResultMultiple {
    public final Result[] runs;
    public final Result average;
    public final Result variance;
    public final Result lowerBound;
    public final Result upperBound;

    /**
     * TODO
     * 
     * @param runs
     */
    public ResultMultiple(Result... runs) {
        this.runs = runs;
        this.average = ResultMultiple.calcAvg(runs);
        this.variance = ResultMultiple.calcVar(this.average, runs);

        var temp = calcInterval(this.average, this.variance, runs.length, 0.95);
        this.lowerBound = temp[0];
        this.upperBound = temp[1];
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
    public static Result[] calcInterval(Result avg, Result stdDev, int sampleSize, double alpha) {
        if (sampleSize <= 1)
            throw new IllegalArgumentException("Il numero di campioni deve essere maggiore di 1.");

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

        // Calculating the lower and the upper bound
        var lowerBound = new Result(avg.seed,
                avg.simulationTime - error.simulationTime,
                avg.timeElapsedMS - error.timeElapsedMS,
                new HashMap<>());
        var upperBound = new Result(avg.seed,
                avg.simulationTime + error.simulationTime,
                avg.timeElapsedMS + error.timeElapsedMS,
                new HashMap<>());
        error.nodes.entrySet().forEach(entry -> {
            var key = entry.getKey();
            var errStat = entry.getValue();

            var avgStat = avg.nodes.get(key);
            var lower = new Statistics();
            var upper = new Statistics();

            Statistics.apply(lower, avgStat, errStat, (a, e) -> a - e);
            Statistics.apply(upper, avgStat, errStat, (a, e) -> a + e);

            lowerBound.nodes.put(key, lower);
            upperBound.nodes.put(key, lower);
        });

        return new Result[] { lowerBound, upperBound };
    }
}