package org.iis.plagiarismdetector.textalignment;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.iis.plagiarismdetector.textalignment.validintervals.Feature;
import org.iis.plagiarismdetector.core.PanDoc;

/***
 * Plagiarism detection for near-duplicate plagiarism.
 *
 * This program provides the baseline for the PAN 2012 Plagiarism Detection
 * task and can be used as outline for a plagiarism detection program.
 *
 * @author Arnd OberlÃ¤nder
 * @email arnd.oberlaender at uni-weimar dot de'
 * @version 1.0
 */

/**
 * The following class implement a very basic baseline comparison, which aims at
 * near duplicate plagiarism. It is only intended to show a simple pipeline your
 * plagiarism detector can follow. Replace the single steps with your
 * implementation to get started.
 */
public class BaseLine {

	// Member
	// ======

	private File susp;
	private File src;
	private ArrayList<org.iis.plagiarismdetector.core.Feature> detections;
	private String suspText = "";
	private String srcText = "";
	private HashMap<String, ArrayList<Range>> tokens = new HashMap<String, ArrayList<Range>>();

	// Const
	// =====
	private static final String SOURCE_FILES_DIR = "evaluations/Ghoghnous2014-jan/Ghoghnous14Jan-plagiarism-text-aliagnment-training-corpus/src/";
	// "/Users/MacBookPro/Documents/Uni-MS/FinalProject/Code/PlagiarismDetection/evaluations/PAN2013/pan13-plagiarism-text-aliagnment-training-corpus-2013-01-21/src/";
	// "/Users/MacBookPro/Documents/Uni-MS/FinalProject/Code/PlagiarismDetection/evaluations/Ghoghnous2013/Ghoghnous13-plagiarism-text-aliagnment-training-corpus/src/";//"/Users/MacBookPro/Documents/Uni-MS/FinalProject/Code/artificial_plagiarism/dataset/norm_src/";
	private static final String SUSP_FILES_DIR = "evaluations/Ghoghnous2014-jan/Ghoghnous14Jan-plagiarism-text-aliagnment-training-corpus/susp/susp-circular-translation/";
	// "/Users/MacBookPro/Documents/Uni-MS/FinalProject/Code/PlagiarismDetection/evaluations/PAN2013/pan13-plagiarism-text-aliagnment-training-corpus-2013-01-21/susp/";
	// "/Users/MacBookPro/Documents/Uni-MS/FinalProject/Code/PlagiarismDetection/evaluations/Ghoghnous2013/Ghoghnous13-plagiarism-text-aliagnment-training-corpus/susp/susp-no-obfuscation/";//"/Users/MacBookPro/Documents/Uni-MS/FinalProject/Code/artificial_plagiarism/dataset/susp-replace/";

	private static final String RESULTS_DIR = "evaluations/Ghoghnous2014-jan/detections/baseline/circular-translation/";
	private static final int LENGTH = 50;

	// Constructor
	// ===========

	public BaseLine(String susp, String src) {
		this.susp = new File(susp);
		this.src = new File(src);
	}

	/*
	 * Helper functions/classes ========================
	 * 
	 * The following functions are some simple helper functions/classes you can
	 * utilize and modify to fit your own program.
	 */

	/**
	 * Simple recursive directory tree walker which finds all files specified by
	 * a FilenameFilter
	 * 
	 * @param path
	 *            The directory to walk
	 * @param fnf
	 *            The FilenameFilter
	 * @return All found files which satisfy the filter.
	 */
	public static ArrayList<File> walk(File path, FilenameFilter fnf) {
		ArrayList<File> files = new ArrayList<File>();
		for (File f : path.listFiles()) {
			if (f.isDirectory()) {
				files.addAll(walk(f, fnf));
			} else if (fnf.accept(path, f.getName())) {
				files.add(f);
			}
		}
		return files;
	}

	/**
	 * Simple class to manage a range, specified by start and end.
	 */
	private class Range {
		int start;
		int end;

		public Range(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public int length() {
			return end - start;
		}
	}

	/**
	 * Simple class to manage the position of a char in a text.
	 */
	private class CharPosition {
		int position;
		char c;

		public CharPosition(int position, char c) {
			this.position = position;
			this.c = c;
		}
	}

	/**
	 * Read a file into a string.
	 * 
	 * @param f
	 *            File to read.
	 * @return Content of the file.
	 * @throws IOException
	 */
	private static String readFile(File f) throws IOException {
		FileInputStream fstream = new FileInputStream(f);
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(
				new InputStreamReader(in, "UTF8"));
		char[] buffer = new char[2048];
		int len = -1;
		StringBuilder sb = new StringBuilder();
		while ((len = br.read(buffer)) > -1) {
			sb.append(buffer, 0, len);
		}
		br.close();
		return sb.toString();

	}

	/**
	 * Tokenize a given text and return a map containing all start and end
	 * positions for each token. Whitespace and punctuation characters will be
	 * ignored.
	 * 
	 * @param text
	 *            the text to tokenize
	 * @param length
	 *            the length of each token
	 * @return A list of all tokens
	 */
	private HashMap<String, ArrayList<Range>> tokenize(String text, int length) {
		text.replaceAll("[.!و.!:'‍‍‍؛،;?؟\\s]+", " ");
		HashMap<String, ArrayList<Range>> tokens = new HashMap<String, ArrayList<Range>>();
		String token = "";
		ArrayList<Integer> pos = new ArrayList<Integer>();

		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) != ' ') {
				token = token + text.charAt(i);
				pos.add(i);
			}
			if (token.length() == length) {
				String ngram = token.toLowerCase();
				if (!tokens.containsKey(ngram))
					tokens.put(ngram, new ArrayList<Range>());
				tokens.get(ngram).add(
						new Range(pos.get(0), pos.get(length - 1)));
				token = token.substring(1);
				pos.remove(0);
			}
		}

		return tokens;
	}

	/**
	 * Serialze a feature list into a xml file. The xml is structured as
	 * described in
	 * http://www.webis.de/research/corpora/pan-pc-12/pan12/readme.txt The
	 * filename will follow the naming scheme {susp}-{src}.xml and is located in
	 * the current directory. Existing files will be overwritten.
	 *
	 * @param susp
	 *            the filename of the suspicious document
	 * @param src
	 *            the filename of the source document
	 * @param features
	 *            a list containing Features
	 * @throws IOException
	 */
	private void serializeFeatures(File susp, File src,
			ArrayList<org.iis.plagiarismdetector.core.Feature> features) throws IOException {
		String srcRef = this.src.getName();
		String suspRef = this.susp.getName();
		String srcID = srcRef.split("\\.")[0].trim();
		String suspID = suspRef.split("\\.")[0].trim();

		PanDoc doc = new PanDoc(suspRef, srcRef);
		for (org.iis.plagiarismdetector.core.Feature f : features) {
			doc.addFeature(f);
		}
		doc.write(RESULTS_DIR + suspID + "-" + srcID + ".xml");
	}

	// Plagiarism detection pipeline
	// =============================

	/**
	 * Process the plagiarism pipeline.
	 * 
	 * @throws IOException
	 */
	private void process() throws IOException {
		this.preprocess();
		this.detections = this.compare();
		this.postprocess();
	}

	/**
	 * Test a suspicious document for near-duplicate plagiarism with regards to
	 * a source document and return a feature list.
	 * 
	 * @return
	 */
	private ArrayList<org.iis.plagiarismdetector.core.Feature> compare() {
		// TODO: Implement your comparison here and replace the following
		// algorithm with your own.

		ArrayList<org.iis.plagiarismdetector.core.Feature> detections = new ArrayList<org.iis.plagiarismdetector.core.Feature>();
		int skipto = -1;
		ArrayList<CharPosition> token = new ArrayList<CharPosition>();

		for (int i = 0; i < srcText.length(); ++i) {
			if (i > skipto) {
				if (this.srcText.charAt(i) != ' ')
					token.add(new CharPosition(i, this.srcText.charAt(i)));
				if (token.size() == LENGTH) {
					String ngram = "";
					for (int j = 0; j < LENGTH; ++j)
						ngram += token.get(j).c;
					ngram = ngram.toLowerCase();
					if (this.tokens.containsKey(ngram)) {
						org.iis.plagiarismdetector.core.Feature d = new org.iis.plagiarismdetector.core.Feature(
								this.tokens.get(ngram).get(0).start,
								this.tokens.get(ngram).get(0).length(),
								token.get(0).position,
								token.get(LENGTH - 1).position
										- token.get(0).position);
						for (Range t : this.tokens.get(ngram)) {
							int startSrc = token.get(0).position;
							int startSusp = t.start;
							while ((startSusp < this.suspText.length())
									&& startSrc < this.srcText.length()
									&& this.srcText.charAt(startSrc) == this.suspText
											.charAt(startSusp)) {
								startSusp += 1;
								startSrc += 1;
								while ((startSusp < this.suspText.length())
										&& this.suspText.charAt(startSusp) == ' ') {
									startSusp += 1;
								}
								while ((startSrc < this.srcText.length())
										&& this.srcText.charAt(startSrc) == ' ') {
									startSrc += 1;
								}
							}
							if ((startSrc - 1) - token.get(0).position > d
									.getLength())
								d = new org.iis.plagiarismdetector.core.Feature(t.start, startSusp - t.start,
										token.get(0).position, startSrc
												- token.get(0).position);
						}
						detections.add(d);
						skipto = (int) (d.getOffset() + d.getLength());
						if (skipto < this.srcText.length()) {
							token = new ArrayList<CharPosition>();
							token.add(new CharPosition(skipto, this.srcText
									.charAt(skipto)));
						} else {
							break;
						}
					} else {
						token.remove(0);
					}
				}
			}
		}

		return detections;
	}

	/**
	 * Preprocess the suspicious and source document.
	 * 
	 * @throws IOException
	 */
	private void preprocess() throws IOException {
		// TODO: Implement your preprocessing steps here.
		this.suspText = readFile(this.susp).replaceAll("[.!و.!:'‍‍‍؛،;?؟\\s]+",
				" ");
		this.tokens = tokenize(suspText, LENGTH);
		this.srcText = readFile(this.src).replaceAll("[.!و.!:'‍‍‍؛،;?؟\\s]+",
				" ");
	}

	/**
	 * Postprocess the results.
	 * 
	 * @throws IOException
	 */
	private void postprocess() throws IOException {
		// TODO: Implement your postprocessing steps here.
		this.serializeFeatures(this.susp, this.src, this.detections);
	}

	// Main
	// ====

	public static void main(String[] args) throws IOException {
		/*
		 * Process the commandline arguments. If there are two arguments we'll
		 * assume that those are a suspicious and a source document. If there is
		 * only one we have to decide if it points directly to a pairs file or
		 * to a directory. In the first case we break it down into pairs and
		 * compare them. In the latter case we scan the directory for all pairs
		 * file and proceed as before.
		 */
		if (args.length == 1) {
			File path = new File(args[0]);
			ArrayList<File> pairs = new ArrayList<File>();

			if (path.isDirectory()) {
				pairs = walk(path, new Pairfilter());
			} else {
				pairs.add(path);
			}

			for (File p : pairs) {
				BufferedReader br = new BufferedReader(new FileReader(p));
				String line = null;
				while ((line = br.readLine()) != null) {
					String[] pair = line.split(" ");
					String susp = pair[0];
					String src = pair[1];
					BaseLine baseline = new BaseLine(SUSP_FILES_DIR + susp,
							SOURCE_FILES_DIR + src);
					baseline.process();
				}
				br.close();
			}
		} else if (args.length == 2) {
			String susp = args[0];
			String src = args[1];
			BaseLine baseline = new BaseLine(susp, src);
			baseline.process();
		} else {
			System.out.println("Unexpected number of commandline arguments.\n"
					+ "Usage: ./baseline.py {susp} {src}\n"
					+ "       ./baseline.py {pairs}");
		}
	}
}