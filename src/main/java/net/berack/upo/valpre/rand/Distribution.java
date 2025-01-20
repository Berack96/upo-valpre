package net.berack.upo.valpre.rand;

/**
 * Represents a probability distribution.
 */
public interface Distribution {
    public double sample(Rng rng);

    /**
     * Represents an exponential distribution.
     */
    public static class Exponential implements Distribution {
        private final double lambda;

        /**
         * Creates a new exponential distribution with the given rate.
         * 
         * @param lambda The rate of the distribution.
         */
        public Exponential(double lambda) {
            this.lambda = lambda;
        }

        @Override
        public double sample(Rng rng) {
            return -Math.log(rng.random()) / lambda;
        }
    }

    /**
     * Represents a normal distribution.
     */
    public static class Normal implements Distribution {
        private final double mean;
        private final double sigma;

        /**
         * Creates a new normal distribution with the given mean and standard deviation.
         * 
         * @param mean  The mean of the distribution.
         * @param sigma The standard deviation of the distribution.
         */
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

    /**
     * Represents a normal distribution using the Box-Muller transform.
     */
    public static class NormalBoxMuller implements Distribution {
        private final double mean;
        private final double sigma;

        /**
         * Creates a new normal distribution with the given mean and standard deviation.
         * 
         * @param mean  The mean of the distribution.
         * @param sigma The standard deviation of the distribution.
         */
        public NormalBoxMuller(double mean, double sigma) {
            this.mean = mean;
            this.sigma = sigma;
        }

        @Override
        public double sample(Rng rng) {
            var sample1 = rng.random();
            var sample2 = rng.random();
            //remove the other value for thread safety
            //next = mean + sigma * Math.sqrt(-2 * Math.log(sample1)) * Math.sin(2 * Math.PI * sample2);
            return mean + sigma * Math.sqrt(-2 * Math.log(sample1)) * Math.cos(2 * Math.PI * sample2);
        }
    }
}
