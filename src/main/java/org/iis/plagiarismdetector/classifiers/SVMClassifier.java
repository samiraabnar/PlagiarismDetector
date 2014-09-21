package org.iis.plagiarismdetector.classifiers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import libsvm.svm;

import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

public class SVMClassifier extends Classifier {

	svm mySvm = new svm();
	svm_model cModel;
	svm_parameter param = new svm_parameter();
	private List<String> featureTites;
	svm_problem trainingSet;

	public void preprocess(List<String> featureTitles, List<Double> classNames) {
		this.featureTites = featureTitles;

		param.svm_type = svm_parameter.ONE_CLASS; // C-SVM;
		param.kernel_type = svm_parameter.RBF;// RBF;
		param.gamma = 1.0 / featureTitles.size();
		param.C = 1;

		param.degree = 3;
		param.coef0 = 0;
		param.nu = 0.5;
		param.cache_size = 100;
		param.eps = 1e-4;
		param.p = 0.1;
		param.shrinking = 1;
		param.probability = 0;
		param.nr_weight = 0;
		param.weight_label = new int[0];
		param.weight = new double[0];

	}

	public void train(List<List<Double>> features)
			throws FileNotFoundException, Exception {

		trainingSet = new svm_problem();
		trainingSet.l = features.size();
		trainingSet.y = new double[features.size()];
		trainingSet.x = new svm_node[features.size()][];

		for (int i = 0; i < features.size(); i++) {
			trainingSet.x[i] = getSvmVector(features.get(i));
			trainingSet.y[i] = features.get(i).get(featureTites.size())
					.doubleValue();

		}
		svm.svm_check_parameter(trainingSet, param);
		cModel = svm.svm_train(trainingSet, param);
	}

	public svm_node[] getSvmVector(List<Double> fvec) {
		List<svm_node> featureVector = new ArrayList<svm_node>();
		for (int j = 0; j < featureTites.size(); j++) {
			if (fvec.get(j) > 0) {
				svm_node sn = new svm_node();
				sn.index = j + 1;
				sn.value = fvec.get(j);

				featureVector.add(sn);
			}
		}
		/*
		 * svm_node lastnode = new svm_node(); lastnode.index = -1;
		 * featureVector.add(lastnode);
		 */
		svm_node[] nodes = new svm_node[featureVector.size()];
		return featureVector.toArray(nodes);
	}

	public svm_model getcModel() {
		return cModel;
	}

	public void setcModel(svm_model cModel) {
		this.cModel = cModel;
	}

	@Override
	public void saveModel(String modelName) throws IOException {
		svm.svm_save_model(modelName, cModel);

	}

	public void loadModel(String modelName) throws IOException,
			ClassNotFoundException {
		cModel = svm.svm_load_model(modelName);
	}

	public Map<Double, List<Double>> test(List<List<Double>> testFeatures)
			throws Exception {
		List<Integer> result = new ArrayList<Integer>();

		svm_problem testingSet = new svm_problem();
		testingSet.l = testFeatures.size();
		testingSet.y = new double[testFeatures.size()];
		testingSet.x = new svm_node[testFeatures.size()][];

		for (int i = 0; i < testFeatures.size(); i++) {
			testingSet.x[i] = getSvmVector(testFeatures.get(i));
			testingSet.y[i] = testFeatures.get(i).get(featureTites.size())
					.doubleValue();
		}

		Map<Double, List<Double>> trueFalsePerClass = new HashMap<Double, List<Double>>();

		for (int i = 0; i < testingSet.l; i++) {
			Double trueClass = testingSet.y[i];
			if (!trueFalsePerClass.containsKey(trueClass)) {
				trueFalsePerClass.put(trueClass, new ArrayList<Double>());
			}
			double pred = classify(testingSet.x[i]);
			trueFalsePerClass.get(trueClass).add(pred);
			// double[] dist =
			// cModel.distributionForInstance(testingSet.instance(i));
			if (pred != trueClass)
				result.add(0);
			else
				result.add(1);
		}
		return trueFalsePerClass;
	}

	public Double classify(svm_node[] testFeatures) throws Exception {

		double pred = svm.svm_predict(cModel, testFeatures);

		return pred;
	}

	public Double classify(List<Double> testFeatures) throws Exception {

		double pred = svm.svm_predict(cModel, getSvmVector(testFeatures));

		return pred;
	}

	public void do_cross_validation(int nr_fold) {
		int i;
		int total_correct = 0;
		double total_error = 0;
		double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
		double[] target = new double[trainingSet.l];

		svm.svm_cross_validation(trainingSet, param, nr_fold, target);
		if (param.svm_type == svm_parameter.EPSILON_SVR
				|| param.svm_type == svm_parameter.NU_SVR) {
			for (i = 0; i < trainingSet.l; i++) {
				double y = trainingSet.y[i];
				double v = target[i];
				total_error += (v - y) * (v - y);
				sumv += v;
				sumy += y;
				sumvv += v * v;
				sumyy += y * y;
				sumvy += v * y;
			}
			System.out.print("Cross Validation Mean squared error = "
					+ total_error / trainingSet.l + "\n");
			System.out
					.print("Cross Validation Squared correlation coefficient = "
							+ ((trainingSet.l * sumvy - sumv * sumy) * (trainingSet.l
									* sumvy - sumv * sumy))
							/ ((trainingSet.l * sumvv - sumv * sumv) * (trainingSet.l
									* sumyy - sumy * sumy)) + "\n");
		} else {
			for (i = 0; i < trainingSet.l; i++)
				if (target[i] == trainingSet.y[i])
					++total_correct;
			System.out.print("Cross Validation Accuracy = " + 100.0
					* total_correct / trainingSet.l + "%\n");
		}
	}

}
