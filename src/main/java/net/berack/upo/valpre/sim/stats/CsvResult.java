package net.berack.upo.valpre.sim.stats;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is used to save the results of the simulation to a CSV file.
 * The CSV file is used to save the results of the simulation in a format that
 * can
 * be easily read by other programs.
 */
public class CsvResult {
    public final String file;

    /**
     * Create a new CSV result object.
     * 
     * @param file the file to save/load the results
     */
    public CsvResult(String file) {
        if (!file.endsWith(".csv"))
            file = file + ".csv";
        this.file = file;
    }

    /**
     * Save all the runs to a csv file.
     * 
     * @throws IOException if anything happens wile wriiting to the file
     */
    public void saveResults(Result[] results) throws IOException {
        var builder = new StringBuilder();
        builder.append("seed,node,");
        builder.append(String.join(",", Statistics.getOrderOfApply()));
        builder.append('\n');

        try (var writer = new FileWriter(this.file)) {
            for (var result : results) {
                for (var entry : result.nodes.entrySet()) {
                    builder.append(result.seed).append(",");
                    builder.append(entry.getKey()).append(",");
                    builder.append(CsvResult.statsToCSV(entry.getValue())).append('\n');
                }
            }
            writer.write(builder.toString());
        }
    }

    /**
     * Load the results from the CSV file.
     * 
     * @return the results loaded from the file
     * @throws IOException if anything happens while reading the file
     */
    public Result[] loadResults() throws IOException {
        try (var stream = new FileInputStream(this.file)) {
            return CsvResult.loadResults(stream);
        }
    }

    /**
     * Load the results from the CSV stream.
     * 
     * @param input the input stream to read
     * @return the results loaded from the stream
     */
    public static Result[] loadResults(InputStream input) {
        var results = new ArrayList<Result>();
        try (var scan = new Scanner(input)) {
            var _ = scan.nextLine();

            var nodes = new HashMap<String, Statistics>();
            var seed = 0L;

            while (scan.hasNextLine()) {
                var line = scan.nextLine().split(",");
                var currentSeed = Long.parseLong(line[0]);
                var node = line[1];

                if (currentSeed != seed && seed != 0) {
                    results.add(new Result(seed, 0.0, 0L, nodes));
                    nodes = new HashMap<>();
                }
                seed = currentSeed;

                var copy = Arrays.copyOfRange(line, 2, line.length);
                var stats = CsvResult.statsFromCSV(copy);
                nodes.put(node, stats);
            }

            results.add(new Result(seed, 0.0, 0L, nodes));
        }
        return results.toArray(new Result[0]);
    }

    /**
     * Converts the statistics object to a CSV string.
     * 
     * @param stats the statistics to convert
     * @return the CSV string
     */
    public static String statsToCSV(Statistics stats) {
        var builder = new StringBuilder();
        stats.apply(val -> {
            builder.append(val).append(",");
            return val;
        });

        builder.deleteCharAt(builder.length() - 1); // remove the last comma
        return builder.toString();
    }

    /**
     * Converts the CSV string to a statistics object.
     * 
     * @param values the values to convert
     * @return the statistics object
     */
    public static Statistics statsFromCSV(String[] values) {
        var i = new AtomicInteger(0);
        var stats = new Statistics();
        stats.apply(_ -> Double.parseDouble(values[i.getAndIncrement()]));
        return stats;
    }

}
