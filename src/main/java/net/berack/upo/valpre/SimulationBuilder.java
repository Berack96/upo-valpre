package net.berack.upo.valpre;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import com.esotericsoftware.kryo.KryoException;

import net.berack.upo.valpre.sim.ConfidenceIndices;
import net.berack.upo.valpre.sim.EndCriteria;
import net.berack.upo.valpre.sim.EndCriteria.MaxArrivals;
import net.berack.upo.valpre.sim.EndCriteria.MaxDepartures;
import net.berack.upo.valpre.sim.EndCriteria.MaxTime;
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
    private Net net;
    private EndCriteria[] endCriteria;
    private ConfidenceIndices confidences;
    private Type type = Type.Normal;

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
            this.confidences = new ConfidenceIndices(this.net);
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
        this.confidences = new ConfidenceIndices(net);
    }

    /**
     * Set the maximum number of runs for the simulation.
     * 
     * @param runs the number of runs
     * @throws IllegalArgumentException if the runs are less than 1
     * @return this simulation
     */
    public SimulationBuilder setMaxRuns(int runs) {
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
        if (parallel && !this.confidences.isEmpty())
            this.type = Type.Parallel;
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
     * Set the end criteria for the simulation.
     * Parses the given string to create an array of end criteria.
     * The string passed must be in the following format:
     * [criteria1];[criteria2];...;[criteriaN]
     * 
     * and each criteria must be in the following format:
     * ClassName:param1,param2,...,paramN
     * 
     * If the string is empty or null, no criteria are set.
     * If one of the criteria is not valid, an exception is thrown.
     * 
     * @param criterias The string to parse.
     * @return this builder
     * @throws IllegalArgumentException If one of the criteria is not valid.
     */
    public SimulationBuilder parseEndCriteria(String criterias) {
        if (criterias == null || criterias.isEmpty()) {
            this.endCriteria = new EndCriteria[0];
            return this;
        }

        var criteria = criterias.split(";");
        this.endCriteria = new EndCriteria[criteria.length];
        for (int i = 0; i < criteria.length; i++) {
            var current = criteria[i].substring(1, criteria[i].length() - 1); // Remove the brackets
            var parts = current.split(":");
            if (parts.length != 2)
                throw new IllegalArgumentException("Invalid criteria: " + current);

            var className = parts[0];
            var params = parts[1].split(",");
            this.endCriteria[i] = switch (className) {
                case "MaxArrivals" -> new MaxArrivals(params[0], Integer.parseInt(params[1]));
                case "MaxDepartures" -> new MaxDepartures(params[0], Integer.parseInt(params[1]));
                case "MaxTime" -> new MaxTime(Double.parseDouble(params[0]));
                default -> throw new IllegalArgumentException("Invalid criteria: " + current);
            };
        }
        return this;
    }

    /**
     * Add a confidence index for the given node and stat.
     * The confidence index is used to determine when the simulation should stop.
     * 
     * 
     * @param node       the node
     * @param stat       the stat to calculate the confidence index for
     * @param confidence the confidence level expressed as a percentage [0,1]
     * @param relError   the relative error expressed as a percentage [0,1]
     * @return this simulation
     * @throws IllegalArgumentException if any of the input parameters is invalid
     */
    public SimulationBuilder addConfidenceIndex(String node, String stat, double confidence, double relError) {
        var index = this.net.getNodeIndex(node);
        if (index < 0)
            throw new IllegalArgumentException("Invalid node: " + node);

        this.confidences.add(index, stat, confidence, relError);
        this.type = Type.Incremental;
        return this;
    }

    /**
     * Parse the confidence indices from a string and add them to the simulation.
     * If the string is null then nothing is done and the builder is returned.
     * The string must be in the following format:
     * "[node1:stat1=confidence1:relError1],..,[nodeN:statN=confidenceN:relErrorN]".
     * 
     * @param indices the indices to parse
     * @return this simulation
     * @throws IllegalArgumentException if indices are not in the correct format
     * @throws IllegalArgumentException if the values are invalid
     */
    public SimulationBuilder parseConfidenceIndices(String indices) {
        if (indices == null)
            return this;

        for (var index : indices.split(",")) {
            var parts = index.split("=");
            if (parts.length != 2)
                throw new IllegalArgumentException("Invalid confidence index: " + index);
            var first = parts[0].split(":");
            if (first.length != 2)
                throw new IllegalArgumentException("Invalid confidence index: " + index);
            var second = parts[1].split(":");
            if (second.length != 2)
                throw new IllegalArgumentException("Invalid confidence index: " + index);

            var node = first[0].substring(1);
            var stat = first[1];
            var confidence = Double.parseDouble(second[0]);
            var relError = Double.parseDouble(second[1].substring(0, second[1].length() - 1));
            this.addConfidenceIndex(node, stat, confidence, relError);
        }

        return this;
    }

    /**
     * Run the simulation with the given parameters.
     * At the end it prints the results and saves them to a CSV file if requested.
     * 
     * @throws InterruptedException If the simulation is interrupted.
     * @throws ExecutionException   If the simulation has an error.
     * @throws IOException          If the CSV file has a problem.
     */
    public void run() throws InterruptedException, ExecutionException, IOException {
        var nano = System.nanoTime();
        var sim = new SimulationMultiple(this.net);
        var summary = switch (this.type) {
            case Incremental -> sim.runIncremental(this.seed, this.runs, this.confidences, this.endCriteria);
            case Parallel -> sim.runParallel(this.seed, this.runs, this.endCriteria);
            case Normal -> sim.run(this.seed, this.runs, this.endCriteria);
        };
        nano = System.nanoTime() - nano;

        System.out.print(summary);
        System.out.println("Final time " + nano / 1e6 + "ms");

        if (csv != null) {
            new CsvResult(this.csv).saveResults(summary.getRuns());
            System.out.println("Data saved to " + this.csv);
        }
    }

    /**
     * Inner class to handle the type of simulation.
     */
    private static enum Type {
        Incremental, Parallel, Normal
    }
}
