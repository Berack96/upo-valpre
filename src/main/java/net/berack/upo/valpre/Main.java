package net.berack.upo.valpre;

import java.io.File;
import java.net.URISyntaxException;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;

public class Main {
    private final static String NAME;

    /**
     * The name of the program, used for the help message.
     */
    static {
        var name = "valpre";
        try {
            var uri = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            name = new File(uri).getName();
        } catch (URISyntaxException e) {
        }
        NAME = name;
    }

    /**
     * The main method of the program. It parses the arguments and runs the
     * simulation or the plotter.
     * 
     * @param args the arguments to parse
     */
    public static void main(String[] args) {
        try {
            var param = Main.getParameters(args);
            var command = param.getString("command");

            switch (command) {
                case "simulation" -> {
                    new SimulationBuilder(param.getString("net"))
                            .setCsv(param.getString("csv"))
                            .setMaxRuns(param.getInt("runs"))
                            .setSeed(param.getLong("seed"))
                            .setParallel(param.getBoolean("p"))
                            .parseEndCriteria(param.getString("end"))
                            .parseConfidenceIndices(param.getString("indices"))
                            .run();
                }
                case "plot" -> {
                    var csv = param.getString("csv");
                    var plot = new Plot(csv);
                    plot.show();
                }
                case "interactive" -> new InteractiveConsole().run();
                default -> throw new RuntimeException("Invalid program!"); // Should never happen
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Parses the arguments of the program. It uses the argparse4j library to parse
     * the arguments and return a Namespace object with the parsed arguments.
     * 
     * @param args the arguments to parse
     * @return a Namespace object with the parsed arguments
     */
    private static Namespace getParameters(String[] args) {
        var parser = ArgumentParsers.newFor(NAME).build()
                .defaultHelp(true)
                .description("Build a network simulation and/or plot the results of a simulation.");
        var subparser = parser.addSubparsers().title("commands").description("valid commands").help("subcommand help");

        var sim = subparser.addParser("simulation").help("Run a simulation of the network.");
        sim.addArgument("-net").help("The file net to use.").required(true);
        sim.addArgument("-csv").help("The filename for saving every run statistics.");
        sim.addArgument("-runs").type(Integer.class).help("How many runs the simulator should run.").setDefault(100);
        sim.addArgument("-seed").type(Long.class).help("The seed of the simulation.").setDefault(0L);
        sim.addArgument("-p").action(Arguments.storeTrue()).help("Parallel (one thread each run).").setDefault(false);
        sim.addArgument("-end").help("When the simulation should end. Format:\n\"[ClassName:param1,..,paramN];[..]\"");
        sim.addArgument("-indices").help("The confidence indices to use for the simulation. If active -p is ignored."
                + " Format:\n\"[node:stat=confidence:relativeError];[..]\"");

        var plot = subparser.addParser("plot").help("Plot the results of a simulation.");
        plot.addArgument("-csv").help("The filename for the csv file to plot.").required(true);

        var _ = subparser.addParser("interactive").help("Run the interactive console.");
        // Interactive console does not need any arguments

        var namespace = parser.parseArgsOrFail(args);
        namespace.getAttrs().put("command", args[0]);
        return namespace;
    }
}