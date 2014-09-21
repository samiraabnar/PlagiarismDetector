package org.iis.plagiarismdetector.classifiers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.iis.plagiarismdetector.textalignment.ngrams.NGramPair;
import org.iis.plagiarismdetector.core.PlagiarismTypeEnum;

public class PlagiarismBinaryClassifier extends PlagiarismTypeDetector {

	@Override
	public void initalizeClassifier() throws ClassNotFoundException,
			IOException {
		wc = new SVMClassifier();
		wc.preprocess(featureTitles, plagiarismTypes);
		wc.loadModel(MODEL_NAME);
	}

	public static void main(String[] args) throws Exception {
		PlagiarismBinaryClassifier ptd = new PlagiarismBinaryClassifier();
		featuresFileShouldBeCreated = false;
		if (featuresFileShouldBeCreated) {

			ptd.createFeatureFiles(
					"evaluations/PAN2013/pan13-text-alignment-training-corpus-2013-01-21",
					"pan13-train-advance");

			ptd.createFeatureFiles(
					"evaluations/PAN2013/pan13-text-alignment-test-corpus1-2013-03-08",
					"pan13-test-advance");

		}

		ptd.train("/Users/MacBookPro/FeatureFiles/pan13-train"
				+ "-advance-features");

		ptd.test("/Users/MacBookPro/FeatureFiles/pan13-test"
				+ "-advance-features",
				"/Users/MacBookPro/FeatureFiles/pan13-train"
						+ "-advance-features");

	}

	@Override
	protected void test(String testFeaturesFilePath,
			String trainFeaturesFilePath) throws Exception {
		List<List<Double>> features = readFeatures(testFeaturesFilePath);

		wc = new SVMClassifier();
		wc.preprocess(featureTitles, plagiarismTypes);
		wc.loadModel(trainFeaturesFilePath + "model");
		Map<Double, List<Double>> trueOrFalseList = wc.test(features);

		Double prec = computePrecision(trueOrFalseList);
		Map<Double, Double> recalls = computeRecall(trueOrFalseList);

		System.out.println("Precision: " + prec);
		System.out.println("Recall for " + "No Plagiarism" + " "
				+ recalls.get(1));

		System.out
				.println("Recall for " + "Plagiarism" + " " + recalls.get(-1));

	}

	@Override
	protected void train(String trainFeaturesFilePath) throws IOException {
		List<List<Double>> features = readFeatures(trainFeaturesFilePath);
		wc = new SVMClassifier();
		wc.preprocess(featureTitles, plagiarismTypes);
		try {
			wc.train(features);
			wc.saveModel(trainFeaturesFilePath + "model");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected List<Double> parseFeatureVectorString(String featureLine) {
		// TODO Auto-generated method stub
		List<Double> featureVector = new ArrayList<Double>();
		String[] values = featureLine.split("\\s+");
		for (int i = 0; i < (values.length - 1); i++) {
			featureVector.add(Double.parseDouble(values[i]));
		}
		featureVector
				.add(Double.parseDouble(values[values.length - 1]) == PlagiarismTypeEnum.NO_PLAGIARISM
						.ordinal() ? -1D : 1D);
		return featureVector;
	}

	public PlagiarismTypeEnum classify(List<NGramPair> ngrampairs,
			List<NGramPair> npgrampairs, int suspLength, int srcLength,
			List<NGramPair> stopwordsmgrampairs) throws Exception {
		List<Double> features = extractPlagiarismPairsFeatures(ngrampairs,
				npgrampairs, stopwordsmgrampairs, suspLength, srcLength);
		if (wc == null) {
			wc = new SVMClassifier();
			wc.loadModel(MODEL_NAME);
		}
		Double dc = wc.classify(features);
		return dc.intValue() == 1 ? PlagiarismTypeEnum.RANDOM_OBFUSCATION
				: PlagiarismTypeEnum.NO_PLAGIARISM;
	}

	@Override
	protected Double computePrecision(Map<Double, List<Double>> trueOrFalseList) {
		Double totalSamples = 0D;
		Double trueDetections = 0D;
		for (Double trueClass : trueOrFalseList.keySet()) {
			for (Double detectedClass : trueOrFalseList.get(trueClass)) {
				totalSamples++;
				if (trueClass.equals(detectedClass)) {
					trueDetections++;
				} else {
					System.out.println(trueClass

					+ " " + detectedClass);
				}
			}
		}
		return trueDetections / (Math.max(1D, totalSamples));
	}
}
