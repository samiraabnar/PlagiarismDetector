/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.iis.plagiarismdetector.core.sourceretrieval.irengine;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.iis.persiannormalizer.PersianNormalizerScheme;
import org.iis.plagiarismdetector.core.TextProcessor;
import org.iis.plagiarismdetector.core.lucene.MyAnalyzer;
import org.iis.plagiarismdetector.core.sourceretrieval.SourceRetrievalConfig;
import org.xml.sax.SAXException;


public class Indexer {
	private  IndexWriter writer;
	private String corpusPath;
	private static String eol = System.getProperty("line.separator");

	public static Set<String> Stoplistloader(String filePath)
			throws FileNotFoundException {
		Set<String> stopCollection = new HashSet<String>();
		Scanner fileScanner = new Scanner(new File(filePath));
		while (fileScanner.hasNextLine()) {
			stopCollection.add(fileScanner.nextLine().trim());
		}
		fileScanner.close();
		return stopCollection;
	}

	public static Analyzer MyPersianAnalyzer(Boolean steming,
			Boolean stopwordRemooving) throws FileNotFoundException {
		final Set<String> stopword = TextProcessor.getStopWords(SourceRetrievalConfig.getLanguage());
		if (stopwordRemooving) {
			return (new MyAnalyzer(steming, stopword)).getAnalyzer("FA");
		} else
			return (new MyAnalyzer(steming)).getAnalyzer("FA");

	}

	public static Analyzer MyEnglishAnalizer(Boolean steming,
			Boolean stopwordRemooving) throws FileNotFoundException {
		final Set<String> stopword = TextProcessor.getStopWords(SourceRetrievalConfig.getLanguage());
		if (stopwordRemooving) {
			return (new MyAnalyzer(steming, stopword)).getAnalyzer("EN");
		} else
			return (new MyAnalyzer(steming)).getAnalyzer("EN");
	}


	public Indexer(String indexPath,String corpusPath)
			throws IOException, ParserConfigurationException, SAXException,
			SQLException {
		this.corpusPath = corpusPath;
		Analyzer analyzer;
		if (SourceRetrievalConfig.getLanguage().equals("EN")) {
			analyzer = MyEnglishAnalizer(SourceRetrievalConfig.get("IF_STEM"),
					SourceRetrievalConfig.get("IF_REMOVE_STOPWORDS"));
		} else if (SourceRetrievalConfig.getLanguage().equals("FA")) {
			analyzer = MyPersianAnalyzer(SourceRetrievalConfig.get("IF_STEM"),
					SourceRetrievalConfig.get("IF_REMOVE_STOPWORDS"));
		} else {
			analyzer = new SimpleAnalyzer(Version.LUCENE_47);
		}
		IndexWriterConfig irc = new IndexWriterConfig(Version.LUCENE_47,
				analyzer);

		writer = new IndexWriter(new SimpleFSDirectory(new File(
				indexPath)), irc);
		readCorpus_plainText();
		writer.commit();
		writer.close();
		analyzer.close();
	}

	public void readCorpus_plainText() throws IOException {
		File curpusPath = new File(corpusPath);
		for (File f : curpusPath.listFiles()) {
			String text = TextProcessor.getMatn(f);

			String docId = f.getName();
			if (f.getName().contains("/"))
				docId = f.getName().substring(f.getName().lastIndexOf("/"));
			docId = docId.replaceAll(".txt", "");
			indexDocument(docId.trim(), text.trim());
			System.out.println("Indexer: Document ID -> " + docId.trim());
		}
	}

	

	public void indexDocument(String docIDBuffer, String subjectBuffer,
			String textBuffer) throws IOException {
		Document doc = new Document();

		doc.add(new Field("DOCID", docIDBuffer, Field.Store.YES, Field.Index.NO));
		doc.add(new Field("SUBJECT", PersianNormalizerScheme
				.PersianStringNormalizer(subjectBuffer), Field.Store.YES,
				Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
		doc.add(new Field("TEXT", PersianNormalizerScheme
				.PersianStringNormalizer(textBuffer), Field.Store.YES,
				Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
		writer.addDocument(doc);
	}

	public void indexDocument(String docIDBuffer, String textBuffer)
			throws IOException {
		Document doc = new Document();

		doc.add(new Field("DOCID", docIDBuffer, Field.Store.YES, Field.Index.NO));
		if (SourceRetrievalConfig.getLanguage().equals("FA")) {
			doc.add(new Field("TEXT", PersianNormalizerScheme
					.PersianStringNormalizer(textBuffer), Field.Store.YES,
					Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
		} else {
			doc.add(new Field("TEXT", textBuffer, Field.Store.YES,
					Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
		}
		writer.addDocument(doc);
	}

}
