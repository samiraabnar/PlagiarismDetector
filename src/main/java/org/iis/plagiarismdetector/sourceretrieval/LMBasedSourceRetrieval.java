package org.iis.plagiarismdetector.sourceretrieval;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.queryparser.classic.ParseException;
import org.xml.sax.SAXException;
import org.iis.plagiarismdetector.settings.PARAMETER;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramExtractor;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramFeature;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramType;
import org.iis.plagiarismdetector.classifiers.PlagiarismTypeDetector;

import com.sun.tools.javac.util.Pair;

import org.iis.plagiarismdetector.core.JudgementFileFormat;
import org.iis.plagiarismdetector.core.SimilarityFunction;
import org.iis.plagiarismdetector.core.TextProcessor;
import org.iis.plagiarismdetector.core.lm.DirichletSmoothedLanguageModelbyLM;
import org.iis.plagiarismdetector.core.lm.JMSmoothedLanguageModel;
import org.iis.plagiarismdetector.core.lm.LanguageModel;
import org.iis.plagiarismdetector.core.lm.LuceneBasedLanguageModel;
import org.iis.plagiarismdetector.core.sourceretrieval.EvaluationSummary;
import org.iis.plagiarismdetector.core.sourceretrieval.QueryResult;
import org.iis.plagiarismdetector.core.sourceretrieval.SourceRetrievalConfig;
import org.iis.plagiarismdetector.core.sourceretrieval.irengine.LuceneIndex;
import org.iis.plagiarismdetector.core.textchunker.Chunker;
import org.iis.plagiarismdetector.core.textchunker.SimpleNonOverlappingChuker;

public class LMBasedSourceRetrieval extends SourceRetriever {

	private static final Integer EXPANSION_DEGREE = 1;
	private static final Integer MAX_HITS = 6;
	private static final Boolean REMOVE_STOPWORDS_QUERYTIME = null;
	private static final Boolean IFSTEM = null;
	private static final boolean STOPWORDS_IN_QUERY = false;
	
	private final Double MIU = 1000D;
	private String LANGUAGE = SourceRetrievalConfig.getLanguage();

	private LuceneIndex bgIndex;
	private LuceneIndex srcCollectionIndex;
	LuceneBasedLanguageModel bgLM;

	private String BG_COLLECTION = SourceRetrievalConfig.getSrcTrainCorpusPath();
	private String BG_INDEX_PATH = SourceRetrievalConfig.getSrcTrainIndexPath();

	private String SRC_COLLECTION = SourceRetrievalConfig.getSrcCorpusPath();
	private String SRC_INDEX_PATH = SourceRetrievalConfig.getSrcIndexPath();

	private Integer N = 1;
	private String SUSP_DIR = SourceRetrievalConfig.getSuspCorpusPath();
	
	// private boolean reIndexCollection = false;

	Map<Integer, LanguageModel> sourceLMz = new HashMap<Integer, LanguageModel>();
	//public PlagiarismTypeDetector pdt = new PlagiarismTypeDetector();

	public LMBasedSourceRetrieval() throws ClassNotFoundException, IOException {
	
		//pdt.initalizeClassifier();
		initialize(BG_INDEX_PATH);
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
		LMBasedSourceRetrieval srcRetriever = new LMBasedSourceRetrieval();

		try {
			/*
			 * srcRetriever.pairToTrecJudgeFileFormatConvertor(
			 * "evaluations/PAN2013/test_source_retrieval_judges",
			 * "evaluations/PAN2013/test_source_retrieval_judges");
			 */
			srcRetriever.run();
			/*
			 * srcRetriever.testIndexLM(); testChunkLanguageModel(srcRetriever);
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void testIndexLM() throws IOException,
			ParserConfigurationException, SAXException, SQLException {
		bgIndex = new LuceneIndex(BG_COLLECTION, LANGUAGE, BG_INDEX_PATH,
				SourceRetrievalConfig.get("IF_STEM"), SourceRetrievalConfig.get("IF_REMOVE_STOPWORDS"));
		bgIndex.loadIndex();
		bgLM = new LuceneBasedLanguageModel(bgIndex);
		srcCollectionIndex =  new LuceneIndex(SRC_COLLECTION, LANGUAGE, SRC_INDEX_PATH,
				SourceRetrievalConfig.get("IF_STEM"), SourceRetrievalConfig.get("IF_REMOVE_STOPWORDS"));
		srcCollectionIndex.loadIndex();
		List<Integer> documents = srcCollectionIndex.getDocumentIDz();
		Map<String, Double> candidSourceCountSeenWords = srcCollectionIndex
				.getDocumentTermFrequencies(documents.get(0));

		DirichletSmoothedLanguageModelbyLM candidSourceLanguageModel = new DirichletSmoothedLanguageModelbyLM(
				candidSourceCountSeenWords, MIU, bgLM);
		candidSourceLanguageModel.setup();
		System.out.println(candidSourceLanguageModel.getWordCount("andrew"));
		System.out.println(candidSourceLanguageModel
				.getMaximumLikelihoodProbability("andrew"));
		System.out.println(bgLM.getMaximumLikelihoodProbability("andrew"));

	}

	public static void testChunkLanguageModel(
			LMBasedSourceRetrieval srcRetriever) throws IOException, Exception {
		List<String> chunks = new ArrayList<String>();
		chunks.add("salam salam! hello! who are you? samira samira ! my name is samira... you are a black board. hi samira and black board!");
		List<Pair<String, Pair<Integer, Integer>>> tokens = NGramExtractor
				.getTokens(chunks.get(0), IFSTEM, REMOVE_STOPWORDS_QUERYTIME);
		List<Pair<String, Pair<Integer, Integer>>> nst = NGramExtractor
				.getNonStopWordTokens(tokens, "EN");
		List<Pair<Map<String, NGramFeature>, Integer>> lm = srcRetriever
				.extractChunksNGrams(1, chunks);
		List<Pair<Map<String, NGramFeature>, Integer>> f = new ArrayList<Pair<Map<String, NGramFeature>, Integer>>();
		LanguageModel mm = srcRetriever.computeChunkLanguageModel(f, lm.get(0),
				f, 0L, (long) chunks.get(0).length());

		System.out.println(mm.getMaximumLikelihoodProbability("samira"));
		System.out.println(4D / nst.size());
	}

	public void run() throws Exception {

		bgIndex = new LuceneIndex(BG_COLLECTION, LANGUAGE, BG_INDEX_PATH,
				SourceRetrievalConfig.get("IF_STEM"), SourceRetrievalConfig.get("IF_REMOVE_STOPWORDS"));
		
		bgIndex.loadIndex();

		bgLM = new LuceneBasedLanguageModel(bgIndex);
		srcCollectionIndex =
									  new LuceneIndex(SRC_COLLECTION, LANGUAGE,
									  SRC_INDEX_PATH, SourceRetrievalConfig.get("IF_STEM"),
									  SourceRetrievalConfig.get("IF_REMOVE_STOPWORDS"));
									 
		srcCollectionIndex.loadIndex();

		List<Integer> documents = srcCollectionIndex.getDocumentIDz();
		for (Integer docId : documents) {
			Map<String, Double> candidSourceCountSeenWords = srcCollectionIndex
					.getDocumentTermFrequencies(docId);

			DirichletSmoothedLanguageModelbyLM candidSourceLanguageModel = new DirichletSmoothedLanguageModelbyLM(
					candidSourceCountSeenWords, MIU, bgLM);
			candidSourceLanguageModel.setup();
			sourceLMz.put(docId, candidSourceLanguageModel);
		}

		File suspDir = new File(SUSP_DIR);
		for (File suspFile : suspDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".txt") && (!name.startsWith("."));
			}
		})) {
			String documentText = TextProcessor.getMatn(suspFile).toLowerCase();
			retrieveSources(documentText, suspFile.getName());
		}

		
	}

	private void retrieveSources(String documentText, String suspFileName)
			throws Exception {
		suspFileName = suspFileName.contains("/") ? suspFileName.substring(
				suspFileName.lastIndexOf("/") + 1).replace(".txt", "")
				: suspFileName.replace(".txt", "");
		Chunker chunker = new SimpleNonOverlappingChuker();
		List<String> chunks = chunker.chunk(documentText);
		List<LanguageModel> chunkLanguageModels = computeLanguageModelsForChunks(
				chunks, N);
		/* Set<Integer> docIdz = getSelectedSources(documentText, suspFileName); */

		Map<String, QueryResult> qrMap = new HashMap<String, QueryResult>();
		for (int i = 0; i < chunkLanguageModels.size(); i++) {
			List<Pair<Object, Double>> sourceDocuments = getRelatedSources(
					documentText, suspFileName, chunkLanguageModels.get(i),
					sourceLMz.keySet());

			for (int k = 0; k < sourceDocuments.size(); k++) {

				if (qrMap
						.containsKey(srcCollectionIndex
								.getRealDocumentId((Integer) sourceDocuments
										.get(k).fst))) {
					/*
					 * if (sourceDocuments.get(k).snd > qrMap.get(
					 * collectionIndex.getRealDocumentId(sourceDocuments
					 * .get(k).fst)).getScore()) {
					 */
					qrMap.put(
							srcCollectionIndex
									.getRealDocumentId((Integer) sourceDocuments
											.get(k).fst),
							new QueryResult(
									suspFileName,
									srcCollectionIndex
											.getRealDocumentId((Integer) sourceDocuments
													.get(k).fst),
									MAX_HITS
											- k
											+ qrMap.get(
													srcCollectionIndex
															.getRealDocumentId((Integer) sourceDocuments
																	.get(k).fst))
													.getScore(), documentText,
									k, (Integer) sourceDocuments.get(k).fst));
					// }
				} else {
					qrMap.put(
							srcCollectionIndex
									.getRealDocumentId((Integer) sourceDocuments
											.get(k).fst),
							new QueryResult(
									suspFileName,
									srcCollectionIndex
											.getRealDocumentId((Integer) sourceDocuments
													.get(k).fst), MAX_HITS
											- (double) k, documentText, k,
									(Integer) sourceDocuments.get(k).fst));
				}
			}

		}
		List<QueryResult> queryResult = new ArrayList<QueryResult>(
				qrMap.values());

		Collections.sort(queryResult, new Comparator<QueryResult>() {

			@Override
			public int compare(QueryResult o1, QueryResult o2) {
				return o2.getScore().compareTo(o1.getScore());
			}

		});

		System.out.println(suspFileName + ": " + queryResult.size());
		reportInTREC(queryResult, suspFileName);
		getFinalResults().put(suspFileName, queryResult);
	}

	private int getIndexOfMaxDiffOfQueryResults(List<QueryResult> queryResult) {
		// TODO Auto-generated method stub
		Integer indx = 0;
		Double diff = 0D;
		for (int i = 0; i < (queryResult.size() - 1); i++) {
			Double tmpDiff = (queryResult.get(i).getScore() - queryResult.get(
					i + 1).getScore())
					/ queryResult.get(i + 1).getScore();
			if (tmpDiff > diff) {
				diff = tmpDiff;
				indx = i;
			}
		}

		return indx;
	}

	private List<Pair<Object, Double>> getRelatedSources(String suspFileText,
			String suspFileName, LanguageModel languageModel,
			Set<Integer> srcCandidz) throws IOException, ParseException {

		List<Pair<Object, Double>> similarityScores = new ArrayList<Pair<Object, Double>>();

		/*
		 * Set<Integer> docIdz = new HashSet<Integer>(); for (String seenTerm :
		 * languageModel.getWordsCount().keySet()) {
		 * docIdz.addAll(collectionIndex.getDocumentsContainingTerm(seenTerm));
		 * }
		 */

		for (Integer docId : srcCandidz/* sourceLMz.keySet() */) {
			Double similarityScore = sourceLMz.get(docId).compareTo(
					languageModel);
			similarityScores.add(new Pair<Object, Double>(docId,
					similarityScore));
		}

		Collections.sort(similarityScores,
				new Comparator<Pair<Object, Double>>() {

					@Override
					public int compare(Pair<Object, Double> o1,
							Pair<Object, Double> o2) {
						return o2.snd.compareTo(o1.snd);
					}
				});

		if (similarityScores.size() == 0)
			return similarityScores;
		int index = getIndexOfMaxDiff(similarityScores);
		if (index > 5)
			index = 0;

		return similarityScores.subList(0, index);

		/*
		 * return similarityScores.subList(0,
		 * Math.min(sourceRetrievalConfig.topK, similarityScores.size()));
		 */
	}

	private Set<Integer> getSelectedSources(String suspFileText,
			String suspFileName) throws IOException, ParseException {
		// TODO Auto-generated method stub
		List<QueryResult> results = new ArrayList<QueryResult>();

		results = submitQuery(suspFileText, suspFileName,100);

		Set<Integer> didz = new HashSet<Integer>();

		for (int i = 0; i < results.size(); i++) {
			didz.add(results.get(i).getIndexedDocId());
		}

		return didz;
	}

	public Integer getIndexOfMaxDiff(List<Pair<Object, Double>> list) {
		Integer indx1 = 0, indx2 = 0, indx3 = 0, indx4 = 0;
		Double diff1 = 0D, diff2 = 0D, diff3 = 0D, diff4 = 0D;
		for (int i = 0; i < (list.size() - 1); i++) {
			Double tmpDiff1 = (list.get(i).snd - list.get(i + 1).snd)
					/ list.get(i).snd;
			Double tmpDiff2 = (list.get(i).snd) / list.get(i + 1).snd;
			Double tmpDiff3 = (list.get(i).snd - list.get(i + 1).snd)
					/ list.get(i + 1).snd;
			Double tmpDiff4 = (list.get(i).snd - list.get(i + 1).snd);

			if (tmpDiff1 > diff1) {
				diff1 = tmpDiff1;
				indx1 = i;
			}
			if (tmpDiff2 > diff2) {
				diff2 = tmpDiff2;
				indx2 = i;
			}
			if (tmpDiff3 > diff3) {
				diff3 = tmpDiff3;
				indx3 = i;
			}
			if (tmpDiff4 > diff4) {
				diff4 = tmpDiff4;
				indx4 = i;
			}
		}

		return Math.min(Math.min(indx1, indx2), Math.min(indx3, indx4)) + 1;
	}

	public List<LanguageModel> computeLanguageModelsForChunks(
			List<String> chunks, Integer N) throws Exception {
		List<LanguageModel> chunkLanguageModels = new ArrayList<LanguageModel>();
		List<Pair<Map<String, NGramFeature>, Integer>> chunkNGrams = extractChunksNGrams(
				N, chunks);
		Long chunkHeadOffset = 0L;
		for (int i = 0; i < chunks.size(); i++) {
			chunkLanguageModels.add(computeChunkLanguageModel(
					chunkNGrams.subList(0, i), chunkNGrams.get(i),
					chunkNGrams.subList(i + 1, chunks.size()), chunkHeadOffset,
					chunkHeadOffset + chunkNGrams.get(i).snd));
			chunkHeadOffset += chunkNGrams.get(i).snd;
		}

		return chunkLanguageModels;
	}

	private LanguageModel computeChunkLanguageModel(
			List<Pair<Map<String, NGramFeature>, Integer>> precedingChunks,
			Pair<Map<String, NGramFeature>, Integer> chunk,
			List<Pair<Map<String, NGramFeature>, Integer>> followingChunks,
			Long chunkHeadOffset, Long chunkEndOffset) {
		LanguageModel chunkLM = new JMSmoothedLanguageModel(0.7);
		for (String ngram : chunk.fst.keySet()) {
			chunkLM.getWordsCount().put(ngram,
					chunk.fst.get(ngram).getSumOfTransparencies());
			chunkLM.getTotalWords().add(ngram);
		}

		for (Pair<Map<String, NGramFeature>, Integer> pchunk : precedingChunks
				.subList(Math.max(0, precedingChunks.size() - 1),
						precedingChunks.size())) {
			for (String ngram : pchunk.fst.keySet()) {
				if (!chunkLM.getBgWordsCount().containsKey(ngram))
					chunkLM.getBgWordsCount().put(ngram, 0D);
				chunkLM.getTotalWords().add(ngram);
				chunkLM.getBgWordsCount().put(
						ngram,
						chunkLM.getBgWordsCount().get(ngram)
								+ pchunk.fst.get(ngram)
										.getSumOfFadedOccuranceScore(
												chunkHeadOffset));
			}
		}

		for (Pair<Map<String, NGramFeature>, Integer> fchunk : followingChunks
				.subList(0, Math.min(1, followingChunks.size()))) {
			for (String ngram : fchunk.fst.keySet()) {
				if (!chunkLM.getBgWordsCount().containsKey(ngram))
					chunkLM.getBgWordsCount().put(ngram, 0D);
				chunkLM.getTotalWords().add(ngram);
				chunkLM.getBgWordsCount().put(
						ngram,
						chunkLM.getBgWordsCount().get(ngram)
								+ fchunk.fst.get(ngram)
										.getSumOfFadedOccuranceScore(
												chunkEndOffset));
			}
		}

		chunkLM.setup();

		return chunkLM;

	}

	public List<Pair<Map<String, NGramFeature>, Integer>> extractChunksNGrams(
			Integer N, List<String> chunks) throws Exception {

		List<Pair<Map<String, NGramFeature>, Integer>> chunksNGrams = new ArrayList<Pair<Map<String, NGramFeature>, Integer>>();

		Long chunkId = 0L;
		for (String chunkString : chunks) {

			List<Pair<String, Pair<Integer, Integer>>> tokens = NGramExtractor
					.getTokens(chunkString, IFSTEM, REMOVE_STOPWORDS_QUERYTIME);
			List<Pair<String, Pair<Integer, Integer>>> stopwordlessTokens = NGramExtractor
					.getNonStopWordTokens(tokens, LANGUAGE);
			Map<String, NGramFeature> chunkNGrams = NGramExtractor
					.extractSegmentNonStopWordNGrams(
							N,
							chunkString,
							chunkId,
							new Pair<Integer, Integer>(0, chunkString.length()),
							NGramType.Word, STOPWORDS_IN_QUERY ? tokens
									: stopwordlessTokens, EXPANSION_DEGREE);

			chunksNGrams.add(new Pair<Map<String, NGramFeature>, Integer>(
					chunkNGrams, tokens.size()));
			chunkId++;
		}

		return chunksNGrams;
	}

	@Override
	protected List<QueryResult> mergeDifferentQueriesResults(
			Map<String, List<QueryResult>> results) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<Pair<String, String>> extractQueries(String suspFileName,
			String suspFileText) throws Exception {

		suspFileName = suspFileName.contains("/") ? suspFileName.substring(
				suspFileName.lastIndexOf("/") + 1).replace(".txt", "")
				: suspFileName.replace(".txt", "");

		Chunker chunker = new SimpleNonOverlappingChuker();

		List<String> chunks = chunker.chunk(suspFileText);
		List<LanguageModel> chunkLanguageModels = computeLanguageModelsForChunks(
				chunks, N);
		List<Pair<String, String>> queries = new ArrayList<Pair<String, String>>();
		String queryId = suspFileName;

		for (int i = 0; i < chunkLanguageModels.size(); i++) {
			// String query = chunkLanguageModels.generateQuery(10);
			// queries.add(new Pair<String, String>(query, queryId + "_" + i));
		}

		return queries;
	}

}
