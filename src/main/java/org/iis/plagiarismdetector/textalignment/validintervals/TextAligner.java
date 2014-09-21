package org.iis.plagiarismdetector.textalignment.validintervals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.iis.plagiarismdetector.settings.TextAlignmentDatasetSettings;
import org.iis.plagiarismdetector.textalignment.BaseLine;
import org.iis.plagiarismdetector.textalignment.Pairfilter;

import com.sun.tools.javac.util.Pair;

import org.iis.plagiarismdetector.core.PanDoc;

public class TextAligner {

	private static List<Pair<Feature, FeatureProperties>> commonFeaturesSortedBasedonSusp;
	private static List<Pair<Feature, FeatureProperties>> commonFeaturesSortedBasedonSrc;
	Map<Feature, List<FeatureProperties>> commonFeaturesInSource = new HashMap<Feature, List<FeatureProperties>>();
	Map<Feature, List<FeatureProperties>> commonFeaturesInSusp = new HashMap<Feature, List<FeatureProperties>>();

	Map<Long, Pair<Feature, FeatureProperties>>[] pairedFeatures = (HashMap<Long, Pair<Feature, FeatureProperties>>[]) (new HashMap[2]);

	// "/Users/MacBookPro/Documents/Uni-MS/FinalProject/Code/PlagiarismDetection/evaluations/PAN2013/detections/validInterval_with-stopwords_Jan6/whole/";
	// "/Users/MacBookPro/Documents/Uni-MS/FinalProject/Code/PlagiarismDetection/evaluations/Ghoghnous2013/detection/validInterval_without-stopwords_Dec30/no-obfuscation/";//;"baseLine_results_replaceWords/";

	public static void main(String[] args) throws IOException,
			InterruptedException {
		/*
		 * Process the commandline arguments. If there are two arguments we'll
		 * assume that those are a suspicious and a source document. If there is
		 * only one we have to decide if it points directly to a pairs file or
		 * to a directory. In the first case we break it down into pairs and
		 * compare them. In the latter case we scan the directory for all pairs
		 * file and proceed as before.
		 */
		if (args.length == 0) {
			File path = new File(TextAlignmentDatasetSettings.GOLD_RESULTS_DIR
					+ "pairs");
			ArrayList<File> pairs = new ArrayList<File>();

			if (path.isDirectory()) {

				pairs = BaseLine.walk(path, new Pairfilter());
			} else {
				pairs.add(path);
			}

			for (File p : pairs) {
				BufferedReader br = new BufferedReader(new FileReader(p));
				String line = null;
				while ((line = br.readLine()) != null) {
					String[] pair = line.split(" ");
					String susp = pair[0].trim();
					String src = pair[1].trim();
					alignTexts(TextAlignmentDatasetSettings.SUSP_FILES_DIR
							+ susp,
							TextAlignmentDatasetSettings.SOURCE_FILES_DIR + src);
				}
				br.close();
			}

			// evaluate();
		} else if (args.length == 2) {
			String susp = args[0];
			String src = args[1];
			alignTexts(TextAlignmentDatasetSettings.SUSP_FILES_DIR + susp,
					TextAlignmentDatasetSettings.SOURCE_FILES_DIR + src);
		} else {
			System.out.println("Unexpected number of commandline arguments.\n");
		}
	}

	/*
	 * public static void main(String[] args) { alignTexts(
	 * "/Users/MacBookPro/Documents/Uni-MS/FinalProject/Code/PlagiarismDetection/evaluations/1/SamDataset-v1/susp-no_obfuscation/suspicious-document00000.txt"
	 * ,
	 * "/Users/MacBookPro/Documents/Uni-MS/FinalProject/Code/PlagiarismDetection/evaluations/1/SamDataset-v1/norm_src/HumboldtsGift_1_BellowSaul_comedy.txt"
	 * ); }
	 */

	private static void evaluate() throws IOException, InterruptedException {
		Runtime rt = Runtime.getRuntime();
		// System.out.println("Exited with error code "+exitVal);
		String[] paramsArray = {
				"/bin/sh",
				"-c",
				"python evaluations/pan_measures.py " + "--micro " + "-p "
						+ TextAlignmentDatasetSettings.GOLD_RESULTS_DIR + ""
						+ " -d " + TextAlignmentDatasetSettings.RESULTS_DIR
						+ " > " + TextAlignmentDatasetSettings.Evaluation_DIR };
		Process proc = rt.exec(paramsArray);// > HunTagger_Output.txt");
		BufferedReader input = new BufferedReader(new InputStreamReader(
				proc.getErrorStream()));

		String lline = null;

		while ((lline = input.readLine()) != null) {
			System.out.println(lline);
		}

		int exitVal = proc.waitFor();
	}

	public static void alignTexts(String suspFileName, String srcFileName) {
		FeatureExtractor fe = new FeatureExtractor();
		Map<Feature, List<FeatureProperties>> suspFeatures = fe
				.extractFeatures(suspFileName);/*
												 * "/Users/MacBookPro/Documents/Uni-MS/FinalProject/Code/PlagiarismDetection/evaluations/1/SamDataset-v1/susp-no_obfuscation/suspicious-document00000.txt"
												 * )
												 */
		;
		Map<Feature, List<FeatureProperties>> srcFeatures = fe
				.extractFeatures(srcFileName);/*
											 * "/Users/MacBookPro/Documents/Uni-MS/FinalProject/Code/PlagiarismDetection/evaluations/1/SamDataset-v1/norm_src/HumboldtsGift_1_BellowSaul_comedy.txt"
											 * )
											 */

		TextAligner ta = new TextAligner();
		ta.findCommonFeatures(srcFeatures, suspFeatures);

		commonFeaturesSortedBasedonSusp = ta.sortCommonFeatures(ta
				.getCommonFeaturesInSusp());
		commonFeaturesSortedBasedonSrc = ta.sortCommonFeatures(ta
				.getCommonFeaturesInSource());

		// List<ValidInterval> validIntervals = ta.formInitialValidIntervals();
		// validIntervals = ta.mergeValidIntervals(validIntervals);
		// List<ValidInterval> finalIntervals =
		// ta.dropOverlappingIntervals(validIntervals);

		List<ValidInterval> finalIntervals = ta
				.formValidIntervalsConsideringSrcAndSusp();
		System.out.println("number of found valid intervals:"
				+ finalIntervals.size());

		ArrayList<org.iis.plagiarismdetector.core.Feature> features = new ArrayList<org.iis.plagiarismdetector.core.Feature>();
		for (ValidInterval interval : finalIntervals) {
			if (interval.isValid()) {
				interval.computeInfo();

				org.iis.plagiarismdetector.core.Feature feature = new org.iis.plagiarismdetector.core.Feature(
						interval.suspStartOffset,
						interval.suspLastFeatureOffset
								+ ta.pairedFeatures[0]
										.get(interval.suspLastFeatureOffset).snd.length
								- interval.suspStartOffset,
						interval.srcStartOffset,
						interval.srcLastFeatureOffset
								+ ta.pairedFeatures[1]
										.get(interval.srcLastFeatureOffset).snd.length
								- interval.srcStartOffset);
				if (!feature.validateFeature(suspFileName, srcFileName))
					System.exit(0);

				features.add(feature);

			} else {
				System.out.println("Empty Interval Detected!");
			}
		}

		try {
			ta.serializeFeatures(new File(suspFileName), new File(srcFileName),
					features);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void serializeFeatures(File susp, File src,
			ArrayList<org.iis.plagiarismdetector.core.Feature> features) throws IOException {
		String srcRef = src.getName();
		String suspRef = susp.getName();
		String srcID = srcRef.split("\\.")[0];
		String suspID = suspRef.split("\\.")[0];

		PanDoc doc = new PanDoc(suspRef, srcRef);
		for (org.iis.plagiarismdetector.core.Feature f : features) {
			doc.addFeature(f);
		}
		doc.write(TextAlignmentDatasetSettings.RESULTS_DIR + suspID + " - "
				+ srcID + ".xml");
	}

	public List<ValidInterval> dropOverlappingIntervals(
			List<ValidInterval> intervals) {

		List<ValidInterval> filteredIntervals = new ArrayList<ValidInterval>();

		if (intervals.size() == 0) {
			System.out.println("Empty Interval List ????!");
			return filteredIntervals;
		}
		int recentIntervalIndex = 0;
		for (int i = 1; i < (intervals.size()); i++) {
			if (intervals.get(recentIntervalIndex).hasOverlap(intervals.get(i))) {
				if (intervals.get(recentIntervalIndex).getLength() <= intervals
						.get(i).getLength()) {
					recentIntervalIndex = i;
				}
			} else {
				filteredIntervals.add(intervals.get(recentIntervalIndex));
				recentIntervalIndex = i;

			}
		}

		filteredIntervals.add(intervals.get(recentIntervalIndex));

		return filteredIntervals;
	}

	public List<ValidInterval> mergeValidIntervals(
			List<ValidInterval> initialValidIntervals) {
		List<ValidInterval> mergedValidIntervals = new ArrayList<ValidInterval>();

		for (int i = 0; i < (initialValidIntervals.size() - 1); i++) {

			int j = i + 1;
			ValidInterval newInterval = initialValidIntervals.get(0);
			ValidInterval newMergedInterval = newInterval
					.merge(initialValidIntervals.get(j));

			while ((newMergedInterval.isValid())
					&& (j < (initialValidIntervals.size() - 1))) {
				j++;
				newInterval = newMergedInterval.clone();
				newMergedInterval = newInterval.merge(initialValidIntervals
						.get(j));
			}
			if (newInterval.isValid())
				mergedValidIntervals.add(newInterval);
		}

		return mergedValidIntervals;
	}

	public List<ValidInterval> formInitialValidIntervals() {
		List<ValidInterval> initialValidIntervals = new ArrayList<ValidInterval>();

		for (Pair<Feature, FeatureProperties> cf : commonFeaturesSortedBasedonSusp) {
			ArrayList<Pair<Feature, FeatureProperties>> IntervalFeatures = new ArrayList<Pair<Feature, FeatureProperties>>();
			IntervalFeatures.add(cf);
			initialValidIntervals.add(new ValidInterval(IntervalFeatures));
		}

		return initialValidIntervals;
	}

	public List<Pair<Long, Long>> createPairsListFromFeatures(
			Map<Feature, List<FeatureProperties>> featuresList1,
			Map<Feature, List<FeatureProperties>> featuresList2) {
		List<Pair<Long, Long>> pairs = new ArrayList<Pair<Long, Long>>();
		pairedFeatures[0] = new HashMap<Long, Pair<Feature, FeatureProperties>>();
		pairedFeatures[1] = new HashMap<Long, Pair<Feature, FeatureProperties>>();
		for (Feature feature : featuresList1.keySet()) {
			for (FeatureProperties fp1 : featuresList1.get(feature)) {
				pairedFeatures[0].put(fp1.startOffset,
						new Pair<Feature, FeatureProperties>(feature, fp1));
				for (FeatureProperties fp2 : featuresList2.get(feature)) {
					pairedFeatures[1].put(fp2.startOffset,
							new Pair<Feature, FeatureProperties>(feature, fp2));
					pairs.add(new Pair<Long, Long>(fp1.startOffset,
							fp2.startOffset));
				}
			}
		}

		return pairs;
	}

	public List<ValidInterval> splitPairsIntoLargestPossibleValidIntervals(
			List<Pair<Long, Long>> pairs, int turn) {
		List<ValidInterval> validIntervals = new ArrayList<ValidInterval>();

		Collections.sort(pairs, new Comparator<Pair<Long, Long>>() {

			@Override
			public int compare(Pair<Long, Long> arg0, Pair<Long, Long> arg1) {
				return arg0.fst.compareTo(arg1.fst);
			}
		});

		System.out.println("Size of Pairs:" + pairs.size());
		for (int i = 0; i < pairs.size(); i++) {

			List<Pair<Feature, FeatureProperties>> initialFeaturelist = new ArrayList<Pair<Feature, FeatureProperties>>();
			List<Pair<Long, Long>> intervalPairs = new ArrayList<Pair<Long, Long>>();
			initialFeaturelist.add(pairedFeatures[turn].get(pairs.get(i).fst));
			// System.out.println("initial feature list size: "+initialFeaturelist.size());
			if (initialFeaturelist.get(0) == null) {
				System.out.println("You should check what has happend!");
			}

			intervalPairs.add(pairs.get(i));
			ValidInterval newInterval = new ValidInterval(initialFeaturelist);

			int j = i + 1;
			while ((j < pairs.size())
					&& (newInterval.canBeExtendedWith(pairedFeatures[turn]
							.get(pairs.get(j).fst)))) {
				newInterval
						.addFeature(pairedFeatures[turn].get(pairs.get(j).fst));
				intervalPairs.add(pairs.get(j));
				j++;

			}
			newInterval.setTurn(turn);
			newInterval.setPairs(intervalPairs);
			newInterval.computeInfo();
			if (newInterval.isValid()) {
				validIntervals.add(newInterval);
			}
		}

		validIntervals = dropOverlappingIntervals(validIntervals);
		return validIntervals;
	}

	public List<ValidInterval> formValidIntervalsConsideringSrcAndSusp() {
		List<ValidInterval> intervals = new ArrayList<ValidInterval>();
		Integer depth = 0;
		int turn = 0;

		List<Pair<Long, Long>> pairs = createPairsListFromFeatures(
				commonFeaturesInSusp, commonFeaturesInSource);
		intervals = recursivelyDetectIntervals(depth, pairs, turn);
		return intervals;
	}

	public List<ValidInterval> recursivelyDetectIntervals(Integer depth,
			List<Pair<Long, Long>> pairs, int turn) {
		List<ValidInterval> intervals;
		intervals = splitPairsIntoLargestPossibleValidIntervals(pairs, turn);

		if ((intervals.size() == 1)) {
			depth++;
		}

		if (depth == 2) {
			return intervals;
		}

		List<ValidInterval> finalIntervals = new ArrayList<ValidInterval>();

		for (ValidInterval interval : intervals) {
			finalIntervals.addAll(recursivelyDetectIntervals(1,
					reversePairs(interval.getPairs()), (turn + 1) % 2));
		}

		return finalIntervals;
	}

	private List<Pair<Long, Long>> reversePairs(List<Pair<Long, Long>> pairs) {
		List<Pair<Long, Long>> reversedPairsList = new ArrayList<Pair<Long, Long>>();
		for (int i = 0; i < pairs.size(); i++) {
			reversedPairsList.add(new Pair<Long, Long>(pairs.get(i).snd, pairs
					.get(i).fst));
		}

		return reversedPairsList;
	}

	public Map<Feature, List<FeatureProperties>> getCommonFeaturesInSource() {
		return commonFeaturesInSource;
	}

	public void setCommonFeaturesInSource(
			Map<Feature, List<FeatureProperties>> commonFeaturesInSource) {
		this.commonFeaturesInSource = commonFeaturesInSource;
	}

	public Map<Feature, List<FeatureProperties>> getCommonFeaturesInSusp() {
		return commonFeaturesInSusp;
	}

	public void setCommonFeaturesInSusp(
			Map<Feature, List<FeatureProperties>> commonFeaturesInSusp) {
		this.commonFeaturesInSusp = commonFeaturesInSusp;
	}

	public void findCommonFeatures(
			Map<Feature, List<FeatureProperties>> srcFeatures,
			Map<Feature, List<FeatureProperties>> suspFeatures) {

		for (Feature candidFeature : suspFeatures.keySet()) {
			if (srcFeatures.containsKey(candidFeature)) {
				commonFeaturesInSource.put(candidFeature,
						srcFeatures.get(candidFeature));
				commonFeaturesInSusp.put(candidFeature,
						suspFeatures.get(candidFeature));
			}
		}
	}

	public List<Pair<Feature, FeatureProperties>> sortCommonFeatures(
			Map<Feature, List<FeatureProperties>> commonFeatures) {
		List<Pair<Feature, FeatureProperties>> sortedCommonFeatures = new ArrayList<Pair<Feature, FeatureProperties>>();
		for (Entry<Feature, List<FeatureProperties>> items : commonFeatures
				.entrySet()) {
			for (FeatureProperties property : items.getValue()) {
				sortedCommonFeatures.add(new Pair<Feature, FeatureProperties>(
						items.getKey(), property));
			}
		}
		Collections.sort(sortedCommonFeatures,
				new Comparator<Pair<Feature, FeatureProperties>>() {
					@Override
					public int compare(Pair<Feature, FeatureProperties> p1,
							Pair<Feature, FeatureProperties> p2) {
						return p1.snd.compareTo(p1.snd);
					}
				});

		return sortedCommonFeatures;
	}

}
