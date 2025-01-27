package net.berack.upo.valpre;

import java.util.HashMap;
import java.util.Map;

import net.berack.upo.valpre.rand.Distribution;
import net.berack.upo.valpre.sim.SimulationMultiple;
import net.berack.upo.valpre.sim.Net;
import net.berack.upo.valpre.sim.ServerNode;

public class Main {
    public static void main(String[] args) throws Exception {
        // Parameters for the simulation
        String csv = null;
        var runs = 100;
        var seed = 2007539552L;
        var total = 10000;
        var lambda = 1.0 / 4.5;
        var mu = 3.2;
        var sigma = 0.6;

        // Evantually change the parameters
        var arguments = parseParameters(args);
        if (arguments.containsKey("seed"))
            seed = Long.parseLong(arguments.get("seed"));
        if (arguments.containsKey("runs"))
            runs = Integer.parseInt(arguments.get("runs"));
        if (arguments.containsKey("csv"))
            csv = arguments.get("csv");
        var parallel = arguments.containsKey("p");

        // Build the network
        var net = new Net();
        var node1 = ServerNode.createLimitedSource("Source", new Distribution.Exponential(lambda), total);
        var node2 = ServerNode.createQueue("Queue", 1, new Distribution.NormalBoxMuller(mu, sigma));
        net.addNode(node1);
        net.addNode(node2);
        net.addConnection(node1, node2, 1.0);
        net.normalizeWeights();

        /// Run multiple simulations
        // var maxDepartures = new EndSimulationCriteria.MaxDepartures("Queue", total);
        // var maxTime = new EndSimulationCriteria.MaxTime(1000.0);
        var nano = System.nanoTime();
        var sim = new SimulationMultiple(net);
        var results = parallel ? sim.runParallel(seed, runs) : sim.run(seed, runs);
        nano = System.nanoTime() - nano;

        System.out.print(results.average.getHeader());
        System.out.print(results.average.getSummary());
        System.out.println("Final time " + nano / 1e6 + "ms");

        if (csv != null) {
            results.saveCSV(csv);
            System.out.println("Data saved to " + csv);
        }
    }

    public static Map<String, String> parseParameters(String[] args) {
        var arguments = new HashMap<String, Boolean>();
        arguments.put("p", false);
        arguments.put("seed", true);
        arguments.put("runs", true);
        arguments.put("csv", true);

        var param = new Parameters("-", arguments);
        try {
            return param.parse(args);
        } catch (IllegalArgumentException e) {
            var descriptions = new HashMap<String, String>();
            descriptions.put("p", "Add this if you want the simulation to use threads (one each run)");
            descriptions.put("seed", "The seed of the simulation");
            descriptions.put("runs", "How many runs the simulator should run");
            descriptions.put("csv", "The filename for saving every run statistics");

            System.out.println(e.getMessage());
            System.out.println(param.helper(descriptions));
            return null;
        }
    }
}