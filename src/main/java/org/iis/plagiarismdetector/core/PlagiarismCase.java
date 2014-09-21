package org.iis.plagiarismdetector.core;

import java.util.HashMap;
import java.util.Map;

public class PlagiarismCase {
	Map<String, Object> features;

	String sourceDocument = "";
	String suspDocunemt = "";

	private String sourceText;

	public String getSourceText() {
		return sourceText;
	}

	public String getPlagiarizedText() {
		return plagiarizedText;
	}

	private String plagiarizedText;

	public String getSuspDocument() {
		if ((suspDocunemt == null) || (suspDocunemt.equals("")))
			suspDocunemt = features.get("this_reference").toString();
		return suspDocunemt;
	}

	public void setSuspDocunemt(String suspDocunemt) {
		this.suspDocunemt = suspDocunemt;
	}

	public Map<String, Object> getFeatures() {
		return features;
	}

	public void setFeatures(Map<String, Object> features) {
		this.features = features;
	}

	public String getSourceDocument() {

		if ((sourceDocument == null) || (sourceDocument.equals("")))
			sourceDocument = features.get("source_reference").toString();

		return sourceDocument;
	}

	public void setSourceDocument(String sourceDocument) {
		this.sourceDocument = sourceDocument;
	}

	public PlagiarismCase() {
		features = new HashMap<String, Object>();
	}

	public void setFeature(String key, String value) {
		features.put(key, value);
	}

	public String getFeature(String key) {
		return features.get(key).toString();
	}

	public void setSuspDocument(String suspFile) {
		suspDocunemt = suspFile;
	}

	public void setSourceText(String srcText) {
		sourceText = srcText;
	}

	public void setPlagiarizedText(String pText) {
		plagiarizedText = pText;
	}

	public String toString() {
		StringBuilder string = new StringBuilder();

		for (String fkey : features.keySet()) {
			string.append(fkey + ": " + features.get(fkey) + " ");
		}
		string.append("\n\n");
		string.append("Source Text:\n"
				+ (sourceText != null ? sourceText : "null")
				+ "\n real Length:"
				+ (sourceText != null ? sourceText.length() : "0") + "\n");
		string.append("Plagiarized Text:\n"
				+ (plagiarizedText != null ? plagiarizedText : "null")
				+ "\n real Length:"
				+ (plagiarizedText != null ? plagiarizedText.length() : "0")
				+ "\n");
		return string.toString();
	}

	@Override
	public PlagiarismCase clone() throws CloneNotSupportedException {
		PlagiarismCase copyOfCase = new PlagiarismCase();
		copyOfCase.features.putAll(this.features);
		copyOfCase.suspDocunemt = this.suspDocunemt;
		copyOfCase.sourceDocument = this.sourceDocument;
		copyOfCase.sourceText = this.sourceText;
		copyOfCase.plagiarizedText = this.plagiarizedText;

		return copyOfCase;
	}

	public PlagiarismCase overlap(PlagiarismCase targetPlagiarismCase)
			throws CloneNotSupportedException {

		if (this.getSourceDocument().equals(
				targetPlagiarismCase.getSourceDocument())
				&& this.getSuspDocument().equals(
						targetPlagiarismCase.getSuspDocument())) {
			Feature sourceRelatedFeature = new Feature(Long.parseLong(features
					.get("this_offset").toString()), Long.parseLong(features
					.get("this_length").toString()), Long.parseLong(features
					.get("source_offset").toString()), Long.parseLong(features
					.get("source_length").toString()));
			Feature targetRelatedFeature = new Feature(
					Long.parseLong(targetPlagiarismCase.features.get(
							"this_offset").toString()),
					Long.parseLong(targetPlagiarismCase.features.get(
							"this_length").toString()),
					Long.parseLong(targetPlagiarismCase.features.get(
							"source_offset").toString()),
					Long.parseLong(targetPlagiarismCase.features.get(
							"source_length").toString()));

			Feature overlappingFeature = sourceRelatedFeature
					.overlap(targetRelatedFeature);
			if (overlappingFeature != null) {
				PlagiarismCase overlappingCase = this.clone();
				overlappingCase.setFeature("this_offset", overlappingFeature
						.getOffset().toString());
				overlappingCase.setFeature("this_length", overlappingFeature
						.getLength().toString());
				overlappingCase.setFeature("source_offset", overlappingFeature
						.getSrcOffset().toString());
				overlappingCase.setFeature("source_length", overlappingFeature
						.getSrcLength().toString());

				return overlappingCase;
			}
		}
		return null;
	}

	public String toShortString() {
		StringBuilder string = new StringBuilder();

		for (String fkey : features.keySet()) {
			string.append(fkey + ": " + features.get(fkey) + " ");
		}
		string.append("\n");
		return string.toString();
	}
}
