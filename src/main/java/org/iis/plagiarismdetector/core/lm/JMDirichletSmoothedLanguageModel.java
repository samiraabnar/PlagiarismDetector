package org.iis.plagiarismdetector.core.lm;

import java.io.IOException;
import java.util.Map;

public class JMDirichletSmoothedLanguageModel extends
		DirichletSmoothedLanguageModel {

	Double lambda;
	LanguageModel bgLM;

	public JMDirichletSmoothedLanguageModel(Map<String, Double> wordsCount,
			Double lambda, LanguageModel bgLM) {
		super();
		this.lambda = lambda;
		this.bgLM = bgLM;
		this.wordsCount = wordsCount;
		computeSizeOfCollection();
	}

	@Override
	Double getWordProbability(String word) throws IOException {
		return (lambda * super.getWordProbability(word))
				+ ((1 - lambda) * bgLM.getWordProbability(word));
	}
}
