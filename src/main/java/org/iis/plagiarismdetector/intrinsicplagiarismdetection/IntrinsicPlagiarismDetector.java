package org.iis.plagiarismdetector.intrinsicplagiarismdetection;

import java.util.ArrayList;
import java.util.List;

public abstract class IntrinsicPlagiarismDetector {

	public abstract List<String> chunkDocument();

	public List<List<Double>> makeChunksFeatures(List<String> chunks) {
		List<List<Double>> featureVectors = new ArrayList<List<Double>>();

		for (String chunk : chunks) {
			featureVectors.add(makeChunkFeatureVector(chunk));
		}

		return featureVectors;
	}

	public abstract List<Double> makeChunkFeatureVector(String chunk);

}
