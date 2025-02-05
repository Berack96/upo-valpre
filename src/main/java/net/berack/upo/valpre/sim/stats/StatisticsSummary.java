package net.berack.upo.valpre.sim.stats;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A summary of the values.
 */
public class StatisticsSummary {
    public final String name;
    public final double average;
    public final double median;
    public final double min;
    public final double max;
    public final double stdDev;
    public final double error95;
    public final double[] values;

    /**
     * Create a summary of the values.
     * This method calculates the average, median, minimum, maximum, standard
     * deviation, and error at the 95% confidence level of the provided values.
     * The values are sorted before calculating the summary.
     * 
     * @param values the values to summarize
     */
    public StatisticsSummary(String name, double[] values) {
        if (values == null || values.length < 2)
            throw new IllegalArgumentException("The values array must have at least two elements.");

        Arrays.sort(values);
        var sum = Arrays.stream(values).sum();
        var avg = sum / values.length;
        var varianceSum = Arrays.stream(values).map(value -> Math.pow(value - avg, 2)).sum();

        this.name = name;
        this.values = values;
        this.average = avg;
        this.stdDev = Math.sqrt(varianceSum / (values.length - 1));
        this.median = this.getPercentile(0.50);
        this.min = values[0];
        this.max = values[values.length - 1];
        this.error95 = this.calcError(0.95);
    }

    /**
     * Calculates the error at the selected alpha level.
     * This method computes the error for the average and standard deviation values,
     * considering the sample size and the confidence level (alpha).
     * The result is adjusted using a t-distribution to account for the variability
     * in smaller sample sizes.
     * 
     * @param alpha the alpha value
     * @return the error of the values
     */
    public double calcError(double alpha) {
        var sampleSize = this.values.length;
        var distr = new org.apache.commons.math3.distribution.TDistribution(sampleSize - 1);
        var percentile = distr.inverseCumulativeProbability(alpha);

        return percentile * (this.stdDev / Math.sqrt(sampleSize));
    }

    /**
     * Get the frequency of the values in the array.
     * 
     * @param numBins the number of bins to use
     * @return an array with the frequency of the values
     */
    public int[] getFrequency(int numBins) {
        var buckets = new int[numBins];
        var range = this.max - this.min;
        var step = numBins / range;

        for (var value : this.values) {
            var index = (int) Math.floor((value - this.min) * step);
            index = Math.min(index, numBins - 1);
            buckets[index] += 1;
        }
        return buckets;
    }

    /**
     * Get the percentile of the values in the array.
     * 
     * @param percentile the percentile to calculate
     * @return the value at the selected percentile
     */
    public double getPercentile(double percentile) {
        var index = (int) Math.floor(percentile * (this.values.length - 1));
        return this.values[index];
    }

    /**
     * Get a summary of the statistics.
     * 
     * @param stats the statistics to summarize
     * @return a map with the summary of the statistics
     * @throws IllegalArgumentException if the fields of the statistics cannot be
     *                                  accessed
     */
    public static Map<String, StatisticsSummary> getSummary(Statistics[] stats) throws IllegalArgumentException {
        try {
            var map = new HashMap<String, StatisticsSummary>();

            for (var field : Statistics.class.getFields()) {
                field.setAccessible(true);

                var values = new double[stats.length];
                for (var i = 0; i < stats.length; i++)
                    values[i] = field.getDouble(stats[i]);

                var name = field.getName();
                map.put(name, new StatisticsSummary(name, values));
            }
            return map;
        } catch (IllegalAccessException e) { // This should not happen normally, but it is better to catch it
            e.printStackTrace();
            throw new IllegalArgumentException("Cannot access the fields of the statistics.");
        }
    }
}