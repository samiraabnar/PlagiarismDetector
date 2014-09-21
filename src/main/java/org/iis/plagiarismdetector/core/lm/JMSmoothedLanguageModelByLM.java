package org.iis.plagiarismdetector.core.lm;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JMSmoothedLanguageModelByLM extends LanguageModel {

	Double lambda;
	LanguageModel bgLM;

	public JMSmoothedLanguageModelByLM(Map<String, Double> wordsCount,
			Double lambda, LanguageModel bgLM) {
		super();
		this.lambda = lambda;
		this.bgLM = bgLM;
		this.wordsCount = wordsCount;
		computeSizeOfCollection();
	}

	@Override
	Double getWordProbability(String word) throws IOException {
		return ((1 - lambda) * getMaximumLikelihoodProbability(word))
				+ (lambda * bgLM.getWordProbability(word));
	}

	@Override
	public Double compareTo(LanguageModel languageModel) throws IOException {
		Double sum = 0D;
		Double alphaD = lambda;
		Set<String> words = new HashSet<String>();
		words.addAll(getWordsCount().keySet());
		words.addAll(languageModel.getWordsCount().keySet());
		for (String word : words) {
			Double p = languageModel.getWordProbability(word); // query
			if (p == 0)
				continue;
			Double q = this.getWordProbability(word); // document
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
