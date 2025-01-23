package net.berack.upo.valpre.rand;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class TestRandom {
    @Test
    public void testRng() {
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
}
