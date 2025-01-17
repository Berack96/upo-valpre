package net.berack.upo.valpre.rand;

/**
 * This is an Java library for random number generation.  The use of this
 * library is recommended as a replacement for the Java class Random,
 * particularly in simulation applications where the statistical
 * 'goodness' of the random number generator is important.
 *
 * The generator used in this library is a so-called 'Lehmer random number
 * generator' which returns a pseudo-random number uniformly distributed
 * between 0.0 and 1.0.  The period is (m - 1) where m = 2,147,483,647 and
 * the smallest and largest possible values are (1 / m) and 1 - (1 / m)
 * respectively.  For more details see:
 *
 *       "Random Number Generators: Good Ones Are Hard To Find"
 *                   Steve Park and Keith Miller
 *              Communications of the ACM, October 1988
 *
 * Note that as of 7-11-90 the multiplier used in this library has changed
 * from the previous "minimal standard" 16807 to a new value of 48271.  To
 * use this library in its old (16807) form change the constants MULTIPLIER
 * and CHECK as indicated in the comments.
 *
 * Name              : Rng.java  (Random Number Generation - Single Stream)
 * Authors           : Steve Park & Dave Geyer
 * Translated by     : Jun Wang & Richard Dutton
 * Language          : Java
 * Latest Revision   : 6-10-04
 *
 * Program rng       : Section 2.2
 */
public class Rng {
	public final static long CHECK = 399268537L; /* use 1043616065 for the "minimal standard" */
	public final static long DEFAULT = 123456789L; /* initial seed, use 0 < DEFAULT < MODULUS */
	public final static long MODULUS = 2147483647; /* DON'T CHANGE THIS VALUE */
	public final static long MULTIPLIER = 48271; /* use 16807 for the "minimal standard" */

	private long seed = DEFAULT; /* seed is the state of the generator */

	public Rng() {
	}

	public Rng(long seed) {
		this.putSeed(seed);
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
	public void putSeed(long x) {
		if (x > 0L) {
			x = x % MODULUS; /* correct if x is too large */
		} else {
			x = System.currentTimeMillis();
			// x = ((unsigned long) time((time_t *) NULL)) % MODULUS;
		}

		this.seed = x;
	}

	/**
	 * Use this (optional) procedure to get the current state of the random
	 * number generator.
	 */
	public long getSeed() {
		return this.seed;
	}

	/**
	 * Use this (optional) procedure to test for a correct implementation.
	 */
	public static boolean testRandom() {
		Rng rng = new Rng(1); /* set initial state to 1 */
		for (var i = 0; i < 10000; i++)
			rng.random();
		return rng.getSeed() == CHECK;
	}

	public static long newSeed(long modulus, long multiplier, long seed) {
		var Q = modulus / multiplier;
		var R = modulus % multiplier;
		var t = multiplier * (seed % Q) - R * (seed / Q);
		return t > 0 ? t : (t + modulus);
	}
}
