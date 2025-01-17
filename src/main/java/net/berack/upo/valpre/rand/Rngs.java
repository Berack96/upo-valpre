package net.berack.upo.valpre.rand;

/**
 * This is an Java library for multi-stream random number generation.
 * The use of this library is recommended as a replacement for the Java
 * class Random, particularly in simulation applications where the
 * statistical 'goodness' of the random number generator is important.
 * The library supplies 256 streams of random numbers; use
 * selectStream(s) to switch between streams indexed s = 0,1,...,255.
 *
 * The streams must be initialized.  The recommended way to do this is by
 * using the function plantSeeds(x) with the value of x used to initialize
 * the default stream and all other streams initialized automatically with
 * values dependent on the value of x.  The following convention is used
 * to initialize the default stream:
 *    if x > 0 then x is the state
 *    if x < 0 then the state is obtained from the system clock
 *    if x = 0 then the state is to be supplied interactively.
 *
 * The generator used in this library is a so-called 'Lehmer random number
 * generator' which returns a pseudo-random number uniformly distributed
 * 0.0 and 1.0.  The period is (m - 1) where m = 2,147,483,647 and the
 * smallest and largest possible values are (1 / m) and 1 - (1 / m)
 * respectively.  For more details see:
 *
 *       "Random Number Generators: Good Ones Are Hard To Find"
 *                   Steve Park and Keith Miller
 *              Communications of the ACM, October 1988
 *
 * Name            : Rngs.java  (Random Number Generation - Multiple Streams)
 * Authors         : Steve Park & Dave Geyer
 * Translated by   : Jun Wang & Richard Dutton
 * Language        : Java
 * Latest Revision : 6-10-04
 */
class Rngs {
	private final static int STREAMS = 256; /* # of streams, DON'T CHANGE THIS VALUE */
	private final static long A256 = 22925; /* jump multiplier, DON'T CHANGE THIS VALUE */

	private Rng[] rngs;
	private int current = 0;

	public Rngs(long seed) {
		this.rngs = new Rng[STREAMS];
		this.plantSeeds(seed);
	}

	public Rng getRng() {
		return this.rngs[this.current];
	}

	public Rng getRng(int stream) {
		return this.rngs[stream % STREAMS];
	}

	/**
	 * Use this function to set the state of all the random number generator
	 * streams by "planting" a sequence of states (seeds), one per stream,
	 * with all states dictated by the state of the default stream.
	 * The sequence of planted states is separated one from the next by
	 * 8,367,782 calls to Random().
	 */
	public void plantSeeds(long seed0) {
		this.rngs[0] = new Rng(seed0);

		for (int j = 1; j < STREAMS; j++) {
			seed0 = Rng.newSeed(Rng.MODULUS, A256, seed0);
			this.rngs[j] = new Rng(seed0);
		}
	}

	/**
	 * Use this function to set the current random number generator
	 * stream -- that stream from which the next random number will come.
	 */
	public void selectStream(int index) {
		this.current = index % STREAMS;
	}

	/**
	 * Use this (optional) function to test for a correct implementation.
	 */
	public static boolean testRandom() {
		var rngs = new Rngs(1);
		var first = rngs.getRng();
		for (int i = 0; i < 10000; i++)
			first.random();

		var x = first.getSeed(); /* get the new state value */
		var ok = (x == Rng.CHECK); /* and check for correctness */

		x = rngs.getRng(1).getSeed(); /* get the state of stream 1 */
		return ok && (x == A256); /* x should be the jump multiplier */
	}
}
