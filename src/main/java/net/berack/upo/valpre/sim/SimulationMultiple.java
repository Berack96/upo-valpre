package net.berack.upo.valpre.sim;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.berack.upo.valpre.rand.Rng;
import net.berack.upo.valpre.sim.stats.Result;

/**
 * A network simulation that uses a discrete event simulation to model the
 * behavior of a network of servers.
 */
public class SimulationMultiple {
    private final Net net;
    private final String[] nodes;

    /**
     * Create a new object that can simulate the net in input multiple times
     * 
     * @param net the net that should be simulated
     */
    public SimulationMultiple(Net net) {
        var nodes = new ArrayList<String>();
        for (var node : net)
            nodes.add(node.name);

        this.net = net;
        this.nodes = nodes.toArray(new String[0]);
    }

    /**
     * Run the simualtion multiple times with the given seed and number of runs.
     * The runs are calculated one after the other. For a parallel run see
     * {@link #runParallel(long, int, EndCriteria...)}.
     * 
     * @param seed      The seed to use for the random number generator.
     * @param runs      The number of runs to perform.
     * @param criterias The criteria to determine when to end the simulation. If
     *                  null then the simulation will run until there are no more
     *                  events.
     * @return The statistics the network.
     */
    public Result.Summary run(long seed, int runs, EndCriteria... criterias) {
        var rngs = Rng.getMultipleStreams(seed, runs);
        var result = new Result.Summary(rngs[0].getSeed(), nodes);

        for (int i = 0; i < runs; i++) {
            var sim = new Simulation(this.net, rngs[i], criterias);
            var res = sim.run();
            result.add(res);
        }
        return result;
    }

    /**
     * Runs the simulation multiple times with the given seed and number of runs.
     * The runs are calculated in parallel using the given number of threads.
     * The maximum number of threads are determined by the available processors
     * and the number of runs.
     * 
     * @param seed      The seed to use for the random number generator.
     * @param runs      The number of runs to perform.
     * @param criterias The criteria to determine when to end the simulation. If
     *                  null then the simulation will run until there are no more
     *                  events.
     * @return The statistics the network.
     * @throws InterruptedException If the threads are interrupted.
     * @throws ExecutionException   If the one of the threads has been aborted.
     */
    public Result.Summary runParallel(long seed, int runs, EndCriteria... criterias)
            throws InterruptedException, ExecutionException {
        var rngs = Rng.getMultipleStreams(seed, runs);
        var futures = new Future[runs];

        var numThreads = Math.min(runs, Runtime.getRuntime().availableProcessors());
        try (var threads = Executors.newFixedThreadPool(numThreads)) {
            var results = new Result.Summary(rngs[0].getSeed(), nodes);

            for (int i = 0; i < runs; i++) {
                final var id = i;
                futures[i] = threads.submit(() -> {
                    var sim = new Simulation(this.net, rngs[id], criterias);
                    return sim.run();
                });
            }

            for (var i = 0; i < runs; i++) {
                var res = (Result) futures[i].get();
                results.add(res);
            }

            return results;
        }
    }

    /**
     * Run the simulation multiple times with the given seed and end criteria. The
     * simulation runs will stop when the relative error of the confidence index is
     * less than the given value.
     * The results are printed on the PrintStream.
     * 
     * @param seed        The seed to use for the random number generator.
     * @param runs        The maximum number of runs to perform.
     * @param stream      The PrintStream to print the results.
     * @param confidences The confidence indices to use to determine when to stop
     *                    the simulation.
     * @param criterias   The criteria to determine when to end the simulation. If
     *                    null then the simulation will run until there are no more
     *                    events.
     * @return The statistics the network.
     * @throws IllegalArgumentException If the confidence is not set.
     */
    public Result.Summary runIncremental(long seed, int runs, PrintStream stream, ConfidenceIndices confidences,
            EndCriteria... criterias) {
        if (confidences == null)
            throw new IllegalArgumentException("Confidence must be not null");

        var rng = new Rng(seed); // Only one RNG for all the simulations
        var results = new Result.Summary(rng.getSeed(), nodes);
        var output = new StringBuilder();
        var stop = false;

        for (int i = 0; !stop && runs > i; i++) {
            var sim = new Simulation(this.net, rng, criterias);
            var result = sim.run();
            results.add(result);

            if (i > 0) {
                output.setLength(0);
                output.append(String.format("\rSimulation [%6d]: ", i + 1));

                var errors = confidences.calcRelativeErrors(results);
                stop = confidences.isOk(errors);

                var errString = confidences.getIndices(errors);
                var oneSting = String.join("], [", errString);

                output.append('[').append(oneSting).append("]");
                stream.print(output);
            }
        }

        stream.println(); // remove last printed line
        return results;
    }

}
