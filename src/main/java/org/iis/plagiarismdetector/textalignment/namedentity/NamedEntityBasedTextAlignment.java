package org.iis.plagiarismdetector.textalignment.namedentity;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iis.plagiarismdetector.settings.TextAlignmentDatasetSettings;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramBasedTextAligner;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramFeature;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramPair;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramSizeType;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramType;
import org.iis.plagiarismdetector.core.ComparisonMethod;
import org.iis.plagiarismdetector.core.FatherOfTextAligners;
import org.iis.plagiarismdetector.core.Feature;
import org.iis.plagiarismdetector.core.TextProcessor;
import de.jungblut.clustering.DBSCANClustering;
import de.jungblut.distance.DistanceMeasurer;
import de.jungblut.distance.EuclidianDistance;
import de.jungblut.math.DoubleVector;
import de.jungblut.math.dense.DenseDoubleVector;
import org.iis.plagiarismdetector.evaluation.tracking.PerformanceAnalyzer;

public class NamedEntityBasedTextAlignment extends FatherOfTextAligners {

	private static final double NANO_TO_MILI = Math.pow(10, 6);
	private static Double DB_SCAN_EPS = 500D;
	private static Integer DB_SCAN_MINPOINTS = 4;
	private static Long docIdCount = 0L;
	private static final Double NAMED_ENTITY_SIMILARITY_THRESHOLD = 0.01D;
	private static final int COMMON_NAMEDENTity_MAXIMUM_COUNT_THRESHOLD = 1000;
	private static final Double NGRAM_DISPERSITY_THRESHOLD = 0.7;
	private static final Double NAMEDENTITY_SIMILARITY_THRESHOLD = 0.5;

	private ComparisonMethod COMPARISON_METHOD = ComparisonMethod.ClusterBased;
	private Map<Long, Map<String, Object>> documents = new HashMap<Long, Map<String, Object>>();
	private Map<DenseDoubleVector, NGramPair> vectorPairMap = new HashMap<DenseDoubleVector, NGramPair>();
	private static Long pairMatchingTime = 0L;
	private static Long clusteringTime = 0L;

	public NamedEntityBasedTextAlignment() {
		super();
		TextAlignmentDatasetSettings.initialize();
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
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
			TextAlignmentDatasetSettings.methodIndex = Arrays.asList(
					TextAlignmentDatasetSettings.methodNames).indexOf(
					"Word" + n + "Gram");
		}
		if (ngramSizeType.equals(NGramSizeType.SentenceBased)) {
			TextAlignmentDatasetSettings.methodIndex = Arrays.asList(
					TextAlignmentDatasetSettings.methodNames).indexOf(
					"Word" + ngramSizeType.name() + "Gram");
		}

		TextAlignmentDatasetSettings.initialize();

		NGramBasedTextAligner nGramBasedTextAligner = new NGramBasedTextAligner();

		nGramBasedTextAligner.readAndAlignPairs(null);

		System.out.println("PairingTime: " + (pairMatchingTime / NANO_TO_MILI)
				+ "     Clustering Time: " + (clusteringTime / NANO_TO_MILI));
		;
		PerformanceAnalyzer pa = new PerformanceAnalyzer();
		pa.AnalyzePerformance();

	}

	@Override
	protected ArrayList<Feature> computeFeatures(String suspFileName,
			String srcFileName) throws IOException, SQLException, Exception {

		ArrayList<Feature> features = new ArrayList<Feature>();
		String suspFileString = TextProcessor.getMatn(new File(suspFileName));
		String srcFileString = TextProcessor.getMatn(new File(srcFileName));

		Long suspFileId = docIdCount++;
		Long srcFileId = docIdCount++;
		documents.put(suspFileId, new HashMap<String, Object>());
		documents.put(srcFileId, new HashMap<String, Object>());

		documents.get(suspFileId).put("docName", suspFileName);
		documents.get(srcFileId).put("docName", srcFileName);
		switch (COMPARISON_METHOD) {
		case ClusterBased:
			features = clustreingNamedEntityBasedTextAlignment(suspFileString,
					srcFileString, suspFileId, srcFileId);
			break;
		case SegmentComparison:
			break;
		default:

		}

		return features;
	}

	private ArrayList<Feature> clustreingNamedEntityBasedTextAlignment(
			String suspFileString, String srcFileString, Long suspFileId,
			Long srcFileId) throws IOException {

		List<NamedEntityFeature> suspDocNamedEntities = NamedEntityFeature
				.extractNamedEntities(suspFileString);
		List<NamedEntityFeature> srcDocNamedEntities = NamedEntityFeature
				.extractNamedEntities(srcFileString);

		List<NamedEntityFeature> filteredSuspDocNamedEntities = new ArrayList<NamedEntityFeature>();
		for (NamedEntityFeature suspNamedEntity : suspDocNamedEntities) {
			if (suspNamedEntity.getOccuranceCount() > 5) {
				Double dispersity = suspNamedEntity
						.dispersionScore((double) suspFileString.length());

				if (dispersity > NGRAM_DISPERSITY_THRESHOLD) {
					continue;
				}

			}

			filteredSuspDocNamedEntities.add(suspNamedEntity);
		}

		List<NGramPair> pairs = new ArrayList<NGramPair>();
		Long startTime = System.nanoTime();
		List<DoubleVector> pairVectors = new ArrayList<DoubleVector>();
		NGramFeature.similarietes = new HashMap<String, Map<String, Double>>();

		for (NamedEntityFeature suspNamedEntity : filteredSuspDocNamedEntities) {
			for (NamedEntityFeature srcNamedEntity : srcDocNamedEntities) {
				Double pairSimilarity = suspNamedEntity
						.computeSimilarity(
								srcNamedEntity,
								NamedEntitySimilarityType.CASE_INSENSITIVE_EXACT_STRING);
				if (pairSimilarity >= NAMEDENTITY_SIMILARITY_THRESHOLD) {
					for (Integer suspPos : suspNamedEntity
							.getOccurancePositionInDocument()) {
						for (Integer srcPos : srcNamedEntity
								.getOccurancePositionInDocument()) {
							DenseDoubleVector vector = new DenseDoubleVector(
									new double[] { suspPos, srcPos });

							pairVectors.add(vector);
						}
					}
				}
			}
		}

		Long endTime = System.nanoTime();
		pairMatchingTime += (endTime - startTime);

		DistanceMeasurer measure = new EuclidianDistance();
		startTime = System.nanoTime();
		List<List<DoubleVector>> clusters = DBSCANClustering.cluster(
				pairVectors, measure, DB_SCAN_MINPOINTS, DB_SCAN_EPS);
		endTime = System.nanoTime();
		clusteringTime += (endTime - startTime);
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
			Feature newFeature = new Feature(
					(long) frstFeatureInSusp.get(0),
					(long) (lastFeatureInSusp.get(0)
							+ vectorPairMap.get(lastFeatureInSusp)
									.getSuspValue().length() - frstFeatureInSusp
							.get(0)), (long) frstFeatureInSrc.get(1),
					(long) (lastFeatureInSrc.get(1)
							+ vectorPairMap.get(lastFeatureInSrc).getSrcValue()
									.length() - frstFeatureInSrc.get(1)));

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

			if (!hasOverlap)
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

}
