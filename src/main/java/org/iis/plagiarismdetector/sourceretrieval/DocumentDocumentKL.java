package org.iis.plagiarismdetector.sourceretrieval;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;
import org.iis.plagiarismdetector.classifiers.PlagiarismTypeDetector;
import org.iis.plagiarismdetector.core.JudgementFileFormat;
import org.iis.plagiarismdetector.core.SimilarityFunction;
import org.iis.plagiarismdetector.core.TextProcessor;
import org.iis.plagiarismdetector.core.lm.DirichletSmoothedLanguageModel;
import org.iis.plagiarismdetector.core.lm.DirichletSmoothedLanguageModelbyLM;
import org.iis.plagiarismdetector.core.lm.JMSmoothedLanguageModelByLM;
import org.iis.plagiarismdetector.core.lm.LanguageModel;
import org.iis.plagiarismdetector.core.lm.LuceneBasedLanguageModel;
import org.iis.plagiarismdetector.core.lm.NotSmoothedLanguageModel;
import org.iis.plagiarismdetector.core.sourceretrieval.EvaluationSummary;
import org.iis.plagiarismdetector.core.sourceretrieval.QueryResult;
import org.iis.plagiarismdetector.core.sourceretrieval.SourceRetrievalConfig;
import org.iis.plagiarismdetector.core.sourceretrieval.irengine.LuceneIndex;
import org.iis.plagiarismdetector.settings.PARAMETER;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramExtractor;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramFeature;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramType;

import com.sun.tools.javac.util.Pair;

public class DocumentDocumentKL extends DocumentChunksAsQuery {

	private Double LAMBDA = 0.7D;
	LuceneBasedLanguageModel bgLM;
	private String BG_COLLECTION = SourceRetrievalConfig.getSrcTrainCorpusPath();
	private String BG_INDEX_PATH = SourceRetrievalConfig.getSrcTrainIndexPath();

	private String SRC_COLLECTION = SourceRetrievalConfig.getSrcCorpusPath();
	private String SRC_INDEX_PATH = SourceRetrievalConfig.getSrcIndexPath();
	
	private String LANGUAGE = "EN";
	private Boolean IF_STEM = false;
	private Boolean IF_REMOVE_STOPWORDS = true;
	private Integer N = 1;
	private LuceneIndex srcCollectionIndex;
	private LuceneIndex bgIndex;

	private String SUSP_DIR = SourceRetrievalConfig.getSuspCorpusPath();

	Map<Integer, LanguageModel> sourceLMz = new HashMap<Integer, LanguageModel>();
	public PlagiarismTypeDetector pdt = new PlagiarismTypeDetector();

	public DocumentDocumentKL() throws ClassNotFoundException, IOException {
		
		initialize(SRC_INDEX_PATH);
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) {
		try {
			DocumentDocumentKL srcRetriever;

			srcRetriever = new DocumentDocumentKL();
			srcRetriever.run();

		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			/*
			 * srcRetriever.pairToTrecJudgeFileFormatConvertor(
			 * "evaluations/PAN2013/test_source_retrieval_judges",
			 * "evaluations/PAN2013/test_source_retrieval_judges");
			 */
			/*
			 * srcRetriever.testIndexLM(); testChunkLanguageModel(srcRetriever);
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected List<Pair<String, String>> extractQueries(String suspFileName,
			String suspFileText) {
		List<Pair<String, String>> queries = new ArrayList<Pair<String, String>>();
		suspFileText = TextProcessor.removeAllKindsOfPunctuations(suspFileText);
		if (suspFileText.length() == 0) {
			System.out.println("Can not Read the File:" + suspFileName);
		}
		String queryId = suspFileName;
		queries.add(new Pair<String, String>(suspFileText.substring(0,
				suspFileText.length() - 1), queryId));

		
		return queries;
	}

	public void run() throws Exception {

		bgIndex = new LuceneIndex(BG_COLLECTION, LANGUAGE, BG_INDEX_PATH,
				IF_STEM, IF_REMOVE_STOPWORDS);
		
		bgIndex.loadIndex();

		bgLM = new LuceneBasedLanguageModel(bgIndex);
		srcCollectionIndex = 
									  new LuceneIndex(SRC_COLLECTION, SourceRetrievalConfig.getLanguage(),
									  SRC_INDEX_PATH, SourceRetrievalConfig.get("IF_STEM"),
									 SourceRetrievalConfig.get("IF_REMOVE_STOPWORDS"));
									 
		/*
		 * if (reIndexCollection) { collectionIndex.index(); }
		 * collectionIndex.loadIndex();
		 */
		srcCollectionIndex.loadIndex();
		List<Integer> documents = srcCollectionIndex.getDocumentIDz();
		for (Integer docId : documents) {
			Map<String, Double> candidSourceCountSeenWords = srcCollectionIndex
					.getDocumentTermFrequencies(docId);

			LanguageModel candidSourceLanguageModel = new DirichletSmoothedLanguageModelbyLM(
					candidSourceCountSeenWords, LAMBDA, bgLM);
			candidSourceLanguageModel.getBgWordsCount().putAll(
					candidSourceCountSeenWords);

			/*
			 * DirichletSmoothedLanguageModelbyLM( candidSourceCountSeenWords,
			 * MIU, bgLM);
			 */
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
		List<String> chunks = new ArrayList<String>();
		chunks.add(documentText);
		List<LanguageModel> chunkLanguageModels = computeLanguageModelsForChunks(
				chunks, N);

		Map<String, QueryResult> qrMap = new HashMap<String, QueryResult>();
		for (int i = 0; i < chunkLanguageModels.size(); i++) {
			List<Pair<Integer, Double>> sourceDocuments = getRelatedSources(
					documentText, suspFileName, chunkLanguageModels.get(i));

			for (int k = 0; k < sourceDocuments.size(); k++) {

				if (qrMap.containsKey(srcCollectionIndex
						.getRealDocumentId(sourceDocuments.get(k).fst))) {
					/*
					 * if (sourceDocuments.get(k).snd > qrMap.get(
					 * collectionIndex.getRealDocumentId(sourceDocuments
					 * .get(k).fst)).getScore()) {
					 */
					qrMap.put(
							srcCollectionIndex.getRealDocumentId(sourceDocuments
									.get(k).fst),
							new QueryResult(
									suspFileName,
									srcCollectionIndex
											.getRealDocumentId(sourceDocuments
													.get(k).fst),
									(sourceDocuments.size() - k)
											+ qrMap.get(
													srcCollectionIndex
															.getRealDocumentId(sourceDocuments
																	.get(k).fst))
													.getScore(), documentText,
									k, sourceDocuments.get(k).fst));
					// }
				} else {
					qrMap.put(
							srcCollectionIndex.getRealDocumentId(sourceDocuments
									.get(k).fst),
							new QueryResult(suspFileName,
									srcCollectionIndex
											.getRealDocumentId(sourceDocuments
													.get(k).fst),
									sourceDocuments.size() - (double) k,
									documentText, k, sourceDocuments.get(k).fst));
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

		queryResult = queryResult.subList(0, SourceRetrievalConfig.getK());

		System.out.println(suspFileName + ": " + queryResult.size());
		reportInTREC(queryResult, suspFileName);
		getFinalResults().put(suspFileName, queryResult);
	}

	private List<Pair<Integer, Double>> getRelatedSources(String suspFileText,
			String suspFileName, LanguageModel languageModel)
			throws IOException, ParseException {

		List<Pair<Integer, Double>> similarityScores = new ArrayList<Pair<Integer, Double>>();

		// Set<Integer> docIdz = getSelectedSources(suspFileText, suspFileName);

		/*
		 * Set<Integer> docIdz = new HashSet<Integer>(); for (String seenTerm :
		 * languageModel.getWordsCount().keySet()) {
		 * docIdz.addAll(collectionIndex.getDocumentsContainingTerm(seenTerm));
		 * }
		 */

		for (Integer docId : sourceLMz.keySet()) {
			Double similarityScore = sourceLMz.get(docId)
					.computeKLSimilarityScore(languageModel);
			similarityScores.add(new Pair<Integer, Double>(docId,
					similarityScore));
		}

		Collections.sort(similarityScores,
				new Comparator<Pair<Integer, Double>>() {

					@Override
					public int compare(Pair<Integer, Double> o1,
							Pair<Integer, Double> o2) {
						return o2.snd.compareTo(o1.snd);
					}
				});

		return similarityScores;

		
	}

	
	
	public List<LanguageModel> computeLanguageModelsForChunks(
			List<String> chunks, Integer N) throws Exception {
		List<LanguageModel> chunkLanguageModels = new ArrayList<LanguageModel>();
		List<Map<String, NGramFeature>> chunkNGrams = extractChunksNGrams(N,
				chunks);
		Long chunkHeadOffset = 0L;
		for (int i = 0; i < chunks.size(); i++) {
			chunkLanguageModels.add(computeChunkLanguageModel(
					chunkNGrams.subList(0, i), chunkNGrams.get(i),
					chunkNGrams.subList(i + 1, chunks.size()), chunkHeadOffset,
					chunkHeadOffset + chunks.get(i).length()));
			chunkHeadOffset += chunks.get(i).length();
		}

		return chunkLanguageModels;
	}

	private LanguageModel computeChunkLanguageModel(
			List<Map<String, NGramFeature>> precedingChunks,
			Map<String, NGramFeature> chunk,
			List<Map<String, NGramFeature>> followingChunks,
			Long chunkHeadOffset, Long chunkEndOffset) {
		LanguageModel chunkLM = new NotSmoothedLanguageModel();
		for (String ngram : chunk.keySet()) {
			chunkLM.getWordsCount().put(ngram,
					chunk.get(ngram).getSumOfTransparencies());
			chunkLM.getTotalWords().add(ngram);
			
			if (!chunkLM.getBgWordsCount().containsKey(ngram))
				chunkLM.getBgWordsCount().put(ngram, 0D);
			chunkLM.getBgWordsCount().put(
					ngram,
					chunkLM.getBgWordsCount().get(ngram)
							+ chunk.get(ngram).getSumOfTransparencies());
		}

		for (Map<String, NGramFeature> pchunk : precedingChunks) {
			for (String ngram : pchunk.keySet()) {
				if (!chunkLM.getBgWordsCount().containsKey(ngram))
					chunkLM.getBgWordsCount().put(ngram, 0D);
				chunkLM.getTotalWords().add(ngram);
				chunkLM.getBgWordsCount().put(
						ngram,
						chunkLM.getBgWordsCount().get(ngram)
								+ pchunk.get(ngram)
										.getSumOfFadedOccuranceScore(
												chunkHeadOffset));
			}
		}

		for (Map<String, NGramFeature> fchunk : followingChunks) {
			for (String ngram : fchunk.keySet()) {
				if (!chunkLM.getBgWordsCount().containsKey(ngram))
					chunkLM.getBgWordsCount().put(ngram, 0D);
				chunkLM.getTotalWords().add(ngram);
				chunkLM.getBgWordsCount().put(
						ngram,
						chunkLM.getBgWordsCount().get(ngram)
								+ fchunk.get(ngram)
										.getSumOfFadedOccuranceScore(
												chunkEndOffset));
			}
		}

		chunkLM.setup();

		return chunkLM;

	}

	public List<Map<String, NGramFeature>> extractChunksNGrams(Integer N,
			List<String> chunks) throws Exception {

		List<Map<String, NGramFeature>> chunksNGrams = new ArrayList<Map<String, NGramFeature>>();

		Long chunkId = 0L;
		for (String chunkString : chunks) {

			List<Pair<String, Pair<Integer, Integer>>> tokens = NGramExtractor
					.getTokens(chunkString, false, false);
			List<Pair<String, Pair<Integer, Integer>>> stopwordlessTokens = NGramExtractor
					.getNonStopWordTokens(tokens, LANGUAGE);
			Map<String, NGramFeature> chunkNGrams = NGramExtractor
					.extractSegmentNonStopWordNGrams(
							N,
							chunkString,
							chunkId,
							new Pair<Integer, Integer>(0, chunkString.length()),
							NGramType.Word, stopwordlessTokens, 1);

			chunksNGrams.add(chunkNGrams);
			chunkId++;
		}

		return chunksNGrams;
	}

}
