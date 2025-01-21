package net.berack.upo.valpre;

import net.berack.upo.valpre.rand.Distribution;

public class Main {
    public static void main(String[] args) throws Exception {
        // Parameters for the simulation
        var seed = 2007539552;
        var total = 10000;
        var lambda = 1.0 / 4.5;
        var mu = 3.2;
        var sigma = 0.6;

        // Build the network
        var node1 = ServerNode.createLimitedSource("Source", new Distribution.Exponential(lambda), total);
        var node2 = ServerNode.createQueue("Queue", 1, new Distribution.NormalBoxMuller(mu, sigma));
        node1.addChild(node2, 1.0);

        /// Run the simulation
        var sim = new NetSimulation();
        sim.addNode(node1);
        sim.addNode(node2);

        // var maxDepartures = new EndSimulationCriteria.MaxDepartures("Queue", total);
        // var maxTime = new EndSimulationCriteria.MaxTime(1000.0);
        var nano = System.nanoTime();
        var results = sim.runParallel(seed, 1000);
        nano = System.nanoTime() - nano;

        System.out.print(results.average.getHeader());
        System.out.print(results.average.getSummaryAsTable());
        System.out.println("Final time " + nano / 1e6 + "ms");
    }
}