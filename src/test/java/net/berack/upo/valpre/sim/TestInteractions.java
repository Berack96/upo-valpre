package net.berack.upo.valpre.sim;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.berack.upo.valpre.rand.Distribution;

public class TestInteractions {

    @Test
    public void netToString() {
        var net = new Net();
        assertEquals("", net.toString());

        net.addNode(new ServerNode.Builder("Source", new Distribution.Exponential(1)).build());
        assertEquals("Source[servers:1, queue:100, spawn:0, Exponential(1.0)] -\n", net.toString());

        net.addNode(new ServerNode.Builder("Server", new Distribution.Normal(3.2, 0.6)).build());
        assertEquals("Source[servers:1, queue:100, spawn:0, Exponential(1.0)] -\n"
                + "Server[servers:1, queue:100, spawn:0, Normal(3.2, 0.6)] -\n", net.toString());

        net.addConnection(0, 1, 1.0);
        assertEquals("Source[servers:1, queue:100, spawn:0, Exponential(1.0)] -> Server(1.0)\n"
                + "Server[servers:1, queue:100, spawn:0, Normal(3.2, 0.6)] -\n", net.toString());

        net.addNode(new ServerNode.Builder("Server2", new Distribution.Normal(4.1, 0.1)).build());
        assertEquals("Source[servers:1, queue:100, spawn:0, Exponential(1.0)] -> Server(1.0)\n"
                + "Server[servers:1, queue:100, spawn:0, Normal(3.2, 0.6)] -\n"
                + "Server2[servers:1, queue:100, spawn:0, Normal(4.1, 0.1)] -\n", net.toString());

        net.addConnection(0, 2, 1.0);
        net.normalizeWeights();
        assertEquals("Source[servers:1, queue:100, spawn:0, Exponential(1.0)] -> Server(0.5), Server2(0.5)\n"
                + "Server[servers:1, queue:100, spawn:0, Normal(3.2, 0.6)] -\n"
                + "Server2[servers:1, queue:100, spawn:0, Normal(4.1, 0.1)] -\n", net.toString());

        net.addConnection(1, 2, 1.0);
        assertEquals("Source[servers:1, queue:100, spawn:0, Exponential(1.0)] -> Server(0.5), Server2(0.5)\n"
                + "Server[servers:1, queue:100, spawn:0, Normal(3.2, 0.6)] -> Server2(1.0)\n"
                + "Server2[servers:1, queue:100, spawn:0, Normal(4.1, 0.1)] -\n", net.toString());
    }
}
