package net.berack.upo.valpre.sim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        node0 = ServerNode.Builder.terminal("First", 0, const1);
        node1 = ServerNode.Builder.queue("Second", 1, const1);

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
        var node = ServerNode.Builder.queue("Nodo", 0, const1);
        assertEquals("Nodo", node.name);
        assertEquals(1, node.maxServers);
        assertEquals(0, node.spawnArrivals);
        assertEquals(1.0, node.getServiceTime(null), DELTA);

        node = ServerNode.Builder.queue("Queue", 50, const1);
        assertEquals("Queue", node.name);
        assertEquals(50, node.maxServers);
        assertEquals(0, node.spawnArrivals);
        assertEquals(1.0, node.getServiceTime(null), DELTA);

        node = ServerNode.Builder.source("Source", const1);
        assertEquals("Source", node.name);
        assertEquals(1, node.maxServers);
        assertEquals(-1, node.spawnArrivals);
        assertEquals(1.0, node.getServiceTime(null), DELTA);

        node = ServerNode.Builder.terminal("Source", 50, const1);
        assertEquals("Source", node.name);
        assertEquals(1, node.maxServers);
        assertEquals(50, node.spawnArrivals);
        assertEquals(1.0, node.getServiceTime(null), DELTA);
    }

    @Test
    public void event() {
        var event = Event.newAvailable(0, 1.0);
        assertEquals(0, event.nodeIndex);
        assertEquals(1.0, event.time, 0.000000000001);
        assertEquals(Event.Type.AVAILABLE, event.type);

        var event2 = Event.newArrival(0, 5.0);
        assertEquals(0, event2.nodeIndex);
        assertEquals(5.0, event2.time, 0.000000000001);
        assertEquals(Event.Type.ARRIVAL, event2.type);

        var event3 = Event.newDeparture(1, 8.0);
        assertEquals(1, event3.nodeIndex);
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

        var node = ServerNode.Builder.source("First", const0);
        var index = net.addNode(node);
        assertEquals(1, net.size());
        assertEquals(0, index);
        assertEquals(node, net.getNode(0));
        assertEquals(node, net.getNode("First"));
        assertEquals(index, net.getNodeIndex("First"));

        var node1 = ServerNode.Builder.queue("Second", 1, const0);
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
        assertEquals(1, conn.get(0).index);
        assertEquals(1.0, conn.get(0).weight, DELTA);
        conn = net.getChildren(1);
        assertEquals(0, conn.size());

        var node2 = ServerNode.Builder.queue("Third", 1, const0);
        net.addNode(node2);
        net.addConnection(0, 2, 1.0);
        conn = net.getChildren(0);
        assertEquals(2, conn.size());
        assertEquals(1, conn.get(0).index);
        assertEquals(2, conn.get(1).index);
        assertEquals(1.0, conn.get(0).weight, DELTA);
        assertEquals(1.0, conn.get(1).weight, DELTA);
        conn = net.getChildren(1);
        assertEquals(0, conn.size());
        conn = net.getChildren(2);
        assertEquals(0, conn.size());

        net.normalizeWeights();
        conn = net.getChildren(0);
        assertEquals(2, conn.size());
        assertEquals(1, conn.get(0).index);
        assertEquals(2, conn.get(1).index);
        assertEquals(0.5, conn.get(0).weight, DELTA);
        assertEquals(0.5, conn.get(1).weight, DELTA);
        conn = net.getChildren(1);
        assertEquals(0, conn.size());
        conn = net.getChildren(2);
        assertEquals(0, conn.size());
    }

    @Test
    public void nodeState() {
        var state = new ServerNodeState(1, simpleNet);

        assertEquals(1, state.index);
        assertEquals(node1, state.node);
        assertEquals(0, state.numServerBusy);
        assertEquals(0, state.numServerUnavailable);
        assertEquals(0, state.queue.size());
        assertFalse(state.isQueueFull());
        assertTrue(state.canServe());
        assertFalse(state.hasRequests());
        assertFalse(state.shouldSpawnArrival());

        state.numServerBusy = 1;
        assertEquals(1, state.numServerBusy);
        assertFalse(state.canServe());
        assertFalse(state.hasRequests());

        state.numServerBusy = 0;
        state.numServerUnavailable = 1;
        assertEquals(1, state.numServerUnavailable);
        assertFalse(state.canServe());
        assertFalse(state.hasRequests());

        state.queue.add(1.0);
        assertEquals(1, state.queue.size());
        assertTrue(state.hasRequests());
        assertFalse(state.isQueueFull());

        state.numServerUnavailable = 0;
        state.numServerBusy = 0;
        assertTrue(state.canServe());
        assertTrue(state.hasRequests());
        state.numServerBusy = 1;
        state.queue.poll();
        assertEquals(0, state.queue.size());
        assertFalse(state.hasRequests());
    }

    @Test
    public void nodeStatsUpdates() {
        var net = new Net();
        net.addNode(ServerNode.Builder.terminal("Source", 50, const1));
        net.addNode(node1);
        net.addConnection(0, 1, 1.0);

        var state = new ServerNodeState(0, net);

        var event = state.spawnArrivalIfPossilbe(0);
        assertNotNull(event);
        assertEquals(0, state.stats.numArrivals, DELTA);
        assertEquals(0, state.stats.numDepartures, DELTA);
        assertEquals(0, state.numServerBusy);
        assertEquals(0, state.numServerUnavailable);
        assertEquals(Event.Type.ARRIVAL, event.type);
        assertEquals(0, event.nodeIndex);
        state.updateArrival(event.time);
        assertEquals(1, state.stats.numArrivals, DELTA);
        assertEquals(0, state.numServerBusy);

        event = state.spawnDepartureIfPossible(event.time, rigged);
        assertNotNull(event);
        assertEquals(1, state.stats.numArrivals, DELTA);
        assertEquals(0, state.stats.numDepartures, DELTA);
        assertEquals(1, state.numServerBusy);
        assertEquals(0, state.numServerUnavailable);
        assertEquals(Event.Type.DEPARTURE, event.type);
        assertEquals(0, event.nodeIndex);
        state.updateDeparture(event.time);
        assertEquals(1, state.stats.numArrivals, DELTA);
        assertEquals(1, state.stats.numDepartures, DELTA);
        assertEquals(0, state.numServerBusy);
        assertEquals(0, state.numServerUnavailable);

        state = new ServerNodeState(1, net);
        event = state.spawnArrivalIfPossilbe(0);
        assertNull(event);
        assertEquals(0, state.stats.numArrivals, DELTA);
        assertEquals(0, state.stats.numDepartures, DELTA);
        assertEquals(0, state.numServerBusy);
        assertEquals(0, state.numServerUnavailable);
        state.updateArrival(0);
        assertEquals(1, state.stats.numArrivals, DELTA);
        assertEquals(0, state.numServerBusy);

        event = state.spawnDepartureIfPossible(0, rigged);
        assertNotNull(event);
        assertEquals(1, state.stats.numArrivals, DELTA);
        assertEquals(0, state.stats.numDepartures, DELTA);
        assertEquals(1, state.numServerBusy);
        assertEquals(0, state.numServerUnavailable);
        assertEquals(Event.Type.DEPARTURE, event.type);
        assertEquals(1, event.nodeIndex);
        state.updateDeparture(event.time);
        assertEquals(1, state.stats.numArrivals, DELTA);
        assertEquals(1, state.stats.numDepartures, DELTA);
        assertEquals(0, state.numServerBusy);
        assertEquals(0, state.numServerUnavailable);

        event = state.spawnUnavailableIfPossible(0, rigged);
        assertNull(event);

        state = new ServerNodeState(0, net);
        event = state.spawnArrivalToChild(0, rigged);
        assertNotNull(event);
        assertEquals(0, state.stats.numArrivals, DELTA);
        assertEquals(0, state.stats.numDepartures, DELTA);
        assertEquals(0, state.numServerBusy);
        assertEquals(0, state.numServerUnavailable);
        assertEquals(Event.Type.ARRIVAL, event.type);
        assertEquals(1, event.nodeIndex);
    }

    @Test
    public void criteriaTime() {
        var criteria = new EndCriteria.MaxTime(3.0);

        var sim = new Simulation(simpleNet, rigged);
        assertTrue(sim.hasEnded());
        assertFalse(criteria.shouldEnd(sim));

        sim.addToFel(Event.newArrival(0, sim.getTime()));
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

        sim.addToFel(Event.newArrival(0, sim.getTime()));
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

        sim.addToFel(Event.newArrival(0, sim.getTime()));
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

        sim.addToFel(Event.newArrival(0, sim.getTime()));
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

        sim.addToFel(Event.newArrival(0, sim.getTime()));
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

        sim.addToFel(Event.newArrival(0, sim.getTime()));
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
        assertThrows(NullPointerException.class, () -> new Simulation(null, rigged));
        assertThrows(NullPointerException.class, () -> new Simulation(simpleNet, null));

        var sim = new Simulation(simpleNet, rigged);
        assertTrue(sim.hasEnded());
        assertEquals(0, sim.getEventsProcessed());
        assertEquals(0.0, sim.getTime(), DELTA);
        var fel = sim.getFutureEventList();
        assertEquals(0, fel.size());

        var start = System.nanoTime();
        sim = new Simulation(simpleNet, rigged);
        // knowing that it takes time to allocate the object
        // we can use the average time
        var endAllocation = System.nanoTime();
        var time = (endAllocation + start) / 2;
        var diff = 1e-6 * (endAllocation - start); // getting the error margin in ms

        assertTrue(sim.hasEnded());
        assertEquals(0, sim.getEventsProcessed());
        assertEquals(0.0, sim.getTime(), DELTA);
        assertEquals(node0, sim.getNode(node0.name));
        assertEquals(node1, sim.getNode(node1.name));
        assertEquals(0, sim.getNodeState(node0.name).numServerBusy);
        assertEquals(0, sim.getNodeState(node0.name).numServerUnavailable);
        assertEquals(0, sim.getNodeState(node1.name).numServerBusy);
        assertEquals(0, sim.getNodeState(node1.name).numServerUnavailable);
        fel = sim.getFutureEventList();
        assertEquals(0, fel.size());

        sim.addToFel(Event.newArrival(0, sim.getTime()));
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
        assertEquals(0, fel.get(0).nodeIndex);
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
        assertEquals(0, fel.get(0).nodeIndex);
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
        assertEquals(1, fel.get(0).nodeIndex);
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
        assertEquals(1, fel.get(0).nodeIndex);
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
        final var s = sim;
        assertThrows(NullPointerException.class, () -> s.processNextEvent());

        var elapsed = (double) (System.nanoTime() - time);
        var result = sim.endSimulation();
        assertEquals(2.0, result.simulationTime, DELTA);
        assertEquals(sim.seed, result.seed);
        assertEquals(elapsed * 1e-6, result.timeElapsedMS, diff);
        assertEquals(2, result.stats.length);
        assertEquals(1, result.stats[0].numArrivals, DELTA);
        assertEquals(1, result.stats[0].numDepartures, DELTA);
        assertEquals(1, result.stats[1].numArrivals, DELTA);
        assertEquals(1, result.stats[1].numDepartures, DELTA);
    }

    @Test
    public void endSim() {
        var criteria = new EndCriteria.MaxDepartures(node0.name, 5);
        var sim = new Simulation(simpleNet, rigged, criteria);
        sim.addToFel(Event.newArrival(0, sim.getTime()));
        sim.addToFel(Event.newArrival(0, sim.getTime()));
        sim.addToFel(Event.newArrival(0, sim.getTime()));
        sim.addToFel(Event.newArrival(0, sim.getTime()));
        sim.addToFel(Event.newArrival(0, sim.getTime()));
        sim.addToFel(Event.newArrival(0, sim.getTime()));

        while (!criteria.shouldEnd(sim)) {
            sim.processNextEvent();
        }

        assertTrue(sim.hasEnded());
        var res = sim.endSimulation();

        assertEquals(6, res.stats[0].numArrivals, DELTA);
        assertEquals(5, res.stats[0].numDepartures, DELTA);
        assertEquals(4, res.stats[1].numArrivals, DELTA);
        assertEquals(3, res.stats[1].numDepartures, DELTA);
    }

    @Test
    public void simulationStats() {
        var net = new Net();
        net.addNode(ServerNode.Builder.terminal("Source", 50, const1));

        var sim = new Simulation(net, rigged);
        var result = sim.run();
        var nodeStat = result.getStat("Source");
        assertEquals(50, nodeStat.numArrivals, DELTA);
        assertEquals(50, nodeStat.numDepartures, DELTA);
        assertEquals(1.0, nodeStat.avgQueueLength, DELTA);
        assertEquals(1.0, nodeStat.avgResponse, DELTA);
        assertEquals(0.0, nodeStat.avgWaitTime, DELTA);
        assertEquals(1.0, nodeStat.maxQueueLength, DELTA);
        assertEquals(50.0, nodeStat.busyTime, DELTA);
        assertEquals(result.simulationTime, nodeStat.lastEventTime, DELTA);
        assertEquals(1.0, nodeStat.throughput, DELTA);
        assertEquals(1.0, nodeStat.utilization, DELTA);
        assertEquals(0.0, nodeStat.unavailable, DELTA);

        net.addNode(ServerNode.Builder.queue("Queue", 1, const1));
        net.addConnection(0, 1, 1.0);

        sim = new Simulation(net, rigged);
        result = sim.run();
        nodeStat = result.getStat("Source");
        assertEquals(50, nodeStat.numArrivals, DELTA);
        assertEquals(50, nodeStat.numDepartures, DELTA);
        assertEquals(1.0, nodeStat.avgQueueLength, DELTA);
        assertEquals(1.0, nodeStat.avgResponse, DELTA);
        assertEquals(0.0, nodeStat.avgWaitTime, DELTA);
        assertEquals(1.0, nodeStat.maxQueueLength, DELTA);
        assertEquals(50.0, nodeStat.busyTime, DELTA);
        assertEquals(result.simulationTime - 1, nodeStat.lastEventTime, DELTA);
        assertEquals(1.0, nodeStat.throughput, DELTA);
        assertEquals(1.0, nodeStat.utilization, DELTA);
        assertEquals(0.0, nodeStat.unavailable, DELTA);
        nodeStat = result.getStat("Queue");
        assertEquals(50, nodeStat.numArrivals, DELTA);
        assertEquals(50, nodeStat.numDepartures, DELTA);
        assertEquals(1.0, nodeStat.avgQueueLength, DELTA);
        assertEquals(1.0, nodeStat.avgResponse, DELTA);
        assertEquals(0.0, nodeStat.avgWaitTime, DELTA);
        assertEquals(1.0, nodeStat.maxQueueLength, DELTA);
        assertEquals(50.0, nodeStat.busyTime, DELTA);
        assertEquals(result.simulationTime, nodeStat.lastEventTime, DELTA);

        assertEquals(nodeStat.busyTime / nodeStat.lastEventTime, nodeStat.utilization, DELTA);
        assertEquals(nodeStat.numDepartures / nodeStat.lastEventTime, nodeStat.throughput, DELTA);
        assertEquals(0.0, nodeStat.unavailable, DELTA);
    }

    @Test
    public void simulationDrop() {
        var net = new Net();
        net.addNode(ServerNode.Builder.terminal("Source", 50, const1));
        net.addNode(new ServerNode.Builder("Queue", _ -> 2.0).queue(20).build());
        net.addConnection(0, 1, 1.0);

        var sim = new Simulation(net, rigged);
        var result = sim.run();

        var nodeStat = result.getStat("Source");
        assertEquals(50, nodeStat.numArrivals, DELTA);
        assertEquals(50, nodeStat.numDepartures, DELTA);
        assertEquals(1.0, nodeStat.avgQueueLength, DELTA);
        assertEquals(1.0, nodeStat.avgResponse, DELTA);
        assertEquals(0.0, nodeStat.avgWaitTime, DELTA);
        assertEquals(1.0, nodeStat.maxQueueLength, DELTA);
        assertEquals(50.0, nodeStat.busyTime, DELTA);
        assertEquals(50.0, nodeStat.lastEventTime, DELTA);
        assertEquals(1.0, nodeStat.throughput, DELTA);
        assertEquals(1.0, nodeStat.utilization, DELTA);
        assertEquals(0.0, nodeStat.unavailable, DELTA);

        nodeStat = result.getStat("Queue");
        assertEquals(44, nodeStat.numArrivals, DELTA);
        assertEquals(44, nodeStat.numDepartures, DELTA);
        assertEquals(20.0, nodeStat.maxQueueLength, DELTA);
        assertEquals(23.0227272, nodeStat.avgResponse, DELTA);
        assertEquals(21.0227272, nodeStat.avgWaitTime, DELTA);
        assertEquals(0.0, nodeStat.unavailable, DELTA);
        assertEquals(result.simulationTime, nodeStat.lastEventTime, DELTA);
    }
}
