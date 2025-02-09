package net.berack.upo.valpre;

import java.util.function.Function;
import net.berack.upo.valpre.rand.Distribution;
import net.berack.upo.valpre.sim.Net;
import net.berack.upo.valpre.sim.ServerNode;

public class NetBuilderInteractive {

    private final Net net = new Net();

    /**
     * Run the interactive net builder.
     * 
     * @param args the arguments
     */
    public void run() {
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
                        var weight = ask("Enter the weight: ", Double::parseDouble, 0.0);
                        var sourceNode = this.net.getNode(source);
                        var targetNode = this.net.getNode(target);
                        this.net.addConnection(sourceNode, targetNode, weight);
                    }
                    case 3 -> this.printNodes();
                    case 4 -> this.net.save(ask("Enter the filename: "));
                    case 5 -> System.exit(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Print the nodes in the net.
     */
    private void printNodes() {
        var builder = new StringBuilder();
        builder.append("Nodes:\n");
        for (var i = 0; i < this.net.size(); i++) {
            var name = this.net.getNode(i).name;
            builder.append(name).append(" -> ");

            for (var connection : this.net.getChildren(i)) {
                var child = this.net.getNode(connection.index);
                builder.append(child.name).append("(").append(connection.weight).append("), ");
            }

            builder.delete(builder.length() - 2, builder.length());
            builder.append("\n");
        }
        System.out.print(builder.toString());
    }

    /**
     * Build a node.
     * 
     * @return the node
     */
    private ServerNode buildNode() {
        var choice = choose("Choose the type of node to create:", "Source", "Queue");
        var name = ask("Node name: ");
        var distribution = askDistribution("Service distribution");

        return switch (choice) {
            case 1 -> {
                var limit = ask("Arrivals limit (0 for Int.Max): ", Integer::parseInt, 1);
                if (limit <= 0)
                    limit = Integer.MAX_VALUE;
                yield ServerNode.Builder.sourceLimited(name, limit, distribution);
            }
            case 2 -> {
                var servers = ask("Number of servers: ", Integer::parseInt, 1);
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
    public static Distribution askDistribution(String ask) {
        var choice = choose(ask + ":", "Exponential", "Uniform", "Erlang",
                "UnavailableTime", "Normal", "NormalBoxMuller", "None");

        return switch (choice) {
            case 1 -> {
                var lambda = ask("Lambda: ", Double::parseDouble, 1.0);
                yield new Distribution.Exponential(lambda);
            }
            case 2 -> {
                var min = ask("Min: ", Double::parseDouble, 0.0);
                var max = ask("Max: ", Double::parseDouble, 1.0);
                yield new Distribution.Uniform(min, max);
            }
            case 3 -> {
                var k = ask("K: ", Integer::parseInt, 1);
                var lambda = ask("Lambda: ", Double::parseDouble, 1.0);
                yield new Distribution.Erlang(k, lambda);
            }
            case 4 -> {
                var probability = ask("Probability: ", Double::parseDouble, 0.0);
                var unavailable = askDistribution("Unavailable distribution");
                yield new Distribution.UnavailableTime(probability, unavailable);
            }
            case 5 -> {
                var mean = ask("Mean: ", Double::parseDouble, 0.0);
                var stdDev = ask("Standard deviation: ", Double::parseDouble, 1.0);
                yield new Distribution.Normal(mean, stdDev);
            }
            case 6 -> {
                var mean = ask("Mean: ", Double::parseDouble, 0.0);
                var stdDev = ask("Standard deviation: ", Double::parseDouble, 1.0);
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
    private static String ask(String ask) {
        return ask(ask, Function.identity(), "");
    }

    /**
     * Ask the user a question.
     * 
     * @param ask          the question to ask
     * @param parser       the parser to use
     * @param defaultValue the default value
     * @return the answer
     */
    private static <T> T ask(String ask, Function<String, T> parser, T defaultValue) {
        System.out.print(ask);
        try {
            var line = System.console().readLine();
            return parser.apply(line);
        } catch (Exception e) {
            System.out.println("Invalid input: " + e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Ask the user to choose an option.
     * 
     * @param ask     the question to ask
     * @param options the options to choose from
     * @return the choice
     */
    private static int choose(String ask, String... options) {
        var builder = new StringBuilder();
        builder.append(ask).append("\n");
        for (int i = 0; i < options.length; i++) {
            builder.append(i + 1).append(". ").append(options[i]).append("\n");
        }
        builder.append("> ");

        var string = builder.toString();
        var choice = 0;
        while (choice < 1 || choice > options.length)
            choice = ask(string, Integer::parseInt, 0);

        return choice;
    }
}
