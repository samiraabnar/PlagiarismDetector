package org.iis.plagiarismdetector.core;

public class MathUtil {
	public static final double log2 = Math.log(2);

	/**
	 * Returns the KL divergence, K(p1 || p2).
	 * 
	 * The log is w.r.t. base 2.
	 * <p>
	 * 
	 * *Note*: If any value in <tt>p2</tt> is <tt>0.0</tt> then the
	 * KL-divergence is <tt>infinite</tt>. Limin changes it to zero instead of
	 * infinite.
	 * 
	 */
	public static double klDivergence(Double[] realDistribution,
			Double[] uniformDistribution) {

		double klDiv = 0.0;

		for (int i = 0; i < realDistribution.length; ++i) {
			if (realDistribution[i] == 0) {
				continue;
			}
			if (uniformDistribution[i] == 0.0) {
				continue;
			} // Limin

			klDiv += realDistribution[i]
					* Math.log(realDistribution[i] / uniformDistribution[i]);
		}

		return Math.pow(Math.E, -1 * klDiv); // moved this division out of the
												// loop
												// -DM
	}
}