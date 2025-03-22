package net.berack.upo.valpre.rand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class TestRandom {

    @Test
    public void testRng() {
        Rng rng = new Rng(1);
        for (var i = 0; i < 10000; i++)
            rng.random();
        assertEquals(399268537L, rng.getSeed());
    }

    @Test
    public void testRngs() {
        var rngs = Rng.getMultipleStreams(1, 200);
        assertEquals(256, rngs.length);

        var rng0 = rngs[0];
        var rng1 = rngs[1];

        for (int i = 0; i < 8367781; i++) {
            rng0.random();
            assertNotEquals(rng1.getSeed(), rng0.getSeed());
        }
        assertEquals(Rng.MULT_256, rng1.getSeed());

        rng0.random();
        assertEquals(rng0.getSeed(), rng1.getSeed());
    }

    @Test
    public void testRngVariance() {
        var numbers = new int[5000];
        var rng = new Rng(4656);

        for (var i = 0; i < 1000000; i++) {
            var sample = rng.random();
            var index = (int) (sample * numbers.length);
            numbers[index] += 1;
        }

        var avg = (double) Arrays.stream(numbers).sum() / numbers.length;
        var variance = Arrays.stream(numbers).mapToDouble(num -> Math.pow(num - avg, 2)).sum() / numbers.length;
        var stdDev = Math.sqrt(variance);
        var expected = Math.sqrt((double) numbers.length / 12);
        expected *= 1.1; // adding a bit of margin

        assertTrue("Standard Dev must be less than [" + expected + "] -> [" + stdDev + "]", stdDev < expected);
    }

    @Test
    public void testMean() {
        var rng = new Rng();
        var normal = new Distribution.NormalBoxMuller(1 / 3.5, 0.6);
        var mean = 0.0;

        for (var i = 0; i < 100000; i++) {
            var sample = Distribution.getPositiveSample(normal, rng);
            mean = (mean * (i + 1) + sample) / (i + 2);
        }
        assertEquals(0.6, mean, 0.01);

        for (var i = 0; i < 100000; i++) {
            var sample = Math.max(0, normal.sample(rng));
            mean = (mean * (i + 1) + sample) / (i + 2);
        }
        assertEquals(0.41, mean, 0.01);
    }
}
