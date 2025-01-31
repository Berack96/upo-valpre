package net.berack.upo.valpre;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

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
    public final String file;

    /**
     * Create a new simulation with the given arguments.
     * 
     * @param args The arguments for the simulation.
     */
    public Simulation(String[] args) {
        // Evantually change the parameters
        var arguments = Simulation.parseParameters(args);
        this.runs = Simulation.getFromArguments(arguments, "runs", Integer::parseInt, 100);
        this.seed = Simulation.getFromArguments(arguments, "seed", Long::parseLong, 2007539552L);
        this.csv = arguments.getOrDefault("csv", null);
        this.parallel = arguments.containsKey("p");

        this.file = Parameters.getFileOrExample(arguments.get("net"));
        if (this.file == null)
            throw new IllegalArgumentException("Net file needed! Use -net <file>");
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
        var net = Net.load(this.file);
        var nano = System.nanoTime();
        var sim = new SimulationMultiple(net);
        var summary = this.parallel ? sim.runParallel(this.seed, this.runs) : sim.run(this.seed, this.runs);
        nano = System.nanoTime() - nano;

        System.out.print(summary);
        System.out.println("Final time " + nano / 1e6 + "ms");

        if (csv != null) {
            new CsvResult(this.csv).saveResults(summary.runs);
            System.out.println("Data saved to " + this.csv);
        }
    }

    /**
     * Get the value from the arguments or the default value if it is not present.
     * 
     * @param args  The arguments for the simulation.
     * @param key   The key to get the value from.
     * @param parse The function to parse the value.
     * @param value The default value if the key is not present.
     * @return The value from the arguments or the default value if it is not
     *         present.
     */
    private static <T> T getFromArguments(Map<String, String> args, String key, Function<String, T> parse, T value) {
        if (args.containsKey(key))
            return parse.apply(args.get(key));
        return value;
    }

    /**
     * Parse the arguments for the simulation.
     * 
     * @param args The arguments for the simulation.
     * @return The parsed arguments for the simulation.
     */
    private static Map<String, String> parseParameters(String[] args) {
        var arguments = new HashMap<String, Boolean>();
        arguments.put("p", false);
        arguments.put("seed", true);
        arguments.put("runs", true);
        arguments.put("net", true);
        arguments.put("csv", true);

        var descriptions = new HashMap<String, String>();
        descriptions.put("p", "Add this if you want the simulation to use threads (one each run).");
        descriptions.put("seed", "The seed of the simulation.");
        descriptions.put("runs", "How many runs the simulator should run.");
        descriptions.put("net",
                "The net to use. It should be a file. Use example1.net or example2.net for the provided ones.");
        descriptions.put("csv", "The filename for saving every run statistics.");

        return Parameters.getArgsOrHelper(args, "-", arguments, descriptions);
    }
}
