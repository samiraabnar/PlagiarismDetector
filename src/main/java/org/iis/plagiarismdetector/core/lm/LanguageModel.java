package org.iis.plagiarismdetector.core.lm;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.iis.plagiarismdetector.core.MathUtilz;

abstract public class LanguageModel {

	protected Set<String> totalWords = new HashSet<String>();

	protected Map<String, Double> wordsCount;
	protected Double sizeOfCollection;

	protected Map<String, Double> bgWordsCount;
	protected Double sizeOfBgCollection;

	protected Map<String, Double> probabilityDistribution = new TreeMap<String, Double>();

	public LanguageModel() {
		wordsCount = new HashMap<String, Double>();
		sizeOfCollection = 0D;
		bgWordsCount = new HashMap<String, Double>();
		sizeOfBgCollection = 0D;
	}

	public void fillProbabilityDistribution() throws IOException {
		probabilityDistribution.clear();

		for (String word : totalWords) {
			probabilityDistribution.put(word, getWordProbability(word));
		}

	}

	public Map<String, Double> getBgWordsCount() {
		return bgWordsCount;
	}

	public Double getWordCount(String word) {
		return wordsCount.get(word) != null ? wordsCount.get(word) : 0;
	}

	public Double getMaximumLikelihoodProbability(String word)
			throws IOException {
		Double wordCount = getWordCount(word);
		if (wordCount > 0) {
			return wordCount / sizeOfCollection;
		}
		return 0D;
	}

	abstract Double getWordProbability(String word) throws IOException;

	public Map<String, Double> getWordsCount() {
		return wordsCount;
	}

	public void computeSizeOfCollection() {
		sizeOfCollection = 0D;
		for (String term : wordsCount.keySet()) {
			sizeOfCollection += wordsCount.get(term);
		}
	}

	public void computeSizeOfBgCollection() {
		sizeOfBgCollection = 0D;
		for (String term : bgWordsCount.keySet()) {
			sizeOfBgCollection += bgWordsCount.get(term);
		}
	}

	public double getBgWordsProbability(String word) {
		Double bgWordCount = getBgWordCount(word);
		if (bgWordCount == 0)
			return getBgWordCount(word) / sizeOfBgCollection;
		return 0;
	}

	public Double getBgWordCount(String word) {
		return bgWordsCount.get(word) != null ? bgWordsCount.get(word) : 0;
	}

	public void setup() {
		computeSizeOfBgCollection();
		computeSizeOfCollection();
	}

	abstract public Double compareTo(LanguageModel languageModel)
			throws IOException;

	public String generateQuery(Integer querySize) {
		String query = "";
		for (int i = 0; i < querySize; i++) {

		}

		return query;
	}

	public Double computeKLSimilarityScore(LanguageModel lm) throws IOException {
		Set<String> words = new HashSet<String>();
		words.addAll(lm.wordsCount.keySet());
		words.retainAll(wordsCount.keySet());
		double[] p1 = new double[words.size()];
		double[] p2 = new double[words.size()];

		int i = 0;
		for (String word : words) {

			p1[i] = getWordProbability(word);
			p2[i] = lm.getWordProbability(word);
			i++;
		}

		return -1 * MathUtilz.klDivergence(p2, p1);
	}

	public Double computeJSSimilarityScore(LanguageModel lm) throws IOException {
		Set<String> words = new HashSet<String>();
		words.addAll(lm.wordsCount.keySet());
		words.addAll(wordsCount.keySet());
		double[] p1 = new double[words.size()];
		double[] p2 = new double[words.size()];

		int i = 0;
		for (String word : words) {

			p1[i] = getWordProbability(word);
			p2[i] = lm.getWordProbability(word);
			i++;
		}

		return -1 * MathUtilz.jensenShannonDivergence(p2, p1);
	}

	public Set<String> getTotalWords() {
		return totalWords;
	}

	public void setTotalWords(Set<String> totalWords) {
		this.totalWords = totalWords;
	}
}
