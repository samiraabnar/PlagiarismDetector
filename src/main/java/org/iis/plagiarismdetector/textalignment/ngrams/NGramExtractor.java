package org.iis.plagiarismdetector.textalignment.ngrams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.lucene.util.BytesRef;
import org.iis.plagiarismdetector.core.MyDictionary;
import org.iis.plagiarismdetector.core.TextProcessor;
import org.iis.plagiarismdetector.core.TokenType;
import org.iis.plagiarismdetector.core.lucene.IndexInfo;
import org.iis.plagiarismdetector.settings.TextAlignmentDatasetSettings;
import org.iis.postagger.HunPOSTagger;
import org.iis.postagger.TaggedWord;

import com.sun.tools.javac.util.Pair;

public class NGramExtractor {

	public static List<Pair<String, Pair<Integer, Integer>>> filterTokensBasedOnIDF(
			List<Pair<String, Pair<Integer, Integer>>> tokens, IndexInfo indexInfo) throws Exception {
		List<Pair<Integer, Double>> idfMap = new ArrayList<Pair<Integer, Double>>();
		for (int i = 0; i < tokens.size(); i++) {
			Double idf = 1D / indexInfo.getDF("TEXT", new BytesRef(tokens.get(i).fst));
			// if (idf < (IndexInfo.number_of_documents / 5))
			idfMap.add(new Pair<Integer, Double>(i, idf));
		}

		Collections.sort(idfMap, new Comparator<Pair<Integer, Double>>() {

			@Override
			public int compare(Pair<Integer, Double> o1,
					Pair<Integer, Double> o2) {
				return o1.snd.compareTo(o2.snd);
			}
		});

		/*
		 * Integer maxDiffFirstIndex = -1; Double maxDifValue = -1D; for (int i
		 * = 1; i < (idfMap.size() - 1); i++) { Double diff =
		 * (idfMap.get(i).snd) - idfMap.get(i - 1).snd; if (diff > maxDifValue)
		 * { maxDifValue = diff; maxDiffFirstIndex = i - 1; } }
		 * 
		 * if (maxDiffFirstIndex > -1)
		 */
		idfMap = idfMap.subList(0, idfMap.size() / 4);

		List<Pair<String, Pair<Integer, Integer>>> newTokens = new ArrayList<Pair<String, Pair<Integer, Integer>>>(
				tokens);
		for (int i = 0; i < idfMap.size(); i++) {
			newTokens.remove(tokens.get(idfMap.get(i).fst));
		}

		return newTokens;
	}

	/*
	 * public static List<NGramFeature> extractSegmentSentenceBasedNGrams(
	 * String text, Long documentId, Pair<Integer, Integer> segmentRange,
	 * NGramType ngramType) { List<NGramFeature> ngrams = new
	 * ArrayList<NGramFeature>();
	 * 
	 * List<String> sentences = Arrays
	 * .asList(TextProcessor.getSentences(text));
	 * 
	 * Map<String, List<Integer>> tokensMap = new HashMap<String,
	 * List<Integer>>(); Map<Integer, List<TaggedWord>> POSzList = new
	 * HashMap<Integer, List<TaggedWord>>(); Integer startOffset = 0; for
	 * (String sentence : sentences) { List<Pair<String, Integer>>
	 * sentenceTokens = TextProcessor .tokenize(sentence,
	 * TokenType.AlphaNumerical, startOffset); startOffset += sentence.length();
	 * List<TaggedWord> posTagged = posTag(sentence);
	 * 
	 * List<Pair<String, Integer>> revisedSentenceTokens = new
	 * ArrayList<Pair<String, Integer>>(); for (int i = 0, j = 0; i <
	 * sentenceTokens.size(); i++) { int k = j; while ((k < posTagged.size()) &&
	 * !posTagged.get(k).getWord() .equals(sentenceTokens.get(i).fst)) {
	 * posTagged.remove(k); } if ((k < posTagged.size()) &&
	 * posTagged.get(k).getWord() .equals(sentenceTokens.get(i).fst)) { j = k +
	 * 1; revisedSentenceTokens.add(sentenceTokens.get(i)); } }
	 * 
	 * for (int k = revisedSentenceTokens.size(); k < posTagged.size(); k++) {
	 * posTagged.remove(k); }
	 * 
	 * String ngramValue = ""; for (int i = 0; i < revisedSentenceTokens.size();
	 * i++) { if (TextProcessor.getStopWords(
	 * TextAlignmentDatasetSettings.LANGUAGE).contains(
	 * revisedSentenceTokens.get(i).fst)) { revisedSentenceTokens.remove(i);
	 * posTagged.remove(i); i--; } else { ngramValue +=
	 * revisedSentenceTokens.get(i).fst + " "; } }
	 * 
	 * ngramValue = ngramValue.trim();
	 * 
	 * if ((revisedSentenceTokens.size() > 0) && (ngramValue.length() > 0)) { if
	 * (!tokensMap.containsKey(ngramValue)) { tokensMap.put(ngramValue, new
	 * ArrayList<Integer>()); } else { //
	 * System.out.println("repeated n-gram :)"); }
	 * tokensMap.get(ngramValue).add(revisedSentenceTokens.get(0).snd);
	 * POSzList.put(revisedSentenceTokens.get(0).snd, posTagged); } }
	 * 
	 * for (String ngramValue : tokensMap.keySet()) {
	 * 
	 * NGramFeature ngramFeature = new NGramFeature(
	 * java.util.Arrays.asList(ngramValue.split("\\s+")),
	 * tokensMap.get(ngramValue), new ArrayList<Pair<Integer, Integer>>(),
	 * documentId, segmentRange, ngramType); ngramFeature.setPOSzList(new
	 * ArrayList<List<TaggedWord>>()); for (Integer beginSentOffset :
	 * tokensMap.get(ngramValue)) {
	 * ngramFeature.getPOSzList().add(POSzList.get(beginSentOffset)); }
	 * ngrams.add(ngramFeature);
	 * 
	 * }
	 * 
	 * return ngrams; }
	 */
	private static List<TaggedWord> posTag(String sentence) {
		return HunPOSTagger.tagSentence(sentence,
				TextAlignmentDatasetSettings.LANGUAGE);
	}

	/*
	 * public static List<NGramFeature> extractSegmentNonStopWordNGramsWithPOS(
	 * Integer n, String text, Long documentId, Pair<Integer, Integer>
	 * segmentRange, NGramType ngramType) { List<NGramFeature> ngrams = new
	 * ArrayList<NGramFeature>();
	 * 
	 * List<String> sentences = Arrays
	 * .asList(TextProcessor.getSentences(text)); List<Pair<TaggedWord,
	 * Integer>> tokens = new ArrayList<Pair<TaggedWord, Integer>>();
	 * Map<String, List<Integer>> tokensMap = new HashMap<String,
	 * List<Integer>>(); Map<Integer, List<TaggedWord>> POSMap = new
	 * HashMap<Integer, List<TaggedWord>>();
	 * 
	 * getSentencesPOSTaggedTokens(sentences, tokens); List<Pair<TaggedWord,
	 * Integer>> stopwordlessTokens = new ArrayList<Pair<TaggedWord,
	 * Integer>>(); for (Pair<TaggedWord, Integer> token : tokens) { if
	 * (!TextProcessor.getStopWords(
	 * TextAlignmentDatasetSettings.LANGUAGE).contains( token.fst.getWord())) {
	 * stopwordlessTokens.add(token); } }
	 * 
	 * for (int i = 0; i < (stopwordlessTokens.size() - n); i++) { String
	 * ngramValue = ""; List<TaggedWord> ngramPOS = new ArrayList<TaggedWord>();
	 * for (Pair<TaggedWord, Integer> value : stopwordlessTokens.subList( i, i +
	 * n)) { ngramValue += " " + value.fst.getWord(); ngramPOS.add(value.fst); }
	 * ngramValue = ngramValue.trim(); if
	 * (!POSMap.containsKey(stopwordlessTokens.get(i).snd))
	 * POSMap.put(stopwordlessTokens.get(i).snd, ngramPOS); else
	 * System.out.println("error occured :)"); if
	 * (!tokensMap.containsKey(ngramValue)) { tokensMap.put(ngramValue, new
	 * ArrayList<Integer>()); } else { //
	 * System.out.println("repeated n-gram :)"); }
	 * tokensMap.get(ngramValue).add(stopwordlessTokens.get(i).snd); }
	 * 
	 * for (String ngramValue : tokensMap.keySet()) {
	 * 
	 * NGramFeature ngramFeature = new NGramFeature(
	 * java.util.Arrays.asList(ngramValue.split("\\s+")),
	 * tokensMap.get(ngramValue), new ArrayList<Pair<Integer, Integer>>(),
	 * documentId, segmentRange, ngramType); ngramFeature.setPOSzList(new
	 * ArrayList<List<TaggedWord>>()); for (Integer beginSentOffset :
	 * tokensMap.get(ngramValue)) {
	 * ngramFeature.getPOSzList().add(POSMap.get(beginSentOffset)); }
	 * ngrams.add(ngramFeature);
	 * 
	 * }
	 * 
	 * return ngrams;
	 * 
	 * }
	 */
	public static void getSentencesPOSTaggedTokens(List<String> sentences,
			List<Pair<TaggedWord, Integer>> tokens) {
		Integer startOffset = 0;
		for (String sentence : sentences) {
			List<Pair<String, Integer>> sentenceTokens = TextProcessor
					.tokenize(sentence, TokenType.AlphaNumerical, startOffset);
			startOffset += sentence.length();
			List<TaggedWord> posTagged = posTag(sentence);

			List<Pair<String, Integer>> revisedSentenceTokens = new ArrayList<Pair<String, Integer>>();
			for (int i = 0, j = 0; i < sentenceTokens.size(); i++) {
				int k = j;
				while ((k < posTagged.size())
						&& !posTagged.get(k).getWord()
								.equals(sentenceTokens.get(i).fst)) {
					posTagged.remove(k);
				}
				if ((k < posTagged.size())
						&& posTagged.get(k).getWord()
								.equals(sentenceTokens.get(i).fst)) {
					j = k + 1;
					revisedSentenceTokens.add(sentenceTokens.get(i));
				}
			}

			for (int i = 0; i < revisedSentenceTokens.size(); i++) {
				tokens.add(new Pair<TaggedWord, Integer>(posTagged.get(i),
						revisedSentenceTokens.get(i).snd));
			}

		}
	}

	public static Map<String, NGramFeature> extractSegmentNonStopWordNGrams(
			Integer n, String text, Long documentId,
			Pair<Integer, Integer> segmentRange, NGramType ngramType,
			List<Pair<String, Pair<Integer, Integer>>> stopwordlessTokens,
			Integer expansionDegree) throws Exception {

		Map<String, NGramFeature> ngrams = new HashMap<String, NGramFeature>();
		/*
		 * List<Pair<String, Integer>> tokens = TextProcessor.tokenize(text,
		 * TokenType.AlphaNumerical, 0); tokens =
		 * filterTokensBasedOnIDF(tokens);
		 */
		Map<String, Pair<List<Integer>, List<Integer>>> tokensMap = new HashMap<String, Pair<List<Integer>, List<Integer>>>();
		Map<String, List<Pair<Integer, Integer>>> tokensSentenceMap = new HashMap<String, List<Pair<Integer, Integer>>>();
		Map<String, List<Double>> tokenTransparencysMap = new HashMap<String, List<Double>>();

		for (int i = 0; i < (stopwordlessTokens.size() - n); i++) {

			List<String> mainngramparts = new ArrayList<String>();
			for (Pair<String, Pair<Integer, Integer>> value : stopwordlessTokens
					.subList(i, i + n)) {
				mainngramparts.add(value.fst);

			}

			Pair<List<List<String>>, List<Double>> synonyms = /*
															 * new
															 * Pair<List<List
															 * <String>>,
															 * List<Double>>(
															 * mainngrampartsList
															 * ,
															 * transparencyList
															 * );
															 */
			MyDictionary.getNGramSynonyms(mainngramparts);

			Set<String> ngramSynValues = new HashSet<String>();
			for (int scount = 0; (scount < synonyms.fst.size())
					&& (scount < expansionDegree); scount++) {
				List<String> ngramparts = synonyms.fst.get(scount);
				String ngramValue = "";
				Collections.sort(ngramparts);
				for (String value : ngramparts)
					ngramValue += " " + value;

				ngramValue = ngramValue.trim();
				if (ngramSynValues.contains(ngramValue))
					continue;
				ngramSynValues.add(ngramValue);
				if (ngramparts.size() > n) {
					System.out.println("wrong n-gram");
				}
				if (!tokensMap.containsKey(ngramValue)) {
					tokensMap.put(ngramValue,
							new Pair<List<Integer>, List<Integer>>(
									new ArrayList<Integer>(),
									new ArrayList<Integer>()));
				}
				if (!tokensSentenceMap.containsKey(ngramValue)) {
					tokensSentenceMap.put(ngramValue,
							new ArrayList<Pair<Integer, Integer>>());

				}
				if (!tokenTransparencysMap.containsKey(ngramValue)) {
					tokenTransparencysMap.put(ngramValue,
							new ArrayList<Double>());

				}
				tokensMap.get(ngramValue).fst
						.add(stopwordlessTokens.get(i).snd.fst);
				tokensMap.get(ngramValue).snd
						.add(stopwordlessTokens.get(i).snd.snd);
				tokenTransparencysMap.get(ngramValue).add(
						synonyms.snd.get(scount));
				tokensSentenceMap.get(ngramValue).add(
						getOffsetPair(text, stopwordlessTokens, i));
			}
		}
		for (String ngramValue : tokensMap.keySet()) {

			NGramFeature ngramFeature = new NGramFeature(
					java.util.Arrays.asList(ngramValue.split("\\s+")),
					tokensMap.get(ngramValue).fst,
					tokensMap.get(ngramValue).snd,
					tokensSentenceMap.get(ngramValue), documentId,
					segmentRange, ngramType,
					tokenTransparencysMap.get(ngramValue));
			ngrams.put(ngramFeature.getStringValue(), ngramFeature);
		}

		return ngrams;
	}

	public static List<Pair<String, Pair<Integer, Integer>>> getFilteredTokens(
			String text, Boolean stemmed, IndexInfo indexInfo) throws IOException, Exception {
		List<Pair<String, Pair<Integer, Integer>>> tokens = TextProcessor
				.luceneTokenizer(text, stemmed, false);
		tokens = filterTokensBasedOnIDF(tokens,indexInfo);
		return tokens;
	}

	public static List<Pair<String, Pair<Integer, Integer>>> getNonStopWordTokens(
			List<Pair<String, Pair<Integer, Integer>>> tokens, String lang)
			throws IOException, Exception {

		List<Pair<String, Pair<Integer, Integer>>> stopwordlessTokens = new ArrayList<Pair<String, Pair<Integer, Integer>>>();
		for (Pair<String, Pair<Integer, Integer>> token : tokens) {
			if (!TextProcessor.getStopWords(lang).contains(token.fst)) {
				stopwordlessTokens.add(token);
			}
		}

		return stopwordlessTokens;
	}

	/*
	 * public static List<NGramFeature> extractSegmentOneGrams(String text, Long
	 * documentId, Pair<Integer, Integer> segmentRange, NGramType ngramType) {
	 * List<NGramFeature> onegrams = new ArrayList<NGramFeature>();
	 * List<Pair<String, Integer>> tokens = TextProcessor.tokenize(text,
	 * TokenType.AlphaNumerical, 0);
	 * 
	 * Map<String, List<Integer>> tokensMap = new HashMap<String,
	 * List<Integer>>(); for (Pair<String, Integer> token : tokens) { if
	 * (!tokensMap.containsKey(token.fst)) { tokensMap.put(token.fst, new
	 * ArrayList<Integer>()); } tokensMap.get(token.fst).add(token.snd); }
	 * 
	 * for (String onegramValue : tokensMap.keySet()) { List<String> grams = new
	 * ArrayList<String>(); grams.add(onegramValue); NGramFeature ngramFeature =
	 * new NGramFeature(grams, tokensMap.get(onegramValue), new
	 * ArrayList<Pair<Integer, Integer>>(), documentId, segmentRange,
	 * ngramType); onegrams.add(ngramFeature); }
	 * 
	 * return onegrams; }
	 */
	/*
	 * public static List<NGramFeature> computeNextLevelNGrams(
	 * List<NGramFeature> ngrams) { List<NGramFeature> nextLevelNGrams =
	 * NGramFeature .getNextLevelNgram(ngrams.get(0));
	 * 
	 * return nextLevelNGrams; }
	 */
	/*
	 * private static List<NGramFeature> extractSegmentNonStopwordOneGrams(
	 * String text, Long documentId, Pair<Integer, Integer> segmentRange,
	 * NGramType ngramType) { List<NGramFeature> onegrams = new
	 * ArrayList<NGramFeature>(); List<Pair<String, Integer>> tokens =
	 * TextProcessor.tokenize(text, TokenType.AlphaNumerical, 0);
	 * 
	 * Map<String, List<Integer>> tokensMap = new HashMap<String,
	 * List<Integer>>(); for (Pair<String, Integer> token : tokens) { if
	 * (!tokensMap.containsKey(token.fst)) { tokensMap.put(token.fst, new
	 * ArrayList<Integer>()); } tokensMap.get(token.fst).add(token.snd); }
	 * 
	 * for (String onegramValue : tokensMap.keySet()) { List<String> grams = new
	 * ArrayList<String>(); grams.add(onegramValue); if
	 * (!TextProcessor.getStopWords(
	 * TextAlignmentDatasetSettings.LANGUAGE).contains( onegramValue)) {
	 * NGramFeature ngramFeature = new NGramFeature(grams,
	 * tokensMap.get(onegramValue), new ArrayList<Pair<Integer, Integer>>(),
	 * documentId, segmentRange, ngramType); onegrams.add(ngramFeature); } }
	 * 
	 * for (int i = 0; i < (onegrams.size() - 1); i++) { FeatureLink link = new
	 * FeatureLink(); link.setLinkType(LinkType.Following);
	 * link.setStartEndNGram(onegrams.get(i));
	 * link.setEndEndNGram(onegrams.get(i + 1)); link.setWeight(1D);
	 * onegrams.get(i).getFollowingLinks().add(link); }
	 * 
	 * return onegrams; }
	 */

	public static Pair<Integer, Integer> getOffsetPair(String text,
			List<Pair<String, Pair<Integer, Integer>>> stopwordlessTokens, int i) {
		/*
		 * try {
		 * 
		 * System.out.println(stopwordlessTokens.get(i).fst + " " +
		 * stopwordlessTokens.get(i).snd + " " +
		 * text.substring(stopwordlessTokens.get(i).snd +
		 * stopwordlessTokens.get(i).fst.length()));
		 * 
		 * } catch (Exception e) { e.printStackTrace(); System.exit(0); }
		 */
		return new Pair<Integer, Integer>(Math.max(0, TextProcessor
				.getLastIndex(
						text.substring(0, stopwordlessTokens.get(i).snd.fst),
						"[!.?:]+")), Math.max(
				TextProcessor.getFirstIndex(
						text.substring(stopwordlessTokens.get(i).snd.fst
								+ stopwordlessTokens.get(i).fst.length()),
						"[!.?:]+")
						+ stopwordlessTokens.get(i).snd.fst
						+ stopwordlessTokens.get(i).fst.length(),
				stopwordlessTokens.get(i).snd.fst
						+ stopwordlessTokens.get(i).fst.length()));
	}

	static Random random = new Random(System.currentTimeMillis());

	public static Map<String, NGramFeature> extractSegmentContextualNonStopWordNGrams(
			Integer n, String text, Long documentId,
			Pair<Integer, Integer> segmentRange, NGramType ngramType,
			List<Pair<String, Pair<Integer, Integer>>> stopwordlessTokens)
			throws Exception {

		Map<String, NGramFeature> ngrams = new HashMap<String, NGramFeature>();
		/*
		 * List<Pair<String, Integer>> tokens = TextProcessor.tokenize(text,
		 * TokenType.AlphaNumerical, 0); tokens =
		 * filterTokensBasedOnIDF(tokens);
		 */
		Map<String, Pair<List<Integer>, List<Integer>>> tokensMap = new HashMap<String, Pair<List<Integer>, List<Integer>>>();
		Map<String, List<Pair<Integer, Integer>>> tokensSentenceMap = new HashMap<String, List<Pair<Integer, Integer>>>();
		Map<String, List<Double>> tokenTransparencysMap = new HashMap<String, List<Double>>();

		for (int i = 0; i < (stopwordlessTokens.size() - n); i++) {

			List<String> mainngramparts = new ArrayList<String>();
			for (Pair<String, Pair<Integer, Integer>> value : stopwordlessTokens
					.subList(i, i + n)) {
				mainngramparts.add(value.fst);
			}
			Integer maxCount = Math.max((mainngramparts.size() / 2 - 1), 0);
			Integer[] removeIndex = new Integer[maxCount];
			for (int r = 0; r < maxCount; r++) {
				removeIndex[r] = (Math.abs(random.nextInt()) % (mainngramparts
						.size() - 1)) + 1;
			}
			Arrays.sort(removeIndex);

			for (int r = 0; r < maxCount; r++) {
				mainngramparts.remove(removeIndex[r] - r);
			}
			Pair<List<List<String>>, List<Double>> synonyms = MyDictionary
					.getNGramSynonyms(mainngramparts);

			/*
			 * new Pair<List<List<String>>, List<Double>>( new
			 * ArrayList<List<String>>(), new ArrayList<Double>());
			 */
			synonyms.fst.add(mainngramparts);
			synonyms.snd.add(1D);
			Set<String> ngramSynValues = new HashSet<String>();
			for (int scount = 0; (scount < synonyms.fst.size()) && (scount < 5); scount++) {
				List<String> ngramparts = synonyms.fst.get(scount);
				String ngramValue = "";
				Collections.sort(ngramparts);
				for (String value : ngramparts)
					ngramValue += " " + value;

				ngramValue = ngramValue.trim();
				if (ngramSynValues.contains(ngramValue))
					continue;
				ngramSynValues.add(ngramValue);
				if (ngramparts.size() > n) {
					System.out.println("wrong n-gram");
				}
				if (!tokensMap.containsKey(ngramValue)) {
					tokensMap.put(ngramValue,
							new Pair<List<Integer>, List<Integer>>(
									new ArrayList<Integer>(),
									new ArrayList<Integer>()));
				}
				if (!tokensSentenceMap.containsKey(ngramValue)) {
					tokensSentenceMap.put(ngramValue,
							new ArrayList<Pair<Integer, Integer>>());

				}
				if (!tokenTransparencysMap.containsKey(ngramValue)) {
					tokenTransparencysMap.put(ngramValue,
							new ArrayList<Double>());

				}
				tokensMap.get(ngramValue).fst
						.add(stopwordlessTokens.get(i).snd.fst);
				tokensMap.get(ngramValue).snd
						.add(stopwordlessTokens.get(i).snd.snd);
				tokenTransparencysMap.get(ngramValue).add(
						synonyms.snd.get(scount));
				tokensSentenceMap.get(ngramValue).add(
						getOffsetPair(text, stopwordlessTokens, i));
			}
		}
		for (String ngramValue : tokensMap.keySet()) {

			NGramFeature ngramFeature = new NGramFeature(
					java.util.Arrays.asList(ngramValue.split("\\s+")),
					tokensMap.get(ngramValue).fst,
					tokensMap.get(ngramValue).snd,
					tokensSentenceMap.get(ngramValue), documentId,
					segmentRange, ngramType,
					tokenTransparencysMap.get(ngramValue));
			ngrams.put(ngramFeature.getStringValue(), ngramFeature);
		}

		return ngrams;
	}

	public static Map<String, NGramFeature> extractSegmentStopWordMGrams(
			Integer n, String text, Long documentId,
			Pair<Integer, Integer> segmentRange, NGramType ngramType,
			List<Pair<String, Pair<Integer, Integer>>> tokens) throws Exception {

		Map<String, NGramFeature> ngrams = new HashMap<String, NGramFeature>();

		List<Pair<String, Pair<Integer, Integer>>> stoptokens = getStopWords(tokens);

		Map<String, Pair<List<Integer>, List<Integer>>> tokensMap = new HashMap<String, Pair<List<Integer>, List<Integer>>>();
		Map<String, List<Pair<Integer, Integer>>> tokensSentenceMap = new HashMap<String, List<Pair<Integer, Integer>>>();
		Map<String, List<Double>> tokenTransparencysMap = new HashMap<String, List<Double>>();

		for (int i = 0; i < (stoptokens.size() - n); i++) {

			List<String> ngramparts = new ArrayList<String>();
			for (Pair<String, Pair<Integer, Integer>> value : stoptokens
					.subList(i, i + n)) {
				ngramparts.add(value.fst);
			}

			String ngramValue = "";
			Collections.sort(ngramparts);
			for (String value : ngramparts)
				ngramValue += " " + value;

			ngramValue = ngramValue.trim();

			if (!tokensMap.containsKey(ngramValue)) {
				tokensMap.put(ngramValue,
						new Pair<List<Integer>, List<Integer>>(
								new ArrayList<Integer>(),
								new ArrayList<Integer>()));
			}
			if (!tokensSentenceMap.containsKey(ngramValue)) {
				tokensSentenceMap.put(ngramValue,
						new ArrayList<Pair<Integer, Integer>>());

			}
			if (!tokenTransparencysMap.containsKey(ngramValue)) {
				tokenTransparencysMap.put(ngramValue, new ArrayList<Double>());

			}
			tokensMap.get(ngramValue).fst.add(stoptokens.get(i).snd.fst);
			tokensMap.get(ngramValue).snd.add(stoptokens.get(i).snd.snd);
			tokenTransparencysMap.get(ngramValue).add(1D);
			tokensSentenceMap.get(ngramValue).add(
					getOffsetPair(text, stoptokens, i));
		}

		for (String ngramValue : tokensMap.keySet()) {

			NGramFeature ngramFeature = new NGramFeature(
					java.util.Arrays.asList(ngramValue.split("\\s+")),
					tokensMap.get(ngramValue).fst,
					tokensMap.get(ngramValue).snd,
					tokensSentenceMap.get(ngramValue), documentId,
					segmentRange, ngramType,
					tokenTransparencysMap.get(ngramValue));
			ngrams.put(ngramFeature.getStringValue(), ngramFeature);
		}

		return ngrams;
	}

	public static List<Pair<String, Pair<Integer, Integer>>> getStopWords(
			List<Pair<String, Pair<Integer, Integer>>> tokens) {
		List<Pair<String, Pair<Integer, Integer>>> stopwordTokens = new ArrayList<Pair<String, Pair<Integer, Integer>>>();
		for (Pair<String, Pair<Integer, Integer>> token : tokens) {
			if (TextProcessor.getStopWords(
					TextAlignmentDatasetSettings.LANGUAGE).contains(token.fst)) {
				stopwordTokens.add(token);
			}
		}

		return stopwordTokens;
	}

	public static List<Pair<String, Pair<Integer, Integer>>> getTokens(
			String text, Boolean stemmed, Boolean removestopwords)
			throws IOException {
		List<Pair<String, Pair<Integer, Integer>>> tokens = TextProcessor
				.luceneTokenizer(text, stemmed, removestopwords);
		return tokens;
	}

}
