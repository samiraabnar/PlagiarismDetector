package org.iis.plagiarismdetector.textalignment.validintervals;

public class Feature implements Comparable<Feature>{

	String featureString;
	FeatureType featureType;
	
	public Feature(String featureString2, FeatureType type) {
		setFeatureString(featureString2);
		setFeatureType(type);
	}
	public String getFeatureString() {
		return featureString;
	}
	public void setFeatureString(String featureString) {
		this.featureString = featureString;
	}
	public FeatureType getFeatureType() {
		return featureType;
	}
	public void setFeatureType(FeatureType featureType) {
		this.featureType = featureType;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((featureString == null) ? 0 : featureString.hashCode());
		result = prime * result
				+ ((featureType == null) ? 0 : featureType.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Feature other = (Feature) obj;
		if (featureString == null) {
			if (other.featureString != null)
				return false;
		} else if (!featureString.equals(other.featureString))
			return false;
		if (featureType != other.featureType)
			return false;
		return true;
	}
	@Override
	public int compareTo(Feature arg0) {
		
		return featureString.compareTo(arg0.featureString);
	}
	
}
