package net.berack.upo.valpre;

import java.util.List;
import java.util.function.BiFunction;

import net.berack.upo.valpre.rand.Distribution;
import net.berack.upo.valpre.sim.Net;
import net.berack.upo.valpre.sim.ServerNode;
import net.berack.upo.valpre.sim.SimulationMultiple;
import net.berack.upo.valpre.sim.stats.CsvResult;
import net.berack.upo.valpre.sim.stats.Result;

/**
 * This class provides two example networks.
 * The first network is composed of a terminal node and a queue node.
 * The second network is composed of a terminal node and two queue nodes.
 */
public final class NetExamples {

    /**
     * Main method to test the networks.
     * The first network will have the distribution changed but the mean will be the
     * same. The second network will have the distribution changed but the mean will
     * be the same. The results will be saved to a csv file.
     * 
     * @param args not needed
     * @throws Exception if the simulation fails or the file is not saved
     */
    public static void main(String[] args) throws Exception {
        var seed = 123456789L;
        runNet(seed, 3.2, 1, "net1.csv", (spawn, dist) -> {
            var name = dist.getClass().getSimpleName() + "_" + spawn;
            return NetExamples.getNet1(spawn, name, dist);
        });
        runNet(seed, 1 / 3.5, 2, "net2.csv", (spawn, dist) -> {
            var name = dist.getClass().getSimpleName() + "_" + spawn;
            return NetExamples.getNet2(spawn, name, dist);
        });
    }

    /**
     * Method to test whatever network you input.
     * The network will have the distribution changed but the mean will be the same.
     * The bifunction requested is to get the network you want to test passing the
     * spawn and the distribution with the same mean.
     * The network will be tested with spawn totals of 1, 2, 5, 7, 10, 25, 50, 75,
     * 100, 250, 500, 750, 1000, 1500, 2000.
     * The results will be saved to a csv passed as argument.
     * 
     * @param seed        the seed for the simulation
     * @param avg         the mean of the distribution
     * @param nodeToWatch the node to watch
     * @param csv         the file to save the results
     * @param getNet      the bifunction to get the network
     * @throws Exception if the simulation fails or the file is not saved
     */
    public static void runNet(long seed, double avg, int nodeToWatch, String csv,
            BiFunction<Integer, Distribution, Net> getNet) throws Exception {
        var build = new Result.Builder().seed(seed);
        var spawnTotals = new int[] { 1, 2, 5, 7, 10, 25, 50, 75, 100, 250, 500, 750, 1000, 1500, 2000 };

        var normal = new Distribution.NormalBoxMuller(avg, 0.6);
        var exponential = new Distribution.Exponential(1 / avg);
        var erlang = new Distribution.Erlang(5, 5 / avg);
        var uniform = new Distribution.Uniform(avg - (avg * 0.1), avg + (avg * 0.1));
        var hyper = new Distribution.HyperExponential(
                new double[] { 1 / (avg * 0.5), 1 / (avg * 1.5) },
                new double[] { 0.5f, 0.5f });

        System.out.println("Normal: " + normal.mean);
        System.out.println("Uniform: " + uniform.min + " - " + uniform.max);

        for (var spawn : spawnTotals) {
            System.out.println("Spawn: " + spawn);
            var nets = new Net[] {
                    getNet.apply(spawn, normal),
                    getNet.apply(spawn, exponential),
                    getNet.apply(spawn, erlang),
                    getNet.apply(spawn, uniform),
                    getNet.apply(spawn, hyper),
            };

            for (var net : nets) {
                var summary = new SimulationMultiple(net).runParallel(build.seed, 1000);
                var name = net.getNode(nodeToWatch).name;
                var stat = summary.getSummaryOf(name).average;
                build.addNode(name, stat);
            }
        }

        var result = build.build();
        new CsvResult(csv).saveResults(List.of(result));
        System.out.println("Results saved to " + csv);
    }

    /**
     * Return the first example network.
     * The net is composed of a terminal node and a queue node.
     * The terminal node generates 10000 jobs with an exponential distribution 4.5.
     * The queue node has a capacity of 1 and a service time of 3.2 with a standard
     * deviation of 0.6.
     * 
     * @return the first example network
     */
    public static Net getNet1() {
        var norm3_2 = new Distribution.NormalBoxMuller(3.2, 0.6);
        return getNet1(10000, "Queue", norm3_2);
    }

    /**
     * Return the first example network.
     * The net is composed of a terminal node and a queue node.
     * The terminal node is connected to the queue node.
     * The terminal node generates N jobs with an exponential distribution 4.5.
     * 
     * @param spawn the number of jobs to generate
     * @param name  the name of the queue node
     * @param queue the distribution of the queue node
     * @return the first example network
     */
    public static Net getNet1(int spawn, String name, Distribution queue) {
        var source = new Distribution.Exponential(1.0 / 4.5);

        var net1 = new Net();
        net1.addNode(ServerNode.Builder.terminal("Source", spawn, source));
        net1.addNode(ServerNode.Builder.queue(name, 1, queue));
        net1.addConnection(0, 1, 1.0);
        return net1;
    }

    /**
     * Return the second example network.
     * The net is composed of a terminal node and two queue nodes.
     * The terminal node generates 10000 jobs with an exponential distribution 1.5.
     * The first queue node has a capacity of 1 and a service time of 2.0.
     * The second queue node has a capacity of 1 and a service time of 3.5.
     * The second queue node has an unavailable time of 0.1 with an exponential
     * distribution of 10.0.
     * The terminal node is connected to the first queue node.
     * The first queue node is connected to the second queue node.
     * 
     * @return the second example network
     */
    public static Net getNet2() {
        var exp3_5 = new Distribution.Exponential(3.5);
        return getNet2(10000, "Service2", exp3_5);
    }

    /**
     * Return the second example network.
     * The net is composed of a terminal node and two queue nodes.
     * The terminal node is connected to the first queue node.
     * The first queue node is connected to the second queue node.
     * 
     * @param spawn    the number of jobs to generate
     * @param name     the name of the second queue node
     * @param service2 the distribution of the second queue node
     * @return the second example network
     */
    public static Net getNet2(int spawn, String name, Distribution service2) {
        var exp1_5 = new Distribution.Exponential(1.5);
        var exp2 = new Distribution.Exponential(2.0);
        var exp10 = new Distribution.Exponential(10.0);
        var unExp = new Distribution.UnavailableTime(0.1, exp10);

        var net3 = new Net();
        net3.addNode(ServerNode.Builder.terminal("Source", spawn, exp1_5));
        net3.addNode(ServerNode.Builder.queue("Service", 1, exp2));
        net3.addNode(ServerNode.Builder.queue(name, 1, service2, unExp));
        net3.addConnection(0, 1, 1.0);
        net3.addConnection(1, 2, 1.0);
        return net3;
    }
}
