package net.berack.upo.valpre.sim.stats;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

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
    public void saveResults(List<Result> results) throws IOException {
        var builder = new StringBuilder();
        builder.append("seed,node,");
        builder.append(String.join(",", NodeStats.getOrderOfApply()));
        builder.append('\n');

        try (var writer = new FileWriter(this.file)) {
            for (var result : results) {
                for (var entry : result) {
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
    public List<Result> loadResults() throws IOException {
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
    public static List<Result> loadResults(InputStream input) {
        var results = new ArrayList<Result>();
        try (var scan = new Scanner(input)) {
            var headerOrder = CsvResult.extractHeaderPositions(scan.nextLine());
            var builder = new Result.Builder();

            while (scan.hasNextLine()) {
                var line = scan.nextLine().split(",");
                var currentSeed = Long.parseLong(line[0]);

                if (builder.seed != currentSeed && builder.seed != 0) {
                    results.add(builder.build());
                    builder.reset();
                }

                var node = line[1];
                var copy = Arrays.copyOfRange(line, 2, line.length);
                var stats = CsvResult.statsFromCSV(headerOrder, copy);

                builder.seed(currentSeed).addNode(node, stats);
            }

            results.add(builder.build());
        }
        return results;
    }

    /**
     * Converts the statistics object to a CSV string.
     * 
     * @param stats the statistics to convert
     * @return the CSV string
     */
    public static String statsToCSV(NodeStats stats) {
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
    public static NodeStats statsFromCSV(String[] header, String[] values) {
        try {
            var stats = new NodeStats();
            var clazz = NodeStats.class;

            for (var i = 0; i < values.length; i++) {
                var value = Double.parseDouble(values[i]);
                var field = header[i];
                clazz.getField(field).setDouble(stats, value);
            }
            return stats;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while parsing the CSV file " + e.getMessage());
        }
    }

    /**
     * Extract the header order from the CSV file.
     * 
     * @param header the header to extract
     * @return the positions of the header
     * @throws IllegalArgumentException if the header is not correct
     */
    public static String[] extractHeaderPositions(String header) {
        var splittedHeader = header.split(",");

        var headerSeed = splittedHeader[0].equals("seed");
        var headerNode = splittedHeader[1].equals("node");
        if (!headerSeed || !headerNode)
            throw new IllegalArgumentException("CSV file doesn't have the node or seed header");

        var allStats = NodeStats.getOrderOfApply();
        splittedHeader = Arrays.copyOfRange(splittedHeader, 2, splittedHeader.length);
        if (splittedHeader.length > allStats.length)
            throw new IllegalArgumentException("CSV file doesn't have the correct header [" + allStats + "]");

        var order = new String[allStats.length];
        var stats = Arrays.asList(allStats);
        for (var i = 0; i < splittedHeader.length; i++) {
            var stat = splittedHeader[i];
            var index = stats.indexOf(stat);
            order[i] = allStats[index];
        }

        return order;
    }
}
