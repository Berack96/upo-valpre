package net.berack.upo.valpre.rand;

/**
 * Represents a probability distribution.
 */
public interface Distribution {
    /**
     * Return a sample from the distribution.
     * 
     * @param rng The random number generator to use.
     * @return A number given from the distribution.
     */
    public double sample(Rng rng);

    /**
     * Gets a positive sample from the distribution.
     * This is useful if you need to generate a positive value from a distribution
     * that can generate negative values. For example, the normal distribution.
     * 
     * @param distribution The distribution to sample
     * @param rng          The random number generator to use.
     * @return A positive or 0 value from the distribution.
     */
    public static double getPositiveSample(Distribution distribution, Rng rng) {
        if (distribution == null)
            return 0;

        double sample;
        do {
            sample = distribution.sample(rng);
        } while (sample < 0);
        return sample;
    }

    /**
     * Represents an exponential distribution.
     */
    public static class Exponential implements Distribution {
        public final double lambda;

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
        public final double mean;
        public final double sigma;

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
        public final double mean;
        public final double sigma;

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
            // remove the other value for thread safety
            // next = mean + sigma * Math.sqrt(-2 * Math.log(sample1)) * Math.sin(2 *
            // Math.PI * sample2);
            return mean + sigma * Math.sqrt(-2 * Math.log(sample1)) * Math.cos(2 * Math.PI * sample2);
        }
    }

    /**
     * Represent a uniform distribution.
     */
    public static class Uniform implements Distribution {
        public final double min;
        public final double max;

        /**
         * Creates a new uniform distribution with the given min value and max value.
         * 
         * @param min the minimum value possible
         * @param max the maximum value possible
         */
        public Uniform(double min, double max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public double sample(Rng rng) {
            return min + rng.random() * (max - min);
        }
    }

    /**
     * Represent an Erlang distribution.
     */
    public static class Erlang implements Distribution {
        public final int k;
        public final double lambda;

        /**
         * Creates a new erlang distribution with the given K exponentials, all with the
         * same lambda.
         * 
         * @param k      the number of exponentials
         * @param lambda the lambda of the exponentials
         */
        public Erlang(int k, double lambda) {
            this.k = k;
            this.lambda = lambda;
        }

        @Override
        public double sample(Rng rng) {
            var product = 1.0;
            for (int i = 0; i < this.k; i++) {
                product *= rng.random();
            }
            return -Math.log(product) / this.lambda;
        }
    }

    /**
     * Represent a HyperExponential distribution.
     */
    public static class HyperExponential implements Distribution {
        private final double[] lambdas;
        private final double[] probabilities;

        /**
         * Creates a new hyperexponential distribution with the given lambdas and their
         * corresponding probabilities.
         * 
         * @param lambdas       the array of lambda values for the exponential
         *                      distributions
         * @param probabilities the array of probabilities for each lambda
         */
        public HyperExponential(double[] lambdas, double[] probabilities) {
            if (lambdas.length != probabilities.length) {
                throw new IllegalArgumentException("Lambdas and probabilities must have the same length");
            }
            this.lambdas = lambdas;
            this.probabilities = probabilities;
        }

        @Override
        public double sample(Rng rng) {
            var randomValue = rng.random();
            var i = 0;

            while (i < probabilities.length) {
                randomValue -= probabilities[i];
                if (randomValue <= 0.0d)
                    break;
                i += 1;
            }

            return -Math.log(rng.random()) / lambdas[i];
        }
    }

    /**
     * Distribution of the UnavailableTime that has a probability of happening.
     * In case the node is unavailable then a value of the second distribution is
     * returned.
     */
    public static class UnavailableTime implements Distribution {
        public final double probability;
        public final Distribution distribution;

        /**
         * Create a new distribution with a probability of happening.
         * In case it happens then a value of the second distribution is returned.
         * 
         * @param probability  the probability of returning a value > 0.0
         * @param distribution the distribution where to get the samples
         */
        public UnavailableTime(double probability, Distribution distribution) {
            this.probability = probability;
            this.distribution = distribution;
        }

        @Override
        public double sample(Rng rng) {
            if (rng.random() < this.probability)
                return Distribution.getPositiveSample(this.distribution, rng);
            return 0.0;
        }
    }
}
