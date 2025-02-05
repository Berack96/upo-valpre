package net.berack.upo.valpre.rand;

/**
 * This class has been modified by Giacomo Bertolazzi in a way that doesn`t
 * resemble the original. It still has the same role, but has been extended and
 * modernized to java 23.
 * 
 * This is an Java library for random number generation. The use of this
 * library is recommended as a replacement for the Java class Random,
 * particularly in simulation applications where the statistical
 * 'goodness' of the random number generator is important.
 *
 * The generator used in this library is a so-called 'Lehmer random number
 * generator' which returns a pseudo-random number uniformly distributed
 * between 0.0 and 1.0. The period is (m - 1) where m = 2,147,483,647 and
 * the smallest and largest possible values are (1 / m) and 1 - (1 / m)
 * respectively. For more details see:
 * "Random Number Generators: Good Ones Are Hard To Find"
 */
public class Rng {
	// Streams multipliers values taken from the table at page 114 of
	// L.M. Leemis, S.K. Park «Discrete event simulation: a first course»
	public static final long MULT_128 = 40509L;
	public static final long MULT_256 = 22925L;
	public static final long MULT_512 = 44857L;
	public static final long MULT_1024 = 97070L;

	// Single Rng values
	public final static long DEFAULT = 123456789L;
	public final static long MODULUS = 2147483647;
	public final static long MULTIPLIER = 48271;

	private long seed = DEFAULT; /* seed is the state of the generator */

	/**
	 * Default constructor, build the RNG with the default seed. {@link #DEFAULT}
	 */
	public Rng() {
	}

	/**
	 * Builde the RNG with a seed passed as paraemter, if negative or zero will be
	 * modified to a positive number by getting the system time.
	 * 
	 * @param seed the seed to start the rng
	 */
	public Rng(long seed) {
		this.setSeed(seed);
	}

	/**
	 * Random is a Lehmer generator that returns a pseudo-random real number
	 * uniformly distributed between 0.0 and 1.0. The period is (m - 1)
	 * where m = 2,147,483,647 amd the smallest and largest possible values
	 * are (1 / m) and 1 - (1 / m) respectively.
	 */
	public double random() {
		this.seed = Rng.newSeed(MODULUS, MULTIPLIER, this.seed);
		return ((double) this.seed / MODULUS);
	}

	/**
	 * Use this (optional) procedure to initialize or reset the state of
	 * the random number generator according to the following conventions:
	 * if x > 0 then x is the initial seed (unless too large)
	 * if x <= 0 then the initial seed is obtained from the system clock
	 */
	public void setSeed(long seed) {
		if (seed <= 0L) {
			seed = System.currentTimeMillis();
		}

		this.seed = seed % MODULUS; /* correct if x is too large */
	}

	/**
	 * Use this procedure to get the current state of the random number generator.
	 */
	public long getSeed() {
		return this.seed;
	}

	/**
	 * Get multiple streams for the generation of random numbers. The streams
	 * generated will have the seeds spaced enough that the sequences will not
	 * overlap (if not after many calls)
	 * Note that for efficiency the total number of streams cannot suprass 1024 and
	 * will be casted to a pow of 2 no less than 128 giving the array only 4
	 * possible lengths: 128, 256, 512, 1024
	 * 
	 * @param seed  the initial seed of the rngs
	 * @param total the total number of streams
	 * @return the streams
	 */
	public static Rng[] getMultipleStreams(long seed, int total) {
		if (total > 1024)
			throw new IllegalArgumentException("Cannot genrate more than 1024 streams");

		// rounding to the highest pow2
		total = Math.max(total, 128);
		total = 1 << (32 - Integer.numberOfLeadingZeros(total - 1));
		var mult = switch (total) {
			case 128 -> MULT_128;
			case 256 -> MULT_256;
			case 512 -> MULT_512;
			default -> MULT_1024;
		};

		// Building the streams
		var streams = new Rng[total];
		for (int i = 0; i < total; i++) {
			streams[i] = new Rng(seed);
			seed = (streams[i].seed * mult) % MODULUS;
		}
		return streams;
	}

	/**
	 * This procedure is used for calculating a new seed starting from the one
	 * passed as input. The modulus and multiplier should be passed as well but it
	 * is advised to use the standard ones {@link #MODULUS} and {@link #MULTIPLIER}
	 * 
	 * @param modulus    the modulus used for the generation
	 * @param multiplier the multiplier used for the generation
	 * @param seed       the seed where to start
	 * @return a new seed used for the calculation
	 */
	public static long newSeed(long modulus, long multiplier, long seed) {
		var Q = modulus / multiplier;
		var R = modulus % multiplier;
		var t = multiplier * (seed % Q) - R * (seed / Q);
		return t > 0 ? t : (t + modulus);
	}
}
