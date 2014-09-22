package org.iis.plagiarismdetector.sourceretrieval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.tools.javac.util.Pair;

import org.iis.plagiarismdetector.core.TextProcessor;
import org.iis.plagiarismdetector.core.sourceretrieval.QueryResult;
import org.iis.plagiarismdetector.core.sourceretrieval.SourceRetrievalConfig;
import org.iis.plagiarismdetector.core.textchunker.Chunker;
import org.iis.plagiarismdetector.core.textchunker.SimpleNonOverlappingChuker;

public class DocumentChunksAsQuery extends DocumentAsQuery {
	public static void main(String[] args) {
		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].startsWith("-")) {
					if (args[i].equals("-refDir")) {

					} else if (args[i].equals("-suspDir")) {

					} else if (args[i].equals("-retrievalModel")) {

					} else if (args[i].equals("-queryModel")) {

					}
				}
			}
		} else {
			DocumentChunksAsQuery DCasQ = new DocumentChunksAsQuery();
			try {
				planAndRun(DCasQ);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected List<QueryResult> mergeDifferentQueriesResults(
			Map<String, List<QueryResult>> results) {

		Map<String, QueryResult> qrMap = new HashMap<String, QueryResult>();
		for (String qId : results.keySet()) {
			Collections.sort(results.get(qId), new Comparator<QueryResult>() {

				@Override
				public int compare(QueryResult o1, QueryResult o2) {
					return o2.getScore().compareTo(o1.getScore());
				}

			});

			for (int k = 0; k < Math.min(results.get(qId).size(),
					SourceRetrievalConfig.getK()); k++) {
				results.get(qId)
						.get(k)
						.setScore(
								(double) (
										SourceRetrievalConfig.getK()- k));
				if (qrMap.containsKey(results.get(qId).get(k).getDocumentId())) {

					qrMap.get(results.get(qId).get(k).getDocumentId())
							.setScore(
									results.get(qId).get(k).getScore()
											+ qrMap.get(
													results.get(qId).get(k)
															.getDocumentId())
													.getScore());
				} else {
					qrMap.put(results.get(qId).get(k).getDocumentId(), results
							.get(qId).get(k));
				}
			}

		}
		List<QueryResult> queryResult = new ArrayList<QueryResult>(
				qrMap.values());

		Collections.sort(queryResult, new Comparator<QueryResult>() {

			@Override
			public int compare(QueryResult o1, QueryResult o2) {
				return o2.getScore().compareTo(o1.getScore());
			}

		});
		return queryResult.subList(0, SourceRetrievalConfig.getK());
	}

	@Override
	protected List<Pair<String, String>> extractQueries(String suspFileName,
			String suspFileText) {
		Chunker chunker = new SimpleNonOverlappingChuker();
		List<Pair<String, String>> queries = new ArrayList<Pair<String, String>>();

		if (suspFileText.length() == 0) {
			System.out.println("Can not Read the File:" + suspFileName);
		}
		List<String> chunks = chunker.chunk(suspFileText.toLowerCase());
		int i = 0;
		String queryId = suspFileName;
		for (String chunk : chunks) {
			chunk = TextProcessor.removeAllKindsOfPunctuations(chunk);
			chunk = chunk.trim();
			if (chunk.length() > 1) {
				queries.add(new Pair<String, String>(chunk.substring(0,
						chunk.length() - 1), queryId + "_" + i));
				i++;
			}

		}
		/*
		 * while(lastIndex < suspFileText.length()) { String queryId =
		 * suspFileName; queryId += "_"+i; queries.add(new
		 * Pair<String,String>(suspFileText.substring(lastIndex,
		 * (lastIndex+MAX_QUERY_LENGTH) <
		 * suspFileText.length()?lastIndex+MAX_QUERY_LENGTH -
		 * 1:suspFileText.length()-1 ),queryId)); lastIndex += MAX_QUERY_LENGTH;
		 * i++; }
		 */
		return queries;
	}
}
