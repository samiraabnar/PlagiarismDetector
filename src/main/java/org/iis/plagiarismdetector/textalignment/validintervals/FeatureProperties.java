package org.iis.plagiarismdetector.textalignment.validintervals;

public class FeatureProperties implements Comparable<FeatureProperties> {
	Long startOffset;
	Long length;
	
	public FeatureProperties(Long offset, Long length)
	{
		this.startOffset = offset;
		this.length = length;
	}
	
	public Long getStartOffset() {
		return startOffset;
	}
	public void setStartOffset(Long startOffset) {
		this.startOffset = startOffset;
	}
	public Long getLength() {
		return length;
	}
	public void setLength(Long length) {
		this.length = length;
	}

	@Override
	public int compareTo(FeatureProperties o) {
		// TODO Auto-generated method stub
		return new Long(startOffset+length).compareTo(o.getStartOffset());
	}
	
	
	
}
