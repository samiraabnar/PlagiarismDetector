package org.iis.plagiarismdetector.sourceretrieval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.queryparser.classic.ParseException;
import org.iis.plagiarismdetector.core.JudgementFileFormat;
import org.iis.plagiarismdetector.core.SimilarityFunction;
import org.iis.plagiarismdetector.core.TextProcessor;
import org.iis.plagiarismdetector.core.sourceretrieval.EvaluationSummary;
import org.iis.plagiarismdetector.core.sourceretrieval.QueryResult;
import org.iis.plagiarismdetector.core.sourceretrieval.SourceRetrievalConfig;
import org.iis.plagiarismdetector.core.sourceretrieval.irengine.Indexer;
import org.iis.plagiarismdetector.core.sourceretrieval.irengine.Retrieval;
import org.iis.plagiarismdetector.settings.PARAMETER;
import org.xml.sax.SAXException;

import com.sun.tools.javac.util.Pair;

public abstract class SourceRetriever {

	protected SourceRetrievalConfig sourceRetrievalConfig;
	String referencesDirectory = "";
	String suspDirectory = "";
	private Map<String, List<QueryResult>> FinalResults = new HashMap<String, List<QueryResult>>();
	protected Retrieval retriever;

	public void initialize(String collectionPath
			) {
		retriever = new Retrieval(collectionPath);
	}

	public void pairToTrecJudgeFileFormatConvertor(String pairFileName,
			String trecFileName) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(pairFileName));

		String line = null;
		String trecJudgeString = "";
		while ((line = br.readLine()) != null) {
			String[] pair = line.split(" ");
			String susp = pair[0].replace(".txt", "").trim();
			String src = pair[1].replace(".txt", "").trim();

			trecJudgeString += susp + " 0 " + src + " 1\n";

		}
		br.close();
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
				trecFileName)));
		bw.write(trecJudgeString);
		bw.close();
	}

	public static void planAndRun(SourceRetriever sourceRetriever)
			throws Exception {
		
		Map<PARAMETER, Float> similarityFunctionparams = new HashMap<PARAMETER, Float>();
	
		Map<String, Object> experimentOptions = new HashMap<String, Object>();
		

		similarityFunctionparams = new HashMap<PARAMETER, Float>();
		similarityFunctionparams.put(PARAMETER.PARAMETERS_BM25_b, 1.2F);
		similarityFunctionparams.put(PARAMETER.PARAMETERS_BM25_K1, 0.75F);
		similarityFunctionparams.put(PARAMETER.PARAMETERS_JM_LAMBDA, 0.7F);
		similarityFunctionparams.put(PARAMETER.PARAMETERS_DIRICHKET_MIU,
				1000.0F);

		experimentOptions = new HashMap<String, Object>();
		experimentOptions.put("CORPUS_MAIN_DIR", SourceRetrievalConfig.getCorpusMainDir());
		experimentOptions.put("CORPUS_DIR",
				SourceRetrievalConfig.getCorpusPath());
		experimentOptions.put("CORPUS_LANG",  SourceRetrievalConfig.getLanguage());
		experimentOptions.put("TOP_K", SourceRetrievalConfig.getK());
		experimentOptions.put("SIMILARITY_FUNCTION", SimilarityFunction.JM);
		experimentOptions.put("PARAMS", similarityFunctionparams);
		experimentOptions.put("JUDGEMENTS_PATH",
				SourceRetrievalConfig.getJudgePath());
		experimentOptions.put("SUSP_FILE_DIR",
				SourceRetrievalConfig.getSuspCorpusPath());

		sourceRetriever.doTheExperiment(1, experimentOptions,
				"adhoc IR with lucene: on PAN2010 Corpus with"
						+ experimentOptions.get("SIMILARITY_FUNCTION")
						+ " as similarity function", false/* if reindex */);
	}

	public void doTheExperiment(Integer expNo,
			Map<String, Object> experimentOptions,
			String experimentDescription, boolean reIndex) throws Exception {
		try {
		

			if (reIndex)
				new Indexer(SourceRetrievalConfig.getSrcIndexPath(),SourceRetrievalConfig.getSrcCorpusPath());
			initialize(SourceRetrievalConfig.getSrcIndexPath());
			retrieve(expNo, experimentOptions, experimentDescription);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public void retrieve(Integer expNo, Map<String, Object> experimentOptions,
			String experimentDescription) throws Exception {
		File suspPath = new File(SourceRetrievalConfig.getSuspCorpusPath());
		for (File f : suspPath.listFiles(new FileFilter() {

			@Override
			public boolean accept(File arg0) {
				// TODO Auto-generated method stub
				return !arg0.getName().startsWith(".");
			}
		})) {
			Map<String, List<QueryResult>> results = processSuspDocument(f
					.getPath());
			List<QueryResult> finalResultsPerSuspDoc = mergeDifferentQueriesResults(results);

			String suspFileName = f.getName().contains("/") ? f.getName()
					.substring(f.getName().lastIndexOf("/") + 1)
					.replace(".txt", "") : f.getName().replace(".txt", "");
			getFinalResults().put(suspFileName, finalResultsPerSuspDoc);

			reportInTREC(finalResultsPerSuspDoc, suspFileName);
		}
		EvaluationSummary evalSummary = evaluateResults(getFinalResults(),
				(String) experimentOptions.get("JUDGEMENTS_PATH"),
				JudgementFileFormat.Pairs);
		evalSummary.setExperimentNumber(expNo.toString());
		evalSummary.setExperimentDescription(experimentDescription);
		evalSummary.setExperimentOptions(experimentOptions);
		System.out.println(evalSummary);
	}

	protected void reportInTREC(List<QueryResult> finalResultsPerSuspDoc,
			String qID) throws IOException {
		File dir = new File(SourceRetrievalConfig.getSearchResultPath());
		if (!dir.exists()) {
			dir.mkdir();
		}
		BufferedWriter res = new BufferedWriter(new FileWriter(
				SourceRetrievalConfig.getSearchResultPath() + qID + ".txt"));
		for (int i = 0; i < finalResultsPerSuspDoc.size(); i++) {
			Double score = finalResultsPerSuspDoc.get(i).getScore();
			String docID = finalResultsPerSuspDoc.get(i).getDocumentId();
			String line = qID + " Q0 " + docID.substring(docID.indexOf("-")+1) + " " + (i + 1) + " " + score
					+ " RUN\n";
			res.write(line);
		}
		res.close();
	}

	protected EvaluationSummary evaluateResults(
			Map<String, List<QueryResult>> results, String judgementsFileDir,
			JudgementFileFormat jformat) throws IOException {
		Map<String, List<String>> goldResults = readJudgeFile(
				judgementsFileDir, jformat);

		Double macroPrecision = calculateMacroPercision(results, goldResults);
		Double microPrecision = calculateMicroPercision(results, goldResults);

		Double macroRecall = calculateMacroRecall(results, goldResults);
		Double microRecall = calculateMicroRecall(results, goldResults);

		EvaluationSummary eSum = new EvaluationSummary();
		eSum.setMicroPrecision(microPrecision);
		eSum.setMicroRecall(microRecall);
		eSum.setMacroPrecision(macroPrecision);
		eSum.setMacroRecall(macroRecall);
		return eSum;

	}

	private Map<String, List<String>> readJudgeFile(String judgementsFileDir,
			JudgementFileFormat jformat) throws IOException {

		Map<String, List<String>> judgements = new HashMap<String, List<String>>();
		BufferedReader br = new BufferedReader(
				new FileReader(judgementsFileDir));

		if (jformat.equals(JudgementFileFormat.Pairs)) {
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] pair = line.split(" ");
				String susp = pair[0].replace(".txt", "").trim();
				String src = pair[1].replace(".txt", "").trim();

				if (!judgements.containsKey(susp))
					judgements.put(susp, new ArrayList<String>());
				judgements.get(susp).add(src);
			}
		}
		br.close();
		return judgements;
	}

	private Double calculateMacroRecall(Map<String, List<QueryResult>> results,
			Map<String, List<String>> goldResults) {
		Double TP = 0D, FN = 0D;
		Double totalCount = 0D;
		for (String suspFile : results.keySet()) {
			for (String foundCandidate : goldResults.get(suspFile)) {
				QueryResult dummyQueryResult = new QueryResult(suspFile,
						foundCandidate, 1D, null, null, null);
				if (results.get(suspFile).contains(dummyQueryResult)) {
					TP++;
				}
			}

			totalCount += (double) goldResults.get(suspFile).size();
		}
		return (TP / totalCount);
	}

	private Double calculateMicroRecall(Map<String, List<QueryResult>> results,
			Map<String, List<String>> goldResults) {

		Map<String, Double> recalls = new HashMap<String, Double>();
		Double averagedRecall = 0D;
		for (String suspFile : results.keySet()) {
			Double TP = 0D, FN = 0D;

			for (String foundCandidate : goldResults.get(suspFile)) {
				QueryResult dummyQueryResult = new QueryResult(suspFile,
						foundCandidate, 1D, null, null, null);
				if (results.get(suspFile).contains(dummyQueryResult)) {
					TP++;
				}
			}

			Double localRecall = 0D;
			if (goldResults.get(suspFile).size() > 0)
				localRecall = (TP / goldResults.get(suspFile).size());
			averagedRecall += localRecall;
			recalls.put(suspFile, localRecall);
		}

		return averagedRecall / recalls.size();
	}

	private Double calculateMicroPercision(
			Map<String, List<QueryResult>> results,
			Map<String, List<String>> goldResults) {

		Map<String, Double> precisions = new HashMap<String, Double>();
		Double averagedPrecison = 0D;
		for (String suspFile : results.keySet()) {
			Double TP = 0D, FP = 0D;
			for (QueryResult foundCandidate : results.get(suspFile)) {
				if (goldResults.get(suspFile).contains(
						foundCandidate.getDocumentId())) {
					TP++;
				} else {
					FP++;
				}
			}
			Double localPrec = 0D;
			if ((TP + FP) > 0)
				localPrec = (TP / (TP + FP));

			averagedPrecison += localPrec;
			precisions.put(suspFile, localPrec);
		}

		return averagedPrecison / precisions.keySet().size();
	}

	private Double calculateMacroPercision(
			Map<String, List<QueryResult>> results,
			Map<String, List<String>> goldResults) {
		Double TP = 0D, FP = 0D;
		for (String suspFile : results.keySet()) {
			for (QueryResult foundCandidate : results.get(suspFile)) {

				if (goldResults.get(suspFile).contains(
						foundCandidate.getDocumentId())) {
					TP++;
				} else {
					FP++;
				}
			}
		}

		if ((TP + FP) == 0)
			return 0D;
		return (TP / (TP + FP));
	}

	public Map<String, List<QueryResult>> processSuspDocument(
			String suspFilePath) throws Exception {
		String suspFileText = TextProcessor.getMatn(new File(suspFilePath));
		String suspFileName = suspFilePath.contains("/") ? suspFilePath
				.substring(suspFilePath.lastIndexOf("/") + 1).replace(".txt",
						"") : suspFilePath.replace(".txt", "");
		List<Pair<String, String>> queries = extractQueries(suspFileName,
				suspFileText);
		Map<String, List<QueryResult>> results = new HashMap<String, List<QueryResult>>();
		for (Pair<String, String> query : queries) {
			if (!results.containsKey(query.snd))
				results.put(query.snd, new ArrayList<QueryResult>());
			results.get(query.snd).addAll(submitQuery(query.fst, query.snd,SourceRetrievalConfig.getK()));
		}
		return results;

	}

	abstract protected List<QueryResult> mergeDifferentQueriesResults(
			Map<String, List<QueryResult>> results);

	abstract protected List<Pair<String, String>> extractQueries(
			String suspFileName, String suspFileText) throws IOException,
			Exception;

	protected List<QueryResult> submitQuery(String query, String queryId, Integer k)
			throws IOException, ParseException {

		List<QueryResult> results = new ArrayList<QueryResult>();

		results = retriever.searchAndReturnResults(query, queryId,k);
		return results;
	}

	public Map<String, List<QueryResult>> getFinalResults() {
		return FinalResults;
	}

	public void setFinalResults(Map<String, List<QueryResult>> finalResults) {
		FinalResults = finalResults;
	}

}