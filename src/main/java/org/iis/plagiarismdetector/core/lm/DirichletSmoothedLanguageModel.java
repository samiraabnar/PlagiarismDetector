package org.iis.plagiarismdetector.core.lm;

import java.io.IOException;

public class DirichletSmoothedLanguageModel extends LanguageModel {

	public DirichletSmoothedLanguageModel() {
		super();
	}

	private static Double Miu = 300D;

	public static Double getMiu() {
		return Miu;
	}

	public static void setMiu(Double miu) {
		Miu = miu;
	}

	@Override
	Double getWordProbability(String word) throws IOException {
		return (getWordCount(word) + Miu * getBgWordsProbability(word))
				/ (sizeOfCollection + Miu);
	}

	@Override
	public Double compareTo(LanguageModel languageModel) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}
