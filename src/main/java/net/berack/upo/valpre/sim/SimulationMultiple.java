package net.berack.upo.valpre.sim;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.berack.upo.valpre.rand.Rng;
import net.berack.upo.valpre.sim.stats.ResultMultiple;
import net.berack.upo.valpre.sim.stats.Result;

/**
 * A network simulation that uses a discrete event simulation to model the
 * behavior of a network of servers.
 */
public class SimulationMultiple {
    private final Net net;

    /**
     * Create a new object that can simulate the net in input multiple times
     * 
     * @param net the net that sould be simulated
     */
    public SimulationMultiple(Net net) {
        this.net = net;
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
    public ResultMultiple run(long seed, int runs, EndCriteria... criterias) {
        var rngs = Rng.getMultipleStreams(seed, runs);
        var stats = new Result[runs];

        for (int i = 0; i < runs; i++) {
            var sim = new Simulation(this.net, rngs[i], criterias);
            stats[i] = sim.run();
        }
        return new ResultMultiple(stats);
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
    public ResultMultiple runParallel(long seed, int runs, EndCriteria... criterias)
            throws InterruptedException, ExecutionException {
        var rngs = Rng.getMultipleStreams(seed, runs);
        var results = new Result[runs];
        var futures = new Future[runs];

        var numThreads = Math.min(runs, Runtime.getRuntime().availableProcessors());
        try (var threads = Executors.newFixedThreadPool(numThreads)) {
            for (int i = 0; i < runs; i++) {
                final var id = i;
                futures[i] = threads.submit(() -> {
                    var sim = new Simulation(this.net, rngs[id], criterias);
                    results[id] = sim.run();
                });
            }

            for (var i = 0; i < runs; i++) {
                futures[i].get();
            }

            return new ResultMultiple(results);
        }
    }

}
