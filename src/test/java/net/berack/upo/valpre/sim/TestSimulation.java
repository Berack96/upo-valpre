package net.berack.upo.valpre.sim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

import net.berack.upo.valpre.rand.Distribution;
import net.berack.upo.valpre.rand.Rng;

public class TestSimulation {

    private static final double DELTA = 0.0000001;
    private static final Distribution const0 = _ -> 0.0;
    private static final Distribution const1 = _ -> 1.0;
    private static final Rng rigged;
    private static final Net simpleNet;
    private static final ServerNode node0;
    private static final ServerNode node1;
    static {
        node0 = ServerNode.createLimitedSource("First", const1, 0);
        node1 = ServerNode.createQueue("Second", 1, const1);

        simpleNet = new Net();
        simpleNet.addNode(node0);
        simpleNet.addNode(node1);
        simpleNet.addConnection(0, 1, 1.0);

        rigged = new Rng() {
            @Override
            public double random() {
                return 0.1;
            }
        };
    }

    @Test
    public void serverNode() {
        var node = ServerNode.createQueue("Nodo", 0, const1);
        assertEquals("Nodo", node.name);
        assertEquals(1, node.maxServers);
        assertFalse(node.shouldSpawnArrival(0));
        assertFalse(node.shouldSpawnArrival(50));
        assertFalse(node.shouldSpawnArrival(1000));
        assertFalse(node.shouldSpawnArrival(Integer.MAX_VALUE));
        assertFalse(node.shouldSpawnArrival(-1));
        assertEquals(1.0, node.getServiceTime(null), DELTA);

        node = ServerNode.createQueue("Queue", 50, const1);
        assertEquals("Queue", node.name);
        assertEquals(50, node.maxServers);
        assertFalse(node.shouldSpawnArrival(0));
        assertFalse(node.shouldSpawnArrival(50));
        assertFalse(node.shouldSpawnArrival(1000));
        assertFalse(node.shouldSpawnArrival(Integer.MAX_VALUE));
        assertFalse(node.shouldSpawnArrival(-1));
        assertEquals(1.0, node.getServiceTime(null), DELTA);

        node = ServerNode.createSource("Source", const1);
        assertEquals("Source", node.name);
        assertEquals(Integer.MAX_VALUE, node.maxServers);
        assertTrue(node.shouldSpawnArrival(0));
        assertTrue(node.shouldSpawnArrival(50));
        assertTrue(node.shouldSpawnArrival(1000));
        assertTrue(node.shouldSpawnArrival(Integer.MAX_VALUE - 1));
        assertFalse(node.shouldSpawnArrival(Integer.MAX_VALUE));
        assertTrue(node.shouldSpawnArrival(-1));
        assertEquals(1.0, node.getServiceTime(null), DELTA);

        node = ServerNode.createLimitedSource("Source", const1, 50);
        assertEquals("Source", node.name);
        assertEquals(Integer.MAX_VALUE, node.maxServers);
        assertTrue(node.shouldSpawnArrival(0));
        assertTrue(node.shouldSpawnArrival(49));
        assertFalse(node.shouldSpawnArrival(50));
        assertFalse(node.shouldSpawnArrival(1000));
        assertFalse(node.shouldSpawnArrival(Integer.MAX_VALUE));
        assertTrue(node.shouldSpawnArrival(-1));
        assertEquals(1.0, node.getServiceTime(null), DELTA);
    }

    @Test
    public void event() {
        var node = ServerNode.createSource("Source", const0);
        var event = Event.newAvailable(node, 1.0);
        assertEquals(node, event.node);
        assertEquals(1.0, event.time, 0.000000000001);
        assertEquals(Event.Type.AVAILABLE, event.type);

        var event2 = Event.newArrival(node, 5.0);
        assertEquals(node, event2.node);
        assertEquals(5.0, event2.time, 0.000000000001);
        assertEquals(Event.Type.ARRIVAL, event2.type);

        var event3 = Event.newDeparture(node, 8.0);
        assertEquals(node, event3.node);
        assertEquals(8.0, event3.time, 0.000000000001);
        assertEquals(Event.Type.DEPARTURE, event3.type);

        assertEquals(0, event2.compareTo(event2));
        assertEquals(1, event2.compareTo(event));
        assertEquals(-1, event2.compareTo(event3));
    }

    @Test
    public void net() {
        var net = new Net();
        assertEquals(0, net.size());

        var node = ServerNode.createSource("First", const0);
        var index = net.addNode(node);
        assertEquals(1, net.size());
        assertEquals(0, index);
        assertEquals(node, net.getNode(0));
        assertEquals(node, net.getNode("First"));
        assertEquals(index, net.getNodeIndex("First"));

        var node1 = ServerNode.createQueue("Second", 1, const0);
        var index1 = net.addNode(node1);
        assertEquals(2, net.size());
        assertEquals(0, index);
        assertEquals(node, net.getNode(0));
        assertEquals(node, net.getNode("First"));
        assertEquals(index, net.getNodeIndex("First"));
        assertEquals(1, index1);
        assertEquals(node1, net.getNode(1));
        assertEquals(node1, net.getNode("Second"));
        assertEquals(index1, net.getNodeIndex("Second"));

        var nodes = new HashSet<ServerNode>();
        nodes.add(node);
        nodes.add(node1);
        net.forEach(n -> assertTrue(nodes.contains(n)));

        net.addConnection(0, 1, 1.0);
        var conn = net.getChildren(0);
        assertEquals(1, conn.size());
        assertEquals(node1, conn.get(0).child);
        assertEquals(1.0, conn.get(0).weight, DELTA);
        conn = net.getChildren(1);
        assertEquals(0, conn.size());

        var node2 = ServerNode.createQueue("Third", 1, const0);
        net.addNode(node2);
        net.addConnection(0, 2, 1.0);
        conn = net.getChildren(0);
        assertEquals(2, conn.size());
        assertEquals(node1, conn.get(0).child);
        assertEquals(node2, conn.get(1).child);
        assertEquals(1.0, conn.get(0).weight, DELTA);
        assertEquals(1.0, conn.get(1).weight, DELTA);
        conn = net.getChildren(1);
        assertEquals(0, conn.size());
        conn = net.getChildren(2);
        assertEquals(0, conn.size());

        net.normalizeWeights();
        conn = net.getChildren(0);
        assertEquals(2, conn.size());
        assertEquals(node1, conn.get(0).child);
        assertEquals(node2, conn.get(1).child);
        assertEquals(0.5, conn.get(0).weight, DELTA);
        assertEquals(0.5, conn.get(1).weight, DELTA);
        conn = net.getChildren(1);
        assertEquals(0, conn.size());
        conn = net.getChildren(2);
        assertEquals(0, conn.size());

        var sample = net.getChildOf(0, rigged);
        assertEquals(node1, sample);
        sample = net.getChildOf(node, rigged);
        assertEquals(node1, sample);
    }

    @Test
    public void criteriaTime() {
        var criteria = new EndCriteria.MaxTime(3.0);

        var sim = new Simulation(simpleNet, rigged);
        assertTrue(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));

        sim.addArrival(node0);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));
        sim.processNextEvent(); // Arrival
        assertEquals(0.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));
        sim.processNextEvent(); // Departure Source
        assertEquals(1.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));

        sim.processNextEvent(); // Arrival Queue
        assertEquals(1.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));
        sim.processNextEvent(); // Departure Queue
        assertEquals(2.0, sim.getTime(), DELTA);
        assertTrue(sim.hasEnded()); // No more events
        assertFalse(criteria.shouldEnd(sim));

        sim.addArrival(node0);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));
        sim.processNextEvent(); // Arrival
        assertEquals(2.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));
        sim.processNextEvent(); // Departure Source
        assertEquals(3.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertTrue(criteria.shouldEnd(sim));
    }

    @Test
    public void criteriaArrivals() {
        var criteria = new EndCriteria.MaxArrivals(node0.name, 2);

        var sim = new Simulation(simpleNet, rigged);
        assertTrue(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));

        sim.addArrival(node0);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));

        sim.processNextEvent(); // Arrival
        assertEquals(0.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));
        sim.processNextEvent(); // Departure Source
        assertEquals(1.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));

        sim.processNextEvent(); // Arrival Queue
        assertEquals(1.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));
        sim.processNextEvent(); // Departure Queue
        assertEquals(2.0, sim.getTime(), DELTA);
        assertTrue(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));

        sim.addArrival(node0);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));
        sim.processNextEvent(); // Arrival
        assertEquals(2.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertTrue(criteria.shouldEnd(sim));
    }

    @Test
    public void criteriaDeparture() {
        var criteria = new EndCriteria.MaxDepartures(node0.name, 2);

        var sim = new Simulation(simpleNet, rigged);
        assertTrue(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));

        sim.addArrival(node0);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));

        sim.processNextEvent(); // Arrival
        assertEquals(0.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));
        sim.processNextEvent(); // Departure Source
        assertEquals(1.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));

        sim.processNextEvent(); // Arrival Queue
        assertEquals(1.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));
        sim.processNextEvent(); // Departure Queue
        assertEquals(2.0, sim.getTime(), DELTA);
        assertTrue(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));

        sim.addArrival(node0);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));
        sim.processNextEvent(); // Arrival
        assertEquals(2.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));
        sim.processNextEvent(); // Departure Source
        assertEquals(3.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertTrue(criteria.shouldEnd(sim));
    }

    @Test
    public void simulation() {
        var start = System.nanoTime();
        var sim = new Simulation(simpleNet, rigged);
        // knowing that it takes time to allocate the object
        // we can use the average time
        var endAllocation = System.nanoTime();
        var time = (endAllocation + start) / 2;
        var diff = 0.5e-6 * (endAllocation - start); // getting the error margin in ms

        assertTrue(sim.hasEnded());
        assertEquals(0, sim.getEventsProcessed());
        assertEquals(0.0, sim.getTime(), DELTA);
        assertEquals(node0, sim.getNode(node0.name));
        assertEquals(node1, sim.getNode(node1.name));
        assertEquals(0, sim.getNodeState(node0.name).numServerBusy);
        assertEquals(0, sim.getNodeState(node0.name).numServerUnavailable);
        assertEquals(0, sim.getNodeState(node1.name).numServerBusy);
        assertEquals(0, sim.getNodeState(node1.name).numServerUnavailable);
        var fel = sim.getFutureEventList();
        assertEquals(0, fel.size());

        sim.addArrival(node0);
        assertFalse(sim.hasEnded());
        assertEquals(0, sim.getEventsProcessed());
        assertEquals(0.0, sim.getTime(), DELTA);
        assertEquals(node0, sim.getNode(node0.name));
        assertEquals(node1, sim.getNode(node1.name));
        assertEquals(0, sim.getNodeState(node0.name).numServerBusy);
        assertEquals(0, sim.getNodeState(node0.name).numServerUnavailable);
        assertEquals(0, sim.getNodeState(node1.name).numServerBusy);
        assertEquals(0, sim.getNodeState(node1.name).numServerUnavailable);
        fel = sim.getFutureEventList();
        assertEquals(1, fel.size());
        assertEquals(Event.Type.ARRIVAL, fel.get(0).type);
        assertEquals(node0, fel.get(0).node);
        assertEquals(0.0, fel.get(0).time, DELTA);

        sim.processNextEvent(); // Arrival
        assertEquals(0.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertEquals(1, sim.getEventsProcessed());
        assertEquals(node0, sim.getNode(node0.name));
        assertEquals(node1, sim.getNode(node1.name));
        assertEquals(1, sim.getNodeState(node0.name).numServerBusy);
        assertEquals(0, sim.getNodeState(node0.name).numServerUnavailable);
        assertEquals(0, sim.getNodeState(node1.name).numServerBusy);
        assertEquals(0, sim.getNodeState(node1.name).numServerUnavailable);
        fel = sim.getFutureEventList();
        assertEquals(1, fel.size());
        assertEquals(Event.Type.DEPARTURE, fel.get(0).type);
        assertEquals(node0, fel.get(0).node);
        assertEquals(1.0, fel.get(0).time, DELTA);

        sim.processNextEvent(); // Departure Source
        assertEquals(1.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertEquals(2, sim.getEventsProcessed());
        assertEquals(node0, sim.getNode(node0.name));
        assertEquals(node1, sim.getNode(node1.name));
        assertEquals(0, sim.getNodeState(node0.name).numServerBusy);
        assertEquals(0, sim.getNodeState(node0.name).numServerUnavailable);
        assertEquals(0, sim.getNodeState(node1.name).numServerBusy);
        assertEquals(0, sim.getNodeState(node1.name).numServerUnavailable);
        fel = sim.getFutureEventList();
        assertEquals(1, fel.size());
        assertEquals(Event.Type.ARRIVAL, fel.get(0).type);
        assertEquals(node1, fel.get(0).node);
        assertEquals(1.0, fel.get(0).time, DELTA);

        sim.processNextEvent(); // Arrival Queue
        assertEquals(1.0, sim.getTime(), DELTA);
        assertFalse(sim.hasEnded());
        assertEquals(3, sim.getEventsProcessed());
        assertEquals(node0, sim.getNode(node0.name));
        assertEquals(node1, sim.getNode(node1.name));
        assertEquals(0, sim.getNodeState(node0.name).numServerBusy);
        assertEquals(0, sim.getNodeState(node0.name).numServerUnavailable);
        assertEquals(1, sim.getNodeState(node1.name).numServerBusy);
        assertEquals(0, sim.getNodeState(node1.name).numServerUnavailable);
        fel = sim.getFutureEventList();
        assertEquals(1, fel.size());
        assertEquals(Event.Type.DEPARTURE, fel.get(0).type);
        assertEquals(node1, fel.get(0).node);
        assertEquals(2.0, fel.get(0).time, DELTA);

        sim.processNextEvent(); // Departure Queue
        assertEquals(2.0, sim.getTime(), DELTA);
        assertTrue(sim.hasEnded());
        assertEquals(4, sim.getEventsProcessed());
        assertEquals(node0, sim.getNode(node0.name));
        assertEquals(node1, sim.getNode(node1.name));
        assertEquals(0, sim.getNodeState(node0.name).numServerBusy);
        assertEquals(0, sim.getNodeState(node0.name).numServerUnavailable);
        assertEquals(0, sim.getNodeState(node1.name).numServerBusy);
        assertEquals(0, sim.getNodeState(node1.name).numServerUnavailable);
        fel = sim.getFutureEventList();
        assertEquals(0, fel.size());

        var elapsed = (double) (System.nanoTime() - time);
        var result = sim.endSimulation();
        assertEquals(2.0, result.simulationTime, DELTA);
        assertEquals(sim.seed, result.seed);
        assertEquals(elapsed * 1e-6, result.timeElapsedMS * 1e-6, diff);
        assertEquals(2, result.nodes.size());
        assertEquals(1, result.nodes.get(node0.name).numArrivals, DELTA);
        assertEquals(1, result.nodes.get(node0.name).numDepartures, DELTA);
        assertEquals(1, result.nodes.get(node1.name).numArrivals, DELTA);
        assertEquals(1, result.nodes.get(node1.name).numDepartures, DELTA);
    }

    @Test
    public void endSim() {
        var criteria = new EndCriteria.MaxDepartures(node0.name, 5);
        var sim = new Simulation(simpleNet, rigged, criteria);
        sim.addArrival(node0);
        sim.addArrival(node0);
        sim.addArrival(node0);
        sim.addArrival(node0);
        sim.addArrival(node0);
        sim.addArrival(node0);

        while (!criteria.shouldEnd(sim)) {
            sim.processNextEvent();
        }

        assertTrue(sim.hasEnded());
        var res = sim.endSimulation();

        assertEquals(6, res.nodes.get(node0.name).numArrivals, DELTA);
        assertEquals(5, res.nodes.get(node0.name).numDepartures, DELTA);
        assertEquals(4, res.nodes.get(node1.name).numArrivals, DELTA);
        assertEquals(0, res.nodes.get(node1.name).numDepartures, DELTA);
    }

    @Test
    public void simulationStats() {
        var net = new Net();
        net.addNode(ServerNode.createLimitedSource("Source", const1, 50));

        var sim = new Simulation(net, rigged);
        var result = sim.run();
        var nodeStat = result.nodes.get("Source");
        assertEquals(50, nodeStat.numArrivals, DELTA);
        assertEquals(50, nodeStat.numDepartures, DELTA);
        assertEquals(1.0, nodeStat.avgQueueLength, DELTA);
        assertEquals(1.0, nodeStat.avgResponse, DELTA);
        assertEquals(0.0, nodeStat.avgWaitTime, DELTA);
        assertEquals(1.0, nodeStat.maxQueueLength, DELTA);
        assertEquals(50.0, nodeStat.busyTime, DELTA);
        assertEquals(result.simulationTime, nodeStat.lastEventTime, DELTA);
        assertEquals(1.0, nodeStat.troughput, DELTA);
        assertEquals(1.0, nodeStat.utilization, DELTA);
        assertEquals(0.0, nodeStat.unavailable, DELTA);

        net.addNode(ServerNode.createQueue("Queue", 1, const1));
        net.addConnection(0, 1, 1.0);

        sim = new Simulation(net, rigged);
        result = sim.run();
        nodeStat = result.nodes.get("Source");
        assertEquals(50, nodeStat.numArrivals, DELTA);
        assertEquals(50, nodeStat.numDepartures, DELTA);
        assertEquals(1.0, nodeStat.avgQueueLength, DELTA);
        assertEquals(1.0, nodeStat.avgResponse, DELTA);
        assertEquals(0.0, nodeStat.avgWaitTime, DELTA);
        assertEquals(1.0, nodeStat.maxQueueLength, DELTA);
        assertEquals(50.0, nodeStat.busyTime, DELTA);
        assertEquals(result.simulationTime - 1, nodeStat.lastEventTime, DELTA);
        assertEquals(1.0, nodeStat.troughput, DELTA);
        assertEquals(1.0, nodeStat.utilization, DELTA);
        assertEquals(0.0, nodeStat.unavailable, DELTA);
        nodeStat = result.nodes.get("Queue");
        assertEquals(50, nodeStat.numArrivals, DELTA);
        assertEquals(50, nodeStat.numDepartures, DELTA);
        assertEquals(1.0, nodeStat.avgQueueLength, DELTA);
        assertEquals(1.0, nodeStat.avgResponse, DELTA);
        assertEquals(0.0, nodeStat.avgWaitTime, DELTA);
        assertEquals(1.0, nodeStat.maxQueueLength, DELTA);
        assertEquals(50.0, nodeStat.busyTime, DELTA);
        assertEquals(result.simulationTime, nodeStat.lastEventTime, DELTA);

        assertEquals(nodeStat.busyTime / nodeStat.lastEventTime, nodeStat.utilization, DELTA);
        assertEquals(nodeStat.numDepartures / nodeStat.lastEventTime, nodeStat.troughput, DELTA);
        assertEquals(0.0, nodeStat.unavailable, DELTA);
    }
}
