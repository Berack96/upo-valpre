package net.berack.upo.valpre;

import net.berack.upo.valpre.rand.Distribution;

public class Main {
    public static void main(String[] args) {
        // Parameters for the simulation
        var seed = System.nanoTime();
        var total = 1000;
        var lambda = 1.0 / 4.5;
        var mu = 3.2;
        var sigma = 0.6;

        // Build the network
        var node1 = ServerNode.createLimitedSource("Source", new Distribution.Exponential(lambda), total);
        var node2 = ServerNode.createQueue("Queue", 1, new Distribution.NormalBoxMuller(mu, sigma));
        node1.addChild(node2, 1.0);

        /// Run the simulation
        var sim = new NetSimulation(seed);
        sim.addNode(node1);
        sim.addNode(node2);

        var maxDepartures = new EndSimulationCriteria.MaxDepartures("Queue", total);
        var maxTime = new EndSimulationCriteria.MaxTime(1000.0);
        var results = sim.run(maxDepartures, maxTime);

        // Display the results
        for (var entry : results.entrySet()) {
            var stats = entry.getValue();
            var size = (int) Math.ceil(Math.max(Math.log10(stats.numArrivals), Math.log10(stats.lastEventTime)));
            var iFormat = "%" + size + "d";
            var fFormat = "%" + (size + 4) + ".3f";

            System.out.println("===== " + entry.getKey() + " =====");
            System.out.printf("  Arrivals:  \t" + iFormat + "\n", stats.numArrivals);
            System.out.printf("  Departures:\t" + iFormat + "\n", stats.numDepartures);
            System.out.printf("  Max Queue: \t" + iFormat + "\n", stats.maxQueueLength);
            System.out.printf("  Response:  \t" + fFormat + "\n", stats.responseTime / stats.numDepartures);
            System.out.printf("  Busy %%:   \t" + fFormat + "\n", stats.busyTime * 100 / stats.lastEventTime);
            System.out.printf("  Last Event:\t" + fFormat + "\n", stats.lastEventTime);
        }
    }
}