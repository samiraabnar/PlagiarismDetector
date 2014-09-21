package org.iis.plagiarismdetector.core;

import java.io.File;

import com.sun.tools.javac.util.Pair;

/**
 * Simple class to manage a detection feature specified by a text offset and
 * length corresponding to a offset and length in a source document.
 */
public class Feature {
	private Long offset;
	private Long length;

	public void setSrcOffset(Long srcOffset) {
		this.srcOffset = srcOffset;
	}

	public void setSrcLength(Long srcLength) {
		this.srcLength = srcLength;
	}

	Long srcOffset;
	Long srcLength;

	public Feature(Long suspStartOffset, long suspLength, Long srcStartOffset,
			Long srcLength) {
		this.setOffset(suspStartOffset);
		this.setLength(suspLength);
		this.srcOffset = srcStartOffset;
		this.srcLength = srcLength;
	}

	public Feature(int suspStartOffset, int suspLength, int srcStartOffset,
			int sourceLength) {
		this.setOffset((long) suspStartOffset);
		this.setLength((long) suspLength);
		this.srcOffset = (long) srcStartOffset;
		this.srcLength = (long) sourceLength;
	}

	public Long getLength() {
		return length;
	}

	public void setLength(Long length) {
		this.length = length;
	}

	public Long getOffset() {
		return offset;
	}

	public void setOffset(Long offset) {
		this.offset = offset;
	}

	public Feature overlap(Feature realFeature) {

		Pair<Long, Long> overlappingSusp = computeSuspOverLap(realFeature);
		if (overlappingSusp.snd > 0) {
			Pair<Long, Long> overlappingSrc = computeSrcOverLap(realFeature);
			if (overlappingSrc.snd > 0) {
				return new Feature(overlappingSusp.fst, overlappingSusp.snd,
						overlappingSrc.fst, overlappingSrc.snd);
			}
		}

		return null;
	}

	private Pair<Long, Long> computeSrcOverLap(Feature realFeature) {
		Long overlappingSrcOffset = 0L;
		Long overlappingSrcLength = 0L;
		if (this.srcOffset <= realFeature.srcOffset) {
			if ((this.srcOffset + this.srcLength) >= realFeature.srcOffset) {
				overlappingSrcOffset = realFeature.srcOffset;
				overlappingSrcLength = Math.min(this.srcOffset + this.srcLength
						- realFeature.srcOffset, realFeature.srcLength);
			}
		} else {
			if ((realFeature.srcOffset + realFeature.srcLength) >= this.srcOffset) {
				overlappingSrcOffset = this.srcOffset;
				overlappingSrcLength = Math.min(realFeature.srcOffset
						+ realFeature.srcLength - this.srcOffset,
						this.srcLength);
			}
		}

		return new Pair<Long, Long>(overlappingSrcOffset, overlappingSrcLength);
	}

	public Pair<Long, Long> computeSuspOverLap(Feature realFeature) {
		Long overlappingSuspOffset = 0L;
		Long overlappingSuspLength = 0L;
		if (this.offset <= realFeature.offset) {
			if ((this.offset + this.length) >= realFeature.offset) {
				overlappingSuspOffset = realFeature.offset;
				overlappingSuspLength = Math.min(this.offset + this.length
						- realFeature.offset, realFeature.length);
			}
		} else {
			if ((realFeature.offset + realFeature.length) >= this.offset) {
				overlappingSuspOffset = this.offset;
				overlappingSuspLength = Math.min(realFeature.offset
						+ realFeature.length - this.offset, this.length);
			}
		}

		return new Pair<Long, Long>(overlappingSuspOffset,
				overlappingSuspLength);
	}

	public Long getSrcLength() {
		// TODO Auto-generated method stub
		return srcLength;
	}

	public Long getSrcOffset() {
		// TODO Auto-generated method stub
		return srcOffset;
	}

	public Boolean validateFeature(String suspFileName, String srcFileName) {

		String suspMatn = TextProcessor.getMatn(new File(suspFileName));
		String srcMatn = TextProcessor.getMatn(new File(srcFileName));
		Boolean valid = true;
		if (srcOffset > srcMatn.length()) {
			System.out.println("Invalid Source Offset");
			valid = false;
		}

		if ((srcOffset + srcLength) > srcMatn.length()) {
			System.out.println("Invalid Source Offset+Length");
			valid = false;
		}
		if (offset > suspMatn.length()) {
			System.out.println("Invalid Susp Offset");
			valid = false;
		}
		if ((offset + length) > suspMatn.length()) {
			System.out.println("Invalid Susp Offset+Length");
			valid = false;
		}

		return valid;
	}
}