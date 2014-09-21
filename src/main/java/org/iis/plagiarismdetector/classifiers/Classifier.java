package org.iis.plagiarismdetector.classifiers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class Classifier {

	public Classifier() {
		super();
	}

	abstract public void preprocess(List<String> featureTitles,
			List<Double> classNames);

	abstract public void train(List<List<Double>> features)
			throws FileNotFoundException, Exception;

	abstract public void saveModel(String modelName) throws IOException;

	abstract public void loadModel(String modelName) throws IOException,
			ClassNotFoundException;

	abstract public Map<Double, List<Double>> test(
			List<List<Double>> testFeatures) throws Exception;

	abstract public Double classify(List<Double> testFeatures) throws Exception;

}