package org.iis.plagiarismdetector.textalignment.ngrams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iis.plagiarismdetector.settings.TextAlignmentDatasetSettings;
import org.iis.plagiarismdetector.classifiers.PlagiarismTypeDetector;

import com.sun.tools.javac.util.Pair;

import org.iis.plagiarismdetector.core.ComparisonMethod;
import org.iis.plagiarismdetector.core.FatherOfTextAligners;
import org.iis.plagiarismdetector.core.Feature;
import org.iis.plagiarismdetector.core.PlagiarismTypeEnum;
import org.iis.plagiarismdetector.core.TextProcessor;
import org.iis.plagiarismdetector.core.lucene.IndexInfo;

import de.jungblut.clustering.DBSCANClustering;
import de.jungblut.distance.DistanceMeasurer;
import de.jungblut.distance.EuclidianDistance;
import de.jungblut.math.DoubleVector;
import de.jungblut.math.dense.DenseDoubleVector;

import org.iis.plagiarismdetector.evaluation.tracking.PerformanceAnalyzer;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramExtractor;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramFeature;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramType;

public class NGramBasedTextAligner extends FatherOfTextAligners {

	private static final double NANO_TO_MILI = Math.pow(10, 6);

	private static Map<String, Map<String, NGramFeature>> lastSuspFileNGrams = new HashMap<String, Map<String, NGramFeature>>();
	private static Map<String, Map<String, NGramFeature>> lastSrcFileNGrams = new HashMap<String, Map<String, NGramFeature>>();

	private static Map<String, Map<String, NGramFeature>> lastSuspStopWordMGrams = new HashMap<String, Map<String, NGramFeature>>();
	private static Map<String, Map<String, NGramFeature>> lastSrcStopWordMGrams = new HashMap<String, Map<String, NGramFeature>>();

	private static Map<String, Map<String, NGramFeature>> lastSuspFileNPlusGrams = new HashMap<String, Map<String, NGramFeature>>();
	private static Map<String, Map<String, NGramFeature>> lastSrcFileNPlusGrams = new HashMap<String, Map<String, NGramFeature>>();

	private static Map<String, Map<String, NGramFeature>> lastSuspFileContextualNPlusGrams = new HashMap<String, Map<String, NGramFeature>>();
	private static Map<String, Map<String, NGramFeature>> lastSrcFileContextualNPlusGrams = new HashMap<String, Map<String, NGramFeature>>();
	PlagiarismTypeDetector pdt = new PlagiarismTypeDetector();

	public NGramBasedTextAligner() throws ClassNotFoundException, IOException {
		super();
		TextAlignmentDatasetSettings.initialize();
		pdt.initalizeClassifier();
	}

	private static Double DB_SCAN_EPS = 400D;// 380D;
	private static Integer DB_SCAN_MINPOINTS = 5;// 4;
	private static Long docIdCount = 0L;
	private static final Double NGRAM_SIMILARITY_THRESHOLD = 0.05D;
	private static final Double NGRAM_DISPERSITY_THRESHOLD = 0.4;
	private static Integer N = 2;
	private static final Integer M = 5;

	private static final Integer EXPANSION_DEGREE = 5;

	private static final Boolean IFSTEM = null;
	private static NGramSizeType nGramSizeType;
	private NGramType ngramType;
	private ComparisonMethod COMPARISON_METHOD = ComparisonMethod.ClusterBased;
	private Map<Long, Map<String, Object>> documents = new HashMap<Long, Map<String, Object>>();
	private static Double pairMatchingTime = 0D;
	private static Double clusteringTime = 0D;

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			NGramBasedTextAligner nGramBasedTextAligner = new NGramBasedTextAligner();
			nGramBasedTextAligner.readAndAlignPairs(args);
		} else {
			Map<String, Object> experimentOptions = new HashMap<String, Object>();
			try {

				for (int i = 0; i < (args.length - 1); i += 2) {
					if (args[i].equals("-datasetId")) {
						experimentOptions.put("datasetId", args[i + 1]);
					} else if (args[i].equals("-ngramType")) {
						experimentOptions.put("ngramType",
								NGramType.valueOf(args[i + 1]));

					} else if (args[i].equals("-n")) {
						experimentOptions.put("n",
								Integer.parseInt(args[i + 1]));

					} else if (args[i].equals("-detectionFolder")) {
						experimentOptions.put("detectionFolder", args[i + 1]);

					} else if (args[i].equals("-comparisonMethod")) {
						experimentOptions.put("comparisonMethod",
								ComparisonMethod.valueOf(args[i + 1]));

					} else if (args[i].equals("-lang")) {
						experimentOptions.put("lang", args[i + 1]);
					} else if (args[i].equals("-ngramSizeType")) {
						experimentOptions.put("ngramSizeType",
								NGramSizeType.valueOf(args[i + 1]));
					}
				}
				doTheExperiment((String) experimentOptions.get("datasetId"),
						(Integer) experimentOptions.get("n"),
						(NGramType) experimentOptions.get("ngramType"),
						(ComparisonMethod) experimentOptions
								.get("comparisonMethod"),
						(String) experimentOptions.get("detectionFolder"),
						(String) experimentOptions.get("lang"),
						(NGramSizeType) experimentOptions.get("ngramSizeType"));
			} catch (NullPointerException e) {
				System.err.println("Wrong Format of Input Arguments!");
				e.printStackTrace();
				System.exit(0);
			}
		}

	}

	public static void doTheExperiment(String datasetId, Integer n,
			NGramType ngramType, ComparisonMethod comparisonMethod,
			String detectionFolder, String lang, NGramSizeType ngramSizeType)
			throws Exception {

		TextAlignmentDatasetSettings.LANGUAGE = lang;

		TextAlignmentDatasetSettings.datasetIndex = Arrays.asList(
				TextAlignmentDatasetSettings.datasetNames).indexOf(datasetId);

		TextAlignmentDatasetSettings.plagiarismTypeIndex = Arrays.asList(
				TextAlignmentDatasetSettings.detectionFolder).indexOf(
				detectionFolder);

		if (ngramType.equals(NGramType.Word)) {
			N = n;
			TextAlignmentDatasetSettings.methodIndex = Arrays.asList(
					TextAlignmentDatasetSettings.methodNames).indexOf(
					"Word" + n + "Gram");
		}
		if (ngramSizeType.equals(NGramSizeType.SentenceBased)) {
			TextAlignmentDatasetSettings.methodIndex = Arrays.asList(
					TextAlignmentDatasetSettings.methodNames).indexOf(
					"Word" + ngramSizeType.name() + "Gram");
		}

		nGramSizeType = ngramSizeType;

		TextAlignmentDatasetSettings.initialize();

		NGramBasedTextAligner nGramBasedTextAligner = new NGramBasedTextAligner();

		nGramBasedTextAligner.readAndAlignPairs(null);

		System.out.println("PairingTime: " + (pairMatchingTime)
				+ "     Clustering Time: " + (clusteringTime));
		;
		PerformanceAnalyzer pa = new PerformanceAnalyzer();
		pa.AnalyzePerformance();

	}

	Long suspFileId = -1L;
	Long srcFileId = -1L;

	private IndexInfo indexInfo;

	@Override
	protected ArrayList<Feature> computeFeatures(String suspFileName,
			String srcFileName) throws Exception {

		ArrayList<Feature> features = new ArrayList<Feature>();
		String suspFileString = "";
		if (!lastSuspFileNGrams.containsKey(suspFileName)) {
			suspFileString = TextProcessor.getMatn(new File(suspFileName));
			lastSuspFileNGrams.clear();
			lastSuspFileNPlusGrams.clear();
			lastSuspFileContextualNPlusGrams.clear();
			lastSuspStopWordMGrams.clear();
			suspFileId = docIdCount++;
		}
		String srcFileString = "";
		if (!lastSrcFileNGrams.containsKey(srcFileName)) {
			srcFileString = TextProcessor.getMatn(new File(srcFileName));
			lastSrcFileNGrams.clear();
			lastSrcFileNPlusGrams.clear();
			lastSrcFileContextualNPlusGrams.clear();
			lastSrcStopWordMGrams.clear();
			srcFileId = docIdCount++;
		}

		if (!documents.containsKey(suspFileId)) {
			documents.put(suspFileId, new HashMap<String, Object>());
			documents.get(suspFileId).put("docName", suspFileName);

		}

		if (!documents.containsKey(srcFileId)) {
			documents.put(srcFileId, new HashMap<String, Object>());
			documents.get(srcFileId).put("docName", srcFileName);
		}
		switch (COMPARISON_METHOD) {
		case ClusterBased:
			features = clustreingNGramBasedTextAlignment(suspFileString,
					srcFileString, suspFileId, srcFileId);
			break;
		case SegmentComparison:
			break;
		default:

		}

		return features;
	}

	/*
	 * private void fillInNGramLists(Long SuspDocId, List<NGramFeature>
	 * suspDocNgrams, String suspFileString, List<NGramFeature>
	 * suspDocNPlusgrams, List<NGramFeature> filteredSuspDocNgrams, Long
	 * SrcDocId, List<NGramFeature> srcDocNgrams, List<NGramFeature>
	 * srcDocNPlusgrams, String srcFileString) throws Exception { if
	 * (nGramSizeType.equals(NGramSizeType.Fixed)) { if
	 * (!lastSuspFileNGrams.containsKey(documents.get(SuspDocId)
	 * .get("docName").toString())) { suspDocNgrams.addAll(NGramExtractor
	 * .extractSegmentNonStopWordNGrams(N, suspFileString, SuspDocId, new
	 * Pair<Integer, Integer>(0, suspFileString.length()), ngramType));
	 * suspDocNPlusgrams.addAll(NGramExtractor
	 * .extractSegmentNonStopWordNGrams(N + 1, suspFileString, SuspDocId, new
	 * Pair<Integer, Integer>(0, suspFileString.length()), ngramType)); for
	 * (NGramFeature suspNGram : suspDocNgrams) { Double dispersity = suspNGram
	 * .dispersionScore((double) suspFileString.length());
	 * 
	 * if (dispersity > NGRAM_DISPERSITY_THRESHOLD) { continue; }
	 * filteredSuspDocNgrams.add(suspNGram); }
	 * lastSuspFileNGrams.put(documents.get(SuspDocId).get("docName")
	 * .toString(), filteredSuspDocNgrams); lastSuspFileNPlusGrams.put(
	 * documents.get(SuspDocId).get("docName").toString(), suspDocNPlusgrams); }
	 * else { filteredSuspDocNgrams.addAll(lastSuspFileNGrams.get(documents
	 * .get(SuspDocId).get("docName").toString()));
	 * suspDocNPlusgrams.addAll(lastSuspFileNPlusGrams.get(documents
	 * .get(SuspDocId).get("docName").toString())); } if
	 * (!lastSrcFileNGrams.containsKey(documents.get(SrcDocId)
	 * .get("docName").toString())) { srcDocNgrams.addAll(NGramExtractor
	 * .extractSegmentNonStopWordNGrams(N, srcFileString, SrcDocId, new
	 * Pair<Integer, Integer>(0, srcFileString.length()), ngramType));
	 * 
	 * lastSrcFileNGrams.put(documents.get(SrcDocId).get("docName") .toString(),
	 * srcDocNgrams);
	 * 
	 * } else { srcDocNgrams.addAll(lastSrcFileNGrams.get(documents
	 * .get(SrcDocId).get("docName").toString()));
	 * 
	 * }
	 * 
	 * if (!lastSrcFileNPlusGrams.containsKey(documents.get(SrcDocId)
	 * .get("docName").toString())) {
	 * 
	 * srcDocNPlusgrams.addAll(NGramExtractor .extractSegmentNonStopWordNGrams(N
	 * + 1, srcFileString, SrcDocId, new Pair<Integer, Integer>(0,
	 * srcFileString.length()), ngramType));
	 * 
	 * lastSrcFileNPlusGrams.put(documents.get(SrcDocId)
	 * .get("docName").toString(), srcDocNPlusgrams); } else {
	 * 
	 * try { srcDocNPlusgrams.addAll(lastSrcFileNPlusGrams.get(documents
	 * .get(SrcDocId).get("docName").toString())); } catch (Exception e) {
	 * e.printStackTrace(); System.out .println("srcDocId: " + SrcDocId + " " +
	 * documents.get(SrcDocId).get("docName") .toString());
	 * System.out.println("list is null:" + (lastSrcFileNPlusGrams == null));
	 * System.out.println(lastSrcFileNPlusGrams.get(
	 * documents.get(SrcDocId).get("docName").toString()) .size()); } }
	 * 
	 * } else if (nGramSizeType.equals(NGramSizeType.SentenceBased)) {
	 * suspDocNgrams.addAll(NGramExtractor
	 * .extractSegmentSentenceBasedNGrams(suspFileString, SuspDocId, new
	 * Pair<Integer, Integer>(0, suspFileString.length()), ngramType));
	 * 
	 * srcDocNgrams.addAll(NGramExtractor .extractSegmentSentenceBasedNGrams(
	 * srcFileString, SrcDocId, new Pair<Integer, Integer>(0, srcFileString
	 * .length()), ngramType)); } }
	 */
	private ArrayList<Feature> clustreingNGramBasedTextAlignment(
			String suspFileString, String srcFileString, Long SuspDocId,
			Long SrcDocId) throws Exception {

		Map<String, NGramFeature> suspDocStopWordMGrams = new HashMap<String, NGramFeature>();
		Map<String, NGramFeature> srcDocStopWordMGrams = new HashMap<String, NGramFeature>();
		Map<String, NGramFeature> suspDocNgrams = new HashMap<String, NGramFeature>();
		Map<String, NGramFeature> srcDocNgrams = new HashMap<String, NGramFeature>();
		Map<String, NGramFeature> suspDocNPlusgrams = new HashMap<String, NGramFeature>();
		Map<String, NGramFeature> srcDocNPlusgrams = new HashMap<String, NGramFeature>();
		Map<String, NGramFeature> suspDocContextualNPlusgrams = new HashMap<String, NGramFeature>();
		Map<String, NGramFeature> srcDocContextualNPlusgrams = new HashMap<String, NGramFeature>();

		Map<String, NGramFeature> filteredSuspDocNgrams = new HashMap<String, NGramFeature>();

		List<NGramPair> ngrampairs = new ArrayList<NGramPair>();
		List<NGramPair> npgrampairs = new ArrayList<NGramPair>();
		List<NGramPair> npcgrampairs = new ArrayList<NGramPair>();
		List<NGramPair> stopgrampairs = new ArrayList<NGramPair>();

		List<DoubleVector> pairVectors = new ArrayList<DoubleVector>();
		List<DoubleVector> pairVectorsnp = new ArrayList<DoubleVector>();
		List<DoubleVector> pairVectorsn = new ArrayList<DoubleVector>();
		List<DoubleVector> pairVectorsnpc = new ArrayList<DoubleVector>();
		List<DoubleVector> pairVectorsstop = new ArrayList<DoubleVector>();

		NGramFeature.similarietes.clear();

		Map<DenseDoubleVector, NGramPair> vectorPairMap = new HashMap<DenseDoubleVector, NGramPair>();

		PlagiarismTypeEnum type = PlagiarismTypeEnum.SUMMARY_OBFUSCATION;

		filteredSuspDocNgrams = new HashMap<String, NGramFeature>();

		// List<Double> featureVector = pdt.getFeatureVector(suspFileString,
		// srcFileString, SuspDocId, SrcDocId);
		// type = pdt.decideOnTypeOfPlagiarism(featureVector);

		suspDocStopWordMGrams = new HashMap<String, NGramFeature>();
		srcDocStopWordMGrams = new HashMap<String, NGramFeature>();
		suspDocNgrams = new HashMap<String, NGramFeature>();
		srcDocNgrams = new HashMap<String, NGramFeature>();
		suspDocNPlusgrams = new HashMap<String, NGramFeature>();
		srcDocNPlusgrams = new HashMap<String, NGramFeature>();
		suspDocContextualNPlusgrams = new HashMap<String, NGramFeature>();
		srcDocContextualNPlusgrams = new HashMap<String, NGramFeature>();

		System.out.println(type.name());
		switch (type) {
		case NO_PLAGIARISM:
			pairVectors.clear();
			vectorPairMap.clear();
			break;
		case NO_OBFUSCATION:
			N = 4;
			forcefillInNGramLists(SuspDocId, suspDocNgrams, suspFileString,
					suspDocNPlusgrams, filteredSuspDocNgrams, SrcDocId,
					srcDocNgrams, srcDocNPlusgrams, srcFileString,
					suspDocContextualNPlusgrams, srcDocContextualNPlusgrams,
					suspDocStopWordMGrams, srcDocStopWordMGrams);
			pairVectors.clear();
			vectorPairMap.clear();
			detectngramPairs(srcDocNPlusgrams, suspDocNPlusgrams, npgrampairs,
					pairVectors, vectorPairMap);

			detectngramPairs(srcDocNgrams, filteredSuspDocNgrams, ngrampairs,
					pairVectors, vectorPairMap);
			break;
		case TRANSLATION_OBFUSCATION:
			N = 3;
			forcefillInNGramLists(SuspDocId, suspDocNgrams, suspFileString,
					suspDocNPlusgrams, filteredSuspDocNgrams, SrcDocId,
					srcDocNgrams, srcDocNPlusgrams, srcFileString,
					suspDocContextualNPlusgrams, srcDocContextualNPlusgrams,
					suspDocStopWordMGrams, srcDocStopWordMGrams);
			pairVectors.clear();
			vectorPairMap.clear();
			detectngramPairs(srcDocNPlusgrams, suspDocNPlusgrams, npgrampairs,
					pairVectors, vectorPairMap);

			detectngramPairs(srcDocNgrams, filteredSuspDocNgrams, ngrampairs,
					pairVectors, vectorPairMap);
			break;
		case RANDOM_OBFUSCATION:
			N = 3;
			forcefillInNGramLists(SuspDocId, suspDocNgrams, suspFileString,
					suspDocNPlusgrams, filteredSuspDocNgrams, SrcDocId,
					srcDocNgrams, srcDocNPlusgrams, srcFileString,
					suspDocContextualNPlusgrams, srcDocContextualNPlusgrams,
					suspDocStopWordMGrams, srcDocStopWordMGrams);
			pairVectors.clear();
			vectorPairMap.clear();
			detectngramPairs(srcDocNPlusgrams, suspDocNPlusgrams, npgrampairs,
					pairVectors, vectorPairMap);

			detectngramPairs(srcDocNgrams, filteredSuspDocNgrams, ngrampairs,
					pairVectors, vectorPairMap);
			break;
		case SUMMARY_OBFUSCATION:
			N = 2;
			forcefillInNGramLists(SuspDocId, suspDocNgrams, suspFileString,
					suspDocNPlusgrams, filteredSuspDocNgrams, SrcDocId,
					srcDocNgrams, srcDocNPlusgrams, srcFileString,
					suspDocContextualNPlusgrams, srcDocContextualNPlusgrams,
					suspDocStopWordMGrams, srcDocStopWordMGrams);
			pairVectors.clear();
			vectorPairMap.clear();
			detectngramPairs(srcDocNPlusgrams, suspDocNPlusgrams, npgrampairs,
					pairVectors, vectorPairMap);

			detectngramPairs(srcDocNgrams, filteredSuspDocNgrams, ngrampairs,
					pairVectors, vectorPairMap);

			break;
		default:
			N = 3;
			forcefillInNGramLists(SuspDocId, suspDocNgrams, suspFileString,
					suspDocNPlusgrams, filteredSuspDocNgrams, SrcDocId,
					srcDocNgrams, srcDocNPlusgrams, srcFileString,
					suspDocContextualNPlusgrams, srcDocContextualNPlusgrams,
					suspDocStopWordMGrams, srcDocStopWordMGrams);
			pairVectors.clear();
			vectorPairMap.clear();
			detectngramPairs(srcDocNPlusgrams, suspDocNPlusgrams, npgrampairs,
					pairVectors, vectorPairMap);

			detectngramPairs(srcDocNgrams, filteredSuspDocNgrams, ngrampairs,
					pairVectors, vectorPairMap);
		}

		if ((pairVectors == null) || (pairVectors.size() <= 0))
			return new ArrayList<Feature>();

		DistanceMeasurer measure = new EuclidianDistance();

		System.out.println("begin Clustering" + SrcDocId + " " + SuspDocId);
		List<List<DoubleVector>> clusters = DBSCANClustering.cluster(
				pairVectors, measure, DB_SCAN_MINPOINTS, DB_SCAN_EPS);

		ArrayList<Feature> features = new ArrayList<Feature>();
		for (List<DoubleVector> cluster : clusters) {
			// System.out.println(cluster.size());
			Collections.sort(cluster, new Comparator<DoubleVector>() {

				@Override
				public int compare(DoubleVector o1, DoubleVector o2) {
					return (o1.get(0) == o2.get(0)) ? 0 : (o1.get(0) > o2
							.get(0) ? 1 : -1);
				}

			});
			DoubleVector frstFeatureInSusp = cluster.get(0);
			DoubleVector lastFeatureInSusp = cluster.get(cluster.size() - 1);

			Collections.sort(cluster, new Comparator<DoubleVector>() {

				@Override
				public int compare(DoubleVector o1, DoubleVector o2) {
					return (o1.get(1) == o2.get(1)) ? 0 : (o1.get(1) > o2
							.get(1) ? 1 : -1);
				}

			});
			DoubleVector frstFeatureInSrc = cluster.get(0);
			DoubleVector lastFeatureInSrc = cluster.get(cluster.size() - 1);
			Feature newFeature = new Feature((long) vectorPairMap.get(
					frstFeatureInSusp).getSuspSentenceOffset().fst,
					(long) (vectorPairMap.get(lastFeatureInSusp)
							.getSuspSentenceOffset().snd - vectorPairMap.get(
							frstFeatureInSusp).getSuspSentenceOffset().fst),
					(long) vectorPairMap.get(frstFeatureInSrc)
							.getSrcSentenceOffset().fst,
					(long) (vectorPairMap.get(lastFeatureInSrc)
							.getSrcSentenceOffset().snd - vectorPairMap.get(
							frstFeatureInSrc).getSrcSentenceOffset().fst));

			Boolean hasOverlap = false;
			for (int i = 0; i < features.size(); i++) {
				Feature overlap = newFeature.overlap(features.get(i));
				if (overlap != null) {
					hasOverlap = true;
					if (features.get(i).getLength() < newFeature.getLength()) {
						features.get(i).setOffset(newFeature.getOffset());
						features.get(i).setLength(newFeature.getLength());
					}

					if (features.get(i).getSrcLength() < newFeature
							.getSrcLength()) {
						features.get(i).setSrcOffset(newFeature.getSrcOffset());
						features.get(i).setSrcLength(newFeature.getSrcLength());
					}
				}
			}

			if ((!hasOverlap) || (newFeature.getLength() > 200))
				features.add(newFeature);

			if (lastFeatureInSrc.get(1)
					+ vectorPairMap.get(lastFeatureInSrc).getSrcValue()
							.length() - frstFeatureInSrc.get(1) > srcFileString
						.length()) {
				System.out.println("too large offset for source");
			}

		}

		/*
		 * double[][] pairsDists = createDistanceMatrix( SuspDocId + "_" +
		 * SrcDocId, pairs); String[] pairNames = createPairNames(pairs);
		 */

		return features;
	}

	private void forcefillInNGramLists(Long SuspDocId,
			Map<String, NGramFeature> suspDocNgrams, String suspFileString,
			Map<String, NGramFeature> suspDocNPlusgrams,
			Map<String, NGramFeature> filteredSuspDocNgrams, Long SrcDocId,
			Map<String, NGramFeature> srcDocNgrams,
			Map<String, NGramFeature> srcDocNPlusgrams, String srcFileString,
			Map<String, NGramFeature> suspDocContextualNPlusgrams,
			Map<String, NGramFeature> srcDocContextualNPlusgrams,
			Map<String, NGramFeature> suspDocStopWordMGrams,
			Map<String, NGramFeature> srcDocStopWordMGrams) throws IOException,
			Exception {

		List<Pair<String, Pair<Integer, Integer>>> tokens = NGramExtractor
				.getFilteredTokens(suspFileString, IFSTEM,indexInfo);
		List<Pair<String, Pair<Integer, Integer>>> stopWordLessTokens = NGramExtractor
				.getNonStopWordTokens(tokens,
						TextAlignmentDatasetSettings.LANGUAGE);
		suspDocNgrams.clear();
		suspDocNgrams.putAll(NGramExtractor.extractSegmentNonStopWordNGrams(N,
				suspFileString, SuspDocId, new Pair<Integer, Integer>(0,
						suspFileString.length()), ngramType,
				stopWordLessTokens, EXPANSION_DEGREE));
		suspDocNPlusgrams.clear();
		suspDocNPlusgrams.putAll(NGramExtractor
				.extractSegmentNonStopWordNGrams(N + 1, suspFileString,
						SuspDocId,
						new Pair<Integer, Integer>(0, suspFileString.length()),
						ngramType, stopWordLessTokens, EXPANSION_DEGREE));
		suspDocContextualNPlusgrams.clear();
		suspDocContextualNPlusgrams.putAll(NGramExtractor
				.extractSegmentContextualNonStopWordNGrams(N + 1,
						suspFileString, SuspDocId, new Pair<Integer, Integer>(
								0, suspFileString.length()), ngramType,
						stopWordLessTokens));
		suspDocStopWordMGrams.clear();
		suspDocStopWordMGrams.putAll(NGramExtractor
				.extractSegmentStopWordMGrams(M, suspFileString, SuspDocId,
						new Pair<Integer, Integer>(0, suspFileString.length()),
						ngramType, tokens));
		filteredSuspDocNgrams.clear();
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

		srcDocNgrams.clear();
		srcDocNgrams.putAll(NGramExtractor.extractSegmentNonStopWordNGrams(N,
				srcFileString, SrcDocId, new Pair<Integer, Integer>(0,
						srcFileString.length()), ngramType,
				srcStopWordLessTokens, EXPANSION_DEGREE));

		srcDocNPlusgrams.clear();
		srcDocNPlusgrams.putAll(NGramExtractor.extractSegmentNonStopWordNGrams(
				N + 1, srcFileString, SrcDocId, new Pair<Integer, Integer>(0,
						srcFileString.length()), ngramType,
				srcStopWordLessTokens, EXPANSION_DEGREE));
		srcDocContextualNPlusgrams.clear();
		srcDocContextualNPlusgrams.putAll(NGramExtractor
				.extractSegmentContextualNonStopWordNGrams(N + 1,
						srcFileString, SrcDocId, new Pair<Integer, Integer>(0,
								srcFileString.length()), ngramType,
						srcStopWordLessTokens));
		srcDocStopWordMGrams.clear();
		srcDocStopWordMGrams.putAll(NGramExtractor
				.extractSegmentStopWordMGrams(M, srcFileString, SrcDocId,
						new Pair<Integer, Integer>(0, suspFileString.length()),
						ngramType, srctokens));

	}

	public static void detectngramPairs(Map<String, NGramFeature> srcDocNgrams,
			Map<String, NGramFeature> suspDocNgrams,
			List<NGramPair> ngrampairs, List<DoubleVector> pairVectors,
			Map<DenseDoubleVector, NGramPair> vectorPairMap) {
		for (String suspNGramString : suspDocNgrams.keySet()) {

			if (srcDocNgrams.containsKey(suspNGramString)) {
				NGramFeature srcNGram = srcDocNgrams.get(suspNGramString);
				NGramFeature suspNGram = suspDocNgrams.get(suspNGramString);

				for (int a = 0; a < suspNGram.getOccurancePositionInDocument()
						.size(); a++) {
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
								suspNGram.getTransparencies().get(a)
										* srcNGram.getTransparencies().get(b),
								suspNGram.getStringValue(),
								srcNGram.getStringValue(),
								suspNGram
										.getOccurancePositionInDocumentBasedOnSentence()
										.get(a),
								srcNGram.getOccurancePositionInDocumentBasedOnSentence()
										.get(b));

						if (!vectorPairMap.containsKey(vector)) {
							pairVectors.add(vector);
							ngrampairs.add(ngrampair);
							vectorPairMap.put(vector, ngrampair);
						}
					}
				}

			}
		}

	}

	private void fillInNGramLists(Long SuspDocId,
			Map<String, NGramFeature> suspDocNgrams, String suspFileString,
			Map<String, NGramFeature> suspDocNPlusgrams,
			Map<String, NGramFeature> filteredSuspDocNgrams, Long SrcDocId,
			Map<String, NGramFeature> srcDocNgrams,
			Map<String, NGramFeature> srcDocNPlusgrams, String srcFileString,
			Map<String, NGramFeature> suspDocContextualNPlusgrams,
			Map<String, NGramFeature> srcDocContextualNPlusgrams,
			Map<String, NGramFeature> suspDocStopWordMGrams,
			Map<String, NGramFeature> srcDocStopWordMGrams) throws Exception {
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
							stopWordLessTokens, 3));
			suspDocNPlusgrams.putAll(NGramExtractor
					.extractSegmentNonStopWordNGrams(N + 1, suspFileString,
							SuspDocId, new Pair<Integer, Integer>(0,
									suspFileString.length()), ngramType,
							stopWordLessTokens, 3));
			suspDocContextualNPlusgrams.putAll(NGramExtractor
					.extractSegmentContextualNonStopWordNGrams(
							N + 1,
							suspFileString,
							SuspDocId,
							new Pair<Integer, Integer>(0, suspFileString
									.length()), ngramType, stopWordLessTokens));
			suspDocStopWordMGrams.putAll(NGramExtractor
					.extractSegmentStopWordMGrams(
							M,
							suspFileString,
							SuspDocId,
							new Pair<Integer, Integer>(0, suspFileString
									.length()), ngramType, tokens));
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
			lastSuspFileContextualNPlusGrams.put(
					documents.get(SuspDocId).get("docName").toString(),
					suspDocContextualNPlusgrams);
			lastSuspStopWordMGrams.put(documents.get(SuspDocId).get("docName")
					.toString(), suspDocStopWordMGrams);
		} else {
			filteredSuspDocNgrams.putAll(lastSuspFileNGrams.get(documents
					.get(SuspDocId).get("docName").toString()));
			suspDocNPlusgrams.putAll(lastSuspFileNPlusGrams.get(documents
					.get(SuspDocId).get("docName").toString()));
			suspDocContextualNPlusgrams.putAll(lastSuspFileContextualNPlusGrams
					.get(documents.get(SuspDocId).get("docName").toString()));
			suspDocStopWordMGrams.putAll(lastSuspStopWordMGrams.get(documents
					.get(SuspDocId).get("docName").toString()));
		}

		List<Pair<String, Pair<Integer, Integer>>> srctokens = NGramExtractor
				.getFilteredTokens(srcFileString, IFSTEM, indexInfo );
		List<Pair<String, Pair<Integer, Integer>>> srcStopWordLessTokens = NGramExtractor
				.getNonStopWordTokens(srctokens,
						TextAlignmentDatasetSettings.LANGUAGE);
		if (!lastSrcFileNGrams.containsKey(documents.get(SrcDocId)
				.get("docName").toString())) {
			srcDocNgrams.putAll(NGramExtractor.extractSegmentNonStopWordNGrams(
					N, srcFileString, SrcDocId, new Pair<Integer, Integer>(0,
							srcFileString.length()), ngramType,
					srcStopWordLessTokens, 3));

			lastSrcFileNGrams.put(documents.get(SrcDocId).get("docName")
					.toString(), srcDocNgrams);

		} else {
			srcDocNgrams.putAll(lastSrcFileNGrams.get(documents.get(SrcDocId)
					.get("docName").toString()));

		}

		if (!lastSrcFileNPlusGrams.containsKey(documents.get(SrcDocId)
				.get("docName").toString())) {

			srcDocNPlusgrams.putAll(NGramExtractor
					.extractSegmentNonStopWordNGrams(N + 1, srcFileString,
							SrcDocId, new Pair<Integer, Integer>(0,
									srcFileString.length()), ngramType,
							srcStopWordLessTokens, 3));
			srcDocContextualNPlusgrams.putAll(NGramExtractor
					.extractSegmentContextualNonStopWordNGrams(
							N + 1,
							srcFileString,
							SrcDocId,
							new Pair<Integer, Integer>(0, srcFileString
									.length()), ngramType,
							srcStopWordLessTokens));
			srcDocStopWordMGrams.putAll(NGramExtractor
					.extractSegmentStopWordMGrams(
							M,
							srcFileString,
							SrcDocId,
							new Pair<Integer, Integer>(0, suspFileString
									.length()), ngramType, srctokens));
			lastSrcFileNPlusGrams.put(documents.get(SrcDocId).get("docName")
					.toString(), srcDocNPlusgrams);
			lastSrcFileContextualNPlusGrams.put(
					documents.get(SrcDocId).get("docName").toString(),
					srcDocContextualNPlusgrams);
			lastSrcStopWordMGrams.put(documents.get(SrcDocId).get("docName")
					.toString(), srcDocStopWordMGrams);
		} else {

			try {
				srcDocNPlusgrams.putAll(lastSrcFileNPlusGrams.get(documents
						.get(SrcDocId).get("docName").toString()));
				srcDocContextualNPlusgrams
						.putAll(lastSrcFileContextualNPlusGrams.get(documents
								.get(SrcDocId).get("docName").toString()));
				srcDocStopWordMGrams.putAll(lastSrcStopWordMGrams.get(documents
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

	public static void detectngramPairs(List<NGramFeature> srcDocNgrams,
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

							if (!vectorPairMap.containsKey(vector)) {
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

	Map<String, Long> nodeIDz = new HashMap<String, Long>();

	public void initializeForPAN2014(String cORPUS_MAIN_DIR, String oUTPUT_DIR) {
		TextAlignmentDatasetSettings.LANGUAGE = "EN";

		TextAlignmentDatasetSettings.datasetIndex = Arrays.asList(
				TextAlignmentDatasetSettings.datasetNames).indexOf("PAN2014");

		ngramType = NGramType.Word;
		N = 3;
		TextAlignmentDatasetSettings.methodIndex = Arrays.asList(
				TextAlignmentDatasetSettings.methodNames).indexOf(
				"Word" + N + "Gram");

		nGramSizeType = NGramSizeType.Fixed;

		TextAlignmentDatasetSettings.initialize();
	}

	public List<Feature> seedsIntegrator() {
		List<Feature> features = new ArrayList<Feature>();
		return features;
	}
}
