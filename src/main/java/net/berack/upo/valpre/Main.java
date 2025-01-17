package net.berack.upo.valpre;

import net.berack.upo.valpre.rand.Distribution;

public class Main {
    public static void main(String[] args) {
        // Build the network
        var node1 = ServerNode.createSource("Source", new Distribution.Exponential(1.0 / 4.5));
        var node2 = ServerNode.createQueue("Queue", 1, new Distribution.NormalBoxMuller(3.2, 0.6));
        node1.addChild(node2, 1.0);

        /// Run the simulation
        var sim = new NetSimulation(System.nanoTime());
        sim.addNode(node1);
        sim.addNode(node2);
        var results = sim.run(1000, "Queue");

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