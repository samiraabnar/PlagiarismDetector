package org.iis.plagiarismdetector.core.lm;

import java.io.IOException;
import java.sql.SQLException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import org.iis.plagiarismdetector.core.sourceretrieval.irengine.LuceneIndex;

public class LuceneBasedLanguageModel extends LanguageModel {

	LuceneIndex index;

	public LuceneBasedLanguageModel(LuceneIndex index) throws IOException,
			ParserConfigurationException, SAXException, SQLException {
		this.index = index;
	}

	@Override
	Double getWordProbability(String word) throws IOException {
		return getMaximumLikelihoodProbability(word);
	}

	@Override
	public Double getMaximumLikelihoodProbability(String word)
			throws IOException {
		return (index.getWordCount(word).doubleValue() / index
				.getCollectionTermCount().doubleValue());
	}

	@Override
	public Double compareTo(LanguageModel languageModel) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
