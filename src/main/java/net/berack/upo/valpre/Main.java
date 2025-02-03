package net.berack.upo.valpre;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;

import net.berack.upo.valpre.sim.EndCriteria;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0)
            exit("No program specified!");

        try {
            var program = args[0];
            var subArgs = Arrays.copyOfRange(args, 1, args.length);
            var param = Main.getParameters(program, subArgs);
            switch (program) {
                case "simulation" -> {
                    new SimulationBuilder(param.get("net"))
                            .setCsv(param.get("csv"))
                            .setRuns(param.getOrDefault("runs", Integer::parseInt, 100))
                            .setSeed(param.getOrDefault("seed", Long::parseLong, 2007539552L))
                            .setParallel(param.get("p") != null)
                            .setEndCriteria(EndCriteria.parse(param.get("end")))
                            .run();
                }
                case "plot" -> {
                    var csv = param.get("csv");
                    var plot = new Plot(csv);
                    plot.show();
                }
                case "net" -> {
                    var net = new NetBuilderInteractive();
                    net.run();
                }
                default -> exit("Invalid program!");
            }
        } catch (Exception e) {
            exit(e.getMessage());
        }
    }

    /**
     * Get the parameters from the arguments.
     * 
     * @param program the program to run
     * @param args    the arguments to parse
     * @return the parameters
     */
    private static Parameters getParameters(String program, String[] args) {
        var arguments = new HashMap<String, Boolean>();
        arguments.put("p", false);
        arguments.put("seed", true);
        arguments.put("runs", true);
        arguments.put("net", true);
        arguments.put("end", true);
        arguments.put("csv", true);

        var descriptions = new HashMap<String, String>();
        descriptions.put("p", "Add this if you want the simulation to use threads (one each run).");
        descriptions.put("seed", "The seed of the simulation.");
        descriptions.put("runs", "How many runs the simulator should run.");
        descriptions.put("end", "When the simulation should end. Format is [ClassName:param1,..,paramN];[..]");
        descriptions.put("net", "The file net to use. Use example1.net or example2.net for the provided ones.");

        var csvDesc = switch (program) {
            case "simulation" -> "The filename for saving every run statistics.";
            case "plot" -> "The filename that contains the previous saved runs.";
            default -> "";
        };
        descriptions.put("csv", csvDesc);

        if (program.equals("net"))
            return null;
        return Parameters.getArgsOrHelper(args, "-", arguments, descriptions);
    }

    /**
     * Exit the program with an error message.
     */
    public static void exit(String message) {
        try {
            var uri = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            var name = new File(uri).getName();
            System.out.println(message);
            System.out.println("Usage: java -jar " + name + ".jar [simulation|plot|net] [args]");
            System.out.println("simulation args: -net <net> -csv <csv> [-runs <runs>] [-seed <seed>] [-p]");
            System.out.println("plot args: -csv <csv>");
            System.out.println("net args: none");
            System.exit(1);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}