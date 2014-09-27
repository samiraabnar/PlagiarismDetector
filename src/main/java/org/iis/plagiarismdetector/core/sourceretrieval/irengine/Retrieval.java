package org.iis.plagiarismdetector.core.sourceretrieval.irengine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.iis.plagiarismdetector.core.sourceretrieval.QueryResult;
import org.iis.plagiarismdetector.core.sourceretrieval.SourceRetrievalConfig;
import org.iis.plagiarismdetector.settings.PARAMETER;

/**
 * 
 * @author Mostafa Dehghani
 * 
 **/

public class Retrieval {
	public IndexReader ireader = null;
	private Similarity[] SIM_FUNCS;

	public Retrieval(String indexPath
			) {
		try {
			ireader = IndexReader.open(new SimpleFSDirectory(
					new File(indexPath)));

		} catch (IOException e) {
			e.printStackTrace();
		}

		SIM_FUNCS = new Similarity[] {
				SourceRetrievalConfig.getParameter(PARAMETER.PARAMETERS_LM_DIRICHLET_MU) != null ? new LMDirichletSimilarity(
						SourceRetrievalConfig.getParameter(PARAMETER.PARAMETERS_LM_DIRICHLET_MU))
						: new LMDirichletSimilarity(),
				(SourceRetrievalConfig.getParameter(PARAMETER.PARAMETERS_BM25_K1) != null && SourceRetrievalConfig.getParameter(PARAMETER.PARAMETERS_BM25_b) != null) ? new BM25Similarity(
						SourceRetrievalConfig.getParameter(PARAMETER.PARAMETERS_BM25_K1),
						SourceRetrievalConfig.getParameter(PARAMETER.PARAMETERS_BM25_b))
						: new BM25Similarity(),
						SourceRetrievalConfig.getParameter(PARAMETER.PARAMETERS_LM_JM_LAMBDA) != null ? new LMJelinekMercerSimilarity(
								SourceRetrievalConfig.getParameter(PARAMETER.PARAMETERS_LM_JM_LAMBDA))
						: new LMJelinekMercerSimilarity(0.1F),
				new DefaultSimilarity() };
	}

	public List<QueryResult> searchAndReturnResults(String query, String qID, Integer k)
			throws IOException {

                try
                {
		QueryParser qParser = new QueryParser(Version.LUCENE_47, "TEXT",
				getAnalayzer(SourceRetrievalConfig.getLanguage()));
              
		BooleanQuery.setMaxClauseCount(query.split("[\\s\\-_]+").length);
		Query q = qParser.parse(QueryParser.escape(query));
 
		Similarity simFunction = SIM_FUNCS[SourceRetrievalConfig.getSimilarityFunction().ordinal()];
		IndexSearcher isearcher = new IndexSearcher(ireader);
		isearcher.setSimilarity(simFunction);
		TopFieldCollector tfc = TopFieldCollector.create(Sort.RELEVANCE,
				k, true, true, true, false);
		isearcher.search(q, tfc);
		TopDocs results = tfc.topDocs();
		ScoreDoc[] hits = results.scoreDocs;

		return fillQueryResultList(hits, qID, q);
                 }
                catch(ParseException pe)
                {
                  pe.printStackTrace();
                  System.err.println("Exceptional Query: " + qID);
                }
                
                return new ArrayList<QueryResult>();

	}

	public List<QueryResult> fillQueryResultList(ScoreDoc[] hits, String qID,
			Query q) throws IOException {

		List<QueryResult> results = new ArrayList<QueryResult>();
		File dir = new File(SourceRetrievalConfig.getSearchResultPath() + "/");
		if (!dir.exists()) {
			dir.mkdir();
		}
		RandomAccessFile res = new RandomAccessFile(
				SourceRetrievalConfig.getSearchResultPath() + "/results.txt", "rw");
		res.seek(res.length());
		for (int i = 0; i < hits.length; i++) {
			Double Score = (double) hits[i].score;
			Document hitDoc = ireader.document(hits[i].doc);
			String docID = hitDoc.get("DOCID");
			Integer srcIndexedId = hits[i].doc;
			String line = qID + " Q0 " + docID + " " + (i + 1) + " " + Score
					+ " RUN\n";
			res.writeBytes(line);

			results.add(new QueryResult(qID, docID, Score, q.toString(),
					(Integer) i + 1, srcIndexedId));
		}
		res.close();

		return results;
	}

	public void search(String query, String qID, Integer k) throws IOException,
			ParseException {

		QueryParser qParser = new QueryParser(Version.LUCENE_47, "TEXT",
				getAnalayzer(SourceRetrievalConfig.getLanguage()));
		// Query q = qParser.parse(QueryParser.escape(query));
		Query q = qParser.parse(query);

		Similarity simFunction = SIM_FUNCS[SourceRetrievalConfig.getSimilarityFunction().ordinal()];
		IndexSearcher isearcher = new IndexSearcher(ireader);
		isearcher.setSimilarity(simFunction);
		TopFieldCollector tfc = TopFieldCollector.create(Sort.RELEVANCE,
				k, true, true, true, false);
		isearcher.search(q, tfc);
		TopDocs results = tfc.topDocs();
		ScoreDoc[] hits = results.scoreDocs;
		reportInTREC(hits, qID);

	}

	private Analyzer getAnalayzer(String corpusLang)
			throws FileNotFoundException {
		if (corpusLang.equals("EN"))
			return Indexer.MyEnglishAnalizer(false, false);

		if (corpusLang.equals("FA"))
			return Indexer.MyPersianAnalyzer(false, false);
		return Indexer.MyEnglishAnalizer(false, false);
	}

	public void reportInTREC(ScoreDoc[] hits, String qID) throws IOException {
		RandomAccessFile res = new RandomAccessFile(
				SourceRetrievalConfig.getSearchResultPath(), "rw");
		res.seek(res.length());
		for (int i = 0; i < hits.length; i++) {
			float Score = hits[i].score;
			Document hitDoc = ireader.document(hits[i].doc);
			String docID = hitDoc.get("DOCID");
			String line = qID + " Q0 " + docID + " " + (i + 1) + " " + Score
					+ " RUN\n";
			res.writeBytes(line);
		}
		res.close();
	}
}