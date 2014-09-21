package org.iis.plagiarismdetector.settings;

import java.util.HashMap;
import java.util.Map;

public class TextAlignmentDatasetSettings {

	public static String JAR_FILE_PATH = "";// "/home/zamani14/Samira/TextAlignment/";
	public static final String GhoghnousDir = "evaluations/Ghoghnous2014/corpus/";
	public static final String PAN2013Dir = "evaluations/PAN2013/pan13-text-alignment-test-corpus1-2013-03-08/";
	// "//pan13-text-alignment-training-corpus-2013-01-21/";
	// pan13-text-alignment-test-corpus1-2013-03-08/";

	public static final String[] DataMainDir = { GhoghnousDir, PAN2013Dir,
			"PAN2014" };
	public static final String[] detectionFolder = { "02-no-obfuscation",
			"04-shuffle-sentences", "03-circular-translation",
			"05-replace-words", "03-random-obfuscation",
			"05-summary-obfuscation", "04-translation-obfuscation",
			"01-no-plagiarism" };
	public static final String[][] suspFolder = {
			{ "susp-no-obfuscation", "susp-shuffle-sentences",
					"susp-circular-translation", "susp-replace-words",
					"susp-random-obfuscation", "susp-summary-obfuscation",
					"susp-translation-obfuscation", "susp-no-plagiarism" },
			{ "", "", "", "", "", "", "", "" },
			{ "", "", "", "", "", "", "", "" } };
	public static final String BackgroundCorpusIndexPath = JAR_FILE_PATH
			+ "backgroundIndex/Index_St-F_SW-F";
	public static String[] methodNames = {
			"validInterval_with-stopwords_march8", "Word1Gram", "Word2Gram",
			"Word3Gram", "Word4Gram", "Word5Gram", "Word6Gram", "Word7Gram",
			"Word8Gram", "Word9Gram", "Word10Gram", "WordSentenceBasedGram" };
	public static String[] datasetNames = { "Ghoghnous2014", "PAN2013",
			"PAN2014" };

	public static Integer plagiarismTypeIndex = 7;
	public static Integer methodIndex = 1;
	public static Integer datasetIndex = 1;

	public static String SOURCE_FILES_DIR = DataMainDir[datasetIndex] + "src/";
	public static String SUSP_FILES_DIR = DataMainDir[datasetIndex]
			+ "susp/"
			+ suspFolder[datasetIndex][plagiarismTypeIndex]
			+ (suspFolder[datasetIndex][plagiarismTypeIndex].length() > 0 ? "/"
					: "");

	public static String RESULTS_DIR = "evaluations/"
			+ datasetNames[datasetIndex] + "/detections/"
			+ methodNames[methodIndex] + "/"
			+ detectionFolder[plagiarismTypeIndex] + "/";
	public static String GOLD_RESULTS_DIR = DataMainDir[datasetIndex]
			+ detectionFolder[plagiarismTypeIndex] + "/";
	public static String Evaluation_DIR = "evaluations/"
			+ datasetNames[datasetIndex] + "/evaluations/"
			+ methodNames[methodIndex] + "/"
			+ detectionFolder[plagiarismTypeIndex];

	public static String DETECTED_DETAILS_DIR = RESULTS_DIR;
	public static String REALCASES_DETAILS_DIR = GOLD_RESULTS_DIR;

	public static String RECALL_BASED_EVALUATION_LOG = Evaluation_DIR
			+ "/log_no-obfucation_recall.xml";
	public static String SPLITLY_DETECTED_CASES_LOG = Evaluation_DIR
			+ "/log_no-obfucation_gran.xml";
	public static String PRECISION_BASED_EVALUATION_LOG = Evaluation_DIR
			+ "/log_no-obfucation_precision.xml";
	public static String NOT_DETECTED_EVALUATION_LOG = Evaluation_DIR
			+ "/log_no-obfucation_notDetected.xml";
	public static String DOCUMENTS_POSTFIX = ".txt";
	public static String MID_RESULTS_DIR = "evaluations/"
			+ datasetNames[datasetIndex] + "/detections/"
			+ methodNames[methodIndex] + "/";
	public static String[] languages = { "FA", "EN", "EN" };
	public static String LANGUAGE = "EN";

	public static String PAIRS_FILE_ADDR = GOLD_RESULTS_DIR + "pairs";

	public static Map<String, String> stopwordsFiles = new HashMap<String, String>();

	static {
		stopwordsFiles.put("FA", "src/main/resources/" + "FA_stopword.txt");
		stopwordsFiles.put("EN", "src/main/resources/" + "EN_stopword.txt");
		LANGUAGE = languages[datasetIndex];
	}

	public static void initialize() {
		SOURCE_FILES_DIR = DataMainDir[datasetIndex] + "src/";
		SUSP_FILES_DIR = DataMainDir[datasetIndex]
				+ "susp/"
				+ suspFolder[datasetIndex][plagiarismTypeIndex]
				+ (suspFolder[datasetIndex][plagiarismTypeIndex].length() > 0 ? "/"
						: "");

		RESULTS_DIR = "evaluations/" + datasetNames[datasetIndex]
				+ "/detections/" + methodNames[methodIndex] + "/"
				+ detectionFolder[plagiarismTypeIndex] + "/";
		GOLD_RESULTS_DIR = DataMainDir[datasetIndex]
				+ detectionFolder[plagiarismTypeIndex] + "/";
		PAIRS_FILE_ADDR = GOLD_RESULTS_DIR + "pairs";
		Evaluation_DIR = "evaluations/" + datasetNames[datasetIndex]
				+ "/evaluations/" + methodNames[methodIndex] + "/"
				+ detectionFolder[plagiarismTypeIndex];

		DETECTED_DETAILS_DIR = RESULTS_DIR;
		REALCASES_DETAILS_DIR = GOLD_RESULTS_DIR;

		RECALL_BASED_EVALUATION_LOG = Evaluation_DIR
				+ "/log_no-obfucation_recall.xml";
		SPLITLY_DETECTED_CASES_LOG = Evaluation_DIR
				+ "/log_no-obfucation_gran.xml";
		PRECISION_BASED_EVALUATION_LOG = Evaluation_DIR
				+ "/log_no-obfucation_precision.xml";
		NOT_DETECTED_EVALUATION_LOG = Evaluation_DIR
				+ "/log_no-obfucation_notDetected.xml";
		DOCUMENTS_POSTFIX = ".txt";
		MID_RESULTS_DIR = "evaluations/" + datasetNames[datasetIndex]
				+ "/detections/" + methodNames[methodIndex] + "/";
	}
}