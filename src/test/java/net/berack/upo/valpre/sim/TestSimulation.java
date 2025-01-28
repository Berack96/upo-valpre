package net.berack.upo.valpre.sim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

import net.berack.upo.valpre.rand.Distribution;
import net.berack.upo.valpre.rand.Rng;

public class TestSimulation {

    private static double DELTA = 0.0000001;
    private static Rng rigged = new RiggedRng();
    private static Distribution const0 = new Constant(0.0);
    private static Distribution const1 = new Constant(1.0);

    private final static class RiggedRng extends Rng {
        @Override
        public double random() {
            return 0.5;
        }
    }

    private final static class Constant implements Distribution {
        public final double value;

        public Constant(double value) {
            this.value = value;
        }

        @Override
        public double sample(Rng rng) {
            return this.value;
        }
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
        var event = Event.newUnavailable(node, 0, 1);
        assertEquals(node, event.node);
        assertEquals(0.0, event.started, 0.000000000001);
        assertEquals(1.0, event.time, 0.000000000001);
        assertEquals(Event.Type.UNAVAILABLE, event.type);

        var event2 = Event.newArrival(node, 1.0, 5.0);
        assertEquals(node, event2.node);
        assertEquals(1.0, event2.started, 0.000000000001);
        assertEquals(5.0, event2.time, 0.000000000001);
        assertEquals(Event.Type.ARRIVAL, event2.type);

        var event3 = Event.newDeparture(node, 7.0, 8.0);
        assertEquals(node, event3.node);
        assertEquals(7.0, event3.started, 0.000000000001);
        assertEquals(8.0, event3.time, 0.000000000001);
        assertEquals(Event.Type.DEPARTURE, event3.type);

        assertEquals(0, event2.compareTo(event2));
        assertEquals(1, event2.compareTo(event));
        assertEquals(-1, event2.compareTo(event3));
    }

    @Test
    public void net() {
        // TODO
        var net = new Net();
        net.addNode(null);
    }

    @Test
    public void criteria() {
        // TODO

        var criteria = new EndCriteria.MaxTime(5.0);
        criteria.shouldEnd(null);
    }

    @Test
    public void simulation() {
        // TODO
        var sim = new Simulation(null, rigged);
        sim.endSimulation();
    }

    @Test
    public void multipleSim() {
        // TODO
    }
}
