package org.iis.plagiarismdetector.core.lucene;

import java.io.FileNotFoundException;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

/**
 *
 * @author Mostafa Dehghani
 */
public class MyAnalyzer {

	private String eol = System.getProperty("line.separator");

	static final org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(MyAnalyzer.class.getName());
	private CharArraySet stopList = null;
	private Boolean steming;
	private Boolean stopwordRemooving;

	public MyAnalyzer(Boolean steming, Set<String> stopCollection) { // In
																		// case
																		// of
																		// stopword
																		// removing
		stopList = new CharArraySet(Version.LUCENE_CURRENT, stopCollection,
				true);
		this.stopwordRemooving = true;
		this.steming = steming;
	}

	public MyAnalyzer(Boolean steming) { // In case of no stopword removing
		this.stopwordRemooving = false;
		this.steming = steming;
	}
	
	public Analyzer MyDefaultAnalyzer()
	{
		
		return new AnalyzerWrapper() {
			
			
			
			@Override
			protected Analyzer getWrappedAnalyzer(String string) {
				return new StandardAnalyzer(Version.LUCENE_CURRENT);
			}

			@Override
			protected Analyzer.TokenStreamComponents wrapComponents(
					String fieldName, Analyzer.TokenStreamComponents tsc) {
				TokenStream tokenStream =  new StandardFilter(
						Version.LUCENE_CURRENT, tsc.getTokenStream());

				tokenStream = new LowerCaseFilter(Version.LUCENE_CURRENT,
						tokenStream);
			
				return new StandardAnalyzer.TokenStreamComponents(
						tsc.getTokenizer(), tokenStream);
			}
			
		};
	}

	
	

	public Analyzer MyNgramAnalyzer()
	{
		
		return new AnalyzerWrapper() {
			
			 int wordDelimiterConfig = WordDelimiterFilter.GENERATE_WORD_PARTS; 
			        
			
			@Override
			protected Analyzer getWrappedAnalyzer(String string) {
				return new StandardAnalyzer(Version.LUCENE_CURRENT);
			}

			@Override
			protected Analyzer.TokenStreamComponents wrapComponents(
					String fieldName, Analyzer.TokenStreamComponents tsc) {
				TokenStream tokenStream = new WordDelimiterFilter( new StandardFilter(
						Version.LUCENE_CURRENT, tsc.getTokenStream()), wordDelimiterConfig, null);

				tokenStream = new LowerCaseFilter(Version.LUCENE_CURRENT,
						tokenStream);
			
				return new StandardAnalyzer.TokenStreamComponents(
						tsc.getTokenizer(), tokenStream);
			}
			
		};
	}

	
	public Analyzer MyEnglishAnalizer() {
		if (steming && stopwordRemooving) {
			return new AnalyzerWrapper() {
				@Override
				protected Analyzer getWrappedAnalyzer(String string) {
					return new StandardAnalyzer(Version.LUCENE_CURRENT);
				}

				@Override
				protected Analyzer.TokenStreamComponents wrapComponents(
						String fieldName, Analyzer.TokenStreamComponents tsc) {

					TokenStream tokenStream = new StandardFilter(
							Version.LUCENE_CURRENT, tsc.getTokenStream());
					tokenStream = new LowerCaseFilter(Version.LUCENE_CURRENT,
							tokenStream);
					tokenStream = new PorterStemFilter(tokenStream);
					tokenStream = new StopFilter(Version.LUCENE_CURRENT,
							tokenStream, stopList);
					return new StandardAnalyzer.TokenStreamComponents(
							tsc.getTokenizer(), tokenStream);
				}
			};
		} else if (!steming && stopwordRemooving) {
			return new AnalyzerWrapper() {
				@Override
				protected Analyzer getWrappedAnalyzer(String string) {
					return new StandardAnalyzer(Version.LUCENE_CURRENT);
				}

				@Override
				protected Analyzer.TokenStreamComponents wrapComponents(
						String fieldName, Analyzer.TokenStreamComponents tsc) {

					TokenStream tokenStream = new StandardFilter(
							Version.LUCENE_CURRENT, tsc.getTokenStream());
					tokenStream = new LowerCaseFilter(Version.LUCENE_CURRENT,
							tokenStream);
					tokenStream = new StopFilter(Version.LUCENE_CURRENT,
							tokenStream, stopList);
					return new StandardAnalyzer.TokenStreamComponents(
							tsc.getTokenizer(), tokenStream);
				}
			};
		} else if (steming && !stopwordRemooving) {
			return new AnalyzerWrapper() {
				@Override
				protected Analyzer getWrappedAnalyzer(String string) {
					return new StandardAnalyzer(Version.LUCENE_CURRENT);
				}

				@Override
				protected Analyzer.TokenStreamComponents wrapComponents(
						String fieldName, Analyzer.TokenStreamComponents tsc) {

					TokenStream tokenStream = new StandardFilter(
							Version.LUCENE_CURRENT, tsc.getTokenStream());
					tokenStream = new LowerCaseFilter(Version.LUCENE_CURRENT,
							tokenStream);
					tokenStream = new PorterStemFilter(tokenStream);
					return new StandardAnalyzer.TokenStreamComponents(
							tsc.getTokenizer(), tokenStream);
				}
			};
		}
		return new StandardAnalyzer(Version.LUCENE_CURRENT);
	}

	public Analyzer getAnalyzer(String Language) throws FileNotFoundException {
		Analyzer analyzer;
		if (Language.equalsIgnoreCase("EN")) {
			analyzer = MyEnglishAnalizer();
		} else {
			analyzer = new SimpleAnalyzer(Version.LUCENE_47);
		}
		return analyzer;
	}

}
