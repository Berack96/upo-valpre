package net.berack.upo.valpre.sim;

import java.io.IOException;

import org.junit.Test;

import com.esotericsoftware.kryo.KryoException;

import net.berack.upo.valpre.rand.Distribution;

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

    @Test
    public void testSaveExample1() throws KryoException, IOException {
        var net = new Net();
        net.addNode(ServerNode.createLimitedSource("Source", exp0_22, 1000));
        net.addNode(ServerNode.createQueue("Queue", 1, norm3_2));
        net.addConnection(0, 1, 1.0);

        net.save("src/main/resources/example1.net");
        net = Net.load("src/main/resources/example1.net");
    }

    @Test
    public void testSaveExample2() throws KryoException, IOException {
        var net = new Net();
        net.addNode(ServerNode.createLimitedSource("Source", exp0_22, 1000));
        net.addNode(ServerNode.createQueue("Queue", 1, norm3_2));
        net.addNode(ServerNode.createQueue("Queue Wait", 1, norm3_2, unNorm));
        net.addConnection(0, 1, 1.0);
        net.addConnection(1, 2, 1.0);

        net.save("src/main/resources/example2.net");
        net = Net.load("src/main/resources/example2.net");
    }

    @Test
    public void testSaveExample3() throws KryoException, IOException {
        var net = new Net();
        net.addNode(ServerNode.createLimitedSource("Source", exp1_5, 1000));
        net.addNode(ServerNode.createQueue("Service1", 1, exp2));
        net.addNode(ServerNode.createQueue("Service2", 1, exp3_5, unExp));
        net.addConnection(0, 1, 1.0);
        net.addConnection(1, 2, 1.0);

        net.save("src/main/resources/example3.net");
        net = Net.load("src/main/resources/example3.net");
    }
}
