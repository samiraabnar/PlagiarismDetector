package org.iis.plagiarismdetector.textalignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iis.plagiarismdetector.textalignment.ngrams.FeatureLink;

import com.sun.tools.javac.util.Pair;

public class GeneralFeature extends Object {
	protected static Long IdCount = 0L;
	protected Long Id;

	protected String stringValue;

	protected List<Integer> occurancePositionInDocument;
	protected List<Integer> wordOrderInDocument;
	protected List<Double> transparencies;

	public List<Integer> getWordOrderInDocument() {
		return wordOrderInDocument;
	}

	public void setWordOrderInDocument(List<Integer> wordOrderInDocument) {
		this.wordOrderInDocument = wordOrderInDocument;
	}

	protected List<Pair<Integer, Integer>> occurancePositionInDocumentBasedOnSentence;
	protected List<Integer> sentenceIz;

	public static Map<String, Map<String, Double>> similarietes = new HashMap<String, Map<String, Double>>();

	protected Long relatedDocumentId;
	protected Integer occuranceCount;
	protected Pair<Integer, Integer> segmentLocation;

	public List<Double> getTransparencies() {
		return transparencies;
	}

	public void setTransparencies(List<Double> transparencies) {
		this.transparencies = transparencies;
	}

	protected List<FeatureLink> followings = new ArrayList<FeatureLink>();

	public GeneralFeature(String value, List<Integer> positions,
			List<Integer> wordOrders,
			List<Pair<Integer, Integer>> sentenceOffsets, Long documentId,
			Pair<Integer, Integer> segmentRange, List<Double> transparencyList) {
		Id = IdCount;
		IdCount++;

		stringValue = value.trim();
		occurancePositionInDocument = new ArrayList<Integer>(positions);
		transparencies = new ArrayList<Double>(transparencyList);
		occurancePositionInDocumentBasedOnSentence = new ArrayList<Pair<Integer, Integer>>(
				sentenceOffsets);

		occuranceCount = occurancePositionInDocument.size();
		relatedDocumentId = documentId;
		segmentLocation = segmentRange;
		wordOrderInDocument = wordOrders;
	}

	public List<Pair<Integer, Integer>> getOccurancePositionInDocumentBasedOnSentence() {
		return occurancePositionInDocumentBasedOnSentence;
	}

	public void setOccurancePositionInDocumentBasedOnSentence(
			List<Pair<Integer, Integer>> occurancePositionInDocumentBasedOnSentence) {
		this.occurancePositionInDocumentBasedOnSentence = occurancePositionInDocumentBasedOnSentence;
	}

	public GeneralFeature() {
		Id = IdCount;
		IdCount++;
	}

	public Long getId() {
		return Id;
	}

	public void setId(Long id) {
		Id = id;
	}

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	public void setOccurancePositionInDocument(
			List<Integer> occurancePositionInDocument) {
		this.occurancePositionInDocument = occurancePositionInDocument;
	}

	public List<Integer> getOccurancePositionInDocument() {
		return occurancePositionInDocument;
	}

	public Long getRelatedDocumentId() {
		return relatedDocumentId;
	}

	public void setRelatedDocumentId(Long relatedDocumentId) {
		this.relatedDocumentId = relatedDocumentId;
	}

	public Integer getOccuranceCount() {
		return occuranceCount;
	}

	public void setOccuranceCount(Integer occuranceCount) {
		this.occuranceCount = occuranceCount;
	}

	public Pair<Integer, Integer> getSegmentLocation() {
		return segmentLocation;
	}

	public void setSegmentLocation(Pair<Integer, Integer> segmentLocation) {
		this.segmentLocation = segmentLocation;
	}

	public Double dispersionScore(Double documentLength) {
		Double score = 0D;

		Double paragrapCharLength = 500D;

		List<Integer> positions = new ArrayList<Integer>(
				occurancePositionInDocument);
		Collections.sort(positions);
		// Integer countContaningParz = 0;

		Map<Integer, Integer> containingParagraphs = new HashMap<Integer, Integer>();
		for (int i = 0; i < occurancePositionInDocument.size(); i++) {
			Integer paragraphId = (int) Math.ceil(occurancePositionInDocument
					.get(i) / paragrapCharLength);
			if (!containingParagraphs.containsKey(paragraphId)) {
				containingParagraphs.put(paragraphId, 0);
			}
			containingParagraphs.put(paragraphId,
					containingParagraphs.get(paragraphId) + 1);

		}

		for (Integer pId : containingParagraphs.keySet()) {
			Double p = containingParagraphs.get(pId).doubleValue()
					/ positions.size();

			score += p * Math.log(p);
		}
		/*
		 * score = ((double) containingParagraphs.size()) / (double)
		 * Math.ceil(documentLength.doubleValue() /
		 * paragrapCharLength.doubleValue());
		 */
		/*
		 * System.out.println(this.stringValue + ": " + score + " " +
		 * containingParagraphs.size() + " " +
		 * occurancePositionInDocument.size());
		 */

		// System.out.println(stringValue + ": " + score + " " +
		// positions.size());
		return -1 * score;
	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return super.equals(obj);
	}

}
