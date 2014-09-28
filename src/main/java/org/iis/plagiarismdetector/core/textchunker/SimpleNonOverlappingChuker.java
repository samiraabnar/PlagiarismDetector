package org.iis.plagiarismdetector.core.textchunker;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.iis.plagiarismdetector.core.TextProcessor;

public class SimpleNonOverlappingChuker extends Chunker {

	private static final Integer CHUNK_CHARS_LIMIT = 75;

	@Override
	public List<String> chunk(String toBeChunked, String language) {
//		List<String> chunks = new ArrayList<String>();
//
//		String[] sentences = TextProcessor.getSentences(toBeChunked);
//
//		chunks.add(new String(""));
//		for (String sentence : sentences) {
//			chunks.set(chunks.size() - 1, chunks.get(chunks.size() - 1)
//					+ sentence);
//			if (chunks.get(chunks.size() - 1).split("\\s+").length >= CHUNK_WORDS_LIMIT) {
//				chunks.add(new String(""));
//			}
//		}
//
//		chunks.remove(new String(""));
//
//		return chunks;
            
            
            List<String> chunks = new ArrayList<String>();

                List<String> sentences = new ArrayList<String>();
		DocumentPreprocessor dp = new DocumentPreprocessor(new InputStreamReader( new ByteArrayInputStream(toBeChunked.getBytes())));
                for (List<HasWord> sentence : dp) {
                    String sentenceString = "";
                    for(int i = 0; i< sentence.size(); i++)
                    {
	    		  sentenceString += " "+sentence.get(i);
                    }
                    sentences.add(sentenceString.toLowerCase());
	    	  }
	      
		chunks.add(new String(""));
		for (String sentence : sentences) {
			chunks.set(chunks.size() - 1, chunks.get(chunks.size() - 1)
					+ sentence);
			if (chunks.get(chunks.size() - 1).length() >= CHUNK_CHARS_LIMIT) {
				chunks.add(new String(""));
			}
		}

		chunks.remove(new String(""));

		return chunks;

	}

	@Override
	public List<String> chunk(String toBeChunked) {
		// TODO Auto-generated method stub
		return chunk(toBeChunked, "EN");
	}

}
