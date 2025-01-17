package net.berack.upo.valpre.rand;

public interface Distribution {
    public double sample(Rng rng);

    public static class Exponential implements Distribution {
        private final double lambda;

        public Exponential(double lambda) {
            this.lambda = lambda;
        }

        @Override
        public double sample(Rng rng) {
            return -Math.log(rng.random()) / lambda;
        }
    }

    public static class Normal implements Distribution {
        private final double mean;
        private final double sigma;

        public Normal(double mean, double sigma) {
            this.mean = mean;
            this.sigma = sigma;
        }

        @Override
        public double sample(Rng rng) {
            var sample = rng.random();
            return mean + sigma * Math.sqrt(-2 * Math.log(sample)) * Math.cos(2 * Math.PI * sample);
        }
    }

    public static class NormalBoxMuller implements Distribution {
        private final double mean;
        private final double sigma;
        private double next = Double.NaN;

        public NormalBoxMuller(double mean, double sigma) {
            this.mean = mean;
            this.sigma = sigma;
        }

        @Override
        public double sample(Rng rng) {
            if (!Double.isNaN(next)) {
                var sample = next;
                next = Double.NaN;
                return sample;
            }

            var sample1 = rng.random();
            var sample2 = rng.random();
            next = mean + sigma * Math.sqrt(-2 * Math.log(sample1)) * Math.sin(2 * Math.PI * sample2);
            return mean + sigma * Math.sqrt(-2 * Math.log(sample1)) * Math.cos(2 * Math.PI * sample2);
        }
    }
}
