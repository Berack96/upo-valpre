package net.berack.upo.valpre;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import com.esotericsoftware.kryo.KryoException;

import net.berack.upo.valpre.sim.EndCriteria;
import net.berack.upo.valpre.sim.Net;
import net.berack.upo.valpre.sim.SimulationMultiple;
import net.berack.upo.valpre.sim.stats.CsvResult;

/**
 * This class is responsible for running the simulation. It parses the arguments
 * and runs the simulation with the given parameters.
 */
public class SimulationBuilder {
    private String csv;
    private int runs;
    private long seed;
    private boolean parallel;
    private Net net;
    private EndCriteria[] endCriteria;

    /**
     * Create a new simulation for the given net.
     * 
     * @param netFile the net file to load
     * @throws IOException if the file has a problem
     */
    public SimulationBuilder(String netFile) throws IOException {
        try {
            var file = Parameters.getFileOrExample(netFile);
            this.net = Net.load(file);
            file.close();
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Net file needed!");
        } catch (KryoException e) {
            throw new IllegalArgumentException("Net file is not valid or corrupted!");
        }
    }

    /**
     * Create a new simulation for the given net.
     * 
     * @param net the net
     * @throws IllegalArgumentException if the net is null
     */
    public SimulationBuilder(Net net) {
        if (net == null)
            throw new IllegalArgumentException("Net needed!");
        this.net = net;
    }

    /**
     * Set the number of runs for the simulation.
     * 
     * @param runs the number of runs
     * @throws IllegalArgumentException if the runs are less than 1
     * @return this simulation
     */
    public SimulationBuilder setRuns(int runs) {
        if (runs <= 0)
            throw new IllegalArgumentException("Runs must be greater than 0!");

        this.runs = runs;
        return this;
    }

    /**
     * Set the seed for the simulation.
     * 
     * @param seed the seed
     * @return this simulation
     */
    public SimulationBuilder setSeed(long seed) {
        this.seed = seed;
        return this;
    }

    /**
     * Set if the simulation should run in parallel.
     * The parallelization is done by running each simulation in a separate thread.
     * 
     * @param parallel if the simulation should run in parallel
     * @return this simulation
     */
    public SimulationBuilder setParallel(boolean parallel) {
        this.parallel = parallel;
        return this;
    }

    /**
     * Set the CSV file to save the results.
     * 
     * @param csv the CSV file
     * @return this simulation
     */
    public SimulationBuilder setCsv(String csv) {
        this.csv = csv;
        return this;
    }

    /**
     * Set the end criteria for the simulation.
     * Can be an empty array if no criteria are needed.
     * Cannot be null.
     * 
     * @param criterias the end criteria
     * @return this simulation
     * @throws IllegalArgumentException if the criteria are null
     */
    public SimulationBuilder setEndCriteria(EndCriteria... criterias) {
        if (criterias == null)
            throw new IllegalArgumentException("End criteria cannot be null!");
        this.endCriteria = criterias;
        return this;
    }

    /**
     * Run the simulation with the given parameters.
     * At the end it prints the results and saves them to a CSV file if requested.
     * 
     * @throws InterruptedException If the simulation is interrupted.
     * @throws ExecutionException   If the simulation fails.
     * @throws KryoException        If the simulation fails.
     * @throws IOException          If the simulation fails.
     */
    public void run() throws InterruptedException, ExecutionException, KryoException, IOException {
        var nano = System.nanoTime();
        var sim = new SimulationMultiple(this.net);
        var summary = this.parallel
                ? sim.runParallel(this.seed, this.runs, this.endCriteria)
                : sim.run(this.seed, this.runs, this.endCriteria);
        nano = System.nanoTime() - nano;

        System.out.print(summary);
        System.out.println("Final time " + nano / 1e6 + "ms");

        if (csv != null) {
            new CsvResult(this.csv).saveResults(summary.getRuns());
            System.out.println("Data saved to " + this.csv);
        }
    }
}
