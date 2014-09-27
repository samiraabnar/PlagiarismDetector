package org.iis.plagiarismdetector.sourceretrieval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iis.plagiarismdetector.core.TextProcessor;
import org.iis.plagiarismdetector.core.sourceretrieval.QueryResult;
import org.iis.plagiarismdetector.core.sourceretrieval.SourceRetrievalConfig;

import com.sun.tools.javac.util.Pair;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

public class DocumentSentenceAsQuery extends DocumentAsQuery{
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
			DocumentSentenceAsQuery DSasQ = new DocumentSentenceAsQuery();
			try {
				planAndRun(DSasQ);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected List<Pair<String, String>> extractQueries(String suspFileName,
			String suspFileText) {
		List<Pair<String, String>> queries = new ArrayList<Pair<String, String>>();

		if (suspFileText.length() == 0) {
			System.out.println("Can not Read the File:" + suspFileName);
		}
		List<String> sentences = new ArrayList<String>();
		
  	  String sentenceString = "";
		DocumentPreprocessor dp = new DocumentPreprocessor(new InputStreamReader( new ByteArrayInputStream(suspFileText.getBytes())));
	      for (List<HasWord> sentence : dp) {
	    	  for(int i = 0; i< sentence.size(); i++)
	    		  sentenceString += " "+sentence.get(i);
	    	  
	    	  if(sentenceString.split(" ").length > 5)
	    	  {
	    		  sentences.add(sentenceString.toLowerCase());
	    		  sentenceString = "";
	    	  }
	      }
	      
		int i = 0;
		String queryId = suspFileName;
		for (String chunk : sentences) {
			chunk = TextProcessor.removeAllKindsOfPunctuations(chunk);
			chunk = chunk.trim();
			if (chunk.length() > 1) {
				queries.add(new Pair<String, String>(chunk.substring(0,
						chunk.length() - 1), queryId + "_" + i));
				i++;
			}

		}
		
		return queries;
	}
}
