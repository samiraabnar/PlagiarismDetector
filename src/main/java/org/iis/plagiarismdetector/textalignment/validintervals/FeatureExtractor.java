package org.iis.plagiarismdetector.textalignment.validintervals;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.tools.javac.util.Pair;

public class FeatureExtractor {

	private static final String eol = System.getProperty("line.separator");

	private static List<String> stopWords;
	private static final String STOPWORDS_FILENAME = "/Users/MacBookPro/Documents/Uni-MS/FinalProject/Code/PlagiarismDetection/PersianST.txt";

	public FeatureExtractor() {
		fillStopWords();
	}

	public void fillStopWords() {
		stopWords = new ArrayList<String>();
		try {

			FileInputStream fstream = new FileInputStream(STOPWORDS_FILENAME);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in,
					"UTF8"));

			String line;
			while ((line = br.readLine()) != null) {
				stopWords.add(line.trim());
			}

			br.close();
			in.close();
			fstream.close();
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	public static void main(String[] args) {
		FeatureExtractor fe = new FeatureExtractor();
		fe.extractFeatures("/Users/MacBookPro/Documents/Uni-MS/FinalProject/Code/PlagiarismDetection/evaluations/1/SamDataset-v1/susp-no_obfuscation/suspicious-document00000.txt");
	}

	public Map<Feature, List<FeatureProperties>> extractFeatures(
			String fileNameandPath) {
		Map<Feature, List<FeatureProperties>> documentFeatures = new HashMap<Feature, List<FeatureProperties>>();

		String fileString = getMatn(new File(fileNameandPath));
		List<Pair<String, Long>> allWordsList = getWordList(fileString);
		List<Pair<String, Long>> stopwordList = removeNonStopwords(allWordsList);
		List<Pair<String, Long>> wordList = removeStopwords(allWordsList);

		documentFeatures.putAll(extractWordNGramFeaturesFromWordsList(wordList,
				4));
		documentFeatures.putAll(extractStopwordMGramFeaturesFromWordsList(
				stopwordList, 4));

		return documentFeatures;
	}

	private Map<Feature, List<FeatureProperties>> extractStopwordMGramFeaturesFromWordsList(
			List<Pair<String, Long>> stopwordList, Integer m) {
		Map<Feature, List<FeatureProperties>> stopwordMGramFeatures = new HashMap<Feature, List<FeatureProperties>>();

		for (int i = 0; i < stopwordList.size() - m; i++) {
			String featureString = "";
			Long featureOffset = stopwordList.get(i).snd;
			Long featureLength = stopwordList.get(i + m).snd
					- stopwordList.get(i).snd
					+ stopwordList.get(i + m).fst.length();
			for (int j = 0; j < m; j++) {
				featureString += stopwordList.get(i + j).fst;
			}

			FeatureProperties featureProps = new FeatureProperties(
					featureOffset, featureLength);
			Feature feature = new Feature(featureString,
					FeatureType.STOP_WORD_M_GRAM);

			if (!stopwordMGramFeatures.containsKey(feature))
				stopwordMGramFeatures.put(feature,
						new ArrayList<FeatureProperties>());

			stopwordMGramFeatures.get(feature).add(featureProps);
		}

		return stopwordMGramFeatures;
	}

	private Map<Feature, List<FeatureProperties>> extractWordNGramFeaturesFromWordsList(
			List<Pair<String, Long>> wordsList, Integer n) {
		Map<Feature, List<FeatureProperties>> wordMGramFeatures = new HashMap<Feature, List<FeatureProperties>>();

		for (int i = 0; i < wordsList.size() - n; i++) {
			String featureString = "";
			Long featureOffset = wordsList.get(i).snd;
			Long featureLength = wordsList.get(i + n).snd
					- wordsList.get(i).snd + wordsList.get(i + n).fst.length();
			for (int j = 0; j < n; j++) {
				featureString += wordsList.get(i + j).fst;
			}

			FeatureProperties featureProps = new FeatureProperties(
					featureOffset, featureLength);
			Feature feature = new Feature(featureString,
					FeatureType.WORD_N_GRAM);

			if (!wordMGramFeatures.containsKey(feature))
				wordMGramFeatures.put(feature,
						new ArrayList<FeatureProperties>());

			wordMGramFeatures.get(feature).add(featureProps);
		}
		return wordMGramFeatures;
	}

	private List<Pair<String, Long>> removeStopwords(
			List<Pair<String, Long>> allWordsList) {

		List<Pair<String, Long>> filterdList = new ArrayList<Pair<String, Long>>();

		for (Pair<String, Long> word : allWordsList) {
			if (!stopWords.contains(word.fst)) {
				filterdList.add(word);
			}
		}

		return filterdList;
	}

	private List<Pair<String, Long>> removeNonStopwords(
			List<Pair<String, Long>> allWordsList) {
		List<Pair<String, Long>> filterdList = new ArrayList<Pair<String, Long>>();

		for (Pair<String, Long> word : allWordsList) {
			if (stopWords.contains(word.fst)) {
				filterdList.add(word);
			}
		}

		return filterdList;
	}

	private List<Pair<String, Long>> getWordList(String fileString) {
		List<Pair<String, Long>> wordFeatures = new ArrayList<Pair<String, Long>>();
		Integer searchPoint1 = 0;
		Integer searchPoint2 = 0;
		fileString = fileString.replaceAll("\n", " ");
		searchPoint2 = fileString.indexOf(" ", searchPoint2 + 1);

		while (searchPoint2 != -1) {
			String word = fileString.substring(searchPoint1 + 1,
					searchPoint2 + 1);
			word = word.replaceAll("[.?!»«;:,.؟!:؛،]+", " ");
			while (word.charAt(0) == ' ') {
				searchPoint1++;
				while (searchPoint1 >= searchPoint2) {
					searchPoint2 = fileString.indexOf(" ", searchPoint2 + 1);
					if (searchPoint2 == -1) {
						return wordFeatures;
					}
				}
				word = fileString.substring(searchPoint1 + 1, searchPoint2 + 1);
				word = word.replaceAll("[.?!»«;:,.؟!:؛،]+", " ");
			}

			word = word.trim();
			if (word.length() >= 3) {
				wordFeatures.add(new Pair<String, Long>(word,
						(long) (searchPoint1 + 1)));
				// System.out.println(word+" "+fileString.subSequence(searchPoint1+1,
				// searchPoint1+1+word.length()));
			}

			searchPoint1 = searchPoint2;
			searchPoint2 = fileString.indexOf(" ", searchPoint2 + 1);
		}

		return wordFeatures;
	}

	public static String getMatn(File f) {
		String matn = "";
		try {

			FileInputStream fstream = new FileInputStream(f);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in,
					"UTF8"));

			String line;
			while ((line = br.readLine()) != null) {
				matn = matn + line + eol;
			}

			br.close();
			in.close();
			fstream.close();
		} catch (Exception e) {
			System.err.println(e);
		}
		return matn;
	}

}
