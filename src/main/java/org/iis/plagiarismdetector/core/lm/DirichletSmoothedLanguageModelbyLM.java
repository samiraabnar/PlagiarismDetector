package org.iis.plagiarismdetector.core.lm;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.iis.plagiarismdetector.core.sourceretrieval.irengine.LuceneIndex;

public class DirichletSmoothedLanguageModelbyLM extends LanguageModel {
	LanguageModel bgLM;
	private Double Miu = 1000D;
	private LuceneIndex collectionIndex;
	private Integer docId;

	public DirichletSmoothedLanguageModelbyLM(Map<String, Double> wordsCount,
			Double miu, LanguageModel bgLM) {
		super();
		this.Miu = miu;
		this.bgLM = bgLM;
		this.wordsCount = wordsCount;
		computeSizeOfCollection();
	}

	public DirichletSmoothedLanguageModelbyLM(LuceneIndex collectionIndex,
			Integer docId, Double miu, LuceneBasedLanguageModel bgLM)
			throws IOException {
		super();
		this.Miu = miu;
		this.bgLM = bgLM;
		this.collectionIndex = collectionIndex;
		this.docId = docId;
		sizeOfCollection = collectionIndex.getDocumentSize(docId).doubleValue();
	}

	public Double getMiu() {
		return Miu;
	}

	public void setMiu(Double miu) {
		Miu = miu;
	}

	@Override
	Double getWordProbability(String word) throws IOException {
		return (getWordCount(word) + Miu * bgLM.getWordProbability(word))
				/ (sizeOfCollection + Miu);
	}

	// public Double compareTo(LanguageModel languageModel) throws IOException {
	// // Map<String, Double> plogpdivq = new HashMap<String, Double>();
	// Double sum = 0D;
	// Double alphaD = Miu / (sizeOfCollection + Miu);
	// Set<String> words = new HashSet<String>();
	// // words.addAll(getWordsCount().keySet());
	// words.addAll(languageModel.getTotalWords());
	// for (String word : words) {
	// Double p = languageModel.getWordProbability(word); // query
	// if (p == 0)
	// continue;
	// Double q = this.getWordProbability(word); // document
	// if (q == 0)
	// continue;
	// Double score = p
	// * Math.log(q / (alphaD * bgLM.getWordProbability(word)));
	// // plogpdivq.put(word, score);
	// // System.out.println("p: " + p + "q:" + q);
	// sum += score;
	// }
	// sum += Math.log(alphaD);
	// return sum;
	// }

	public Double compareTo(LanguageModel languageModel) throws IOException {
		// Map<String, Double> plogpdivq = new HashMap<String, Double>();
		Double sum = 0D;
		Double alphaD = Miu / (sizeOfCollection + Miu);
		Map<String, Double> termFreqs = collectionIndex
				.getDocumentTermFrequencies(docId);
		Set<String> words = new HashSet<String>();
		// words.addAll(getWordsCount().keySet());
		words.addAll(languageModel.getTotalWords());
		for (String word : words) {
			Double p = languageModel.getWordProbability(word); // query
			if (p == 0)
				continue;

			Double q = termFreqs.containsKey(word) ? ((termFreqs.get(word) + Miu
					* bgLM.getWordProbability(word)) / (sizeOfCollection + Miu))
					: 0; // document
			if (q == 0)
				continue;
			Double score = p
					* Math.log(q / (alphaD * bgLM.getWordProbability(word)));
			// plogpdivq.put(word, score);
			// System.out.println("p: " + p + "q:" + q);
			sum += score;
		}
		sum += Math.log(alphaD);
		return sum;
	}
}
