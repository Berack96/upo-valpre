package net.berack.upo.valpre;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.berack.upo.valpre.sim.stats.CsvResult;
import net.berack.upo.valpre.sim.stats.ResultMultiple;

/**
 * This class is used to plot the results of the simulation.
 * The results are saved in a CSV file and then loaded to be plotted.
 */
public class Plot {
    public final ResultMultiple results;

    /**
     * Create a new plot object.
     * 
     * @param args the arguments to create the plot
     * @throws IOException if anything happens while reading the file
     */
    public Plot(String[] args) throws IOException {
        var arguments = Plot.parseParameters(args);
        var file = Parameters.getFileOrExample(arguments.get("csv"));
        if (file == null)
            throw new IllegalArgumentException("CSV file needed! Use -csv <file>");

        var results = new CsvResult(file).loadResults();
        this.results = new ResultMultiple(results);
    }

    /**
     * Show the plot of the results.
     */
    public void show() {
        // TODO: Use JavaFX to show the plot
    }

    /**
     * Parse the arguments to get the CSV file.
     * 
     * @param args the arguments to parse
     * @return a map with the arguments
     */
    private static Map<String, String> parseParameters(String[] args) {
        var arguments = new HashMap<String, Boolean>();
        arguments.put("csv", true);

        var descriptions = new HashMap<String, String>();
        descriptions.put("csv", "The filename that contains the previous saved runs.");

        return Parameters.getArgsOrHelper(args, "-", arguments, descriptions);
    }
}
