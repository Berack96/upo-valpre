package net.berack.upo.valpre.sim.stats;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * TODO
 */
public class Statistics {
    public double numArrivals = 0;
    public double numDepartures = 0;
    public double maxQueueLength = 0;
    public double averageQueueLength = 0.0d;
    public double busyTime = 0.0d;
    public double responseTime = 0.0d;
    public double lastEventTime = 0.0d;

    /**
     * Resets the statistics to their initial values.
     */
    public void reset() {
        this.applyToAll(_ -> 0.0d);
    }

    /**
     * Apply a function to ALL the stats in this class.
     * The input of the function is the current value of the stat.
     * 
     * @param func a function to apply
     */
    public void applyToAll(Function<Double, Double> func) {
        this.numArrivals = func.apply(this.numArrivals);
        this.numDepartures = func.apply(this.numDepartures);
        this.maxQueueLength = func.apply(this.maxQueueLength);
        this.averageQueueLength = func.apply(this.averageQueueLength);
        this.busyTime = func.apply(this.busyTime);
        this.responseTime = func.apply(this.responseTime);
        this.lastEventTime = func.apply(this.lastEventTime);
    }

    /**
     * A function used to merge two stats.
     * @param other
     * @param func
     */
    public void mergeWith(Statistics other, BiFunction<Double, Double, Double> func) {
        this.numArrivals = func.apply(other.numArrivals, this.numArrivals);
        this.numDepartures = func.apply(other.numDepartures, this.numDepartures);
        this.maxQueueLength = func.apply(other.maxQueueLength, this.maxQueueLength);
        this.averageQueueLength = func.apply(other.averageQueueLength, this.averageQueueLength);
        this.busyTime = func.apply(other.busyTime, this.busyTime);
        this.responseTime = func.apply(other.responseTime, this.responseTime);
        this.lastEventTime = func.apply(other.lastEventTime, this.lastEventTime);
    }
}