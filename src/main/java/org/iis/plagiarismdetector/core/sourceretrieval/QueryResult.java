package org.iis.plagiarismdetector.core.sourceretrieval;

public class QueryResult implements Comparable<QueryResult> {
	String queryId;
	String documentId;
	Integer rank;

	Double score;

	public String getQueryId() {
		return queryId;
	}

	public void setQueryId(String queryId) {
		this.queryId = queryId;
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public Integer getRank() {
		return rank;
	}

	public void setRank(Integer rank) {
		this.rank = rank;
	}

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

	public String getQueryText() {
		return queryText;
	}

	public void setQueryText(String queryText) {
		this.queryText = queryText;
	}

	String queryText;
	private Integer indexedDocId;

	public QueryResult(String qID, String docID, Double score, String qText,
			Integer rank, Integer indexedDocId) {

		this.queryId = qID;
		this.documentId = docID;
		this.score = score;
		this.queryText = qText;
		this.rank = rank;
		this.indexedDocId = indexedDocId;
	}

	public Integer getIndexedDocId() {
		return indexedDocId;
	}

	public void setIndexedDocId(Integer indexedDocId) {
		this.indexedDocId = indexedDocId;
	}

	public int compareTo(String arg0) {
		return documentId.compareTo(arg0);
	}

	@Override
	public int compareTo(QueryResult o) {
		return documentId.compareTo(o.documentId);
	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return documentId.equals(((QueryResult) obj).documentId);
	}

	@Override
	public int hashCode() {
		return this.documentId.hashCode();
	}
}
