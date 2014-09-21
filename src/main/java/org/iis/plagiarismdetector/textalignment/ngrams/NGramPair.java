package org.iis.plagiarismdetector.textalignment.ngrams;

import com.sun.tools.javac.util.Pair;
import org.iis.plagiarismdetector.textalignment.ngrams.DistanceType;

public class NGramPair {
	private static DistanceType DISTANCE_TYPE = DistanceType.EUCLIDEAN;
	Integer suspPos;
	Integer srcPos;

	Double weight;

	String suspValue;
	String srcValue;
	private Pair<Integer, Integer> srcSentenceOffset;
	private Pair<Integer, Integer> suspSentenceOffset;

	public String getSuspValue() {
		return suspValue;
	}

	public void setSuspValue(String suspValue) {
		this.suspValue = suspValue;
	}

	public String getSrcValue() {
		return srcValue;
	}

	public void setSrcValue(String srcValue) {
		this.srcValue = srcValue;
	}

	public Integer getSuspPos() {
		return suspPos;
	}

	public void setSuspPos(Integer suspPos) {
		this.suspPos = suspPos;
	}

	public Integer getSrcPos() {
		return srcPos;
	}

	public void setSrcPos(Integer srcPos) {
		this.srcPos = srcPos;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public NGramPair(Integer suspPos, Integer srcPos, Double weight,
			String suspvalue, String srcvalue,
			Pair<Integer, Integer> suspSentenceOffset,
			Pair<Integer, Integer> srcSentenceOffset) {
		super();
		this.suspPos = suspPos;
		this.srcPos = srcPos;
		this.weight = weight;
		this.suspValue = suspvalue;
		this.srcValue = srcvalue;
		this.srcSentenceOffset = srcSentenceOffset;
		this.suspSentenceOffset = suspSentenceOffset;

	}

	public Pair<Integer, Integer> getSrcSentenceOffset() {
		return srcSentenceOffset;
	}

	public void setSrcSentenceOffset(Pair<Integer, Integer> srcSentenceOffset) {
		this.srcSentenceOffset = srcSentenceOffset;
	}

	public Pair<Integer, Integer> getSuspSentenceOffset() {
		return suspSentenceOffset;
	}

	public void setSuspSentenceOffset(Pair<Integer, Integer> suspSentenceOffset) {
		this.suspSentenceOffset = suspSentenceOffset;
	}

	public Double computeDistance(NGramPair nGramPair) {
		Double distance = 0D;
		switch (DISTANCE_TYPE) {
		case EUCLIDEAN:
			distance = Math.sqrt(Math.pow(suspPos - nGramPair.suspPos, 2)
					+ Math.pow(srcPos - nGramPair.srcPos, 2));
			break;
		default:
			distance = Math.sqrt(Math.pow(suspPos - nGramPair.suspPos, 2)
					+ Math.pow(srcPos - nGramPair.srcPos, 2));
		}
		return distance;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return suspPos + "_" + srcPos + "_" + weight;
	}

}
