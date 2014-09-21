package org.iis.plagiarismdetector.classifiers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iis.plagiarismdetector.settings.TextAlignmentDatasetSettings;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramBasedTextAligner;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramExtractor;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramFeature;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramPair;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramSimilarityType;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramType;

import com.sun.tools.javac.util.Pair;

import org.iis.plagiarismdetector.core.PlagiarismTypeEnum;
import org.iis.plagiarismdetector.core.TextProcessor;
import org.iis.plagiarismdetector.core.lucene.IndexInfo;

import de.jungblut.math.DoubleVector;
import de.jungblut.math.dense.DenseDoubleVector;

public class PlagiarismTypeDetector {

	private static Long docIdCount = 0L;
	private static final Double NGRAM_SIMILARITY_THRESHOLD = 0.05D;
	private static final Double NGRAM_DISPERSITY_THRESHOLD = 0.3;
	protected static final String MODEL_NAME = "featureModel";
	private static final Integer N = 2;
	private static final Integer M = 6;
	private static final Boolean IFSTEM = null;
	private Map<Long, Map<String, Object>> documents = new HashMap<Long, Map<String, Object>>();
	protected static List<Double> plagiarismTypes = new ArrayList<Double>();

	private Map<String, Map<String, NGramFeature>> lastSuspFileNGrams = new HashMap<String, Map<String, NGramFeature>>();
	private Map<String, Map<String, NGramFeature>> lastSrcFileNGrams = new HashMap<String, Map<String, NGramFeature>>();

	private Map<String, Map<String, NGramFeature>> lastSuspFileNPlusGrams = new HashMap<String, Map<String, NGramFeature>>();
	private Map<String, Map<String, NGramFeature>> lastSrcFileNPlusGrams = new HashMap<String, Map<String, NGramFeature>>();
	BufferedWriter writer = null;
	private NGramType ngramType = NGramType.Word;
	private Map<String, Map<String, NGramFeature>> lastSuspFileStopWordMGrams = new HashMap<String, Map<String, NGramFeature>>();
	private Map<String, Map<String, NGramFeature>> lastSrcFileStopWordMGrams = new HashMap<String, Map<String, NGramFeature>>();
	/**
	 * @param args
	 */

	public static List<String> featureTitles = new ArrayList<String>();
	protected static boolean featuresFileShouldBeCreated = false;
	static {
		featureTitles.add("2gramPairsCount");
		featureTitles.add("3gramPairsCount");

		featureTitles.add("2gramPairsRatio");
		featureTitles.add("3gramPairsRatio");

		featureTitles.add("2-3gramPairsRatio");

		featureTitles.add("suspLength");
		featureTitles.add("sourceLength");
		featureTitles.add("susp2sourceLength");

		featureTitles.add("2gramPairsAvgWeight");
		featureTitles.add("3gramPairsAvgWeight");

		featureTitles.add("2gramPairsWeightSDEV");
		featureTitles.add("3gramPairsWeightSDEV");

		featureTitles.add("2gramPairsMaxWeight");
		featureTitles.add("3gramPairsMaxWeight");

		featureTitles.add("2gramPairsMinWeight");
		featureTitles.add("3gramPairsMinWeight");
		featureTitles.add("CommonStopwordsCount");

		featureTitles.add("CommonStopwordsRatio");

		// featureTitles.add("2gramPairsMaxDist");
		// featureTitles.add("3gramPairsMaxDist");

		// featureTitles.add("2gramPairsMinDist");
		// featureTitles.add("3gramPairsMinDist");

		// featureTitles.add("SuspLength");
		// featureTitles.add("SourceLength");

		// featureTitles.add("SuspToSrcLengthRatio");
		// featureTitles.add("SuspSourceLengthDiff");

		plagiarismTypes
				.add((double) PlagiarismTypeEnum.NO_PLAGIARISM.ordinal());
		plagiarismTypes.add((double) PlagiarismTypeEnum.NO_OBFUSCATION
				.ordinal());
		plagiarismTypes.add((double) PlagiarismTypeEnum.RANDOM_OBFUSCATION
				.ordinal());
		plagiarismTypes.add((double) PlagiarismTypeEnum.TRANSLATION_OBFUSCATION
				.ordinal());
		plagiarismTypes.add((double) PlagiarismTypeEnum.SUMMARY_OBFUSCATION
				.ordinal());
	}

	List<Double> extractPlagiarismPairsFeatures(List<NGramPair> ngrampairs,
			List<NGramPair> npgrampairs, List<NGramPair> stopwordsmgrampairs,
			int i, int j) {
		List<Double> features = new ArrayList<Double>();

		List<NGramPair> sortedngrampairs = new ArrayList<NGramPair>(ngrampairs);
		Collections.sort(sortedngrampairs, new Comparator<NGramPair>() {

			@Override
			public int compare(NGramPair o1, NGramPair o2) {
				return o1.getWeight().compareTo(o2.getWeight());
			}
		});

		List<NGramPair> sortednpgrampairs = new ArrayList<NGramPair>(
				npgrampairs);
		Collections.sort(sortednpgrampairs, new Comparator<NGramPair>() {

			@Override
			public int compare(NGramPair o1, NGramPair o2) {
				return o1.getWeight().compareTo(o2.getWeight());
			}
		});

		Double ngramMean = computeNGramsAvgWeight(sortedngrampairs);
		Double npgramMean = computeNGramsAvgWeight(sortednpgrampairs);

		Double ngramSDev = getStdDev(sortedngrampairs, ngramMean);
		Double npgramSDev = getStdDev(sortednpgrampairs, npgramMean);

		for (String title : featureTitles) {
			if (title.equals("2gramPairsCount")) {
				features.add((double) ngrampairs.size());
			}

			if (title.equals("3gramPairsCount")) {
				features.add((double) npgrampairs.size());
			}

			if (title.equals("2gramPairsRatio")) {
				features.add(((double) ngrampairs.size()) / (i + j));
			}

			if (title.equals("3gramPairsRatio")) {
				features.add(((double) npgrampairs.size()) / (i + j));
			}

			if (title.equals("2-3gramPairsRatio")) {
				if (npgrampairs.size() > 0)
					features.add((double) ngrampairs.size()
							/ (double) npgrampairs.size());
				else {
					if (ngrampairs.size() > 0)
						features.add(Double.POSITIVE_INFINITY);
					else {
						features.add(1D);
					}
				}

			}
			if (title.equals("suspLength")) {
				features.add((double) (i));
			}

			if (title.equals("sourceLength")) {
				features.add((double) (j));
			}

			if (title.equals("susp2sourceLength")) {
				features.add(((double) i / (double) j));
			}

			if (title.equals("2gramPairsAvgWeight")) {
				features.add(ngramMean);
			}
			if (title.equals("3gramPairsAvgWeight")) {
				features.add(npgramMean);
			}

			if (title.equals("2gramPairsWeightSDEV")) {
				features.add(ngramSDev);
			}

			if (title.equals("3gramPairsWeightSDEV")) {
				features.add(npgramSDev);
			}

			if (title.equals("2gramPairsMaxWeight")) {
				if (sortedngrampairs.size() > 0)
					features.add(sortedngrampairs.get(
							sortedngrampairs.size() - 1).getWeight());
				else
					features.add(0D);
			}

			if (title.equals("3gramPairsMaxWeight")) {
				if (sortednpgrampairs.size() > 0)
					features.add(sortednpgrampairs.get(
							sortednpgrampairs.size() - 1).getWeight());
				else
					features.add(0D);
			}

			if (title.equals("2gramPairsMinWeight")) {
				if (sortedngrampairs.size() > 0)
					features.add(sortedngrampairs.get(0).getWeight());
				else
					features.add(0D);
			}

			if (title.equals("3gramPairsMinWeight")) {
				if (sortednpgrampairs.size() > 0)
					features.add(sortednpgrampairs.get(0).getWeight());
				else
					features.add(0D);
			}

			if (title.equals("CommonStopwordsCount")) {
				features.add((double) stopwordsmgrampairs.size());
			}
			if (title.equals("CommonStopwordsRatio")) {
				features.add((double) stopwordsmgrampairs.size() / (i + j));
			}

		}
		return features;
	}

	private Double computeNGramsAvgWeight(List<NGramPair> ngrampairs) {
		Double totalWeights = 0D;
		for (int i = 0; i < ngrampairs.size(); i++) {
			totalWeights += ngrampairs.get(i).getWeight();
		}
		return (totalWeights / Math.max((double) ngrampairs.size(), 1D));
	}

	double getVariance(List<NGramPair> ngrampairs, Double mean) {
		double temp = 0;
		for (NGramPair a : ngrampairs)
			temp += (mean - a.getWeight()) * (mean - a.getWeight());
		return temp / Math.max((double) ngrampairs.size(), 1D);
	}

	double getStdDev(List<NGramPair> ngrampairs, Double mean) {
		return Math.sqrt(getVariance(ngrampairs, mean));
	}

	public double median(List<NGramPair> sortedngrampairs) {

		if (sortedngrampairs.size() % 2 == 0) {
			return (sortedngrampairs.get((sortedngrampairs.size() / 2) - 1)
					.getWeight() + sortedngrampairs.get(
					sortedngrampairs.size() / 2).getWeight()) / 2.0;
		} else {
			return sortedngrampairs.get(sortedngrampairs.size() / 2)
					.getWeight();
		}
	}

	Classifier wc = new WekaBasedClassifier();
	private Map<String, Integer> lastSuspFileLength = new HashMap<String, Integer>();
	private Map<String, Integer> lastSrcFileLength = new HashMap<String, Integer>();
	private IndexInfo indexInfo;;

	public PlagiarismTypeEnum decideOnTypeOfPlagiarism(
			List<NGramPair> ngrampairs, List<NGramPair> npgrampairs,
			List<NGramPair> stopwordsmgrampairs, int suspLength, int srcLength)
			throws Exception {

		return PlagiarismTypeEnum.values()[wc.classify(
				extractPlagiarismPairsFeatures(ngrampairs, npgrampairs,
						stopwordsmgrampairs, suspLength, srcLength)).intValue()];
	}

	protected void createFeatureFiles(String trainingCorpus, String corpusId) {

		try {
			writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(corpusId + "-features"), "utf-8"));
			writer.write(getFeatureVectorHeader() + "\n");

		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// no-obfuscation
		try {

			createFeatureFilesPerType(trainingCorpus, "01-no-plagiarism",
					PlagiarismTypeEnum.NO_PLAGIARISM);
			writer.flush();
			createFeatureFilesPerType(trainingCorpus, "02-no-obfuscation",
					PlagiarismTypeEnum.NO_OBFUSCATION);
			writer.flush();
			createFeatureFilesPerType(trainingCorpus, "03-random-obfuscation",
					PlagiarismTypeEnum.RANDOM_OBFUSCATION);

			writer.flush();
			createFeatureFilesPerType(trainingCorpus, "05-summary-obfuscation",
					PlagiarismTypeEnum.SUMMARY_OBFUSCATION);
			writer.flush();

			createFeatureFilesPerType(trainingCorpus,
					"04-translation-obfuscation",
					PlagiarismTypeEnum.TRANSLATION_OBFUSCATION);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private String getFeatureVectorHeader() {
		String header = "";
		for (int i = 0; i < featureTitles.size(); i++)
			header += featureTitles.get(i) + " ";
		return header.trim();
	}

	public List<Double> getFeatureVector(String suspFileString,
			String srcFileString, Long suspDocId, Long srcDocId)
			throws Exception {

		Map<String, NGramFeature> suspDocNgrams = new HashMap<String, NGramFeature>();
		Map<String, NGramFeature> srcDocNgrams = new HashMap<String, NGramFeature>();
		Map<String, NGramFeature> suspDocNPlusgrams = new HashMap<String, NGramFeature>();
		Map<String, NGramFeature> srcDocNPlusgrams = new HashMap<String, NGramFeature>();

		Map<String, NGramFeature> suspDocStopwordMgrams = new HashMap<String, NGramFeature>();
		Map<String, NGramFeature> srcDocStopwordMgrams = new HashMap<String, NGramFeature>();

		Map<String, NGramFeature> filteredSuspDocNgrams = new HashMap<String, NGramFeature>();

		forcefillInNGramLists(suspDocId, suspDocNgrams, suspDocNPlusgrams,
				filteredSuspDocNgrams, suspDocStopwordMgrams, suspFileString,
				srcDocId, srcDocNgrams, srcDocNPlusgrams, srcDocStopwordMgrams,
				srcFileString);

		List<NGramPair> ngrampairs = new ArrayList<NGramPair>();
		List<NGramPair> npgrampairs = new ArrayList<NGramPair>();
		List<NGramPair> stopwordsmgrampairs = new ArrayList<NGramPair>();

		List<DoubleVector> pairVectorsp = new ArrayList<DoubleVector>();
		Map<DenseDoubleVector, NGramPair> vectorPairMapp = new HashMap<DenseDoubleVector, NGramPair>();
		List<DoubleVector> pairVectorsnp = new ArrayList<DoubleVector>();
		Map<DenseDoubleVector, NGramPair> vectorPairMapnp = new HashMap<DenseDoubleVector, NGramPair>();

		List<DoubleVector> pairVectorsStop = new ArrayList<DoubleVector>();
		Map<DenseDoubleVector, NGramPair> vectorPairMapStop = new HashMap<DenseDoubleVector, NGramPair>();

		NGramFeature.similarietes.clear();

		NGramBasedTextAligner.detectngramPairs(srcDocNPlusgrams,
				suspDocNPlusgrams, npgrampairs, pairVectorsnp, vectorPairMapnp);
		NGramBasedTextAligner
				.detectngramPairs(srcDocNgrams, filteredSuspDocNgrams,
						ngrampairs, pairVectorsp, vectorPairMapp);

		NGramBasedTextAligner.detectngramPairs(srcDocStopwordMgrams,
				suspDocStopwordMgrams, stopwordsmgrampairs, pairVectorsStop,
				vectorPairMapStop);

		List<Double> features = extractPlagiarismPairsFeatures(ngrampairs,
				npgrampairs, stopwordsmgrampairs, suspFileString.length(),
				srcFileString.length());
		if (features.size() > 19) {
			System.out.println("error!");
		}

		return features;
	}

	public void createFeatureFilesPerType(String trainingCorpus, String type,
			PlagiarismTypeEnum ptype) throws Exception {
		String pairFileAdd = trainingCorpus + "/" + type + "/pairs";
		String srcDir = trainingCorpus + "/" + "src/";
		String suspDir = trainingCorpus + "/" + "susp/";
		File pairFile = new File(pairFileAdd);
		BufferedReader br = new BufferedReader(new FileReader(pairFile));
		String line = null;
		Long suspFileId = -1L;
		Long srcFileId = -1L;
		while ((line = br.readLine()) != null) {
			String[] pair = line.split(" ");
			String susp = pair[0].trim();
			String src = pair[1].trim();

			String suspFileString = "";

			if (!lastSuspFileNGrams.containsKey(susp)) {
				suspFileString = TextProcessor
						.getMatn(new File(suspDir + susp));
				lastSuspFileNGrams.clear();
				lastSuspFileNPlusGrams.clear();
				lastSuspFileStopWordMGrams.clear();
				lastSuspFileLength.clear();
				lastSuspFileLength.put(susp, suspFileString.length());
				suspFileId = docIdCount++;
			}
			String srcFileString = "";
			if (!lastSrcFileNGrams.containsKey(src)) {
				srcFileString = TextProcessor.getMatn(new File(srcDir + src));
				lastSrcFileNGrams.clear();
				lastSrcFileNPlusGrams.clear();
				lastSrcFileStopWordMGrams.clear();
				lastSrcFileLength.clear();
				lastSrcFileLength.put(src, srcFileString.length());
				srcFileId = docIdCount++;
			}

			if (!documents.containsKey(suspFileId)) {
				documents.put(suspFileId, new HashMap<String, Object>());
				documents.get(suspFileId).put("docName", susp);

			}

			if (!documents.containsKey(srcFileId)) {
				documents.put(srcFileId, new HashMap<String, Object>());
				documents.get(srcFileId).put("docName", src);
			}

			Map<String, NGramFeature> suspDocNgrams = new HashMap<String, NGramFeature>();
			Map<String, NGramFeature> srcDocNgrams = new HashMap<String, NGramFeature>();
			Map<String, NGramFeature> suspDocNPlusgrams = new HashMap<String, NGramFeature>();
			Map<String, NGramFeature> srcDocNPlusgrams = new HashMap<String, NGramFeature>();

			Map<String, NGramFeature> suspDocStopwordMgrams = new HashMap<String, NGramFeature>();
			Map<String, NGramFeature> srcDocStopwordMgrams = new HashMap<String, NGramFeature>();

			Map<String, NGramFeature> filteredSuspDocNgrams = new HashMap<String, NGramFeature>();

			fillInNGramLists(suspFileId, suspDocNgrams, suspDocNPlusgrams,
					filteredSuspDocNgrams, suspDocStopwordMgrams,
					suspFileString, srcFileId, srcDocNgrams, srcDocNPlusgrams,
					srcDocStopwordMgrams, srcFileString);

			List<NGramPair> ngrampairs = new ArrayList<NGramPair>();
			List<NGramPair> npgrampairs = new ArrayList<NGramPair>();
			List<NGramPair> stopwordsmgrampairs = new ArrayList<NGramPair>();

			List<DoubleVector> pairVectorsp = new ArrayList<DoubleVector>();
			Map<DenseDoubleVector, NGramPair> vectorPairMapp = new HashMap<DenseDoubleVector, NGramPair>();
			List<DoubleVector> pairVectorsnp = new ArrayList<DoubleVector>();
			Map<DenseDoubleVector, NGramPair> vectorPairMapnp = new HashMap<DenseDoubleVector, NGramPair>();

			List<DoubleVector> pairVectorsStop = new ArrayList<DoubleVector>();
			Map<DenseDoubleVector, NGramPair> vectorPairMapStop = new HashMap<DenseDoubleVector, NGramPair>();

			NGramFeature.similarietes.clear();

			NGramBasedTextAligner.detectngramPairs(srcDocNPlusgrams,
					suspDocNPlusgrams, npgrampairs, pairVectorsnp,
					vectorPairMapnp);
			NGramBasedTextAligner.detectngramPairs(srcDocNgrams,
					filteredSuspDocNgrams, ngrampairs, pairVectorsp,
					vectorPairMapp);

			NGramBasedTextAligner.detectngramPairs(srcDocStopwordMgrams,
					suspDocStopwordMgrams, stopwordsmgrampairs,
					pairVectorsStop, vectorPairMapStop);

			List<Double> features = extractPlagiarismPairsFeatures(ngrampairs,
					npgrampairs, stopwordsmgrampairs,
					lastSuspFileLength.get(susp), lastSrcFileLength.get(src));
			features.add((double) ptype.ordinal());
			if (features.size() > 19) {
				System.out.println("error!");
			}
			System.out.println("docId:" + srcFileId);
			writer.write(getFeatureVectorString(features) + "\n");
		}
		br.close();
	}

	private String getFeatureVectorString(List<Double> features) {
		String featureVectorString = "";

		for (int i = 0; i < features.size(); i++)
			featureVectorString += features.get(i) + " ";

		return featureVectorString.trim();
	}

	public void detectngramPairs(List<NGramFeature> srcDocNgrams,
			List<NGramFeature> filteredSuspDocNgrams,
			List<NGramPair> ngrampairs, List<DoubleVector> pairVectors,
			Map<DenseDoubleVector, NGramPair> vectorPairMap) {
		for (NGramFeature suspNGram : filteredSuspDocNgrams) {
			/* for (NGramFeature srcNGram : srcDocNgrams) */
			Integer lastIndex = 0;
			Integer foundIndex = srcDocNgrams.subList(lastIndex,
					srcDocNgrams.size()).indexOf(suspNGram);

			if (foundIndex >= 0)
				lastIndex += foundIndex;
			else
				continue;
			while ((lastIndex >= 0) && (lastIndex <= srcDocNgrams.size())) {

				NGramFeature srcNGram = srcDocNgrams.get(lastIndex);

				Double pairSimilarity = suspNGram.computeSimilarity(srcNGram,
						NGramSimilarityType.SEMANTIC_SIMILARITY);
				if (pairSimilarity >= NGRAM_SIMILARITY_THRESHOLD) {

					for (int a = 0; a < suspNGram
							.getOccurancePositionInDocument().size(); a++) {
						Integer suspPos = suspNGram
								.getOccurancePositionInDocument().get(a);
						for (int b = 0; b < srcNGram
								.getOccurancePositionInDocument().size(); b++) {
							Integer srcPos = srcNGram
									.getOccurancePositionInDocument().get(b);

							DenseDoubleVector vector = new DenseDoubleVector(
									new double[] { suspPos, srcPos });

							NGramPair ngrampair = new NGramPair(
									suspPos,
									srcPos,
									pairSimilarity,
									suspNGram.getStringValue(),
									srcNGram.getStringValue(),
									suspNGram
											.getOccurancePositionInDocumentBasedOnSentence()
											.get(a),
									srcNGram.getOccurancePositionInDocumentBasedOnSentence()
											.get(b));

							if (!pairVectors.contains(vector)) {
								pairVectors.add(vector);
								ngrampairs.add(ngrampair);
								vectorPairMap.put(vector, ngrampair);
							}
						}
					}

				}
				lastIndex++;
				foundIndex = srcDocNgrams.subList(lastIndex,
						srcDocNgrams.size()).indexOf(suspNGram);
				if (foundIndex >= 0)
					lastIndex += foundIndex;
				else
					break;
			}
		}
	}

	public static void main(String[] args) throws Exception {
		PlagiarismTypeDetector ptd = new PlagiarismTypeDetector();
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

	protected void test(String testFeaturesFilePath,
			String trainFeaturesFilePath) throws Exception {
		List<List<Double>> features = readFeatures(testFeaturesFilePath);

		wc = new WekaBasedClassifier();
		wc.preprocess(featureTitles, plagiarismTypes);
		wc.loadModel(trainFeaturesFilePath + "model");
		Map<Double, List<Double>> trueOrFalseList = wc.test(features);

		Double prec = computePrecision(trueOrFalseList);
		Map<Double, Double> recalls = computeRecall(trueOrFalseList);

		System.out.println("Precision: " + prec);
		for (Double cval : recalls.keySet()) {
			System.out.println("Recall for "
					+ PlagiarismTypeEnum.values()[cval.intValue()].name() + " "
					+ recalls.get(cval));
		}
	}

	protected Double computePrecision(Map<Double, List<Double>> trueOrFalseList) {
		Double totalSamples = 0D;
		Double trueDetections = 0D;
		for (Double trueClass : trueOrFalseList.keySet()) {
			for (Double detectedClass : trueOrFalseList.get(trueClass)) {
				totalSamples++;
				if (trueClass.equals(detectedClass)) {
					trueDetections++;
				} else {
					System.out.println(PlagiarismTypeEnum.values()[trueClass
							.intValue()]
							+ " "
							+ PlagiarismTypeEnum.values()[detectedClass
									.intValue()]);
				}
			}
		}
		return trueDetections / (Math.max(1D, totalSamples));
	}

	Map<Double, Double> computeRecall(Map<Double, List<Double>> trueOrFalseList) {

		Map<Double, Double> recalls = new HashMap<Double, Double>();
		for (Double trueClass : trueOrFalseList.keySet()) {
			Double detected = 0D;

			for (Double detectedClass : trueOrFalseList.get(trueClass)) {
				if (trueClass.equals(detectedClass)) {
					detected++;
				}
			}
			recalls.put(trueClass, detected
					/ trueOrFalseList.get(trueClass).size());
		}
		return recalls;
	}

	protected void train(String trainFeaturesFilePath) throws IOException {
		List<List<Double>> features = readFeatures(trainFeaturesFilePath);
		wc = new WekaBasedClassifier();
		wc.preprocess(featureTitles, plagiarismTypes);
		try {
			wc.train(features);
			wc.saveModel(trainFeaturesFilePath + "model");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected List<List<Double>> readFeatures(String featuresFilePath)
			throws IOException {
		List<List<Double>> features = new ArrayList<List<Double>>();

		BufferedReader file = new BufferedReader(new InputStreamReader(
				new FileInputStream(featuresFilePath), "UTF-8"));

		file.readLine();

		String featureLine = "";

		while ((featureLine = file.readLine()) != null) {
			features.add(parseFeatureVectorString(featureLine));
		}
		file.close();
		return features;
	}

	protected List<Double> parseFeatureVectorString(String featureLine) {
		// TODO Auto-generated method stub
		List<Double> featureVector = new ArrayList<Double>();
		String[] values = featureLine.split("\\s+");
		for (int i = 0; i < values.length; i++) {
			featureVector.add(Double.parseDouble(values[i]));
		}
		return featureVector;
	}

	public PlagiarismTypeEnum classify(List<NGramPair> ngrampairs,
			List<NGramPair> npgrampairs, int suspLength, int srcLength,
			List<NGramPair> stopwordsmgrampairs) throws Exception {
		List<Double> features = extractPlagiarismPairsFeatures(ngrampairs,
				npgrampairs, stopwordsmgrampairs, suspLength, srcLength);
		if (wc == null) {
			wc = new WekaBasedClassifier();
			wc.loadModel(MODEL_NAME);
		}
		Double dc = wc.classify(features);
		return dc == 0 ? PlagiarismTypeEnum.NO_PLAGIARISM
				: PlagiarismTypeEnum.RANDOM_OBFUSCATION;
	}

	public void initalizeClassifier() throws ClassNotFoundException,
			IOException {
		wc = new WekaBasedClassifier();
		wc.preprocess(featureTitles, plagiarismTypes);
		wc.loadModel(MODEL_NAME);
	}

	private void fillInNGramLists(Long SuspDocId,
			Map<String, NGramFeature> suspDocNgrams,
			Map<String, NGramFeature> suspDocNPlusgrams,
			Map<String, NGramFeature> filteredSuspDocNgrams,
			Map<String, NGramFeature> suspDocStopwordsMGrams,
			String suspFileString, Long SrcDocId,
			Map<String, NGramFeature> srcDocNgrams,
			Map<String, NGramFeature> srcDocNPlusgrams,
			Map<String, NGramFeature> srcDocStopwordsMGrams,
			String srcFileString) throws Exception {
		if (!lastSuspFileNGrams.containsKey(documents.get(SuspDocId)
				.get("docName").toString())) {
			List<Pair<String, Pair<Integer, Integer>>> tokens = NGramExtractor
					.getFilteredTokens(suspFileString, IFSTEM,indexInfo);
			List<Pair<String, Pair<Integer, Integer>>> stopWordLessTokens = NGramExtractor
					.getNonStopWordTokens(tokens,
							TextAlignmentDatasetSettings.LANGUAGE);
			suspDocNgrams.putAll(NGramExtractor
					.extractSegmentNonStopWordNGrams(N, suspFileString,
							SuspDocId, new Pair<Integer, Integer>(0,
									suspFileString.length()), ngramType,
							stopWordLessTokens, 1));
			suspDocNPlusgrams.putAll(NGramExtractor
					.extractSegmentNonStopWordNGrams(N + 1, suspFileString,
							SuspDocId, new Pair<Integer, Integer>(0,
									suspFileString.length()), ngramType,
							stopWordLessTokens, 1));

			List<Pair<String, Pair<Integer, Integer>>> stopwordTokens = NGramExtractor
					.getStopWords(tokens);
			suspDocStopwordsMGrams.putAll(NGramExtractor
					.extractSegmentStopWordMGrams(
							M,
							suspFileString,
							SuspDocId,
							new Pair<Integer, Integer>(0, suspFileString
									.length()), ngramType, stopwordTokens));

			for (String suspNGramStringValue : suspDocNgrams.keySet()) {
				NGramFeature suspNGram = suspDocNgrams
						.get(suspNGramStringValue);
				Double dispersity = suspNGram
						.dispersionScore((double) suspFileString.length());

				if (dispersity > NGRAM_DISPERSITY_THRESHOLD) {
					continue;
				}
				filteredSuspDocNgrams
						.put(suspNGram.getStringValue(), suspNGram);
			}
			lastSuspFileNGrams.put(documents.get(SuspDocId).get("docName")
					.toString(), filteredSuspDocNgrams);
			lastSuspFileNPlusGrams.put(documents.get(SuspDocId).get("docName")
					.toString(), suspDocNPlusgrams);
			lastSuspFileStopWordMGrams.put(
					documents.get(SuspDocId).get("docName").toString(),
					suspDocStopwordsMGrams);

		} else {
			filteredSuspDocNgrams.putAll(lastSuspFileNGrams.get(documents
					.get(SuspDocId).get("docName").toString()));
			suspDocNPlusgrams.putAll(lastSuspFileNPlusGrams.get(documents
					.get(SuspDocId).get("docName").toString()));
			suspDocStopwordsMGrams.putAll(lastSuspFileStopWordMGrams
					.get(documents.get(SuspDocId).get("docName").toString()));
		}

		List<Pair<String, Pair<Integer, Integer>>> srctokens = NGramExtractor
				.getFilteredTokens(srcFileString, IFSTEM, indexInfo);
		List<Pair<String, Pair<Integer, Integer>>> srcStopWordLessTokens = NGramExtractor
				.getNonStopWordTokens(srctokens,
						TextAlignmentDatasetSettings.LANGUAGE);
		if (!lastSrcFileNGrams.containsKey(documents.get(SrcDocId)
				.get("docName").toString())) {
			srcDocNgrams.putAll(NGramExtractor.extractSegmentNonStopWordNGrams(
					N, srcFileString, SrcDocId, new Pair<Integer, Integer>(0,
							srcFileString.length()), ngramType,
					srcStopWordLessTokens, 1));

			lastSrcFileNGrams.put(documents.get(SrcDocId).get("docName")
					.toString(), srcDocNgrams);

			srcDocStopwordsMGrams.putAll(NGramExtractor
					.extractSegmentStopWordMGrams(
							M,
							srcFileString,
							SrcDocId,
							new Pair<Integer, Integer>(0, suspFileString
									.length()), ngramType, srctokens));
			lastSrcFileStopWordMGrams.put(documents.get(SrcDocId)
					.get("docName").toString(), srcDocStopwordsMGrams);

		} else {
			srcDocNgrams.putAll(lastSrcFileNGrams.get(documents.get(SrcDocId)
					.get("docName").toString()));

			srcDocStopwordsMGrams.putAll(lastSrcFileStopWordMGrams
					.get(documents.get(SrcDocId).get("docName").toString()));
		}

		if (!lastSrcFileNPlusGrams.containsKey(documents.get(SrcDocId)
				.get("docName").toString())) {

			srcDocNPlusgrams.putAll(NGramExtractor
					.extractSegmentNonStopWordNGrams(N + 1, srcFileString,
							SrcDocId, new Pair<Integer, Integer>(0,
									srcFileString.length()), ngramType,
							srcStopWordLessTokens, 1));

			lastSrcFileNPlusGrams.put(documents.get(SrcDocId).get("docName")
					.toString(), srcDocNPlusgrams);

		} else {

			try {
				srcDocNPlusgrams.putAll(lastSrcFileNPlusGrams.get(documents
						.get(SrcDocId).get("docName").toString()));
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("srcDocId: " + SrcDocId + " "
						+ documents.get(SrcDocId).get("docName").toString());
				System.out.println("list is null:"
						+ (lastSrcFileNPlusGrams == null));
				System.out.println(lastSrcFileNPlusGrams.get(
						documents.get(SrcDocId).get("docName").toString())
						.size());
			}
		}

	}

	private void forcefillInNGramLists(Long SuspDocId,
			Map<String, NGramFeature> suspDocNgrams,
			Map<String, NGramFeature> suspDocNPlusgrams,
			Map<String, NGramFeature> filteredSuspDocNgrams,
			Map<String, NGramFeature> suspDocStopwordsMGrams,
			String suspFileString, Long SrcDocId,
			Map<String, NGramFeature> srcDocNgrams,
			Map<String, NGramFeature> srcDocNPlusgrams,
			Map<String, NGramFeature> srcDocStopwordsMGrams,
			String srcFileString) throws Exception {

		List<Pair<String, Pair<Integer, Integer>>> tokens = NGramExtractor
				.getFilteredTokens(suspFileString, IFSTEM,indexInfo);
		List<Pair<String, Pair<Integer, Integer>>> stopWordLessTokens = NGramExtractor
				.getNonStopWordTokens(tokens,
						TextAlignmentDatasetSettings.LANGUAGE);
		suspDocNgrams.putAll(NGramExtractor.extractSegmentNonStopWordNGrams(N,
				suspFileString, SuspDocId, new Pair<Integer, Integer>(0,
						suspFileString.length()), ngramType,
				stopWordLessTokens, 1));
		suspDocNPlusgrams.putAll(NGramExtractor
				.extractSegmentNonStopWordNGrams(N + 1, suspFileString,
						SuspDocId,
						new Pair<Integer, Integer>(0, suspFileString.length()),
						ngramType, stopWordLessTokens, 1));

		List<Pair<String, Pair<Integer, Integer>>> stopwordTokens = NGramExtractor
				.getStopWords(tokens);
		suspDocStopwordsMGrams.putAll(NGramExtractor
				.extractSegmentStopWordMGrams(M, suspFileString, SuspDocId,
						new Pair<Integer, Integer>(0, suspFileString.length()),
						ngramType, stopwordTokens));

		for (String suspNGramStringValue : suspDocNgrams.keySet()) {
			NGramFeature suspNGram = suspDocNgrams.get(suspNGramStringValue);
			Double dispersity = suspNGram
					.dispersionScore((double) suspFileString.length());

			if (dispersity > NGRAM_DISPERSITY_THRESHOLD) {
				continue;
			}
			filteredSuspDocNgrams.put(suspNGram.getStringValue(), suspNGram);
		}

		List<Pair<String, Pair<Integer, Integer>>> srctokens = NGramExtractor
				.getFilteredTokens(srcFileString, IFSTEM,indexInfo);
		List<Pair<String, Pair<Integer, Integer>>> srcStopWordLessTokens = NGramExtractor
				.getNonStopWordTokens(srctokens,
						TextAlignmentDatasetSettings.LANGUAGE);

		srcDocNgrams.putAll(NGramExtractor.extractSegmentNonStopWordNGrams(N,
				srcFileString, SrcDocId, new Pair<Integer, Integer>(0,
						srcFileString.length()), ngramType,
				srcStopWordLessTokens, 1));

		srcDocStopwordsMGrams.putAll(NGramExtractor
				.extractSegmentStopWordMGrams(M, srcFileString, SrcDocId,
						new Pair<Integer, Integer>(0, suspFileString.length()),
						ngramType, srctokens));

		srcDocNPlusgrams.putAll(NGramExtractor.extractSegmentNonStopWordNGrams(
				N + 1, srcFileString, SrcDocId, new Pair<Integer, Integer>(0,
						srcFileString.length()), ngramType,
				srcStopWordLessTokens, 1));
	}

	public PlagiarismTypeEnum decideOnTypeOfPlagiarism(
			List<Double> featureVector) throws Exception {
		return PlagiarismTypeEnum.values()[wc.classify(featureVector)
				.intValue()];
	}

}
