//package org.iis.plagiarismdetector.core.sourceretrieval.lm;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//
//import org.apache.lucene.analysis.TokenStream;
//import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
//import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
//
//import com.sun.tools.javac.util.Pair;
//
//import org.iis.plagiarismdetector.core.MyDictionary;
//import org.iis.plagiarismdetector.core.TextProcessor;
//import core.sourceretrieval.irengine.LuceneNGramAnalyzer;
//
//public class ChunkLanguageModel {
//
//	private static final String BACK_GROUND_INDEX_PATH = null;
//	private static final Boolean IFSTEM = null;
//	private static final Boolean REMOVE_STOPWORDS = null;
//	private String chunkText;
//	private List<Pair<String, Pair<Integer, Integer>>> chunkTokens;
//	private Map<String, Long> oneGrams = new HashMap<String, Long>();
//	private Map<Integer, Map<String, Long>> NGrams = new HashMap<Integer, Map<String, Long>>();
//	private Integer chunkLength = 0;
//	private Map<String, TermProperties> termCollection = new HashMap<String, TermProperties>();
//	private List<Pair<String, Double>> cumulativeProbabilities = new ArrayList<Pair<String, Double>>();
//
//	private static Random random = new Random();
//
//	public ChunkLanguageModel(String documentText) {
//		this.chunkText = documentText;
//	}
//
//	public String sampleWordsRandomly() {
//		Double p = random.nextDouble();
//
//		int index = cumulativeProbabilities.size() - 1;
//		while (!((p <= cumulativeProbabilities.get(index).snd) && (p > cumulativeProbabilities
//				.get(index - 1).snd))) {
//			if (p < cumulativeProbabilities.get(index).snd)
//				index -= (index / 2);
//			if (p > cumulativeProbabilities.get(index).snd)
//				index += (index / 2);
//		}
//
//		return cumulativeProbabilities.get(index).fst;
//	}
//
//	public void fillNGramFrequencies(Integer[] Ns) throws IOException {
//		chunkTokens = TextProcessor.luceneTokenizer(chunkText, IFSTEM,
//				REMOVE_STOPWORDS);
//		for (Pair<String, Pair<Integer, Integer>> token : chunkTokens) {
//			chunkLength++;
//			if (!oneGrams.containsKey(token.fst)) {
//				oneGrams.put(token.fst, 1L);
//			} else {
//				oneGrams.put(token.fst, oneGrams.get(token.fst) + 1L);
//			}
//		}
//
//		for (Integer n : Ns) {
//			NGrams.put(n, new HashMap<String, Long>());
//			{
//				for (int i = 0; i < (chunkTokens.size() - n); i++) {
//
//					String token = TextProcessor
//							.convertListToString(chunkTokens.subList(i, i + n));
//					if (!NGrams.get(n).containsKey(token)) {
//						NGrams.get(n).put(token, 1L);
//					} else {
//						NGrams.get(n).put(token, NGrams.get(n).get(token) + 1L);
//					}
//				}
//			}
//		}
//	}
//
//	public void computeDocumentLanguageModel(Integer n) throws IOException {
//		fillNGramFrequencies(new Integer[] { 1, 2 });
//		if (n == 1)
//			fillTermCollectionWithExpansion(n);
//		else
//			fillTermCollection(n);
//	}
//
//	private void fillTermCollectionWithExpansion(Integer n) throws IOException {
//		for (String termValue : NGrams.get(n).keySet()) {
//			TermProperties t = new TermProperties();
//			t.setDocumentFrequency(TextProcessor
//					.getIDFFromBackGroundCollection(BACK_GROUND_INDEX_PATH));
//			t.setTermFrequency(NGrams.get(n).get(termValue));
//			t.setRelativetermFrequency(NGrams.get(n).get(termValue)
//					.doubleValue()
//					/ chunkLength.doubleValue());
//			t.setTransparency(1.0);
//			t.setValue(termValue);
//			termCollection.put(termValue, t);
//
//			for (String replacement : MyDictionary.getSynonyms(termValue)) {
//				TermProperties tp = new TermProperties();
//				tp.setDocumentFrequency(TextProcessor
//						.getIDFFromBackGroundCollection(replacement));
//				tp.setTermFrequency(NGrams.get(n).get(termValue));
//				tp.setRelativetermFrequency(NGrams.get(n).get(termValue)
//						.doubleValue()
//						/ chunkLength.doubleValue());
//				tp.setTransparency(MyDictionary.getTranslationProbability(
//						termValue, replacement));
//				tp.setValue(replacement);
//
//				if (termCollection.containsKey(replacement)) {
//					tp.setTransparency(Math.min(termCollection.get(replacement)
//							.getTransparency() + tp.getTransparency(), 1.0));
//				}
//				termCollection.put(replacement, tp);
//			}
//		}
//
//	}
//
//	private void fillTermCollection(Integer n) throws IOException {
//		for (String termValue : NGrams.get(n).keySet()) {
//			TermProperties t = new TermProperties();
//			t.setDocumentFrequency(TextProcessor
//					.getIDFFromBackGroundCollection(BACK_GROUND_INDEX_PATH));
//			t.setTermFrequency(NGrams.get(n).get(termValue));
//			t.setRelativetermFrequency(NGrams.get(n).get(termValue)
//					.doubleValue()
//					/ chunkLength.doubleValue());
//			t.setTransparency(1.0);
//			t.setValue(termValue);
//			termCollection.put(termValue, t);
//		}
//	}
//
//	public void computeNGramChunkLanguageModel() throws IOException {
//		LuceneNGramAnalyzer analyzer = new LuceneNGramAnalyzer();
//		TokenStream documenTokenStream = analyzer
//				.tokenStream("TEXT", chunkText);
//
//		OffsetAttribute offsetAttribute = documenTokenStream
//				.addAttribute(OffsetAttribute.class);
//		CharTermAttribute charTermAttribute = documenTokenStream
//				.addAttribute(CharTermAttribute.class);
//
//		documenTokenStream.reset();
//		while (documenTokenStream.incrementToken()) {
//			int startOffset = offsetAttribute.startOffset();
//			int endOffset = offsetAttribute.endOffset();
//			String term = charTermAttribute.toString();
//			System.out.println(term + " " + startOffset + " " + endOffset);
//		}
//		analyzer.close();
//	}
//
//	public static void main(String[] args) throws IOException {
//		ChunkLanguageModel dm = new ChunkLanguageModel(
//				" hello! My name is Samira Abnar! I am 25... I am in live with mosi...We are living together in my parents house \n I am currently working on my theses...:)!");
//		dm.computeNGramChunkLanguageModel();
//		List<Map<String, Double>> languageModelsOfPrecedingChunks = new ArrayList<Map<String, Double>>();
//		List<Map<String, Double>> languageModelsOfFollowingChunks = new ArrayList<Map<String, Double>>();
//		dm.smoothNGramChunkLanguageModel(languageModelsOfPrecedingChunks,
//				languageModelsOfFollowingChunks);
//	}
//
//	private void smoothNGramChunkLanguageModel(
//			List<Map<String, Double>> languageModelsOfPreceingChunks,
//			List<Map<String, Double>> languageModelsOfFollowingChunks) {
//
//	}
//}
