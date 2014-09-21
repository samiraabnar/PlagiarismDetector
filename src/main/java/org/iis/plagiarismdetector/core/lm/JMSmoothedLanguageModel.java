package org.iis.plagiarismdetector.core.lm;

import java.io.IOException;

public class JMSmoothedLanguageModel extends LanguageModel {

	public JMSmoothedLanguageModel(Double lambda) {
		super();
		Lambda = lambda;
	}

	private Double Lambda = 0.7;

	public Double getMiu() {
		return Lambda;
	}

	public void setMiu(Double lambda) {
		Lambda = lambda;
	}

	@Override
	Double getWordProbability(String word) throws IOException {
		return ((1 - Lambda) * getMaximumLikelihoodProbability(word))
				+ (Lambda * getBgWordsProbability(word));
	}

	@Override
	public Double compareTo(LanguageModel languageModel) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}
