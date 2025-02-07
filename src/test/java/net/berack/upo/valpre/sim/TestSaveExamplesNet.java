package net.berack.upo.valpre.sim;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.esotericsoftware.kryo.KryoException;

import net.berack.upo.valpre.rand.Distribution;
import net.berack.upo.valpre.rand.Rng;
import net.berack.upo.valpre.sim.stats.NodeStats;

public class TestSaveExamplesNet {

    private static final Distribution exp0_22 = new Distribution.Exponential(1.0 / 4.5);
    private static final Distribution exp2 = new Distribution.Exponential(2.0);
    private static final Distribution exp1_5 = new Distribution.Exponential(1.5);
    private static final Distribution exp3_5 = new Distribution.Exponential(3.5);
    private static final Distribution exp10 = new Distribution.Exponential(10.0);

    private static final Distribution norm3_2 = new Distribution.NormalBoxMuller(3.2, 0.6);
    private static final Distribution norm4_2 = new Distribution.NormalBoxMuller(4.2, 0.6);

    private static final Distribution unNorm = new Distribution.UnavailableTime(0.2, norm4_2);
    private static final Distribution unExp = new Distribution.UnavailableTime(0.1, exp10);

    private static final int spawn = 10000;
    private static final String path = "src/main/resources/example%d.%s";
    private static final String net1 = path.formatted(1, "net");
    private static final String net2 = path.formatted(2, "net");
    private static final String net3 = path.formatted(3, "net");

    @Test
    public void testSaveExample1() throws KryoException, IOException {
        var net = new Net();
        net.addNode(ServerNode.Builder.sourceLimited("Source", spawn, exp0_22));
        net.addNode(ServerNode.Builder.queue("Queue", 1, norm3_2));
        net.addConnection(0, 1, 1.0);

        net.save(net1);
        net = Net.load(net1);

        var sim = new Simulation(net, new Rng());
        var res = sim.run();
        var time = 44782.0;
        var maxErr = time / 1000.0;

        assertEquals(Rng.DEFAULT, res.seed);
        assertEquals(time, res.simulationTime, maxErr);
        testNode(res.nodes.get("Source"), 10000, time, 1.0, 4.5, 0.0, 0.0);
        testNode(res.nodes.get("Queue"), 10000, time, 2.6, 7.2, 4.0, 0.0);
    }

    @Test
    public void testSaveExample2() throws KryoException, IOException {
        var net = new Net();
        net.addNode(ServerNode.Builder.sourceLimited("Source", spawn, exp0_22));
        net.addNode(ServerNode.Builder.queue("Queue", 1, norm3_2));
        net.addNode(ServerNode.Builder.queue("Queue Wait", 1, norm3_2, unNorm));
        net.addConnection(0, 1, 1.0);
        net.addConnection(1, 2, 1.0);

        net.save(net2);
        net = Net.load(net2);

        var sim = new Simulation(net, new Rng());
        var res = sim.run();
        var time = 45417.0;
        var maxErr = time / 1000.0;

        assertEquals(Rng.DEFAULT, res.seed);
        assertEquals(time, res.simulationTime, maxErr);
        testNode(res.nodes.get("Source"), 10000, time, 1.0, 4.5, 0.0, 0.0);
        testNode(res.nodes.get("Queue"), 10000, time, 2.6, 7.2, 4.0, 0.0);
        testNode(res.nodes.get("Queue Wait"), 10000, time, 5.8, 22.3, 19.1, 8497.7);
    }

    @Test
    public void testSaveExample3() throws KryoException, IOException {
        var net = new Net();
        net.addNode(ServerNode.Builder.sourceLimited("Source", spawn, exp1_5));
        net.addNode(ServerNode.Builder.queue("Service1", 1, exp2));
        net.addNode(ServerNode.Builder.queue("Service2", 1, exp3_5, unExp));
        net.addConnection(0, 1, 1.0);
        net.addConnection(1, 2, 1.0);

        net.save(net3);
        net = Net.load(net3);

        var sim = new Simulation(net, new Rng());
        var res = sim.run();
        var time = 6736.0;
        var maxErr = time / 1000.0;

        assertEquals(Rng.DEFAULT, res.seed);
        assertEquals(time, res.simulationTime, maxErr);
        testNode(res.nodes.get("Source"), 10000, time, 1.0, 0.6, 0.0, 0.0);
        testNode(res.nodes.get("Service1"), 10000, time, 3.5, 1.7, 1.2, 0.0);
        testNode(res.nodes.get("Service2"), 10000, time, 1.7, 0.5, 0.22, 102.2);
    }

    private void testNode(NodeStats stat, double numClients, double time, double avgQueue,
            double avgResponse, double avgWait, double totalUnavailable) {
        assertEquals("Num Arrivals", numClients, stat.numArrivals, 0.1);
        assertEquals("Num Departures", numClients, stat.numDepartures, 0.1);

        var maxErr = time / 1000.0;
        assertEquals(time, stat.lastEventTime, maxErr);
        assertEquals("Avg Queue", avgQueue, stat.avgQueueLength, 0.1);
        assertEquals("Avg Wait", avgWait, stat.avgWaitTime, 0.1);
        assertEquals("Avg Response", avgResponse, stat.avgResponse, 0.1);

        var totalWait = numClients * stat.avgWaitTime;
        var totalResponse = numClients * stat.avgResponse;
        var totalBusy = totalResponse - totalWait;

        assertEquals("Tot Wait", totalWait, stat.waitTime, maxErr);
        assertEquals("Tot Response", totalResponse, stat.responseTime, maxErr);
        assertEquals("Tot Busy", totalBusy, stat.busyTime, maxErr);
        assertEquals("Tot Unavailable", totalUnavailable, stat.unavailableTime, maxErr);

        assertEquals("Throughput", stat.numDepartures / stat.lastEventTime, stat.throughput, 0.001);
        assertEquals("% Busy", stat.busyTime / stat.lastEventTime, stat.utilization, 0.001);
        assertEquals("% Unavailable", stat.unavailableTime / stat.lastEventTime, stat.unavailable, 0.001);
    }
}
