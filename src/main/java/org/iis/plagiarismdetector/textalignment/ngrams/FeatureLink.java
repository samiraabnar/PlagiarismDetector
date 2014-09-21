package org.iis.plagiarismdetector.textalignment.ngrams;

import java.util.HashMap;
import java.util.Map;

public class FeatureLink {
	Double weight;

	NGramFeature startEndNGram;
	NGramFeature endEndNGram;

	LinkType linkType;

	Map<String, Object> properties;

	public FeatureLink() {
		properties = new HashMap<String, Object>();
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public NGramFeature getStartEndNGram() {
		return startEndNGram;
	}

	public void setStartEndNGram(NGramFeature startEndNGram) {
		this.startEndNGram = startEndNGram;
	}

	public NGramFeature getEndEndNGram() {
		return endEndNGram;
	}

	public void setEndEndNGram(NGramFeature endEndNGram) {
		this.endEndNGram = endEndNGram;
	}

	public LinkType getLinkType() {
		return linkType;
	}

	public void setLinkType(LinkType linkType) {
		this.linkType = linkType;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

}
