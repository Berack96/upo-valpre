package net.berack.upo.valpre.sim;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.Test;

import net.berack.upo.valpre.InteractiveConsole;
import net.berack.upo.valpre.rand.Distribution;

public class TestInteractions {

    @Test
    public void distributionToString() throws Exception {
        var exp = new Distribution.Exponential(1.0);
        assertEquals("Exponential(1.0)", Distribution.toString(exp));

        var uniform = new Distribution.Uniform(0.0, 1.0);
        assertEquals("Uniform(0.0, 1.0)", Distribution.toString(uniform));

        var erlang = new Distribution.Erlang(2, 1.0);
        assertEquals("Erlang(2, 1.0)", Distribution.toString(erlang));

        var normal = new Distribution.Normal(3.2, 0.6);
        assertEquals("Normal(3.2, 0.6)", Distribution.toString(normal));

        var normalBoxMuller = new Distribution.NormalBoxMuller(3.2, 0.6);
        assertEquals("NormalBoxMuller(3.2, 0.6)", Distribution.toString(normalBoxMuller));

        var unavailable = new Distribution.UnavailableTime(0.1, exp);
        assertEquals("UnavailableTime(0.1, Exponential(1.0))", Distribution.toString(unavailable));
    }

    @Test
    public void nodeToString() {
        var exp = new Distribution.Exponential(1.0);
        var normal = new Distribution.Normal(3.2, 0.6);
        var unavailable = new Distribution.UnavailableTime(0.1, exp);

        var node = new ServerNode.Builder("Source", exp).build();
        assertEquals("Source[servers:1, queue:100, spawn:0, Exponential(1.0)]", node.toString());

        node = new ServerNode.Builder("Queue", normal).build();
        assertEquals("Queue[servers:1, queue:100, spawn:0, Normal(3.2, 0.6)]", node.toString());

        node = new ServerNode.Builder("Queue", normal).queue(10).servers(5).spawn(100).build();
        assertEquals("Queue[servers:5, queue:10, spawn:100, Normal(3.2, 0.6)]", node.toString());

        node = new ServerNode.Builder("Queue", normal).queue(10).servers(5).spawn(100).unavailable(unavailable).build();
        assertEquals(
                "Queue[servers:5, queue:10, spawn:100, Normal(3.2, 0.6), u:UnavailableTime(0.1, Exponential(1.0))]",
                node.toString());
    }

    @Test
    public void netToString() {
        var net = new Net();
        assertEquals("", net.toString());

        var node1 = new ServerNode.Builder("Source", new Distribution.Exponential(1)).build();
        net.addNode(node1);
        assertEquals(node1 + " -\n", net.toString());

        var node2 = new ServerNode.Builder("Server", new Distribution.Normal(3.2, 0.6)).build();
        net.addNode(node2);
        assertEquals(node1 + " -\n" + node2 + " -\n", net.toString());

        net.addConnection(0, 1, 1.0);
        assertEquals(node1 + " -> Server(1.0)\n" + node2 + " -\n", net.toString());

        var node3 = new ServerNode.Builder("Server2", new Distribution.Normal(4.1, 0.1)).build();
        net.addNode(node3);
        assertEquals(node1 + " -> Server(1.0)\n" + node2 + " -\n" + node3 + " -\n", net.toString());

        net.addConnection(0, 2, 1.0);
        net.normalizeWeights();
        assertEquals(node1 + " -> Server(0.5), Server2(0.5)\n" + node2 + " -\n" + node3 + " -\n", net.toString());

        net.addConnection(1, 2, 1.0);
        assertEquals(node1 + " -> Server(0.5), Server2(0.5)\n"
                + node2 + " -> Server2(1.0)\n"
                + node3 + " -\n",
                net.toString());

        var const0 = new Distribution.Uniform(0, 0);
        var unavailable = new Distribution.UnavailableTime(0.1, new Distribution.Exponential(1));
        var other = ServerNode.Builder.queue("Other", 1, const0, unavailable);
        net.addNode(other);
        assertEquals(node1 + " -> Server(0.5), Server2(0.5)\n"
                + node2 + " -> Server2(1.0)\n"
                + node3 + " -\n"
                + other + " -\n",
                net.toString());
    }

    @Test(timeout = 1000)
    public void netBuilderInteractive() {

        // Test the interactive console EXIT
        var net = runInteraction("7");
        assertEquals("", net.toString());

        // Test the interactive console ADD NODE
        net = runInteraction("1", "1", "Source", "1", "1.0", "7");
        assertEquals("Source[servers:1, queue:100, spawn:-1, Exponential(1.0)] -\n", net.toString());

        // Test the interactive console ADD SECOND NODE
        net = runInteraction("1", "2", "Terminal", "1", "2.0", "500",
                "1", "3", "Queue", "5", "3.2", "0.6", "1",
                "7");
        assertEquals("Terminal[servers:1, queue:100, spawn:500, Exponential(2.0)] -\n"
                + "Queue[servers:1, queue:100, spawn:0, Normal(3.2, 0.6)] -\n", net.toString());

        // Test the interactive console ADD CONNECTION
        net = runInteraction("1", "1", "Source", "1", "2.0",
                "1", "3", "Queue", "5", "3.2", "0.6", "1",
                "2", "Source", "Queue", "1.0",
                "7");
        assertEquals("Source[servers:1, queue:100, spawn:-1, Exponential(2.0)] -> Queue(1.0)\n"
                + "Queue[servers:1, queue:100, spawn:0, Normal(3.2, 0.6)] -\n", net.toString());

        // Test the interactive console CLEAR
        net = runInteraction("1", "1", "Source", "1", "2.0",
                "1", "3", "Queue", "5", "3.2", "0.6", "1",
                "2", "Source", "Queue", "1.0",
                "2", "Queue", "Queue", "1.0",
                "5", "7");
        assertEquals("", net.toString());

        // Test the interactive console LOAD
        net = runInteraction("4", "1", "src/test/resources/example1.net", "7");
        assertEquals("Source[servers:1, queue:100, spawn:10000, Exponential(0.2222222222222222)] -> Queue(1.0)\n"
                + "Queue[servers:1, queue:100, spawn:0, NormalBoxMuller(3.2, 0.6)] -\n",
                net.toString());

        // Test the interactive console LOAD EXAMPLE 1
        net = runInteraction("4", "2", "1", "7");
        assertEquals("Source[servers:1, queue:100, spawn:10000, Exponential(0.2222222222222222)] -> Queue(1.0)\n"
                + "Queue[servers:1, queue:100, spawn:0, NormalBoxMuller(3.2, 0.6)] -\n",
                net.toString());

        // Test the interactive console LOAD EXAMPLE 2
        net = runInteraction("4", "2", "2", "7");
        assertEquals("Source[servers:1, queue:100, spawn:10000, Exponential(1.5)] -> Service1(1.0)\n"
                + "Service1[servers:1, queue:100, spawn:0, Exponential(2.0)] -> Service2(1.0)\n"
                + "Service2[servers:1, queue:100, spawn:0, Exponential(3.5), u:UnavailableTime(0.1, Exponential(10.0))] -\n",
                net.toString());
    }

    private static Net runInteraction(String... commands) {
        var out = new PrintStream(OutputStream.nullOutputStream());
        var inputs = String.join("\n", commands);
        var bytes = inputs.getBytes();
        var in = new ByteArrayInputStream(bytes);
        return new InteractiveConsole(out, in).run();
    }

    /*
     * An interaction example is like this:
     * 1. Add a node
     * - Choose the type of node to create:
     * - 1. Source
     * - 2. Queue
     * - 3. Queue with unavailable time
     * - Node name: Name
     * - Arrivals limit (0 for Int.Max) / Number of servers: 1
     * - Choose the type of service distribution:
     * - - 1. Exponential
     * - - 2. Uniform
     * - - 3. Erlang
     * - - 4. UnavailableTime
     * - - 5. Normal
     * - - 6. NormalBoxMuller
     * - - 7. None
     * 2. Add a connection
     * - Enter the source node: Source
     * - Enter the target node: Queue
     * - Enter the weight: 1.0
     * 3. Save the net
     * 4. Load net
     * 5. Clear
     * 6. Exit
     */
}
