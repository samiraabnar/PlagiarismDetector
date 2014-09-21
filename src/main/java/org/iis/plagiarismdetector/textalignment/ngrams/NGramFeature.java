package org.iis.plagiarismdetector.textalignment.ngrams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.iis.plagiarismdetector.textalignment.GeneralFeature;
import org.iis.plagiarismdetector.textalignment.LCS;

import com.sun.tools.javac.util.Pair;

import org.iis.plagiarismdetector.core.MyDictionary;
import org.iis.plagiarismdetector.core.TextProcessor;
import org.iis.plagiarismdetector.core.algorithms.MWBMatchingAlgorithm;
import edu.stanford.nlp.ling.TaggedWord;

public class NGramFeature extends GeneralFeature implements
		Comparable<NGramFeature> {

	Integer N;
	NGramType type;
	List<List<TaggedWord>> POSzList;
	List<String> ngrams;
	private static final Double NGRAM_SIMILARITY_THRESHOLD = 0.01D;

	public List<List<TaggedWord>> getPOSzList() {
		return POSzList;
	}

	public void setPOSzList(List<List<TaggedWord>> pOSzList) {
		POSzList = pOSzList;
	}

	public NGramFeature(List<String> ngramValues, List<Integer> positions,
			List<Integer> wordOrders,
			List<Pair<Integer, Integer>> sentenceOffsets, Long documentId,
			Pair<Integer, Integer> segmentRange, NGramType ngramType,
			List<Double> transparencyList) {
		super("", positions, wordOrders, sentenceOffsets, documentId,
				segmentRange, transparencyList);
		stringValue = "";

		ngrams = ngramValues;
		for (String gram : ngrams) {
			stringValue += gram + " ";
		}

		stringValue = stringValue.trim();

		ngrams = new ArrayList<String>(ngramValues);
		type = ngramType;
	}

	public NGramFeature() {
		super();
	}

	public Integer getN() {
		return N;
	}

	public void setN(Integer n) {
		N = n;
	}

	public NGramType getType() {
		return type;
	}

	public void setType(NGramType type) {
		this.type = type;
	}

	public List<String> getNgrams() {
		return ngrams;
	}

	public void setNgrams(List<String> ngrams) {
		this.ngrams = ngrams;
	}

	public List<FeatureLink> getFollowingLinks() {
		return followings;
	}

	public void setFollowingLinks(List<FeatureLink> links) {
		this.followings = links;
	}

	/*
	 * public static List<NGramFeature> getNextLevelNgram( NGramFeature
	 * startingNgram) { List<NGramFeature> nextLevelNgrams = new
	 * ArrayList<NGramFeature>();
	 * 
	 * List<NGramFeature> toBeProcessed = new ArrayList<NGramFeature>();
	 * toBeProcessed.add(startingNgram); while (toBeProcessed.size() > 0) {
	 * NGramFeature toBeProcessedNGram = toBeProcessed.get(0);
	 * toBeProcessed.remove(0); for (FeatureLink link :
	 * toBeProcessedNGram.getFollowingLinks()) { List<String> grams = new
	 * ArrayList<String>(); grams.addAll(toBeProcessedNGram.getNgrams());
	 * grams.add(link.getEndEndNGram().getNgrams()
	 * .get(link.getEndEndNGram().getNgrams().size() - 1));
	 * 
	 * List<Integer> positions = new ArrayList<Integer>();
	 * positions.addAll(toBeProcessedNGram .getOccurancePositionInDocument());
	 * 
	 * NGramFeature ngramFeature = new NGramFeature(grams, positions, new
	 * ArrayList<Pair<Integer, Integer>>(),
	 * toBeProcessedNGram.getRelatedDocumentId(),
	 * toBeProcessedNGram.getSegmentLocation(), toBeProcessedNGram.getType());
	 * 
	 * nextLevelNgrams.add(ngramFeature);
	 * toBeProcessed.add(link.getEndEndNGram()); } }
	 * 
	 * for (int i = 0; i < (nextLevelNgrams.size() - 1); i++) {
	 * List<FeatureLink> links = new ArrayList<FeatureLink>(); FeatureLink link
	 * = new FeatureLink(); link.setEndEndNGram(nextLevelNgrams.get(i + 1));
	 * link.setLinkType(LinkType.Following);
	 * link.setStartEndNGram(nextLevelNgrams.get(i)); link.setWeight(1D);
	 * links.add(link); nextLevelNgrams.get(i).setFollowingLinks(links); }
	 * return nextLevelNgrams; }
	 */
	@Override
	public int compareTo(NGramFeature o) {
		return computeSimilarity(o, NGramSimilarityType.SEMANTIC_SIMILARITY) > NGRAM_SIMILARITY_THRESHOLD ? 0
				: -1;
	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return computeSimilarity((NGramFeature) obj,
				NGramSimilarityType.SEMANTIC_SIMILARITY) > NGRAM_SIMILARITY_THRESHOLD ? true
				: false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}

	public Double computeSimilarity(NGramFeature srcNGram,
			NGramSimilarityType similarityType) {
		Double similarityValue = 0D;

		switch (similarityType) {
		case EXACT_STRING:
			if (stringValue.equals(srcNGram.stringValue))
				similarityValue = 1D;
			else
				similarityValue = 0D;
			break;
		case SEMANTIC_SIMILARITY:
			similarityValue = computeSemanticSimilarityWithLowerCost(srcNGram);// computeSemanticSimilarity(srcNGram);
			break;
		default:
			if (stringValue.equals(srcNGram.stringValue))
				similarityValue = 1D;
			else
				similarityValue = 0D;
		}

		return similarityValue;
	}

	private Double computeSemanticSimilarity(NGramFeature srcNGram) {

		if (!similarietes.containsKey(this.getStringValue())) {
			similarietes.put(this.getStringValue(),
					new HashMap<String, Double>());
		}

		if (similarietes.get(this.getStringValue()).containsKey(
				srcNGram.getStringValue())) {
			return similarietes.get(this.getStringValue()).get(
					srcNGram.getStringValue());

		} else {
			String[] srcTokens = srcNGram.getStringValue().split("\\s+");
			String[] thisTokens = this.getStringValue().split("\\s+");

			MWBMatchingAlgorithm ma = new MWBMatchingAlgorithm(
					thisTokens.length, srcTokens.length);
			for (int i = 0; i < thisTokens.length; i++) {

				for (int j = 0; j < srcTokens.length; j++) {

					Double sim = computeWordSimilarity(thisTokens[i],
							srcTokens[j]);
					ma.setWeight(i, j, sim);

				}
			}

			int[] matching = ma.getMatching();
			Double totalWeight = 1D;
			Integer matched = 0;
			for (int i = 0; i < thisTokens.length; i++) {
				if ((matching[i] > -1)) {
					Double weight = 0D;
					if (ma.getMinWeight() >= 1) {
						weight = ma.getWeight(i, matching[i])
								- ma.getMinWeight();
					} else {
						weight = ma.getWeight(i, matching[i]);
					}

					if (weight > 0) {
						totalWeight *= weight;
						matched++;
					}
				}
			}
			Double similarityScore = totalWeight
					* (matched / thisTokens.length);
			similarietes.get(this.getStringValue()).put(
					srcNGram.getStringValue(), similarityScore);
			/*
			 * if (similarityScore >= 1) { System.out.println("(" +
			 * getStringValue() + " , " + srcNGram.getStringValue() +
			 * ") similarityScore: " + similarityScore); } else if
			 * (getStringValue().equals(srcNGram.getStringValue())) {
			 * System.out.println("(" + getStringValue() + " , " +
			 * srcNGram.getStringValue() + ") similarityScore: " +
			 * similarityScore); }
			 */

			return similarityScore;
		}
	}

	private Double computeSemanticSimilarityWithLowerCost(NGramFeature srcNGram) {

		if (!similarietes.containsKey(this.getStringValue())) {
			similarietes.put(this.getStringValue(),
					new HashMap<String, Double>());
		}

		if (similarietes.get(this.getStringValue()).containsKey(
				srcNGram.getStringValue())) {
			return similarietes.get(this.getStringValue()).get(
					srcNGram.getStringValue());

		} else {
			String[] srcTokens = srcNGram.getStringValue().split("\\s+");
			String[] thisTokens = this.getStringValue().split("\\s+");
			Integer matchedCount = 0;
			Double score = 1D;

			for (int i = 0; i < thisTokens.length; i++) {

				Double tokenSim = 0D;
				for (int j = 0; j < srcTokens.length; j++) {

					tokenSim += computeWordSimilarity(thisTokens[i],
							srcTokens[j]);

				}

				if (tokenSim > 0) {
					matchedCount++;
					score *= tokenSim;
				}

			}

			Double similarityScore = score * (matchedCount / thisTokens.length);
			similarietes.get(this.getStringValue()).put(
					srcNGram.getStringValue(), similarityScore);
			/*
			 * if (similarityScore >= 1) { System.out.println("(" +
			 * getStringValue() + " , " + srcNGram.getStringValue() +
			 * ") similarityScore: " + similarityScore); } else if
			 * (getStringValue().equals(srcNGram.getStringValue())) {
			 * System.out.println("(" + getStringValue() + " , " +
			 * srcNGram.getStringValue() + ") similarityScore: " +
			 * similarityScore); }
			 */

			return similarityScore;
		}
	}

	private Double computeWordSimilarity(String thisToken, String srcToken) {

		// topic --> nadarim
		// stem
		// pos --> pos o injaa nadarim
		// word
		// wordnet distance

		Double totalSimilarity = 0D;
		/*
		 * Double characterSimilarity = computeNumberOfUniqueCommonCharacters(
		 * thisToken, srcToken) /
		 * (computeTotalNumberofUniqueCharacters(thisToken, srcToken));
		 * 
		 * Double totalSimilarity = characterSimilarity;
		 */

		try {
			if (TextProcessor.stemEnglishWord(thisToken).equals(
					TextProcessor.stemEnglishWord(srcToken)))
				totalSimilarity = 1D;
			else {
				totalSimilarity = computeSemanticSimilarityBasedOnMonoDic(
						thisToken, srcToken);
			}
		} catch (Exception e) {
			System.out.println("srcToken: " + srcToken);
			System.out.println("thisToken: " + thisToken);
			e.printStackTrace();
		}
		return totalSimilarity;

	}

	private Double computeTotalNumberofUniqueCharacters(String thisToken,
			String srcToken) {
		List<Character> totalChars = new ArrayList<Character>();
		for (int i = 0; i < thisToken.length(); i++) {
			if (!totalChars.contains(thisToken.charAt(i))) {
				totalChars.add(thisToken.charAt(i));
			}
		}

		for (int i = 0; i < srcToken.length(); i++) {
			if (!totalChars.contains(srcToken.charAt(i))) {
				totalChars.add(srcToken.charAt(i));
			}
		}

		if (totalChars.size() == 0) {
			System.out.println("unusual!");
		}
		return (double) totalChars.size();
	}

	private Double computeNumberOfUniqueCommonCharacters(String thisToken,
			String srcToken) {
		List<Character> commonChars = new ArrayList<Character>();
		for (int i = 0; i < thisToken.length(); i++) {
			if (srcToken.contains(thisToken.charAt(i) + "")) {
				if (!commonChars.contains(thisToken.charAt(i))) {
					commonChars.add(thisToken.charAt(i));
				}
			}
		}
		return (double) commonChars.size();
	}

	private Double computeSimilarityBasedOnLCS(String token1, String token2) {
		Integer LCS_Length = LCS.LengthOfLCS(token1, token2);

		return (LCS_Length.doubleValue() * 2)
				/ (token1.length() + token2.length());
	}

	private Double computeSemanticSimilarityBasedOnMonoDic(String token1,
			String token2) {
		Double fst2snd = MyDictionary.getTranslationProbability(token1, token2);
		Double snd2fst = MyDictionary.getTranslationProbability(token2, token1);

		return (fst2snd + snd2fst) / 2.0;

	}

	public Double getSumOfFadedOccuranceScore(Long chunkOffset) {
		Double sumFadedScore = 0D;
		for (int i = 0; i < wordOrderInDocument.size(); i++) {
			sumFadedScore += (transparencies.get(i) / Math
					.pow((double) Math.max(1, Math.abs((wordOrderInDocument
							.get(i) - chunkOffset))), 1D));
		}
		return sumFadedScore;
	}

	public Double getSumOfTransparencies() {
		Double sumTransparency = 0D;
		for (Double transparency : transparencies) {
			sumTransparency += transparency;
		}
		return sumTransparency;
	}

	public Double getGaussianPropagatedScore(Long offset, Double sigma) {
		Double gaussianPropagationScore = 0D;
		for (int i = 0; i < occurancePositionInDocument.size(); i++) {
			gaussianPropagationScore += transparencies.get(i)
					* Math.exp(-1
							* (Math.pow(offset - wordOrderInDocument.get(i), 2))
							/ (2 * Math.pow(sigma, 2)));
		}
		return gaussianPropagationScore;
	}
}
