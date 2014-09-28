package org.iis.plagiarismdetector.sourceretrieval;

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
import org.iis.plagiarismdetector.textalignment.ngrams.NGramExtractor;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramFeature;
import org.iis.plagiarismdetector.textalignment.ngrams.NGramType;
import org.iis.plagiarismdetector.classifiers.PlagiarismTypeDetector;

import com.sun.tools.javac.util.Pair;

import org.iis.plagiarismdetector.core.lm.DirichletSmoothedLanguageModelbyLM;
import org.iis.plagiarismdetector.core.lm.LanguageModel;
import org.iis.plagiarismdetector.core.lm.LuceneBasedLanguageModel;
import org.iis.plagiarismdetector.core.lm.NotSmoothedLanguageModel;
import org.iis.plagiarismdetector.core.lucene.IndexInfo;
import org.iis.plagiarismdetector.core.sourceretrieval.QueryResult;
import org.iis.plagiarismdetector.core.sourceretrieval.SourceRetrievalConfig;
import org.iis.plagiarismdetector.core.sourceretrieval.irengine.LuceneIndex;
import org.iis.plagiarismdetector.core.textchunker.Chunker;
import org.iis.plagiarismdetector.core.textchunker.SimpleNonOverlappingChuker;

public class PLMBasedSourceRetrieval extends LMBasedSourceRetrieval {

    private static final Integer EXPANSION_DEGREE = 1;
    private static final Integer MAX_HITS = SourceRetrievalConfig.getK();
    private static final boolean STOPWORDS_IN_QUERY = false;

    private final Double MIU = 1000D;
    private String LANGUAGE = SourceRetrievalConfig.getLanguage();

    private LuceneIndex bgIndex;
    private LuceneIndex srcCollectionIndex;
    LuceneBasedLanguageModel bgLM;

    private String BG_COLLECTION = SourceRetrievalConfig.getSrcCorpusPath();//SourceRetrievalConfig.getSrcTrainCorpusPath();
    private String BG_INDEX_PATH = SourceRetrievalConfig.getSrcIndexPath();//SourceRetrievalConfig.getSrcTrainIndexPath();

    private String SRC_COLLECTION = SourceRetrievalConfig.getSrcCorpusPath();
    private String SRC_INDEX_PATH = SourceRetrievalConfig.getSrcIndexPath();

    private Integer N = 1;
    private String SUSP_DIR = SourceRetrievalConfig.getSuspCorpusPath();

    Map<Integer, LanguageModel> sourceLMz = new HashMap<Integer, LanguageModel>();

    public PlagiarismTypeDetector pdt = new PlagiarismTypeDetector();
    IndexInfo srcIndexInfo;
    private Double SIGMA = 100D;

    public PLMBasedSourceRetrieval() throws IOException {
        super();
    }

    /**
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws ClassNotFoundException,
            IOException {
        PLMBasedSourceRetrieval srcRetriever = new PLMBasedSourceRetrieval();

        try {
            /*
             * srcRetriever.pairToTrecJudgeFileFormatConvertor(
             * "evaluations/PAN2013/test_source_retrieval_judges",
             * "evaluations/PAN2013/test_source_retrieval_judges");
             */
            srcRetriever.run();
			// srcRetriever.index();
			/*
             * srcRetriever.testIndexLM(); testChunkLanguageModel(srcRetriever);
             */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() throws Exception {

        bgIndex = new LuceneIndex(BG_COLLECTION, LANGUAGE, BG_INDEX_PATH,
                SourceRetrievalConfig.get("IF_STEM"), SourceRetrievalConfig.get("IF_REMOVE_STOPWORDS_INDEXTIME"));

        bgIndex.loadIndex();

        bgLM = new LuceneBasedLanguageModel(bgIndex);
        /*srcCollectionIndex = 
         new LuceneIndex(SRC_COLLECTION, LANGUAGE,
         SRC_INDEX_PATH, SourceRetrievalConfig.get("IF_STEM"),
         SourceRetrievalConfig.get("IF_REMOVE_STOPWORDS"));
									 
		
         srcCollectionIndex.loadIndex();
         */
        srcCollectionIndex = bgIndex;
        initialize(SRC_INDEX_PATH);
        srcIndexInfo = new IndexInfo(srcCollectionIndex.getIndexReader());

        List<Integer> documents = srcCollectionIndex.getDocumentIDz();
        for (Integer docId : documents) {

            DirichletSmoothedLanguageModelbyLM candidSourceLanguageModel = new DirichletSmoothedLanguageModelbyLM(
                    srcCollectionIndex, docId, MIU, bgLM);

            sourceLMz.put(docId, candidSourceLanguageModel);
        }

        for (int i = 0; i < suspIndexInfo.getIndexReader().numDocs(); i++) {
            String documentText = suspIndexInfo.getIndexReader().document(i).get("TEXT");//TextProcessor.getMatn(suspFile).toLowerCase();
            retrieveSources(documentText, suspIndexInfo.getIndexReader().document(i).get("DOCID"));
        }
    }

    private void retrieveSources(String documentText, String suspFileName)
            throws Exception {

        Chunker chunker = new SimpleNonOverlappingChuker();
        List<String> chunks = chunker.chunk(documentText);
        List<LanguageModel> chunkLanguageModels = computeLanguageModelsForChunks(
                chunks, N);
        //Set<Integer> docIdz = getSelectedSources(documentText, suspFileName);

        Map<String, QueryResult> qrMap = new HashMap<String, QueryResult>();
        for (int i = 0; i < chunkLanguageModels.size(); i++) {
            List<Pair<Object, Double>> sourceDocuments = getRelatedSources(
                    chunkLanguageModels.get(i),
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
        //	queryResult = queryResult.subList(0, Math.min(queryResult.size(),SourceRetrievalConfig.getK()));
        System.out.println(suspFileName + ": " + queryResult.size());
        reportInTREC(queryResult, suspFileName.substring(suspFileName.indexOf("-") + 1));
        getFinalResults().put(suspFileName.substring(suspFileName.indexOf("-") + 1), queryResult);
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

    private List<Pair<Object, Double>> getRelatedSources(
            LanguageModel languageModel,
            Set<Integer> srcCandidz) throws IOException, ParseException {

        List<Pair<Object, Double>> similarityScores = new ArrayList<Pair<Object, Double>>();

        Set<Integer> searchSpace = new HashSet<Integer>();

        /*       for(String word: languageModel.getWordsCount().keySet())
         {
         searchSpace.addAll(srcCollectionIndex.getDocumentsContainingTerm(word));
         }
         */
        for (Integer docId : srcCandidz) {
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

        if (similarityScores.size() == 0) {
            return similarityScores;
        }
        int index = Math.min(similarityScores.size(), SourceRetrievalConfig.getK());// getIndexOfMaxDiff(similarityScores);
		/*if (index > 5)
         index = 0;
         */
        return similarityScores.subList(0, index);

        /*
         * return similarityScores.subList(0,
         * Math.min(sourceRetrievalConfig.topK, similarityScores.size()));
         */
    }

    private Set<Integer> getSelectedSources(String suspFileText,
            String suspFileName) throws IOException, ParseException {
        List<QueryResult> results = new ArrayList<QueryResult>();

        results = submitQuery(suspFileText, suspFileName, 100);

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
            /*Map<String, Map<String, Double>> expandingTerms = TextProcessor
             .getExpandigTerms(chunks.get(i), new  org.iis.plagiarismdetector.core.lucene.IndexInfo(
             bgIndex.getIndexReader()));*/

            Map<String, Double> expansionModel = new HashMap<String, Double>();// getWeightedExpandedTerms(expandingTerms);
            chunkLanguageModels.add(computeChunkLanguageModel(
                    chunkNGrams.subList(0, i), chunkNGrams.get(i),
                    chunkNGrams.subList(i + 1, chunks.size()), chunkHeadOffset,
                    chunkHeadOffset + chunkNGrams.get(i).snd, expansionModel));
            chunkHeadOffset += chunkNGrams.get(i).snd;
        }

        return chunkLanguageModels;
    }

    private Map<String, Double> getWeightedExpandedTerms(
            Map<String, Map<String, Double>> expandingTerms) {

        Map<String, Double> expansionModel = new HashMap<String, Double>();
        List<Pair<Object, Double>> weightedList = new ArrayList<Pair<Object, Double>>();
        for (String term : expandingTerms.keySet()) {
            Double weight = expandingTerms.get(term).get("SENSE_FREQUENCY")
                    * expandingTerms.get(term).get("SELECTION_COUNT");
            weightedList.add(new Pair<Object, Double>(term, weight));
        }

        Collections.sort(weightedList, new Comparator<Pair<Object, Double>>() {

            @Override
            public int compare(Pair<Object, Double> o1, Pair<Object, Double> o2) {

                return o2.snd.compareTo(o1.snd);
            }
        });

        if (weightedList.size() > 0) {
            Integer index = getIndexOfMaxSimpleDiff(weightedList);
            Double totalWight = 0D;
            for (Pair<Object, Double> term : weightedList.subList(0, index)) {
                totalWight += term.snd;
            }

            for (Pair<Object, Double> term : weightedList.subList(0, index)) {
                expansionModel.put((String) term.fst, term.snd / totalWight);
            }
        }
        return expansionModel;
    }

    private Integer getIndexOfMaxSimpleDiff(List<Pair<Object, Double>> list) {
        Integer indx1 = 0;
        Double diff1 = 0D;
        for (int i = 0; i < (list.size() - 1); i++) {
            Double tmpDiff1 = (list.get(i).snd - list.get(i + 1).snd)
                    / list.get(i).snd;

            if (tmpDiff1 > diff1) {
                diff1 = tmpDiff1;
                indx1 = i;
            }

        }

        return indx1 + 1;
    }

    private LanguageModel computeChunkLanguageModel(
            List<Pair<Map<String, NGramFeature>, Integer>> precedingChunks,
            Pair<Map<String, NGramFeature>, Integer> chunk,
            List<Pair<Map<String, NGramFeature>, Integer>> followingChunks,
            Long chunkHeadOffset, Long chunkEndOffset,
            Map<String, Double> expansionModel) {
        LanguageModel chunkLM = new NotSmoothedLanguageModel();

        for (String ngram : chunk.fst.keySet()) {
            /*
             * chunkLM.getWordsCount().put(ngram,
             * chunk.fst.get(ngram).getSumOfTransparencies());
             * 
             * chunkLM.getTotalWords().add(ngram);
             */
            if (!chunkLM.getWordsCount().containsKey(ngram)) {
                chunkLM.getWordsCount().put(ngram, 0D);
            }

            chunkLM.getTotalWords().add(ngram);
            chunkLM.getWordsCount().put(
                    ngram,
                    chunkLM.getWordsCount().get(ngram)
                    + chunk.fst.get(ngram).getSumOfTransparencies());

            if (!chunkLM.getBgWordsCount().containsKey(ngram)) {
                chunkLM.getBgWordsCount().put(ngram, 0D);
            }

            chunkLM.getBgWordsCount().put(
                    ngram,
                    chunkLM.getBgWordsCount().get(ngram)
                    + chunk.fst.get(ngram).getSumOfTransparencies());
        }

        /*
         * for (String expTerm : expansionModel.keySet()) { if
         * (!chunkLM.getWordsCount().containsKey(expTerm)) {
         * chunkLM.getTotalWords().add(expTerm);
         * chunkLM.getWordsCount().put(expTerm, 0D);
         * chunkLM.getWordsCount().put( expTerm,
         * chunkLM.getWordsCount().get(expTerm) + expansionModel.get(expTerm) *
         * chunk.snd); } }
         */
        for (Pair<Map<String, NGramFeature>, Integer> pchunk
                : precedingChunks.subList(Math.max(0, precedingChunks.size() - 1),
                        precedingChunks.size())) {
            for (String ngram : pchunk.fst.keySet()) {
                if (!chunkLM.getWordsCount().containsKey(ngram)) {
                    chunkLM.getWordsCount().put(ngram, 0D);
                }
                chunkLM.getTotalWords().add(ngram);
                chunkLM.getWordsCount()
                        .put(ngram, chunkLM.getWordsCount().get(ngram) + pchunk.fst
                                .get(ngram).getGaussianPropagatedScore((chunkHeadOffset
                                        + chunkEndOffset) / 2, SIGMA));
            }
        }

        for (Pair<Map<String, NGramFeature>, Integer> fchunk
                : followingChunks.subList(0, Math.min(1, followingChunks.size()))) {
            for (String ngram : fchunk.fst.keySet()) {
                if (!chunkLM.getWordsCount().containsKey(ngram)) {
                    chunkLM.getWordsCount().put(ngram, 0D);
                }
                chunkLM.getTotalWords().add(ngram);
                chunkLM.getWordsCount()
                        .put(ngram, chunkLM.getWordsCount().get(ngram) + fchunk.fst
                                .get(ngram).getGaussianPropagatedScore((chunkHeadOffset
                                        + chunkEndOffset) / 2, SIGMA));
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
                    .getTokens(chunkString, SourceRetrievalConfig.get("IF_STEM"), SourceRetrievalConfig.get("REMOVE_STOPWORDS_QUERYTIME"));
            List<Pair<String, Pair<Integer, Integer>>> stopwordlessTokens = NGramExtractor
                    .getNonStopWordTokens(tokens, srcIndexInfo.getTopTerms_DF("TEXT", 100));
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
