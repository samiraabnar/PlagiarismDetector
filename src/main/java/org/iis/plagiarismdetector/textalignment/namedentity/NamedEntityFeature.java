package org.iis.plagiarismdetector.textalignment.namedentity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.iis.plagiarismdetector.textalignment.GeneralFeature;
import org.iis.plagiarismdetector.core.TextProcessor;
import org.iis.plagiarismdetector.core.NamedEntityTagger.NamedEntityRecognizer;
import org.iis.plagiarismdetector.core.algorithms.MWBMatchingAlgorithm;

public class NamedEntityFeature extends GeneralFeature implements
		Comparable<NamedEntityFeature> {

	String type;

	public NamedEntityFeature(String key, String value,
			List<Integer> occurancePositionInDocument) {
		stringValue = key;
		type = value;
		this.occurancePositionInDocument = occurancePositionInDocument;
		this.occuranceCount = occurancePositionInDocument.size();

		Id = IdCount;
		IdCount++;
	}

	public NamedEntityFeature() {
		super();
	}

	@Override
	public int compareTo(NamedEntityFeature o) {
		return this.compareTo(o);
	}

	public static List<NamedEntityFeature> extractNamedEntities(String fileText)
			throws IOException {
		List<Entry<String, String>> namedEntities = NamedEntityRecognizer
				.NER(fileText);
		List<NamedEntityFeature> namedEntityFeatures = new ArrayList<NamedEntityFeature>();

		for (Entry<String, String> nameString : namedEntities) {

			int index = fileText.indexOf(nameString.getKey(), 0);
			List<Integer> occurancePositionInDocument = new ArrayList<Integer>();
			while (index >= 0) {
				occurancePositionInDocument.add(index);
				index = fileText.indexOf(nameString.getKey(), index + 1);
			}
			NamedEntityFeature namedEntity = new NamedEntityFeature(
					nameString.getKey(), nameString.getValue(),
					occurancePositionInDocument);

		}
		return namedEntityFeatures;
	}

	public Double computeSimilarity(NamedEntityFeature srcNamedEntity,
			NamedEntitySimilarityType caseInsensitiveExactString) {

		String[] srcTokens = srcNamedEntity.getStringValue().split("\\s+");
		String[] thisTokens = this.getStringValue().split("\\s+");

		MWBMatchingAlgorithm ma = new MWBMatchingAlgorithm(thisTokens.length,
				srcTokens.length);
		for (int i = 0; i < thisTokens.length; i++) {

			for (int j = 0; j < srcTokens.length; j++) {

				Double sim = TextProcessor
						.computeWordExactSimilarity(
								thisTokens[i].toLowerCase(),
								srcTokens[j].toLowerCase());
				ma.setWeight(i, j, sim);

			}
		}

		int[] matching = ma.getMatching();
		Double totalWeight = 1D;
		Integer matched = 0;
		for (int i = 0; i < thisTokens.length; i++) {
			if ((matching[i] > -1)) {
				Double weight = 0D;
				if (ma.getMinWeight() >= 1) {
					weight = ma.getWeight(i, matching[i]) - ma.getMinWeight();
				} else {
					weight = ma.getWeight(i, matching[i]);
				}

				if (weight > 0) {
					totalWeight *= weight;
					matched++;
				}
			}
		}
		Double similarityScore = totalWeight * (matched / thisTokens.length);
		return similarityScore;
	}
}
