package net.berack.upo.valpre;

import net.berack.upo.valpre.rand.Distribution;

public class Main {
    public static void main(String[] args) throws Exception {
        // Parameters for the simulation
        var seed = System.nanoTime();
        var total = 100000;
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
        var results = sim.runParallel(seed, 100);
        results.runs[80].printSummary();
    }
}