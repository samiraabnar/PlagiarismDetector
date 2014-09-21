/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iis.plagiarismdetector.core.lucene;

/**
 *
 * @author Sam
 */

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.PriorityQueue;

/**
 *
 * @author Mostafa Dehghani
 * 
 *         <code>LowFreqTerms</code> class extracts the down n most frequent
 *         terms (by document frequency or by total term frequency) from an
 *         existing Lucene.
 */
public class LowFreqTerms {

	static final org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(LowFreqTerms.class.getName());
	private final TermStats[] EMPTY_STATS = new TermStats[0];
	private IndexReader ireader = null;

	/**
	 * Constructor
	 * 
	 * @param ireader
	 *            IndexReader instance that determines the target index
	 */
	public LowFreqTerms(IndexReader ireader) {
		this.ireader = ireader;
	}

	/**
	 *
	 * @param numTerms
	 *            a threshold for determining size of output list
	 * @param fieldName
	 *            name of the index field which is desired
	 * @return TermStats[] ordered by terms with lowest docFreq first.
	 */
	public TermStats[] getLowDFTerms(int numTerms, String fieldName) {
		TermStatsDFInverseQueue tiq = null;
		TermsEnum te = null;
		try {
			tiq = new TermStatsDFInverseQueue(numTerms);
			Terms terms = MultiFields.getTerms(ireader, fieldName);
			if (terms != null) {
				te = terms.iterator(te);
				this.fillQueue(te, tiq, fieldName);
			}
		} catch (IOException ex) {
			log.error(ex);
		} catch (Exception ex) {
			log.error(ex);
		}
		TermStats[] result = new TermStats[tiq.size()];
		// we want lowest first so we read the queue and populate the array
		// starting at the end and work backwards
		int count = tiq.size() - 1;
		while (tiq.size() != 0) {
			result[count] = tiq.pop();
			count--;
		}
		return result;
	}

	/**
	 *
	 * @param numTerms
	 *            a threshold for determining size of output list
	 * @param fieldName
	 *            name of the index field which is desired
	 * @return TermStats[] ordered by terms with lowest docFreq first.
	 */
	public TermStats[] getLowTFTerms(int numTerms, String fieldName) {
		TermStatsTFInverseQueue tiq = null;
		TermsEnum te = null;
		try {
			// Fields fields = MultiFields.getFields(this.ireader);
			tiq = new TermStatsTFInverseQueue(numTerms);
			// Iterator<String> fieldIterator = fields.iterator();
			// while (fieldIterator.hasNext()) {
			// String fieldName = fieldIterator.next();
			// Terms terms = fields.terms(fieldName);
			Terms terms = MultiFields.getTerms(ireader, fieldName);
			if (terms != null) {
				te = terms.iterator(te);
				this.fillQueue(te, tiq, fieldName);
			}
			// }
		} catch (IOException ex) {
			log.error(ex);
		} catch (Exception ex) {
			log.error(ex);
		}
		TermStats[] result = new TermStats[tiq.size()];
		int count = tiq.size() - 1;
		while (tiq.size() != 0) {
			result[count] = tiq.pop();
			count--;
		}
		return result;
	}

	/**
	 * <code>getTotalTF_PerField</code> calculate total term frequency for the
	 * given term in the given field of index
	 * 
	 * @param field
	 *            name of the index field which is desired
	 * @param term
	 * @return
	 */
	private Long getTotalTF_PerField(String field, BytesRef text) {
		Long TF = 0L;
		Term term = new Term(field, text);
		try {
			TF = this.ireader.totalTermFreq(term);
		} catch (IOException ex) {
			log.error(ex);
		}
		return TF;
	}

	/**
	 * 
	 * <code>fillQueue</code> is a function that fill given priority queue with
	 * given object
	 * 
	 * @param termsEnum
	 *            term enumerator that contains the terms those should be pushed
	 *            to the given queue
	 * @param tiq
	 *            the priority queue
	 * @param field
	 *            name of the index field which terms belong to
	 * @throws Exception
	 */
	public void fillQueue(TermsEnum termsEnum, PriorityQueue<TermStats> tiq,
			String field) throws Exception {
		BytesRef term;
		while ((term = termsEnum.next()) != null) {
			if(term.utf8ToString().matches("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?"))
			{
				System.out.println(term.utf8ToString());
			}
			if((termsEnum.docFreq() > 1) && (!term.utf8ToString().matches("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?")))
			{
				BytesRef r = new BytesRef();
				r.copyBytes(term);
				tiq.insertWithOverflow(new TermStats(field, r, termsEnum.docFreq(),
					this.getTotalTF_PerField(field, term)));
			}

		}
	}
}

/**
 * Priority queue for TermStats objects ordered by docFreq
 *
 */
final class TermStatsDFInverseQueue extends PriorityQueue<TermStats> {

	TermStatsDFInverseQueue(int size) {
		super(size);
	}

	@Override
	protected boolean lessThan(TermStats termInfoA, TermStats termInfoB) {
		return termInfoA.docFreq > termInfoB.docFreq;
	}
}

/**
 * Priority queue for TermStats objects ordered by termFreq
 *
 */
final class TermStatsTFInverseQueue extends PriorityQueue<TermStats> {

	TermStatsTFInverseQueue(int size) {
		super(size);
	}

	@Override
	protected boolean lessThan(TermStats termInfoA, TermStats termInfoB) {
		return termInfoA.totalTermFreq > termInfoB.totalTermFreq;
	}
}
