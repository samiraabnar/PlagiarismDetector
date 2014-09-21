package org.iis.plagiarismdetector.textalignment.validintervals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sun.tools.javac.util.Pair;

public class ValidInterval extends Object {

	public static final Long FEATURES_DISTANCE_THRESHOLD = 2000L;
	private static final long INTERVAL_LENGTH_THRESHOLD = 250;
	private static final Long INTERVAL_FEATURES_COUNT_THRESHOLD = 10L;
	private static final Long MAXIMUM_FEATURED_DISTANCE = 29L;
	List<Pair<Feature, FeatureProperties>> features;
	List<Pair<Long,Long>> pairs;
	Long firstFeatureOffset;
	Long lastFeatureOffset;
	Long lastFeatureLength;
	Long endOffset;
	int turn;
	public Long suspStartOffset;
	public Long srcStartOffset;
	Long srcLastFeatureOffset;
	Long suspLastFeatureOffset;
	public int getTurn() {
		return turn;
	}

	public void setTurn(int turn) {
		this.turn = turn;
	}

	public ValidInterval(List<Pair<Feature, FeatureProperties>> features)
	{
		try
		{
		this.features = features;
		firstFeatureOffset = features.get(0).snd.getStartOffset();
		lastFeatureOffset = features.get(features.size()-1).snd.getStartOffset();
		lastFeatureLength = features.get(features.size()-1).snd.getLength();
		endOffset = lastFeatureOffset+lastFeatureLength;
		}
		catch(Exception e)
		{
			System.out.println("Something Bad Happend");
		}
	}
	
	public void initialize(List<Pair<Feature, FeatureProperties>> features)
	{
		this.features = features;
		firstFeatureOffset = features.get(0).snd.getStartOffset();
		lastFeatureOffset = features.get(features.size()-1).snd.getStartOffset();
		lastFeatureLength = features.get(features.size()-1).snd.getLength();
		endOffset = lastFeatureOffset+lastFeatureLength;
	}
	
	public void computeInfo()
	{
		ArrayList<Pair<Long, Long>> copy_of_pairs = new ArrayList<Pair<Long,Long>>(pairs);

		if(turn == 0)
		{
			Collections.sort(copy_of_pairs, new Comparator<Pair<Long,Long>>() {

				@Override
				public int compare(Pair<Long,Long> arg0, Pair<Long,Long> arg1) {
					return arg0.fst.compareTo(arg1.fst);
				}
			});
			suspStartOffset = copy_of_pairs.get(0).fst;
			suspLastFeatureOffset = copy_of_pairs.get(copy_of_pairs.size() - 1).fst;
			
			Collections.sort(copy_of_pairs, new Comparator<Pair<Long,Long>>() {

				@Override
				public int compare(Pair<Long,Long> arg0, Pair<Long,Long> arg1) {
					return arg0.snd.compareTo(arg1.snd);
				}
			});
			
			srcStartOffset = copy_of_pairs.get(0).snd;
			srcLastFeatureOffset = copy_of_pairs.get(pairs.size() - 1).snd;
		}
		else
		{
			Collections.sort(copy_of_pairs, new Comparator<Pair<Long,Long>>() {

				@Override
				public int compare(Pair<Long,Long> arg0, Pair<Long,Long> arg1) {
					return arg0.fst.compareTo(arg1.fst);
				}
			});
			srcStartOffset = copy_of_pairs.get(0).fst;
			srcLastFeatureOffset = copy_of_pairs.get(copy_of_pairs.size() - 1).fst;
			
			Collections.sort(copy_of_pairs, new Comparator<Pair<Long,Long>>() {

				@Override
				public int compare(Pair<Long,Long> arg0, Pair<Long,Long> arg1) {
					return arg0.snd.compareTo(arg1.snd);
				}
			});
			
			suspStartOffset = copy_of_pairs.get(0).snd;
			suspLastFeatureOffset = copy_of_pairs.get(copy_of_pairs.size() - 1).snd;
		}
	}
	public List<Pair<Feature, FeatureProperties>> getFeatures() {
		return features;
	}
	public void setFeatures(List<Pair<Feature, FeatureProperties>> features) {
		this.features = features;
	}
	public Long getFirstFeatureOffset() {
		return firstFeatureOffset;
	}
	public void setFirstFeatureOffset(Long firstFeatureOffset) {
		this.firstFeatureOffset = firstFeatureOffset;
	}
	public Long getLastFeatureOffset() {
		return lastFeatureOffset;
	}
	public void setLastFeatureOffset(Long lastFeatureOffset) {
		this.lastFeatureOffset = lastFeatureOffset;
	}

	public ValidInterval merge(ValidInterval validInterval) {
		
		List<Pair<Feature, FeatureProperties>> newListofFeatures = new ArrayList<Pair<Feature, FeatureProperties>>();
		newListofFeatures.addAll(this.features);
		newListofFeatures.addAll(validInterval.getFeatures());
		ValidInterval mergedInterval = new ValidInterval(newListofFeatures);
		return mergedInterval;
	}

	public boolean isValid() {
		// TODO Auto-generated method stub
		// featuresDistances. 
		for(int i=0; i < (features.size()-1); i++)
		{
			if((features.get(i+1).snd.startOffset -
					(features.get(i).snd.startOffset+features.get(i).snd.length) 
					) > FEATURES_DISTANCE_THRESHOLD)
					{
						return false;
					}
		}
		
		//length threshold
		if(( features.get(features.size() - 1).snd.startOffset+features.get(features.size() - 1).snd.length
			- (features.get(0).snd.startOffset) 	
				) < INTERVAL_LENGTH_THRESHOLD)
				{
					return false;
				}
		
		//The interval should have at least 20 (pos- sibly overlapping) chunks, which are also present in D2.		
		if(features.size() < INTERVAL_FEATURES_COUNT_THRESHOLD)
			return false;
		
		//Between each two adjacent chunks from the interval which are also present in D2, there should be at most 49 chunks which have no matching chunk in D2.

		for(int i = 0; i < (pairs.size()-1); i++)
		{

			List<Pair<Long,Long>> copy_of_pairs = new ArrayList<Pair<Long,Long>>(pairs);
			Collections.sort(copy_of_pairs, new Comparator<Pair<Long,Long>>() {

				@Override
				public int compare(Pair<Long,Long> arg0, Pair<Long,Long> arg1) {
					return arg0.snd.compareTo(arg1.snd);
				}
			});
			
			if(Math.abs(copy_of_pairs.indexOf(new Pair<Long,Long>(pairs.get(i+1).snd,pairs.get(i+1).fst)) - 
					copy_of_pairs.indexOf(new Pair<Long,Long>(pairs.get(i).snd,pairs.get(i).fst))) > MAXIMUM_FEATURED_DISTANCE)
			{
				return false;
			}
		}
		return true;
	}
	
	 public ValidInterval clone()
	 {
		 return new ValidInterval(this.features);
	 }

	public boolean isMergeValid(
			List<Pair<Feature, FeatureProperties>> commonFeaturesSortedBasedonSrc) {
		
			
		
		return false;
	}

	public boolean isValid(
			List<Pair<Feature, FeatureProperties>> commonFeaturesSortedBasedonSrc) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean hasOverlap(ValidInterval validInterval) {

		if(this.firstFeatureOffset >= validInterval.firstFeatureOffset)
		{
			if(this.firstFeatureOffset <= validInterval.endOffset)
				return true;
		}
		
		if(validInterval.firstFeatureOffset >= this.firstFeatureOffset)
		{
			if(validInterval.firstFeatureOffset <= this.endOffset)
				return true;
		}
		return false;
	}

	public Long getLength() {
		// TODO Auto-generated method stub
		return endOffset - firstFeatureOffset;
	}

	public boolean canBeExtendedWith(Pair<Feature, FeatureProperties> pair) {

		if((pair.snd.startOffset - endOffset
				) > FEATURES_DISTANCE_THRESHOLD)
				{
					return false;
				}
		return true;
	}

	public void addFeature(Pair<Feature, FeatureProperties> pair) {
		features.add(pair);
		
		firstFeatureOffset = features.get(0).snd.getStartOffset();
		lastFeatureOffset = features.get(features.size()-1).snd.getStartOffset();
		lastFeatureLength = features.get(features.size()-1).snd.getLength();
		endOffset = lastFeatureOffset+lastFeatureLength;
	}

	public List<Pair<Long,Long>> getPairs() {
		return pairs;
	}

	public void setPairs(List<Pair<Long, Long>> intervalPairs) {
		this.pairs = intervalPairs;
	}

	
}
