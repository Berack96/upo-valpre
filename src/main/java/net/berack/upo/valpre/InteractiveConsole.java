package net.berack.upo.valpre;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import com.esotericsoftware.kryo.KryoException;

import net.berack.upo.valpre.rand.Distribution;
import net.berack.upo.valpre.sim.Net;
import net.berack.upo.valpre.sim.ServerNode;

/**
 * Interactive net builder. This class allows the user to build a net by adding
 * nodes and connections. The user can also save the net to a file.
 */
public class InteractiveConsole {

    private Net net = new Net();
    private final PrintStream out;
    private final Scanner scanner;

    /**
     * Create a new interactive net builder. Uses System.in and System.out.
     */
    public InteractiveConsole() {
        this(System.out, System.in);
    }

    /**
     * Create a new interactive net builder.
     * 
     * @param out the output stream
     * @param in  the input stream
     */
    public InteractiveConsole(PrintStream out, InputStream in) {
        this.out = out;
        this.scanner = new Scanner(in);
    }

    /**
     * Run the interactive net builder.
     */
    public Net run() {
        while (true) {
            try {
                var choice = choose(this.net + "\nChoose the next step to do:",
                        "Add a node", "Add a connection", "Save the net", "Load net", "Clear", "Run", "Exit");
                switch (choice) {
                    case 1 -> this.buildNode();
                    case 2 -> this.buildConnection();
                    case 3 -> this.net.save(ask("Enter the filename: "));
                    case 4 -> this.loadNet();
                    case 5 -> this.net = new Net();
                    case 6 -> this.simpleRuns();
                    default -> {
                        this.scanner.close();
                        return this.net;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Build a node as a source, terminal, queue, or queue with unavailable time.
     */
    private void buildNode() {
        var choice = choose("Choose the type of node to create:", "Source", "Terminal", "Queue",
                "Queue with unavailable time");
        var name = ask("Node name: ");
        var distribution = askDistribution("Service distribution");

        var node = switch (choice) {
            case 1 -> ServerNode.Builder.source(name, distribution);
            case 2 -> {
                var limit = ask("Arrivals limit (0 for Int.Max): ", Integer::parseInt);
                if (limit <= 0)
                    limit = Integer.MAX_VALUE;
                yield ServerNode.Builder.terminal(name, limit, distribution);
            }
            case 3 -> {
                var servers = ask("Number of servers: ", Integer::parseInt);
                yield ServerNode.Builder.queue(name, servers, distribution, null);
            }
            case 4 -> {
                var servers = ask("Number of servers: ", Integer::parseInt);
                var unavailable = askDistribution("Unavailable distribution");
                yield ServerNode.Builder.queue(name, servers, distribution, unavailable);
            }
            default -> null;
        };

        if (node != null)
            this.net.addNode(node);
    }

    /**
     * Build a connection.
     */
    private void buildConnection() {
        var source = ask("Enter the source node: ");
        var target = ask("Enter the target node: ");
        var weight = ask("Enter the weight: ", Double::parseDouble);
        var sourceNode = this.net.getNode(source);
        var targetNode = this.net.getNode(target);
        this.net.addConnection(sourceNode, targetNode, weight);
    }

    /**
     * Load a net from a file or from examples.
     */
    private void loadNet() throws KryoException, IOException {
        var choice = choose("Choose the type of net to load:", "From file", "From examples");
        this.net = switch (choice) {
            case 1 -> Net.load(ask("Enter the filename: "));
            case 2 -> {
                var choice2 = choose("Choose the example to load:",
                        "Example 1: Source -> Queue",
                        "Example 2: Source -> Queue -> Queue");
                yield switch (choice2) {
                    case 1 -> NetExamples.getNet1();
                    case 2 -> NetExamples.getNet2();
                    default -> null;
                };
            }
            default -> null;
        };
    }

    /**
     * Run the simulation with the net.
     */
    private void simpleRuns() throws InterruptedException, ExecutionException, IOException {
        var choice = choose("Choose what to do:", "100 Run", "1K Runs", "1K Runs + Plot");
        switch (choice) {
            case 1 -> new SimulationBuilder(net).setMaxRuns(100).setParallel(true).run();
            case 2 -> new SimulationBuilder(net).setMaxRuns(1000).setParallel(true).run();
            case 3 -> {
                var randName = "rand" + System.currentTimeMillis() + ".csv";
                new SimulationBuilder(net).setMaxRuns(1000).setParallel(true).setCsv(randName).run();
                new Plot(randName).show();
                new File(randName).delete();
            }
            default -> {
            }
        }
    }

    /**
     * Ask the user for a distribution.
     * 
     * @return the distribution
     */
    private Distribution askDistribution(String ask) {
        var choice = choose(ask + ":", "Exponential", "Uniform", "Erlang",
                "UnavailableTime", "Normal", "NormalBoxMuller", "None");

        return switch (choice) {
            case 1 -> {
                var lambda = ask("Lambda: ", Double::parseDouble);
                yield new Distribution.Exponential(lambda);
            }
            case 2 -> {
                var min = ask("Min: ", Double::parseDouble);
                var max = ask("Max: ", Double::parseDouble);
                yield new Distribution.Uniform(min, max);
            }
            case 3 -> {
                var k = ask("K: ", Integer::parseInt);
                var lambda = ask("Lambda: ", Double::parseDouble);
                yield new Distribution.Erlang(k, lambda);
            }
            case 4 -> {
                var probability = ask("Probability: ", Double::parseDouble);
                var unavailable = askDistribution("Unavailable distribution");
                yield new Distribution.UnavailableTime(probability, unavailable);
            }
            case 5 -> {
                var mean = ask("Mean: ", Double::parseDouble);
                var stdDev = ask("Standard deviation: ", Double::parseDouble);
                yield new Distribution.Normal(mean, stdDev);
            }
            case 6 -> {
                var mean = ask("Mean: ", Double::parseDouble);
                var stdDev = ask("Standard deviation: ", Double::parseDouble);
                yield new Distribution.NormalBoxMuller(mean, stdDev);
            }
            default -> null;
        };
    }

    /**
     * Ask the user a question.
     * 
     * @param ask the question to ask
     * @return the answer
     */
    private String ask(String ask) {
        return ask(ask, Function.identity());
    }

    /**
     * Ask the user a question.
     * 
     * @param ask          the question to ask
     * @param parser       the parser to use
     * @param defaultValue the default value
     * @return the answer
     */
    private <T> T ask(String ask, Function<String, T> parser) {
        var totalRows = ask.chars().filter(ch -> ch == '\n').count();

        var ask2 = "\033[" + totalRows + "A" + ask;
        var first = true;

        try {
            while (true) {
                this.out.print(first ? ask : ask2);
                var line = this.scanner.nextLine();
                first = false;

                try {
                    var value = parser.apply(line);
                    return value;
                } catch (Exception e) {
                    this.out.print("\033[A\033[K"); // clear the line
                }
            }
        } catch (Exception e) {
            return null; // normally when the scanner is closed
        }
    }

    /**
     * Ask the user to choose an option.
     * 
     * @param ask     the question to ask
     * @param options the options to choose from
     * @return the choice
     */
    private int choose(String ask, String... options) {
        var builder = new StringBuilder();
        builder.append(ask).append("\n");
        for (int i = 0; i < options.length; i++) {
            builder.append(i + 1).append(". ").append(options[i]).append("\n");
        }
        builder.append("> ");

        var string = builder.toString();
        var choice = 0;
        choice = ask(string, val -> {
            var x = Integer.parseInt(val);
            if (x < 1 || x > options.length)
                throw new NumberFormatException(); // retry the question
            return x;
        });

        return choice;
    }
}
