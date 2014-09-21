package org.iis.plagiarismdetector.textalignment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.iis.plagiarismdetector.core.Feature;
import org.iis.plagiarismdetector.core.TextProcessor;

/**
 * This is an implementation, in Java, of the Longest Common Subsequence
 * algorithm. That is, given two strings A and B, this program will find the
 * longest sequence of letters that are common and ordered in A and B.
 * 
 * There are only two reasons you are reading this: - you don't care what the
 * algorithm is but you need a piece of code to do it - you're trying to
 * understand the algorithm, and a piece of code might help In either case, you
 * should either read an entire chapter of an algorithms textbook on the subject
 * of dynamic programming, or you should consult a webpage that describes this
 * particular algorithm. It is important, for example, that we use arrays of
 * size |A|+1 x |B|+1.
 * 
 * This code is provided AS-IS. You may use this code in any way you see fit,
 * EXCEPT as the answer to a homework problem or as part of a term project in
 * which you were expected to arrive at this code yourself.
 * 
 * Copyright (C) 2005 Neil Jones.
 * 
 */
public class LCS {
	// These are "constants" which indicate a direction in the backtracking
	// array.
	private static final int NEITHER = 0;
	private static final int UP = 1;
	private static final int LEFT = 2;
	private static final int UP_AND_LEFT = 3;

	public static Integer LengthOfLCS(String a, String b) {
		int n = a.length();
		int m = b.length();
		int S[][] = new int[n + 1][m + 1];
		int R[][] = new int[n + 1][m + 1];
		int ii, jj;

		// It is important to use <=, not <. The next two for-loops are
		// initialization
		for (ii = 0; ii <= n; ++ii) {
			S[ii][0] = 0;
			R[ii][0] = UP;
		}
		for (jj = 0; jj <= m; ++jj) {
			S[0][jj] = 0;
			R[0][jj] = LEFT;
		}

		// This is the main dynamic programming loop that computes the score and
		// backtracking arrays.
		for (ii = 1; ii <= n; ++ii) {
			for (jj = 1; jj <= m; ++jj) {

				if (a.charAt(ii - 1) == b.charAt(jj - 1)) {
					S[ii][jj] = S[ii - 1][jj - 1] + 1;
					R[ii][jj] = UP_AND_LEFT;
				}

				else {
					S[ii][jj] = S[ii - 1][jj - 1] + 0;
					R[ii][jj] = NEITHER;
				}

				if (S[ii - 1][jj] >= S[ii][jj]) {
					S[ii][jj] = S[ii - 1][jj];
					R[ii][jj] = UP;
				}

				if (S[ii][jj - 1] >= S[ii][jj]) {
					S[ii][jj] = S[ii][jj - 1];
					R[ii][jj] = LEFT;
				}
			}
		}

		return S[n][m];
	}

	public static Feature LCSAlgorithm(String a, String b,
			String suspFileMatn_main, String srcFileMatn_main) {
		int n = a.length();
		int m = b.length();
		int S[][] = new int[n + 1][m + 1];
		int R[][] = new int[n + 1][m + 1];
		int ii, jj;

		// It is important to use <=, not <. The next two for-loops are
		// initialization
		for (ii = 0; ii <= n; ++ii) {
			S[ii][0] = 0;
			R[ii][0] = UP;
		}
		for (jj = 0; jj <= m; ++jj) {
			S[0][jj] = 0;
			R[0][jj] = LEFT;
		}

		// This is the main dynamic programming loop that computes the score and
		// backtracking arrays.
		for (ii = 1; ii <= n; ++ii) {
			for (jj = 1; jj <= m; ++jj) {

				if (a.charAt(ii - 1) == b.charAt(jj - 1)) {
					S[ii][jj] = S[ii - 1][jj - 1] + 1;
					R[ii][jj] = UP_AND_LEFT;
				}

				else {
					S[ii][jj] = S[ii - 1][jj - 1] + 0;
					R[ii][jj] = NEITHER;
				}

				if (S[ii - 1][jj] >= S[ii][jj]) {
					S[ii][jj] = S[ii - 1][jj];
					R[ii][jj] = UP;
				}

				if (S[ii][jj - 1] >= S[ii][jj]) {
					S[ii][jj] = S[ii][jj - 1];
					R[ii][jj] = LEFT;
				}
			}
		}

		// The length of the longest substring is S[n][m]
		ii = n;
		jj = m;
		int pos = S[ii][jj] - 1;
		char lcs[] = new char[pos + 1];
		long length = S[ii][jj];
		long sourceOffset = jj;
		long suspOffset = ii;

		// Trace the backtracking matrix.
		while (ii > 0 || jj > 0) {
			if (R[ii][jj] == UP_AND_LEFT) {
				ii--;
				jj--;
				lcs[pos--] = a.charAt(ii);
				sourceOffset = jj;
				suspOffset = ii;
			}

			else if (R[ii][jj] == UP) {
				ii--;
			}

			else if (R[ii][jj] == LEFT) {
				jj--;
			}
		}

		if (length == 0)
			return null;
		return new Feature((long) suspFileMatn_main.indexOf(new String(lcs)),
				length, (long) srcFileMatn_main.indexOf(new String(lcs)),
				length);
	}

	List<Feature> findAllPlagiarismCases(String suspFile, String srcFile) {
		List<Feature> features = new ArrayList<Feature>();

		String suspFileMatn = TextProcessor.getMatn(new File(suspFile));
		String srcFileMatn = TextProcessor.getMatn(new File(srcFile));
		;
		String suspFileMatn_main = new String(suspFileMatn);
		String srcFileMatn_main = new String(srcFileMatn);
		Feature lastFoundFeature = null;
		do {

			lastFoundFeature = LCSAlgorithm(suspFileMatn, srcFileMatn,
					suspFileMatn_main, srcFileMatn_main);
			if (lastFoundFeature != null) {
				features.add(lastFoundFeature);
				suspFileMatn.replaceAll(suspFileMatn_main.substring(
						lastFoundFeature.getOffset().intValue(),
						lastFoundFeature.getOffset().intValue()
								+ lastFoundFeature.getLength().intValue()), "");
				srcFileMatn.replaceAll(srcFileMatn_main.substring(
						lastFoundFeature.getSrcOffset().intValue(),
						lastFoundFeature.getSrcOffset().intValue()
								+ lastFoundFeature.getSrcLength().intValue()),
						"");

			}
		} while (lastFoundFeature != null);

		return features;
	}

	public static void main(String args[]) {
		try {
			// String s = LCSAlgorithm(args[0], args[1]);
			// System.out.println(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}