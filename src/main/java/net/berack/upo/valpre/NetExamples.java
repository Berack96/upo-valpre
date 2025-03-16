package net.berack.upo.valpre;

import net.berack.upo.valpre.rand.Distribution;
import net.berack.upo.valpre.sim.Net;
import net.berack.upo.valpre.sim.ServerNode;

/**
 * This class provides two example networks.
 * The first network is composed of a terminal node and a queue node.
 * The second network is composed of a terminal node and two queue nodes.
 */
public final class NetExamples {
    /**
     * Return the first example network.
     * The net is composed of a terminal node and a queue node.
     * The terminal node generates 10000 jobs with an exponential distribution 4.5.
     * The queue node has a capacity of 1 and a service time of 3.2 with a standard
     * deviation of 0.6.
     * The terminal node is connected to the queue node with a probability of 1.0.
     * 
     * @return the first example network
     */
    public static Net getNet1() {
        var exp0_22 = new Distribution.Exponential(1.0 / 4.5);
        var norm3_2 = new Distribution.NormalBoxMuller(3.2, 0.6);
        var spawn = 10000;
        return getNet1(spawn, exp0_22, norm3_2);
    }

    /**
     * Return the first example network.
     * The net is composed of a terminal node and a queue node.
     * The terminal node is connected to the queue node.
     * 
     * @param spawn  the number of jobs to generate
     * @param source the distribution of the source node
     * @param queue  the distribution of the queue node
     * @return the first example network
     */
    public static Net getNet1(int spawn, Distribution source, Distribution queue) {
        var net1 = new Net();
        net1.addNode(ServerNode.Builder.terminal("Source", spawn, source));
        net1.addNode(ServerNode.Builder.queue("Queue", 1, queue));
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
