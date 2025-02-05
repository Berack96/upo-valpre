package net.berack.upo.valpre.sim.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.math3.distribution.TDistribution;

/**
 * This class keeps track of various statistical metrics related to the net
 * performance, such as the number of arrivals and departures, queue lengths,
 * wait times, response times, server utilization, and unavailability. These
 * statistics are updated during simulation events, such as arrivals and
 * departures, and can be used to analyze the net's behavior and performance.
 */
public class NodeStats implements Cloneable {
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
    public double throughput = 0.0d;
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
        this.throughput = this.numDepartures / time;
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
    public NodeStats apply(Function<Double, Double> func) {
        return NodeStats.operation(this, this, this, (val1, _) -> func.apply(val1));
    }

    /**
     * A function used to merge tree stats.
     * The only stats that are not updated with this function are the one that
     * starts with max, min (since they are special)
     * 
     * @param other
     * @param func
     */
    public NodeStats merge(NodeStats other, BiFunction<Double, Double, Double> func) {
        return NodeStats.operation(this, this, other, func);
    }

    @Override
    protected NodeStats clone() {
        try {
            return (NodeStats) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the value of the stat.
     * 
     * @param statName the name of the stat
     * @return the value of the stat
     */
    public double of(String statName) {
        return switch (statName) {
            case "numArrivals" -> this.numArrivals;
            case "numDepartures" -> this.numDepartures;
            case "maxQueueLength" -> this.maxQueueLength;
            case "avgQueueLength" -> this.avgQueueLength;
            case "avgWaitTime" -> this.avgWaitTime;
            case "avgResponse" -> this.avgResponse;
            case "busyTime" -> this.busyTime;
            case "waitTime" -> this.waitTime;
            case "unavailableTime" -> this.unavailableTime;
            case "responseTime" -> this.responseTime;
            case "lastEventTime" -> this.lastEventTime;
            case "throughput" -> this.throughput;
            case "utilization" -> this.utilization;
            case "unavailable" -> this.unavailable;
            default -> throw new IllegalArgumentException("Invalid stat name");
        };
    }

    /**
     * Get the order of update of the stats in the apply function.
     * 
     * @return the order of the stats
     */
    public static String[] getOrderOfApply() {
        return new String[] { "numArrivals", "numDepartures", "avgQueueLength", "avgWaitTime", "avgResponse",
                "busyTime", "waitTime", "unavailableTime", "responseTime", "lastEventTime", "throughput", "utilization",
                "unavailable" };
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
     * @return The `save` object with the merged statistics.
     */
    public static NodeStats operation(NodeStats save, NodeStats val1, NodeStats val2,
            BiFunction<Double, Double, Double> func) {
        save.numArrivals = func.apply(val1.numArrivals, val2.numArrivals);
        save.numDepartures = func.apply(val1.numDepartures, val2.numDepartures);
        // save.maxQueueLength = func.apply(val1.maxQueueLength, val2.maxQueueLength);
        save.avgQueueLength = func.apply(val1.avgQueueLength, val2.avgQueueLength);
        save.avgWaitTime = func.apply(val1.avgWaitTime, val2.avgWaitTime);
        save.avgResponse = func.apply(val1.avgResponse, val2.avgResponse);
        save.busyTime = func.apply(val1.busyTime, val2.busyTime);
        save.waitTime = func.apply(val1.waitTime, val2.waitTime);
        save.unavailableTime = func.apply(val1.unavailableTime, val2.unavailableTime);
        save.responseTime = func.apply(val1.responseTime, val2.responseTime);
        save.lastEventTime = func.apply(val1.lastEventTime, val2.lastEventTime);
        save.throughput = func.apply(val1.throughput, val2.throughput);
        save.utilization = func.apply(val1.utilization, val2.utilization);
        save.unavailable = func.apply(val1.unavailable, val2.unavailable);
        return save;
    }

    /**
     * A class to store incremental statistics.
     * This class is used to store incremental statistics for a confidence index.
     * It keeps track of the average, variance, minimum, and maximum values of the
     * statistics over time. The statistics are updated incrementally as new data
     * points are added, allowing for real-time monitoring of the confidence index.
     */
    public static class Summary {
        public final NodeStats average = new NodeStats();
        public final NodeStats variance = new NodeStats();
        public final NodeStats min = new NodeStats().apply(_ -> Double.MAX_VALUE);
        public final NodeStats max = new NodeStats().apply(_ -> Double.MIN_VALUE);
        private List<NodeStats> stats = new ArrayList<>();

        /**
         * Update the incremental statistics with new data.
         * This method updates the incremental statistics with new data points. It
         * calculates the average, variance, minimum, and maximum values of the
         * statistics over time, based on the new data points. The statistics are
         * stored in the `average`, `variance`, `min`, and `max` fields of the
         * `Incremental` object, respectively.
         * 
         * @param other The `NodeStats` object containing the new data points to add to
         *              the incremental statistics.
         */
        public void update(NodeStats other) {
            var n = this.stats.size() + 1;
            this.stats.add(other);

            var delta = this.average.clone().merge(other, (avg, newVal) -> newVal - avg);
            this.average.merge(delta, (val1, val2) -> val1 + val2 / n);

            var mergedDelta = this.average.clone()
                    .merge(other, (avg, newVal) -> newVal - avg)
                    .merge(delta, (dNew, dOld) -> dNew * dOld);

            var nSampleSize = Math.max(n - 1, 1);
            this.variance.merge(mergedDelta, (var, deltas) -> var + deltas / nSampleSize);
            this.min.merge(other, Math::min);
            this.max.merge(other, Math::max);
        }

        /**
         * Get the standard deviation of the values in the array.
         * 
         * @return the standard deviation value
         */
        public NodeStats stdDev() {
            return this.variance.clone().apply(Math::sqrt);
        }

        /**
         * Calculates the error at the selected alpha level.
         * This method computes the error for the average and standard deviation values,
         * considering the sample size and the confidence level (alpha).
         * The result is adjusted using a t-distribution to account for the variability
         * in smaller sample sizes.
         * 
         * @param distribution the t-distribution to use
         * @param stdDev       the standard deviation of the values
         * @param alpha        the alpha value
         * @return the error of the values
         */
        public NodeStats calcError(double alpha) {
            var n = this.stats.size();
            var distr = new TDistribution(null, n - 1);
            var tValue = distr.inverseCumulativeProbability(alpha);

            return this.stdDev().apply(std -> tValue * (std / Math.sqrt(n)));
        }

        /**
         * Get the frequency of the values in the array.
         * In the function passed you can choose which value to use for the frequency.
         * 
         * @param numBins  the number of bins to use
         * @param getValue the function to get the value from the stats
         * @return an array with the frequency of the values
         */
        public int[] getFrequency(int numBins, Function<NodeStats, Double> getValue) {
            var buckets = new int[numBins];
            var min = getValue.apply(this.min);
            var max = getValue.apply(this.max);
            var range = max - min;
            var step = numBins / range;

            for (var stat : this.stats) {
                var value = getValue.apply(stat);
                var index = (int) Math.floor((value - min) * step);
                index = Math.min(index, numBins - 1);
                buckets[index] += 1;
            }
            return buckets;
        }
    }
}