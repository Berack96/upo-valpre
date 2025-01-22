package net.berack.upo.valpre.sim.stats;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * TODO
 */
public class Statistics {
    public double numArrivals = 0.0d;
    public double numDepartures = 0.0d;
    public double maxQueueLength = 0.0d;
    public double avgQueueLength = 0.0d;
    public double busyTime = 0.0d;
    public double responseTime = 0.0d;
    public double lastEventTime = 0.0d;

    // derived stats, you can calculate them even at the end
    public double avgResponse = 0.0d;
    public double troughput = 0.0d;
    public double utilization = 0.0d;

    /**
     * TODO
     * 
     * @param time
     * @param newQueueSize
     * @param updateBusy
     */
    public void updateArrival(double time, double newQueueSize, boolean updateBusy) {
        var total = this.avgQueueLength * this.numArrivals;

        this.numArrivals++;
        this.avgQueueLength = (total + newQueueSize) / this.numArrivals;
        this.maxQueueLength = Math.max(this.maxQueueLength, newQueueSize);
        if (updateBusy)
            this.busyTime += time - this.lastEventTime;

        this.lastEventTime = time;
    }

    /**
     * TODO
     * 
     * @param time
     * @param response
     */
    public void updateDeparture(double time, double response) {
        this.numDepartures++;
        this.responseTime += response;
        this.busyTime += time - this.lastEventTime;
        this.lastEventTime = time;

        this.avgResponse = this.responseTime / this.numDepartures;
        this.troughput = this.numDepartures / this.lastEventTime;
        this.utilization = this.busyTime / this.lastEventTime;
    }

    /**
     * Resets the statistics to 0.
     */
    public void reset() {
        this.apply(_ -> 0.0d);
    }

    /**
     * Apply a function to ALL the stats in this class.
     * The only stats that are not updated with this function are the one that
     * starts with max, min (since they are special)
     * The input of the function is the current value of the stat.
     * 
     * @param func a function to apply
     */
    public void apply(Function<Double, Double> func) {
        Statistics.apply(this, this, this, (val1, _) -> func.apply(val1));
    }

    /**
     * A function used to merge tree stats.
     * The only stats that are not updated with this function are the one that
     * starts with max, min (since they are special)
     * 
     * @param other
     * @param func
     */
    public void merge(Statistics other, BiFunction<Double, Double, Double> func) {
        Statistics.apply(this, this, other, func);
    }

    /**
     * TODO
     * 
     * @param save
     * @param val1
     * @param val2
     * @param func
     */
    public static void apply(Statistics save, Statistics val1, Statistics val2,
            BiFunction<Double, Double, Double> func) {
        save.numArrivals = func.apply(val1.numArrivals, val2.numArrivals);
        save.numDepartures = func.apply(val1.numDepartures, val2.numDepartures);
        save.avgQueueLength = func.apply(val1.avgQueueLength, val2.avgQueueLength);
        save.busyTime = func.apply(val1.busyTime, val2.busyTime);
        save.responseTime = func.apply(val1.responseTime, val2.responseTime);
        save.lastEventTime = func.apply(val1.lastEventTime, val2.lastEventTime);
        // derived stats
        save.troughput = func.apply(val1.troughput, val2.troughput);
        save.utilization = func.apply(val1.utilization, val2.utilization);
        save.avgResponse = func.apply(val1.avgResponse, val2.avgResponse);
    }
}