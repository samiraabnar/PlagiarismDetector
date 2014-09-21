package org.iis.plagiarismdetector.core.textchunker;

import java.util.ArrayList;
import java.util.List;

import org.iis.plagiarismdetector.core.TextProcessor;

public class SimpleNonOverlappingChuker extends Chunker {

	private static final Integer CHUNK_WORDS_LIMIT = 75;

	@Override
	public List<String> chunk(String toBeChunked, String language) {
		List<String> chunks = new ArrayList<String>();

		String[] sentences = TextProcessor.getSentences(toBeChunked);

		chunks.add(new String(""));
		for (String sentence : sentences) {
			chunks.set(chunks.size() - 1, chunks.get(chunks.size() - 1)
					+ sentence);
			if (chunks.get(chunks.size() - 1).length() >= CHUNK_WORDS_LIMIT) {
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
