package net.berack.upo.valpre.sim.stats;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This class keeps track of various statistical metrics related to the net
 * performance, such as the number of arrivals and departures, queue lengths,
 * wait times, response times, server utilization, and unavailability. These
 * statistics are updated during simulation events, such as arrivals and
 * departures, and can be used to analyze the net's behavior and performance.
 */
public class Statistics {
    public double numArrivals = 0.0d;
    public double numDepartures = 0.0d;
    public double maxQueueLength = 0.0d;
    public double avgQueueLength = 0.0d;
    public double unavailableTime = 0.0d;
    public double busyTime = 0.0d;
    public double waitTime = 0.0d;
    public double responseTime = 0.0d;
    public double lastEventTime = 0.0d;

    // derived stats, you can calculate them even at the end
    public double avgWaitTime = 0.0d;
    public double avgResponse = 0.0d;
    public double troughput = 0.0d;
    public double utilization = 0.0d;
    public double unavailable = 0.0d;

    /**
     * Updates statistics when a new arrival occurs. It updates the number of
     * arrivals, the average queue length, and the maximum queue length.
     *
     * @param time         The current time of the arrival event.
     * @param newQueueSize The size of the queue after the arrival.
     */
    public void updateArrival(double time, double newQueueSize) {
        var total = this.avgQueueLength * this.numArrivals;

        this.numArrivals++;
        this.avgQueueLength = (total + newQueueSize) / this.numArrivals;
        this.maxQueueLength = Math.max(this.maxQueueLength, newQueueSize);
    }

    /**
     * Updates statistics when a departure occurs. It increments the number of
     * departures and calculates the total response time.
     *
     * @param time     The current time of the departure event.
     * @param response The time at which the response was generated for this
     *                 departure.
     */
    public void updateDeparture(double time, double response) {
        this.numDepartures++;
        this.responseTime += time - response;
    }

    /**
     * Updates statistics related to server busy time, unavailable time, and derived
     * stats. It also calculates the average wait time, response time, throughput,
     * utilization, and unavailability.
     *
     * @param time              The current time when the event occurs.
     * @param serverBusy        The number of servers currently busy.
     * @param serverUnavailable The number of servers currently unavailable.
     * @param maxServers        The total number of servers available.
     */
    public void updateTimes(double time, int serverBusy, int serverUnavailable, int maxServers) {
        if (serverBusy > 0)
            this.busyTime += time - this.lastEventTime;
        else if (serverUnavailable == maxServers)
            this.unavailableTime += time - this.lastEventTime;

        this.waitTime = this.responseTime - this.busyTime;
        this.avgWaitTime = this.waitTime / this.numDepartures;
        this.avgResponse = this.responseTime / this.numDepartures;
        this.troughput = this.numDepartures / time;
        this.utilization = this.busyTime / time;
        this.unavailable = this.unavailableTime / time;

        this.lastEventTime = time;
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
     * Applies a binary function to merge two sets of statistics into a third one.
     * This method combines the statistics from two `Statistics` objects (`val1` and
     * `val2`) and stores the result in the `save` object. The provided function is
     * applied to each pair of corresponding statistics from `val1` and `val2` to
     * compute the merged value. This is useful for merging or combining statistics
     * from different sources (e.g., different simulation runs), allowing the
     * creation of aggregated statistics.
     * 
     * @param save The `Statistics` object where the merged results will be stored.
     * @param val1 The first `Statistics` object to merge.
     * @param val2 The second `Statistics` object to merge.
     * @param func The binary function that defines how to merge each pair of values
     *             from `val1` and `val2`. It takes two `Double` values (from `val1`
     *             and `val2`) and returns a new `Double` value.
     */
    public static void apply(Statistics save, Statistics val1, Statistics val2,
            BiFunction<Double, Double, Double> func) {
        save.numArrivals = func.apply(val1.numArrivals, val2.numArrivals);
        save.numDepartures = func.apply(val1.numDepartures, val2.numDepartures);
        save.avgQueueLength = func.apply(val1.avgQueueLength, val2.avgQueueLength);
        save.busyTime = func.apply(val1.busyTime, val2.busyTime);
        save.responseTime = func.apply(val1.responseTime, val2.responseTime);
        save.unavailableTime = func.apply(val1.unavailableTime, val2.unavailableTime);
        save.waitTime = func.apply(val1.waitTime, val2.waitTime);
        save.lastEventTime = func.apply(val1.lastEventTime, val2.lastEventTime);
        // derived stats
        save.avgWaitTime = func.apply(val1.avgWaitTime, val2.avgWaitTime);
        save.avgResponse = func.apply(val1.avgResponse, val2.avgResponse);
        save.unavailable = func.apply(val1.unavailable, val2.unavailable);
        save.troughput = func.apply(val1.troughput, val2.troughput);
        save.utilization = func.apply(val1.utilization, val2.utilization);
    }
}