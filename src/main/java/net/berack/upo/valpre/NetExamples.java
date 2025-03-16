package net.berack.upo.valpre;

import java.util.concurrent.ExecutionException;

import net.berack.upo.valpre.rand.Distribution;
import net.berack.upo.valpre.sim.Net;
import net.berack.upo.valpre.sim.ServerNode;
import net.berack.upo.valpre.sim.SimulationMultiple;
import net.berack.upo.valpre.sim.stats.Result;

/**
 * This class provides two example networks.
 * The first network is composed of a terminal node and a queue node.
 * The second network is composed of a terminal node and two queue nodes.
 */
public final class NetExamples {

    /**
     * Main method to test the example networks.
     * It runs the fist network and prints the results.
     * The network will have the distribution changed but the mean will be the same.
     * 
     * @param args not needed
     * @throws ExecutionException   if the execution fails
     * @throws InterruptedException if the execution is interrupted
     */
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        var avg1 = 3.2;
        var seed = 0l;

        var nets = new Net[] {
                getNet1("Normal", new Distribution.NormalBoxMuller(avg1, 0.6)),
                getNet1("Exponential", new Distribution.Exponential(1 / avg1)),
                getNet1("Erlang", new Distribution.Erlang(5, 5 / avg1)),
                getNet1("Uniform", new Distribution.Uniform(avg1 - 1, avg1 + 1))
        };

        for (var net : nets) {
            var summary = new SimulationMultiple(net).runParallel(seed, 1000);
            var table = Result.getResultString(summary.getNodes(), summary.getStats());
            System.out.println(table);
        }
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
        return getNet1("Queue", norm3_2);
    }

    /**
     * Return the first example network.
     * The net is composed of a terminal node and a queue node.
     * The terminal node is connected to the queue node.
     * The terminal node generates 10000 jobs with an exponential distribution 4.5.
     * 
     * @param queue the distribution of the queue node
     * @return the first example network
     */
    public static Net getNet1(String name, Distribution queue) {
        var spawn = 10000;
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
        var exp1_5 = new Distribution.Exponential(1.5);
        var exp2 = new Distribution.Exponential(2.0);
        var exp3_5 = new Distribution.Exponential(3.5);
        var exp10 = new Distribution.Exponential(10.0);
        var unExp = new Distribution.UnavailableTime(0.1, exp10);
        var spawn = 10000;
        return getNet2(spawn, exp1_5, exp2, exp3_5, unExp);
    }

    /**
     * Return the second example network.
     * The net is composed of a terminal node and two queue nodes.
     * The terminal node is connected to the first queue node.
     * The first queue node is connected to the second queue node.
     * 
     * @param spawn    the number of jobs to generate
     * @param source   the distribution of the source node
     * @param service1 the distribution of the first queue node
     * @param service2 the distribution of the second queue node
     * @param unExp    the distribution of the unavailable time
     * @return the second example network
     */
    public static Net getNet2(int spawn, Distribution source, Distribution service1, Distribution service2,
            Distribution unExp) {
        var net3 = new Net();
        net3.addNode(ServerNode.Builder.terminal("Source", spawn, source));
        net3.addNode(ServerNode.Builder.queue("Service1", 1, service1));
        net3.addNode(ServerNode.Builder.queue("Service2", 1, service2, unExp));
        net3.addConnection(0, 1, 1.0);
        net3.addConnection(1, 2, 1.0);
        return net3;
    }
}
