package org.iis.plagiarismdetector.core.sourceretrieval.lm;

class TermProperties {
	private String value;
	private Long termFrequency;
	private Double relativetermFrequency;
	private Double documentFrequency;
	private Double transparency;

	public Double getTransparency() {
		return transparency;
	}

	public void setTransparency(Double transparency) {
		this.transparency = transparency;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Long getTermFrequency() {
		return termFrequency;
	}

	public void setTermFrequency(Long termFrequency) {
		this.termFrequency = termFrequency;
	}

	public Double getRelativetermFrequency() {
		return relativetermFrequency;
	}

	public void setRelativetermFrequency(Double d) {
		this.relativetermFrequency = d;
	}

	public Double getDocumentFrequency() {
		return documentFrequency;
	}

	public void setDocumentFrequency(Double documentFrequency) {
		this.documentFrequency = documentFrequency;
	}

}