package org.iis.plagiarismdetector.core;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.iis.plagiarismdetector.core.sourceretrieval.SourceRetrievalConfig;
import org.iis.plagiarismdetector.settings.TextAlignmentDatasetSettings;

import com.sun.tools.javac.util.Pair;

public class MyDictionary {
	private static Map<String, Map<String, Double>> probabilisticWordMap = new HashMap<String, Map<String, Double>>();
	private static String DICS_PATH = SourceRetrievalConfig.getDictionaryPath();
	static {
		loadDictionary(TextAlignmentDatasetSettings.LANGUAGE.toLowerCase()
				+ "-" + TextAlignmentDatasetSettings.LANGUAGE.toLowerCase()
				+ "_top50" + ".txt");
	}

	private static void loadDictionary(String dicFileName) {
		try {
			probabilisticWordMap = new HashMap<String, Map<String, Double>>();

			FileInputStream fstream = new FileInputStream(DICS_PATH
					+ dicFileName);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in,
					"UTF8"));

			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split("\t");
				if (!probabilisticWordMap.containsKey(parts[0])) {
					probabilisticWordMap.put(parts[0],
							new HashMap<String, Double>());
				}
				probabilisticWordMap.get(parts[0]).put(parts[1],
						Double.parseDouble(parts[2]));

			}

			br.close();
			in.close();
			fstream.close();
			System.out.println("Mono Dic Key Size: "
					+ probabilisticWordMap.keySet().size());
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	public static Double getTranslationProbability(String word,
			String translation) {
		if (probabilisticWordMap.get(word) != null) {
			if (probabilisticWordMap.get(word).containsKey(translation))
				return probabilisticWordMap.get(word).get(translation);
		}

		return 0D;
	}

	public static Set<String> getSynonyms(String termValue) {
		// TODO Auto-generated method stub
		if (probabilisticWordMap.containsKey(termValue))
			return probabilisticWordMap.get(termValue).keySet();
		return new HashSet<String>();
	}

	public static Pair<List<List<String>>, List<Double>> getNGramSynonyms(
			List<String> ngramparts) {

		Map<String, List<Pair<String, Double>>> synonyms = new HashMap<String, List<Pair<String, Double>>>();

		for (String word : ngramparts) {
			synonyms.put(word, new ArrayList<Pair<String, Double>>());
			synonyms.get(word).add(new Pair<String, Double>(word, 1D));
			for (String syn : MyDictionary.getSynonyms(word)) {
				Double probability = MyDictionary.getTranslationProbability(
						word, syn);
				if (probability >= 0.06)
					synonyms.get(word).add(
							new Pair<String, Double>(syn, probability));
				if (synonyms.get(word).size() > 3) {
					break;
				}
			}
		}

		List<List<Pair<String, Double>>> allNGramSyns = getAllNGramSyns(0,
				ngramparts, synonyms);

		Pair<List<List<String>>, List<Double>> results = new Pair<List<List<String>>, List<Double>>(
				new ArrayList<List<String>>(), new ArrayList<Double>());
		for (List<Pair<String, Double>> ngramSyn : allNGramSyns) {
			List<String> ngramsyn = new ArrayList<String>();
			Double ngramSynScore = 1D;
			for (Pair<String, Double> part : ngramSyn) {
				ngramsyn.add(part.fst);
				ngramSynScore *= part.snd;
			}

			if ((ngramSyn.size() > 0) && (ngramSynScore > 0.005)) {
				results.fst.add(ngramsyn);
				results.snd.add(ngramSynScore);
			}
		}
		return results;
	}

	public static List<List<Pair<String, Double>>> getAllNGramSyns(Integer i,
			List<String> ngramparts,
			Map<String, List<Pair<String, Double>>> synonyms) {
		List<List<Pair<String, Double>>> allSynNGrams = new ArrayList<List<Pair<String, Double>>>();
		if (i == (ngramparts.size() - 1)) {
			List<List<Pair<String, Double>>> syns = new ArrayList<List<Pair<String, Double>>>();
			for (Pair<String, Double> syn : synonyms.get(ngramparts.get(i))) {
				ArrayList<Pair<String, Double>> synList = new ArrayList<Pair<String, Double>>();
				synList.add(syn);
				syns.add(synList);
			}
			return syns;
		}

		List<List<Pair<String, Double>>> restSyns = getAllNGramSyns(i + 1,
				ngramparts, synonyms);

		for (Pair<String, Double> syn : synonyms.get(ngramparts.get(i))) {
			for (List<Pair<String, Double>> rest : restSyns) {
				List<Pair<String, Double>> newNGram = new ArrayList<Pair<String, Double>>();
				newNGram.add(syn);
				newNGram.addAll(rest);

				allSynNGrams.add(newNGram);
			}

		}

		return allSynNGrams;
	}

	public static void main(String[] args) {
		List<String> sampleNGram = new ArrayList<String>();
		sampleNGram.add("in");
		sampleNGram.add("our");
		sampleNGram.add("philosophy");
		sampleNGram.add("you");
		sampleNGram.add("cat");

		Pair<List<List<String>>, List<Double>> result = getNGramSynonyms(sampleNGram);

		System.out.println(result.fst.size());

	}
}
