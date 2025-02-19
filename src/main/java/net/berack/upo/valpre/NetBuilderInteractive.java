package net.berack.upo.valpre;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.function.Function;
import net.berack.upo.valpre.rand.Distribution;
import net.berack.upo.valpre.sim.Net;
import net.berack.upo.valpre.sim.ServerNode;

/**
 * Interactive net builder. This class allows the user to build a net by adding
 * nodes and connections. The user can also save the net to a file.
 */
public class NetBuilderInteractive {

    private final Net net = new Net();
    private final PrintStream out;
    private final Scanner scanner;

    /**
     * Create a new interactive net builder. Uses System.in and System.out.
     */
    public NetBuilderInteractive() {
        this(System.out, System.in);
    }

    /**
     * Create a new interactive net builder.
     * 
     * @param out the output stream
     * @param in  the input stream
     */
    public NetBuilderInteractive(PrintStream out, InputStream in) {
        this.out = out;
        this.scanner = new Scanner(in);
    }

    /**
     * Run the interactive net builder.
     * 
     * @param args the arguments
     */
    public Net run() {
        while (true) {
            try {
                var choice = choose("Choose the next step to do:",
                        "Add a node", "Add a connection", "Print Nodes", "Save the net", "Exit");
                switch (choice) {
                    case 1 -> {
                        var node = this.buildNode();
                        this.net.addNode(node);
                    }
                    case 2 -> {
                        var source = ask("Enter the source node: ");
                        var target = ask("Enter the target node: ");
                        var weight = ask("Enter the weight: ", Double::parseDouble);
                        var sourceNode = this.net.getNode(source);
                        var targetNode = this.net.getNode(target);
                        this.net.addConnection(sourceNode, targetNode, weight);
                    }
                    case 3 -> this.out.println(this.net);
                    case 4 -> this.net.save(ask("Enter the filename: "));
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
     * Build a node.
     * 
     * @return the node
     */
    private ServerNode buildNode() {
        var choice = choose("Choose the type of node to create:", "Source", "Queue", "Queue with unavailable time");
        var name = ask("Node name: ");
        var distribution = askDistribution("Service distribution");

        return switch (choice) {
            case 1 -> {
                var limit = ask("Arrivals limit (0 for Int.Max): ", Integer::parseInt);
                if (limit <= 0)
                    limit = Integer.MAX_VALUE;
                yield ServerNode.Builder.sourceLimited(name, limit, distribution);
            }
            case 2 -> {
                var servers = ask("Number of servers: ", Integer::parseInt);
                yield ServerNode.Builder.queue(name, servers, distribution, null);
            }
            case 3 -> {
                var servers = ask("Number of servers: ", Integer::parseInt);
                var unavailable = askDistribution("Unavailable distribution");
                yield ServerNode.Builder.queue(name, servers, distribution, unavailable);
            }
            default -> null;
        };
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
        var totalRows = ask.chars().filter(ch -> ch == '\n').count() + 1;

        var startLine = "\033[" + totalRows + "A";
        var clearLine = "\033[K";
        var ask2 = startLine + clearLine + ask;
        var first = true;

        while (true) {
            this.out.print(first ? ask : ask2);
            var line = this.scanner.nextLine();
            first = false;

            try {
                var value = parser.apply(line);
                return value;
        } catch (Exception e) {
            }
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
        while (choice < 1 || choice > options.length)
            choice = ask(string, Integer::parseInt);

        return choice;
    }
}
