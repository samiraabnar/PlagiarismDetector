package org.iis.plagiarismdetector.classifiers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iis.plagiarismdetector.core.PlagiarismTypeEnum;

import weka.classifiers.trees.NBTree;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * 
 * @author Hamed
 */
class WekaBasedClassifier extends Classifier {
	weka.classifiers.Classifier cModel;
	Instances trainingSet;
	FastVector attributes;

	public void preprocess(List<String> featureTitles, List<Double> classNames) {
		attributes = new FastVector();
		for (int i = 0; i < featureTitles.size(); i++)
			attributes.addElement(new Attribute("feature-" + i));

		FastVector classVal = new FastVector(classNames.size());
		for (Double className : classNames)
			classVal.addElement(PlagiarismTypeEnum.values()[className
					.intValue()].toString());

		attributes.addElement(new Attribute("class", classVal));
	}

	public void train(List<List<Double>> features)
			throws FileNotFoundException, Exception {
		trainingSet = new Instances("Rel", attributes, features.size());
		trainingSet.setClassIndex(attributes.size() - 1);
		for (int i = 2; i < features.size(); i++) {
			Instance inst = new DenseInstance(attributes.size());
			for (int j = 0; j < attributes.size() - 1; j++) {
				inst.setValue((Attribute) attributes.elementAt(j), features
						.get(i).get(j)/* /max[j] */);
			}
			inst.setValue(
					(Attribute) attributes.elementAt(attributes.size() - 1),
					PlagiarismTypeEnum.values()[features.get(i)
							.get(features.get(i).size() - 1).intValue()]
							.toString());
			trainingSet.add(inst);
		}
		cModel = new NBTree();
		cModel.buildClassifier(trainingSet);
	}

	public weka.classifiers.Classifier getcModel() {
		return cModel;
	}

	public void setcModel(weka.classifiers.Classifier cModel) {
		this.cModel = cModel;
	}

	public void saveModel(String modelName) throws IOException {
		OutputStream file = new FileOutputStream(modelName);
		OutputStream buffer = new BufferedOutputStream(file);
		ObjectOutput output = new ObjectOutputStream(buffer);
		output.writeObject(cModel);
		output.close();

	}

	public void loadModel(String modelName) throws IOException,
			ClassNotFoundException {
		InputStream file = new FileInputStream(modelName);
		InputStream buffer = new BufferedInputStream(file);
		ObjectInput input = new ObjectInputStream(buffer);

		cModel = (weka.classifiers.Classifier) input.readObject();
		input.close();
	}

	public Map<Double, List<Double>> test(List<List<Double>> testFeatures)
			throws Exception {
		List<Integer> result = new ArrayList<Integer>();
		Instances testingSet = new Instances("Rel", attributes,
				testFeatures.size());
		testingSet.setClassIndex(attributes.size() - 1);
		for (int i = 0; i < testFeatures.size(); i++) {
			Instance inst = new DenseInstance(attributes.size());
			for (int j = 0; j < attributes.size() - 1; j++) {
				inst.setValue((Attribute) attributes.elementAt(j), testFeatures
						.get(i).get(j)/* /max[j] */);
			}
			inst.setValue(
					(Attribute) attributes.elementAt(attributes.size() - 1),
					PlagiarismTypeEnum.values()[testFeatures.get(i)
							.get(testFeatures.get(i).size() - 1).intValue()]
							.toString());
			testingSet.add(inst);
		}
		/*
		 * if (!trainingSet.equalHeaders(testingSet)) throw new
		 * IllegalArgumentException( "Train and test set are not compatible!");
		 */

		Map<Double, List<Double>> trueFalsePerClass = new HashMap<Double, List<Double>>();

		for (int i = 0; i < testingSet.numInstances(); i++) {
			Double trueClass = testingSet.instance(i).classValue();
			if (!trueFalsePerClass.containsKey(trueClass)) {
				trueFalsePerClass.put(trueClass, new ArrayList<Double>());
			}
			double pred = cModel.classifyInstance(testingSet.instance(i));
			trueFalsePerClass.get(trueClass).add(pred);
			// double[] dist =
			// cModel.distributionForInstance(testingSet.instance(i));
			if (pred != testingSet.instance(i).classValue())
				result.add(0);
			else
				result.add(1);
		}
		return trueFalsePerClass;
	}

	public Double classify(List<Double> testFeatures) throws Exception {

		Instances testingSet = new Instances("Rel", attributes,
				testFeatures.size());
		testingSet.setClassIndex(attributes.size() - 1);
		Instance inst = new DenseInstance(attributes.size());
		for (int j = 0; j < attributes.size() - 1; j++) {
			inst.setValue((Attribute) attributes.elementAt(j),
					testFeatures.get(j)/* /max[j] */);
		}
		inst.setValue(
				(Attribute) attributes.elementAt(attributes.size() - 1),
				PlagiarismTypeEnum.values()[testFeatures.get(
						testFeatures.size() - 1).intValue()].toString());
		testingSet.add(inst);

		/*
		 * if (!trainingSet.equalHeaders(testingSet)) throw new
		 * IllegalArgumentException( "Train and test set are not compatible!");
		 */

		double pred = cModel.classifyInstance(testingSet.instance(0));

		return pred;
	}

}