package net.berack.upo.valpre;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import com.esotericsoftware.kryo.KryoException;

import net.berack.upo.valpre.sim.Net;
import net.berack.upo.valpre.sim.SimulationMultiple;
import net.berack.upo.valpre.sim.stats.CsvResult;

/**
 * This class is responsible for running the simulation. It parses the arguments
 * and runs the simulation with the given parameters.
 */
public class Simulation {
    public final String csv;
    public final int runs;
    public final long seed;
    public final boolean parallel;
    public final Net net;

    /**
     * Create a new simulation with the given arguments.
     * 
     * @param args The arguments for the simulation.
     * @throws IOException if the file is has a problem
     */
    public Simulation(String netFile, long seed, int runs, boolean parallel, String csv) throws IOException {
        if (runs <= 0)
            throw new IllegalArgumentException("Runs must be greater than 0!");

        this.runs = runs;
        this.seed = seed;
        this.csv = csv;
        this.parallel = parallel;

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
        var summary = this.parallel ? sim.runParallel(this.seed, this.runs) : sim.run(this.seed, this.runs);
        nano = System.nanoTime() - nano;

        System.out.print(summary);
        System.out.println("Final time " + nano / 1e6 + "ms");

        if (csv != null) {
            new CsvResult(this.csv).saveResults(summary.runs);
            System.out.println("Data saved to " + this.csv);
        }
    }
}
