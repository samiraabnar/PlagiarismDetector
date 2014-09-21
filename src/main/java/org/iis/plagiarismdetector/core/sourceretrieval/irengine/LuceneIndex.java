package org.iis.plagiarismdetector.core.sourceretrieval.irengine;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.xml.sax.SAXException;
import org.iis.plagiarismdetector.core.TextProcessor;
import org.iis.plagiarismdetector.core.lucene.MyAnalyzer;
import org.iis.plagiarismdetector.core.sourceretrieval.SourceRetrievalConfig;
import org.iis.persiannormalizer.PersianNormalizerScheme;

public class LuceneIndex {

	private IndexWriter writer;
	private static String eol = System.getProperty("line.separator");

	private String collectionPath;
	private String collectionLang;
	private String indexPath;
	private Boolean ifStem = false;
	private Boolean ifRemoveStopwords = true;

	// private IndexReader indexReader;
	private FSDirectory dir;
	private IndexReader ir;
	private Fields fields;
	private Terms indexedTerms;

	public Set<String> Stoplistloader(String filePath)
			throws FileNotFoundException {
		Set<String> stopCollection = new HashSet<String>();
		Scanner fileScanner = new Scanner(new File(filePath));
		while (fileScanner.hasNextLine()) {
			stopCollection.add(fileScanner.nextLine().trim());
		}
		fileScanner.close();
		return stopCollection;
	}

	public String getCollectionPath() {
		return collectionPath;
	}

	public void setCollectionPath(String collectionPath) {
		this.collectionPath = collectionPath;
	}

	public String getCollectionLang() {
		return collectionLang;
	}

	public void setCollectionLang(String collectionLang) {
		this.collectionLang = collectionLang;
	}

	public Analyzer MyPersianAnalyzer(Boolean steming, Boolean stopwordRemooving)
			throws FileNotFoundException {
		final Set<String> stopword = TextProcessor.getStopWords(SourceRetrievalConfig.getLanguage());
		if (stopwordRemooving) {
			return (new MyAnalyzer(steming, stopword)).getAnalyzer("FA");
		} else
			return (new MyAnalyzer(steming)).getAnalyzer("FA");

	}

	public Analyzer MyEnglishAnalizer(Boolean steming, Boolean stopwordRemooving)
			throws FileNotFoundException {
		final Set<String> stopword = TextProcessor.getStopWords(SourceRetrievalConfig.getLanguage());
		if (stopwordRemooving) {
			return (new MyAnalyzer(steming, stopword)).getAnalyzer("EN");
		} else
			return (new MyAnalyzer(steming)).getAnalyzer("EN");
	}

	public LuceneIndex(String collectionPath, String collectionLang,
			String indexPath, Boolean ifStem, Boolean ifRemoveStopwords)
			throws IOException, ParserConfigurationException, SAXException,
			SQLException {
		this.collectionLang = collectionLang;
		this.collectionPath = collectionPath;
		this.indexPath = indexPath;
		this.ifStem = ifStem;
		this.ifRemoveStopwords = ifRemoveStopwords;
	}

	public void index() throws IOException {
		Analyzer analyzer;

		if (getCollectionLang().equals("EN")) {
			analyzer = MyEnglishAnalizer(ifStem, ifRemoveStopwords);
		} else if (getCollectionLang().equals("FA")) {
			analyzer = MyPersianAnalyzer(ifStem, ifRemoveStopwords);
		} else {
			analyzer = MyEnglishAnalizer(ifStem, ifRemoveStopwords);
		}
		IndexWriterConfig irc = new IndexWriterConfig(Version.LUCENE_47,
				analyzer);

		writer = new IndexWriter(new SimpleFSDirectory(new File(indexPath)),
				irc);
		readCorpus_plainText();
		writer.commit();
		writer.close();
		analyzer.close();
	}

	public void loadIndex() throws IOException {
		dir = FSDirectory.open(new File(indexPath));
		ir = IndexReader.open(dir);
		fields = MultiFields.getFields(ir);
		indexedTerms = fields.terms("TEXT");

	}

	public void readCorpus_plainText() throws IOException {
		File curpusPath = new File(collectionPath);
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

		doc.add(new Field("DOCID", docIDBuffer, Field.Store.YES,
				Field.Index.NOT_ANALYZED_NO_NORMS));
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
		if (collectionLang.equals("FA")) {
			doc.add(new Field("TEXT", PersianNormalizerScheme
					.PersianStringNormalizer(textBuffer), Field.Store.YES,
					Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
		} else {
			doc.add(new Field("TEXT", textBuffer, Field.Store.YES,
					Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
		}
		writer.addDocument(doc);
	}

	public Long getWordCount(String word) throws IOException {
		return ir.totalTermFreq(new Term("TEXT", word));
	}

	public Long getWordDocumentCount(String word) throws IOException {
		return ir.getSumDocFreq(word);
	}

	public Long getCollectionTermCount() throws IOException {
		return indexedTerms.getSumTotalTermFreq();
	}

	public Long getCollectionUniqueTermCount() throws IOException {
		return fields.getUniqueTermCount();
	}

	public int getDocumentCount() throws IOException {
		return indexedTerms.getDocCount();
	}

	public List<Document> getDocuments() throws IOException {
		List<Document> docs = new ArrayList<Document>();
		Bits liveDocs = MultiFields.getLiveDocs(ir);
		for (int i = 0; i < ir.maxDoc(); i++) {
			if (liveDocs != null && !liveDocs.get(i))
				continue;

			Document doc = ir.document(i);
			docs.add(doc);
		}

		return docs;
	}

	public List<Integer> getDocumentIDz() throws IOException {
		List<Integer> docs = new ArrayList<Integer>();
		Bits liveDocs = MultiFields.getLiveDocs(ir);
		for (int i = 0; i < ir.maxDoc(); i++) {
			if (liveDocs != null && !liveDocs.get(i))
				continue;

			docs.add(i);
		}

		return docs;
	}

	public Map<String, Double> getDocumentTermFrequencies(Integer docId)
			throws IOException {
		Terms vector = ir.getTermVector(docId, "TEXT");

		TermsEnum termsEnum = null;
		termsEnum = vector.iterator(termsEnum);
		Map<String, Double> frequencies = new HashMap<>();
		BytesRef text = null;
		while ((text = termsEnum.next()) != null) {
			String term = text.utf8ToString();
			Integer freq = (int) termsEnum.totalTermFreq();
			frequencies.put(term, freq.doubleValue());
		}

		return frequencies;
	}

	public String getRealDocumentId(Integer fst) throws IOException {
		return ir.document(fst).get("DOCID");
	}

	public List<Integer> getDocumentsContainingTerm(String term)
			throws IOException {
		DocsEnum de = MultiFields.getTermDocsEnum(ir,
				MultiFields.getLiveDocs(ir), "TEXT", new BytesRef(term));
		List<Integer> docIdz = new ArrayList<Integer>();
		if (de == null)
			return docIdz;
		int doc;
		try {
			while ((doc = de.nextDoc()) != DocsEnum.NO_MORE_DOCS) {

				docIdz.add(de.docID());
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		return docIdz;
	}

	public Long getDocumentSize(Integer docId) throws IOException {
		return ir.getTermVector(docId, "TEXT").size();
	}

	public IndexReader getIndexReader() {
		// TODO Auto-generated method stub
		return ir;
	}

}
