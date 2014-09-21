package org.iis.plagiarismdetector.core.lm;

import java.io.IOException;

public class NotSmoothedLanguageModel extends LanguageModel {

	@Override
	public Double getWordProbability(String word) throws IOException {
		// TODO Auto-generated method stub
		return getMaximumLikelihoodProbability(word);
	}

	@Override
	public Double compareTo(LanguageModel languageModel) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
