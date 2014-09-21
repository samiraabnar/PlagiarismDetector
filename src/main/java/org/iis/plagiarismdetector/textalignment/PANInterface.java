package org.iis.plagiarismdetector.textalignment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.iis.plagiarismdetector.settings.TextAlignmentDatasetSettings;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramBasedTextAligner;

public class PANInterface {

	private static String CORPUS_MAIN_DIR;
	private static String OUTPUT_DIR;

	/**
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) throws ClassNotFoundException,
			IOException {

		if (args.length == 4) {
			Map<String, Object> experimentOptions = new HashMap<String, Object>();

			for (int i = 0; i < (args.length - 1); i += 2) {
				if (args[i].equals("-i")) {
					CORPUS_MAIN_DIR = args[i + 1].trim();
					if (CORPUS_MAIN_DIR.charAt(CORPUS_MAIN_DIR.length() - 1) != '/') {
						CORPUS_MAIN_DIR += "/";
					}
				} else if (args[i].equals("-o")) {
					OUTPUT_DIR = args[i + 1].trim();
					if (OUTPUT_DIR.charAt(OUTPUT_DIR.length() - 1) != '/') {
						OUTPUT_DIR += "/";
					}
				}
			}

			if ((CORPUS_MAIN_DIR != null) && (OUTPUT_DIR != null)) {

				TextAlignmentDatasetSettings.LANGUAGE = "EN";
				TextAlignmentDatasetSettings.JAR_FILE_PATH = "/home/zamani14/Samira/TextAlignment/";
				TextAlignmentDatasetSettings.datasetIndex = Arrays.asList(
						TextAlignmentDatasetSettings.datasetNames).indexOf(
						"PAN2014");

				NGramBasedTextAligner aligner = new NGramBasedTextAligner();
				TextAlignmentDatasetSettings.DataMainDir[TextAlignmentDatasetSettings.datasetIndex] = CORPUS_MAIN_DIR;
				aligner.initializeForPAN2014(CORPUS_MAIN_DIR, OUTPUT_DIR);
				TextAlignmentDatasetSettings.RESULTS_DIR = OUTPUT_DIR;
				TextAlignmentDatasetSettings.DETECTED_DETAILS_DIR = OUTPUT_DIR;
				File pairFile = new File(CORPUS_MAIN_DIR + "pairs");
				BufferedReader br = null;
				try {
					br = new BufferedReader(new FileReader(pairFile));

					String line = null;
					while ((line = br.readLine()) != null) {
						String[] pair = line.split("\\s+");
						String susp = pair[0].trim();
						String src = pair[1].trim();

						aligner.alignTexts(CORPUS_MAIN_DIR + "susp/" + susp,
								CORPUS_MAIN_DIR + "src/" + src);
					}
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		} else {
			System.out.println("Wrong Format of Input Arguments");
		}
	}
}
