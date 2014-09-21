package org.iis.plagiarismdetector.sourceretrieval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.sun.tools.javac.util.Pair;

import org.iis.plagiarismdetector.core.TextProcessor;
import org.iis.plagiarismdetector.core.sourceretrieval.QueryResult;
import org.iis.plagiarismdetector.core.sourceretrieval.SourceRetrievalConfig;

enum QueryModel {
	EntireDocument
}

public class DocumentAsQuery extends SourceRetriever {

	public static void main(String[] args) throws Exception {
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
			DocumentAsQuery DasQ = new DocumentAsQuery();
			planAndRun(DasQ);
		}
	}

	protected List<QueryResult> mergeDifferentQueriesResults(
			Map<String, List<QueryResult>> results) {
		List<QueryResult> queryResult = new ArrayList<QueryResult>();

		for (String qId : results.keySet()) {
			Collections.sort(results.get(qId), new Comparator<QueryResult>() {

				@Override
				public int compare(QueryResult o1, QueryResult o2) {
					return o1.getScore().compareTo(o2.getScore());
				}

			});
			queryResult.addAll(results.get(qId).subList(
					0,
					Math.min(SourceRetrievalConfig.getK(), results.get(qId)
							.size())));
		}

		Collections.sort(queryResult, new Comparator<QueryResult>() {

			@Override
			public int compare(QueryResult o1, QueryResult o2) {
				return o1.getScore().compareTo(o2.getScore());
			}

		});
		return queryResult;
	}

	@Override
	protected List<Pair<String, String>> extractQueries(String suspFileName,
			String suspFileText) {
		List<Pair<String, String>> queries = new ArrayList<Pair<String, String>>();
		suspFileText = TextProcessor.removeAllKindsOfPunctuations(suspFileText);
		if (suspFileText.length() == 0) {
			System.out.println("Can not Read the File:" + suspFileName);
		}

		String queryId = suspFileName;
		queries.add(new Pair<String, String>(suspFileText.substring(0,
				suspFileText.length() - 1), queryId));


		return queries;
	}
}
