package org.iis.plagiarismdetector.core;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.BytesRef;
import org.iis.plagiarismdetector.core.lucene.IndexInfo;
import org.iis.plagiarismdetector.core.sourceretrieval.SourceRetrievalConfig;
import org.iis.plagiarismdetector.core.sourceretrieval.irengine.Indexer;
import org.iis.plagiarismdetector.core.wordnet.WordNetUtil;
import org.iis.plagiarismdetector.settings.TextAlignmentDatasetSettings;
import org.tartarus.snowball.ext.EnglishStemmer;

import com.sun.tools.javac.util.Pair;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class TextProcessor {

	public static String[] englishPunctuations = { "~", "!", "@", "#", "$",
			"%", "^", "&", "*", "(", ")", "{", "}", "|", "\\", "+", "-", "_",
			"=", "/", "?", ">", "<", "\'", "\"", ":", ";", "`", ",", "." };
	public static String[] englishPunctuationsReplacements = { " ", " ", " ",
			" ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ",
			" ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ",
			" " };
	public static String[] persianPunctuations = { "÷", "‍", "!", "٬", "٫",
			"٪", "×", "،", "*", ")", "(", "-", "ـ", "=", "+", "}", "{", "\\",
			"|", "{", "}", "[", "]", "؛", ":", "؟", "/", ".", ">", "<", "«",
			"»", "ٓ", "ٰ", "‌", "ٔ", "ء", "ْ", "ٌ", "ٍ", "ً", "ُ", "ِ", "َ",
			"ّ" };

	public static String[] persianPunctuationsReplacements = { " ", " ", " ",
			" ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ",
			" ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ",
			" ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ", " ",
			" ", " ", " " };
	private static final String eol = System.getProperty("line.separator");
	private static WordNetUtil wordnetUtil;

	private static Set<String> stopwords;
//	private static Connector SEI = new Connector();
	static {
		fillStopWords();
		wordnetUtil = new WordNetUtil();
	}

	public static String stemEnglishWord(String word) {
		if ((word == null) || (word.length() == 0))
			return word;
		try {
			EnglishStemmer englishStemmer = new EnglishStemmer();
			englishStemmer.setCurrent(word.toLowerCase());
			englishStemmer.stem();
			return englishStemmer.getCurrent();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(word);
			return word;
		}
	}

	public static List<Pair<String, Pair<Integer, Integer>>> luceneTokenizer(
			String text, Boolean ifstem, Boolean ifremovestopwords)
			throws IOException {
		List<Pair<String, Pair<Integer, Integer>>> tokensList = new ArrayList<Pair<String, Pair<Integer, Integer>>>();

		Analyzer analyzer = Indexer
				.MyEnglishAnalizer(ifstem, ifremovestopwords);
		try {
			TokenStream stream = analyzer.tokenStream("TEXT", new StringReader(
					text));
			stream.reset();
			while (stream.incrementToken()) {
				/*
				 * System.out.println(stream.getAttribute(CharTermAttribute.class
				 * ) .toString());
				 * System.out.println(stream.getAttribute(OffsetAttribute.class)
				 * .startOffset() + " " +
				 * stream.getAttribute(OffsetAttribute.class) .endOffset());
				 */
				tokensList.add(new Pair<String, Pair<Integer, Integer>>(stream
						.getAttribute(CharTermAttribute.class).toString(),
						new Pair<Integer, Integer>(stream.getAttribute(
								OffsetAttribute.class).startOffset(),
								tokensList.size())));

			}
		} catch (IOException e) {
			// not thrown b/c we're using a string reader...
			throw new RuntimeException(e);
		}

		return tokensList;
	}

	public static List<Pair<String, Integer>> tokenize(String text,
			TokenType tokensType, Integer startOffset) {
		// TODO Auto-generated method stub
		text = text.replaceAll("\n", "\\s");
		switch (tokensType) {
		case AlphaNumerical:
			text = removeAllKindsOfPunctuations(text);
			break;
		default:
		}

		Integer index = 0;
		List<Pair<String, Integer>> tokensList = new ArrayList<Pair<String, Integer>>();

		while ((index < (text.length() - 1)) && (text.charAt(index) == ' '))
			index++;

		while ((index >= 0) && (index < text.length())) {
			Integer sndIndex = text.indexOf(" ", index);
			String newToken = text.substring(index, sndIndex > 0 ? sndIndex
					: text.length());

			if (newToken.replaceAll("\\s+", "").length() > 0) {
				tokensList.add(new Pair<String, Integer>(newToken, startOffset
						+ index));
			}
			if (sndIndex < 0)
				break;
			index = sndIndex + 1;
		}

		return tokensList;
	}

	public static String removeAllKindsOfPunctuations(String text) {

		text = StringUtils.replaceEach(text, englishPunctuations,
				englishPunctuationsReplacements);

		text = StringUtils.replaceEach(text, persianPunctuations,
				persianPunctuationsReplacements);
		return text;

	}

	public static String getMatn(File f) {
		String matn = "";
		try {

			FileInputStream fstream = new FileInputStream(f);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in,
					"UTF8"));

			String line;
			while ((line = br.readLine()) != null) {
				matn = matn + line + eol;
			}

			br.close();
			in.close();
			fstream.close();
		} catch (Exception e) {
			System.err.println(e);
		}
		return matn;
	}

	public static Set<String> getStopWords(String language) {
		// TODO Auto-generated method stub
		return stopwords;
	}

	public static void fillStopWords() {
		stopwords = new HashSet<String>();
		try {

			FileInputStream fstream = new FileInputStream(
					"../LetoR/src/main/resources/EN_stopword.txt");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in,
					"UTF8"));

			String line;
			while ((line = br.readLine()) != null) {
				stopwords.add(line.trim());
			}

			br.close();
			in.close();
			fstream.close();
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	public static String[] getSentences(String matn) {

		List<String> Allsentences = new ArrayList<String>();

		Pattern re = Pattern
				.compile(
						"# Match a sentence ending in punctuation or EOS.\n"
								+ "[^.!?.!؟\\s]    # First char is non-punct, non-ws\n"
								+ "[^.!?.!؟]*      # Greedily consume up to punctuation.\n"
								+ "(?:          # Group for unrolling the loop.\n"
								+ "  [.!?.!؟]      # (special) inner punctuation ok if\n"
								+ "  (?!['\"]?\\s|$)  # not followed by ws or EOS.\n"
								+ "  [^.!?.!؟]*    # Greedily consume up to punctuation.\n"
								+ ")*           # Zero or more (special normal*)\n"
								+ "[.!?.!؟]?       # Optional ending punctuation.\n"
								+ "['\"]?       # Optional closing quote.\n"
								+ "(?=\\s|$)", Pattern.MULTILINE
								| Pattern.COMMENTS);
		Matcher reMatcher = re.matcher(matn);
		while (reMatcher.find()) {
			String group = reMatcher.group();
			Integer startPoint = 0;
			Integer eolIndex = group.indexOf(eol, startPoint);
			while (eolIndex < group.length()) {
				try {
					if (eolIndex < 0) {
						Allsentences.add(group.substring(startPoint));
						break;
					} else {
						Allsentences.add(group.substring(startPoint,
								eolIndex + 1));
						startPoint = eolIndex + 1;
						eolIndex = group.indexOf(eol, startPoint);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return Allsentences.toArray(new String[Allsentences.size()]);
	}

//	public static Double computePMI(String thisToken, String srcToken,
//			Boolean normalized) throws Exception {
//
//		Connector SEI = new Connector();
//		ConfigLoader.getInstance().load("myConfig.xml");
//		BigDecimal coOccuranceHit = SEI
//				.associationHitCount(thisToken, srcToken);
//		BigDecimal thisHits = SEI.hitCount(thisToken);
//		BigDecimal srcHits = SEI.hitCount(srcToken);
//		if (coOccuranceHit.doubleValue() == -1 || thisHits.doubleValue() == -1
//				|| srcHits.doubleValue() == -1)
//			return -1D;
//
//		double fraction = Math.log((coOccuranceHit.doubleValue() / thisHits
//				.multiply(srcHits).doubleValue()));
//		if (normalized) {
//			fraction = fraction / Math.log(coOccuranceHit.doubleValue());
//		}
//
//		return (fraction / Math.log(2.0));
//	}
//
//	public static double computeAndStoreNormalizedPMI(String thisToken,
//			String srcToken) throws Exception {
//
//		if (PMIDataset.containsNormalizedPMI(thisToken, srcToken, ConfigLoader
//				.getInstance().getDistance())) {
//			return PMIDataset.getNormalizedPMI(thisToken, srcToken,
//					ConfigLoader.getInstance().getDistance());
//		}
//
//		BigDecimal coOccuranceHit = SEI
//				.associationHitCount(thisToken, srcToken);
//		BigDecimal thisHits = new BigDecimal(0);
//		BigDecimal srcHits = new BigDecimal(0);
//		// BigDecimal totalDocumentsCount = PMIDataset.getHits("a");
//
//		if (PMIDataset.contains(thisToken)) {
//			thisHits = PMIDataset.getHits(thisToken);
//		} else {
//			thisHits = SEI.associationHitCount(thisToken, thisToken, 0);
//			PMIDataset.saveHits(thisToken, thisHits);
//		}
//
//		if (PMIDataset.contains(srcToken)) {
//			srcHits = PMIDataset.getHits(srcToken);
//		} else {
//			srcHits = SEI.associationHitCount(srcToken, srcToken, 0);
//			PMIDataset.saveHits(srcToken, srcHits);
//		}
//
//		/*
//		 * double px = thisHits.divide(totalDocumentsCount, 10,
//		 * BigDecimal.ROUND_DOWN).doubleValue(); double py =
//		 * srcHits.divide(totalDocumentsCount, 10,
//		 * BigDecimal.ROUND_DOWN).doubleValue(); double pxy =
//		 * coOccuranceHit.divide(totalDocumentsCount, 10,
//		 * BigDecimal.ROUND_DOWN).doubleValue();
//		 */
//		double pmi = coOccuranceHit.divide(thisHits.multiply(srcHits), 50,
//				BigDecimal.ROUND_HALF_EVEN).doubleValue();
//
//		double normalizedPMI = pmi
//				/ Math.max(
//						BigDecimal.ONE.divide(srcHits, 50,
//								BigDecimal.ROUND_HALF_EVEN).doubleValue(),
//						BigDecimal.ONE.divide(thisHits, 50,
//								BigDecimal.ROUND_HALF_EVEN).doubleValue());
//
//		PMIDataset.saveNormalizedPMI(thisToken, srcToken, ConfigLoader
//				.getInstance().getDistance(), normalizedPMI);
//		PMIDataset.savePMI(thisToken, srcToken, ConfigLoader.getInstance()
//				.getDistance(), pmi);
//
//		return normalizedPMI;
//	}
//
//	public static void computeAndStoreAllPMIs(String folderPath)
//			throws Exception {
//		File path = new File(folderPath);
//
//		for (String p : path.list(new FilenameFilter() {
//
//			@Override
//			public boolean accept(File dir, String name) {
//				return !name.startsWith(".");
//			}
//		})) {
//
//			String fileString = TextProcessor.getMatn(new File(folderPath + "/"
//					+ p));
//			computeAndStorePMIs(fileString);
//		}
//
//	}
//
//	private static void computeAndStorePMIs(String string) throws Exception {
//		List<Pair<String, Integer>> tokens = TextProcessor.tokenize(string,
//				TokenType.AlphaNumerical, 0);
//
//		List<Pair<String, Integer>> stopwordlessTokens = new ArrayList<Pair<String, Integer>>();
//		for (Pair<String, Integer> token : tokens) {
//			if (!TextProcessor.getStopWords(
//					TextAlignmentDatasetSettings.LANGUAGE).contains(token.fst)) {
//				stopwordlessTokens.add(token);
//			}
//		}
//
//		for (int i = 0; i < (stopwordlessTokens.size() - 1); i++) {
//			computeAndStoreNormalizedPMI(stopwordlessTokens.get(i).fst,
//					stopwordlessTokens.get(i + 1).fst);
//		}
//	}

	public static void main(String[] args) throws Exception {
		/*
		 * ConfigLoader.getInstance().load("myConfig.xml"); double pmi; try {
		 * pmi = computeAndStoreNormalizedPMI("sunflower", "sunflower");
		 * System.out.println("pmi: " + pmi);
		 * 
		 * computeAndStoreAllPMIs(TextAlignmentDatasetSettings.SUSP_FILES_DIR);
		 * computeAndStoreAllPMIs
		 * (TextAlignmentDatasetSettings.SOURCE_FILES_DIR);
		 * 
		 * } catch (SQLException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */
		// luceneTokenizer("Hi My Name is Samira!");
		Integer value = getFirstIndex(
				"Salaam! man samira hastam :p! chetori mosi ... mosiii ??? ha ha ha",
				"[!.?:]+");
		System.out.println(value);
	}

//	public static Double getTokenIDF(String word, String lANGUAGE)
//			throws Exception {
//		BigDecimal hits = new BigDecimal(0);
//		if (PMIDataset.contains(word)) {
//			hits = PMIDataset.getHits(word);
//		} else {
//			hits = SEI.associationHitCount(word, word, 0);
//			PMIDataset.saveHits(word, hits);
//		}
//
//		if (hits.equals(BigDecimal.ZERO))
//			return 2.0;
//
//		return BigDecimal.ONE.divide(PMIDataset.getHits(word), 50,
//				BigDecimal.ROUND_HALF_EVEN).doubleValue();
//	}

	public static Double computeWordExactSimilarity(String word1, String word2) {

		if (word1.equals(word2))
			return 1D;
		return 0D;
	}

	public static Integer getLastIndex(String string, String toFind) {
		// Need to add an extra character to message because to ensure
		// split works if toFind is right at the end of the message.
		String separated[] = string.split(toFind);
		if (separated == null || separated.length == 0 || separated.length == 1) {
			return -1;
		}
		return string.length() - separated[separated.length - 1].length();
	}

	public static Integer getFirstIndex(String string, String toFind) {
		// Need to add an extra character to message because to ensure
		// split works if toFind is right at the end of the message.
		String separated[] = string.split(toFind);
		if (separated == null || separated.length == 0 || separated.length == 1) {
			return -1;
		}
		return separated[0].length();
	}

	public static String convertListToString(
			List<Pair<String, Pair<Integer, Integer>>> list) {
		String value = "";
		for (Pair<String, Pair<Integer, Integer>> item : list)
			value += " " + item.fst;
		return value;
	}

        
	public static Double getIDFFromBackGroundCollection(String word, IndexInfo indexInfo)
			throws IOException {

		return indexInfo.getDF("TEXT", new BytesRef(word)).doubleValue();
	}

	public static Map<String, Map<String, Double>> getExpandigTerms(
			String chunkString,IndexInfo bgIndexInfo) {
		Map<String, Map<String, Double>> expandingTerms = new HashMap<String, Map<String, Double>>();
		List<List<TaggedWord>> sentences = tagText(chunkString);

		for (List<TaggedWord> sentence : sentences) {
			for (TaggedWord word : sentence) {
				if (word.tag().toLowerCase().startsWith("n")) {
					List<Pair<String, Double>> senseFreqencies = wordnetUtil
							.getAllWordSenses(word.word(), word.tag(),
									bgIndexInfo);
					for (Pair<String, Double> senseFreq : senseFreqencies) {
						if (!expandingTerms.containsKey(senseFreq.fst)) {
							expandingTerms.put(senseFreq.fst,
									new HashMap<String, Double>());

							expandingTerms.get(senseFreq.fst).put(
									"SENSE_FREQUENCY", senseFreq.snd);
							expandingTerms.get(senseFreq.fst).put(
									"SELECTION_COUNT", 0D);
						}
						expandingTerms.get(senseFreq.fst).put(
								"SELECTION_COUNT",
								expandingTerms.get(senseFreq.fst).get(
										"SELECTION_COUNT") + 1);
						expandingTerms.get(senseFreq.fst).put(
								"SENSE_FREQUENCY",
								Math.max(
										senseFreq.snd,
										expandingTerms.get(senseFreq.fst).get(
												"SENSE_FREQUENCY")));
					}
				}
			}
		}

		return expandingTerms;
	}

	static MaxentTagger tagger = new MaxentTagger(
			"../LetoR/src/main/resources/stanford-postagger/models/english-left3words-distsim.tagger");

	public static List<List<TaggedWord>> tagText(String chunkString) {
		List<List<TaggedWord>> tsentences = new ArrayList<List<TaggedWord>>();

		List<List<HasWord>> sentences = MaxentTagger
				.tokenizeText(new StringReader(chunkString));
		for (List<HasWord> sentence : sentences) {
			List<TaggedWord> tSentence = tagger.tagSentence(sentence);
			tsentences.add(tSentence);
		}

		return tsentences;
	}
        
        

}
