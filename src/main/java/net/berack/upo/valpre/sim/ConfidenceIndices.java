package net.berack.upo.valpre.sim;

import java.lang.reflect.Field;
import java.util.ArrayList;

import net.berack.upo.valpre.sim.stats.NodeStats;
import net.berack.upo.valpre.sim.stats.Result;

/**
 * Confidence indices for a simulation.
 * This class is used to store the confidence indices for a simulation.
 * The confidence indices are used to determine when the simulation has
 * reached a certain level of confidence.
 */
public class ConfidenceIndices {
    private final String[] nodes;
    private final NodeStats[] confidences;
    private final NodeStats[] relativeErrors;
    private boolean isEmpty = true;

    /**
     * Create a new confidence indices object for the given network.
     * 
     * @param net the network to create the confidence indices for
     */
    public ConfidenceIndices(Net net) {
        var size = net.size();
        this.nodes = new String[size];
        this.confidences = new NodeStats[size];
        this.relativeErrors = new NodeStats[size];

        for (var i = 0; i < size; i++) {
            this.nodes[i] = net.getNode(i).name;
            this.confidences[i] = new NodeStats();
            this.relativeErrors[i] = new NodeStats();
            this.relativeErrors[i].apply(_ -> 1.0);
        }
    }

    /**
     * Get the number of confidence indices added to the simulation.
     * 
     * @return the number of confidence indices
     */
    public boolean isEmpty() {
        return this.isEmpty;
    }

    /**
     * Add a confidence index to the simulation. The simulation will stop when the
     * relative error of the confidence index is less than the given value.
     * 
     * @param node       The node to calculate the confidence index for.
     * @param stat       The statistic to calculate the confidence index for.
     * @param confidence The confidence level of the confidence index.
     * @param relError   The relative error of the confidence index.
     * @throws IllegalArgumentException If the node is invalid, the confidence is
     *                                  not between 0 and 1, or the relative error
     *                                  is not between 0 and 1.
     * @throws IllegalArgumentException If the statistic is invalid.
     */
    public void add(int node, String stat, double confidence, double relError) {
        if (node < 0 || node >= this.nodes.length)
            throw new IllegalArgumentException("Invalid node: " + node);
        if (confidence <= 0 || confidence > 1)
            throw new IllegalArgumentException("Confidence must be between 0 and 1");
        if (relError <= 0 || relError > 1)
            throw new IllegalArgumentException("Relative error must be between 0 and 1");

        try {
            Field field = NodeStats.class.getField(stat);
            field.set(this.confidences[node], confidence);
            field.set(this.relativeErrors[node], relError);
            this.isEmpty = false;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid statistic: " + stat);
        }
    }

    /**
     * Calculate the relative errors of the statistics of the network.
     * 
     * @param summary the summary of the network statistics
     * @return the relative errors of the statistics
     */
    public NodeStats[] calcRelativeErrors(Result.Summary summary) {
        var errors = new NodeStats[this.nodes.length];
        for (var i = 0; i < this.confidences.length; i++) {
            var node = this.nodes[i];
            var stat = summary.getSummaryOf(node);

            var confidence = this.confidences[i];
            var relativeError = stat.calcError(confidence);
            relativeError.merge(stat.average, (err, avg) -> err / avg);
            errors[i] = relativeError;
        }

        return errors;
    }

    /**
     * Check if the errors are within the confidence indices.
     * The errors within the confidence indices are calculated using the
     * {@link #calcRelativeErrors(Result.Summary)} method.
     * 
     * @param errors the relative errors of the statistics
     * @return true if the simulation is ok, false otherwise
     */
    public boolean isOk(NodeStats[] errors) {
        for (var i = 0; i < this.relativeErrors.length; i++) {
            var error = errors[i].clone();
            var relError = this.relativeErrors[i];

            error.merge(relError, (err, rel) -> err - rel);
            for (var value : error)
                if (!value.isInfinite() && !value.isNaN() && value > 0)
                    return false;
        }

        return true;
    }

    /**
     * Get the errors of the statistics of the network.
     * The errors are calculated using the
     * {@link #calcRelativeErrors(Result.Summary)} method.
     * Each error is formatted as a string in the format: "node:stat=value".
     * 
     * @param errors the relative errors of the statistics
     * @return the errors of the statistics
     */
    public String[] getIndices(NodeStats[] errors) {
        var statistics = NodeStats.getOrderOfApply();
        var retValues = new ArrayList<String>();

        for (var i = 0; i < this.relativeErrors.length; i++) {
            var error = errors[i].clone();

            for (var stat : statistics) {
                var err = error.of(stat);
                if (!Double.isFinite(err))
                    continue;

                retValues.add("%s:%s=%.3f".formatted(this.nodes[i], stat, err));
            }
        }

        return retValues.toArray(new String[0]);
    }
}