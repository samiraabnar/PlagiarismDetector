package org.iis.plagiarismdetector.core.sourceretrieval.irengine;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.util.Version;

public class LuceneNGramAnalyzer extends Analyzer {

	private static Integer MaxN = 3;
	private static Integer MinN = 1;

	@Override
	protected TokenStreamComponents createComponents(String fieldName,
			Reader reader) {
		Tokenizer source = new NGramTokenizer(Version.LUCENE_47, reader, MinN,
				MaxN);
		TokenStream filter = new LowerCaseFilter(Version.LUCENE_47, source);
		return new TokenStreamComponents(source, filter);
	}

}
