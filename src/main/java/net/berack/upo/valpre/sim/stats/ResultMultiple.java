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
        this.average = ResultMultiple.calcAvg(runs);
        this.variance = ResultMultiple.calcVar(this.average, runs);
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
            varElapsed += Math.pow(run.timeElapsedNano - avg.simulationTime, 2);

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
}