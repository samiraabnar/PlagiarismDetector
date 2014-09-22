package org.iis.plagiarismdetector.sourceretrieval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.queryparser.classic.ParseException;
import org.iis.plagiarismdetector.core.JudgementFileFormat;
import org.iis.plagiarismdetector.core.SimilarityFunction;
import org.iis.plagiarismdetector.core.TextProcessor;
import org.iis.plagiarismdetector.core.sourceretrieval.EvaluationSummary;
import org.iis.plagiarismdetector.core.sourceretrieval.QueryResult;
import org.iis.plagiarismdetector.core.sourceretrieval.SourceRetrievalConfig;
import org.iis.plagiarismdetector.core.sourceretrieval.irengine.Indexer;
import org.iis.plagiarismdetector.core.sourceretrieval.irengine.Retrieval;
import org.iis.plagiarismdetector.settings.PARAMETER;
import org.xml.sax.SAXException;

import com.sun.tools.javac.util.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.SimpleFSDirectory;
import org.iis.plagiarismdetector.core.lucene.IndexInfo;

public abstract class SourceRetriever {

    protected SourceRetrievalConfig sourceRetrievalConfig;
    String referencesDirectory = "";
    String suspDirectory = "";
    private Map<String, List<QueryResult>> FinalResults = new HashMap<String, List<QueryResult>>();
    protected Retrieval retriever;
    protected IndexInfo suspIndexInfo;

    protected Map<String, Integer> suspDocIndexedIdMap = new HashMap<String, Integer>();

    public void initialize(String collectionPath
    ) throws IOException {
        retriever = new Retrieval(collectionPath);
        suspIndexInfo = new IndexInfo(IndexReader.open(new SimpleFSDirectory(
                new File(SourceRetrievalConfig.getSuspIndexPath()))));

        suspDocIndexedIdMap = new HashMap<String, Integer>();
        for (int id = 0; id < suspIndexInfo.getIndexReader().numDocs(); id++) {
            suspDocIndexedIdMap.put(suspIndexInfo.getIndexReader().document(id).get("DOCID"), id);
        }
    }

    public void pairToTrecJudgeFileFormatConvertor(String pairFileName,
            String trecFileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(pairFileName));

        String line = null;
        String trecJudgeString = "";
        while ((line = br.readLine()) != null) {
            String[] pair = line.split(" ");
            String susp = pair[0].replace(".txt", "").trim();
            String src = pair[1].replace(".txt", "").trim();

            trecJudgeString += susp + " 0 " + src + " 1\n";

        }
        br.close();
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
                trecFileName)));
        bw.write(trecJudgeString);
        bw.close();
    }

    public static void planAndRun(SourceRetriever sourceRetriever)
            throws Exception {

        Map<PARAMETER, Float> similarityFunctionparams = new HashMap<PARAMETER, Float>();

        Map<String, Object> experimentOptions = new HashMap<String, Object>();

        similarityFunctionparams = new HashMap<PARAMETER, Float>();
        similarityFunctionparams.put(PARAMETER.PARAMETERS_BM25_b, 1.2F);
        similarityFunctionparams.put(PARAMETER.PARAMETERS_BM25_K1, 0.75F);
        similarityFunctionparams.put(PARAMETER.PARAMETERS_JM_LAMBDA, 0.7F);
        similarityFunctionparams.put(PARAMETER.PARAMETERS_DIRICHKET_MIU,
                1000.0F);

        experimentOptions = new HashMap<String, Object>();
        experimentOptions.put("CORPUS_MAIN_DIR", SourceRetrievalConfig.getCorpusMainDir());
        experimentOptions.put("CORPUS_DIR",
                SourceRetrievalConfig.getCorpusPath());
        experimentOptions.put("CORPUS_LANG", SourceRetrievalConfig.getLanguage());
        experimentOptions.put("TOP_K", SourceRetrievalConfig.getK());
        experimentOptions.put("SIMILARITY_FUNCTION", SimilarityFunction.JM);
        experimentOptions.put("PARAMS", similarityFunctionparams);
        experimentOptions.put("JUDGEMENTS_PATH",
                SourceRetrievalConfig.getJudgePath());
        experimentOptions.put("SUSP_FILE_DIR",
                SourceRetrievalConfig.getSuspCorpusPath());

        sourceRetriever.doTheExperiment(1, experimentOptions,
                "adhoc IR with lucene: on PAN2010 Corpus with"
                + experimentOptions.get("SIMILARITY_FUNCTION")
                + " as similarity function", false/* if reindex */);
    }

    public void doTheExperiment(Integer expNo,
            Map<String, Object> experimentOptions,
            String experimentDescription, boolean reIndex) throws Exception {
        try {

            initialize(SourceRetrievalConfig.getSrcIndexPath());
            retrieve(expNo, experimentOptions, experimentDescription);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void retrieve(Integer expNo, Map<String, Object> experimentOptions,
            String experimentDescription) throws Exception {
       
         for (int id = 0; id < suspIndexInfo.getIndexReader().numDocs(); id++) {
        
            String suspFileName = suspIndexInfo.getIndexReader().document(id).get("DOCID");
            processSuspDocument(suspIndexInfo.getIndexReader().document(id),suspFileName);
        }
    }

    protected void reportInTREC(List<QueryResult> finalResultsPerSuspDoc,
            String qID) throws IOException {
        File dir = new File(SourceRetrievalConfig.getSearchResultPath());
        if (!dir.exists()) {
            dir.mkdir();
        }
        BufferedWriter res = new BufferedWriter(new FileWriter(
                SourceRetrievalConfig.getSearchResultPath() + qID + ".txt"));
        for (int i = 0; i < finalResultsPerSuspDoc.size(); i++) {
            Double score = finalResultsPerSuspDoc.get(i).getScore();
            String docID = finalResultsPerSuspDoc.get(i).getDocumentId();
            String line = qID + " Q0 " + docID.substring(docID.indexOf("-") + 1) + " " + (i + 1) + " " + score
                    + " RUN\n";
            res.write(line);
        }
        res.close();
    }

    protected EvaluationSummary evaluateResults(
            Map<String, List<QueryResult>> results, String judgementsFileDir,
            JudgementFileFormat jformat) throws IOException {
        Map<String, List<String>> goldResults = readJudgeFile(
                judgementsFileDir, jformat);

        Double macroPrecision = calculateMacroPercision(results, goldResults);
        Double microPrecision = calculateMicroPercision(results, goldResults);

        Double macroRecall = calculateMacroRecall(results, goldResults);
        Double microRecall = calculateMicroRecall(results, goldResults);

        EvaluationSummary eSum = new EvaluationSummary();
        eSum.setMicroPrecision(microPrecision);
        eSum.setMicroRecall(microRecall);
        eSum.setMacroPrecision(macroPrecision);
        eSum.setMacroRecall(macroRecall);
        return eSum;

    }

    private Map<String, List<String>> readJudgeFile(String judgementsFileDir,
            JudgementFileFormat jformat) throws IOException {

        Map<String, List<String>> judgements = new HashMap<String, List<String>>();
        BufferedReader br = new BufferedReader(
                new FileReader(judgementsFileDir));

        if (jformat.equals(JudgementFileFormat.Pairs)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] pair = line.split(" ");
                String susp = pair[0].replace(".txt", "").trim();
                String src = pair[1].replace(".txt", "").trim();

                if (!judgements.containsKey(susp)) {
                    judgements.put(susp, new ArrayList<String>());
                }
                judgements.get(susp).add(src);
            }
        }
        br.close();
        return judgements;
    }

    private Double calculateMacroRecall(Map<String, List<QueryResult>> results,
            Map<String, List<String>> goldResults) {
        Double TP = 0D, FN = 0D;
        Double totalCount = 0D;
        for (String suspFile : results.keySet()) {
            for (String foundCandidate : goldResults.get(suspFile)) {
                QueryResult dummyQueryResult = new QueryResult(suspFile,
                        foundCandidate, 1D, null, null, null);
                if (results.get(suspFile).contains(dummyQueryResult)) {
                    TP++;
                }
            }

            totalCount += (double) goldResults.get(suspFile).size();
        }
        return (TP / totalCount);
    }

    private Double calculateMicroRecall(Map<String, List<QueryResult>> results,
            Map<String, List<String>> goldResults) {

        Map<String, Double> recalls = new HashMap<String, Double>();
        Double averagedRecall = 0D;
        for (String suspFile : results.keySet()) {
            Double TP = 0D, FN = 0D;

            for (String foundCandidate : goldResults.get(suspFile)) {
                QueryResult dummyQueryResult = new QueryResult(suspFile,
                        foundCandidate, 1D, null, null, null);
                if (results.get(suspFile).contains(dummyQueryResult)) {
                    TP++;
                }
            }

            Double localRecall = 0D;
            if (goldResults.get(suspFile).size() > 0) {
                localRecall = (TP / goldResults.get(suspFile).size());
            }
            averagedRecall += localRecall;
            recalls.put(suspFile, localRecall);
        }

        return averagedRecall / recalls.size();
    }

    private Double calculateMicroPercision(
            Map<String, List<QueryResult>> results,
            Map<String, List<String>> goldResults) {

        Map<String, Double> precisions = new HashMap<String, Double>();
        Double averagedPrecison = 0D;
        for (String suspFile : results.keySet()) {
            Double TP = 0D, FP = 0D;
            for (QueryResult foundCandidate : results.get(suspFile)) {
                if (goldResults.get(suspFile).contains(
                        foundCandidate.getDocumentId())) {
                    TP++;
                } else {
                    FP++;
                }
            }
            Double localPrec = 0D;
            if ((TP + FP) > 0) {
                localPrec = (TP / (TP + FP));
            }

            averagedPrecison += localPrec;
            precisions.put(suspFile, localPrec);
        }

        return averagedPrecison / precisions.keySet().size();
    }

    private Double calculateMacroPercision(
            Map<String, List<QueryResult>> results,
            Map<String, List<String>> goldResults) {
        Double TP = 0D, FP = 0D;
        for (String suspFile : results.keySet()) {
            for (QueryResult foundCandidate : results.get(suspFile)) {

                if (goldResults.get(suspFile).contains(
                        foundCandidate.getDocumentId())) {
                    TP++;
                } else {
                    FP++;
                }
            }
        }

        if ((TP + FP) == 0) {
            return 0D;
        }
        return (TP / (TP + FP));
    }

    public List<QueryResult> processSuspDocument(
            Document suspDoc,String suspFileName) throws Exception {
        String suspFileText = suspDoc.get("TEXT");
        
        List<Pair<String, String>> queries = extractQueries(suspFileName,
                suspFileText);
        Map<String, QueryResult> qrMap = new HashMap<String, QueryResult>();
        for (Pair<String, String> query : queries) {
            List<Pair<Object, Double>> queryResult = getResultsPerQuery(query);

            for (int k = 0; k < queryResult.size(); k++) {
                String docRealId = ((QueryResult) queryResult.get(k).fst).getDocumentId();
                if (qrMap
                        .containsKey(docRealId)) {

                    qrMap.put(
                            docRealId,
                            new QueryResult(
                                    suspFileName,
                                    docRealId,
                                    SourceRetrievalConfig.getK()
                                    - k
                                    + qrMap.get(
                                            docRealId)
                                    .getScore(), query.snd,
                                    k, ((QueryResult) queryResult.get(k).fst).getIndexedDocId()));
                } else {
                    qrMap.put(
                            docRealId,
                            new QueryResult(
                                    suspFileName,
                                    docRealId,
                                    (double) (SourceRetrievalConfig.getK()
                                    - k), query.snd,
                                    k, ((QueryResult) queryResult.get(k).fst).getIndexedDocId()));
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
        queryResult.subList(0, Math.min(queryResult.size(), SourceRetrievalConfig.getK()));
        System.out.println(suspFileName + ": " + queryResult.size());
        reportInTREC(queryResult, suspFileName);
        getFinalResults().put(suspFileName, queryResult);
        return queryResult;
    }

    private List<Pair<Object, Double>> getResultsPerQuery(Pair<String, String> query)
            throws IOException, ParseException {
        List<QueryResult> resultPerQ = submitQuery(query.fst, query.snd, SourceRetrievalConfig.getK());

        List<Pair<Object, Double>> similarityScores = new ArrayList<Pair<Object, Double>>();
        for (QueryResult resDoc : resultPerQ/* sourceLMz.keySet() */) {

            similarityScores.add(new Pair<Object, Double>(resDoc,
                    resDoc.getScore()));
        }

        Collections.sort(similarityScores,
                new Comparator<Pair<Object, Double>>() {

                    @Override
                    public int compare(Pair<Object, Double> o1,
                            Pair<Object, Double> o2) {
                        return o2.snd.compareTo(o1.snd);
                    }
                });

        if (similarityScores.isEmpty()) {
            return similarityScores;
        }
        int index = getIndexOfMaxDiff(similarityScores);
        if (index > 5) {
            index = 0;
        }

        return similarityScores.subList(0, index);
    }

  

    abstract protected List<Pair<String, String>> extractQueries(
            String suspFileName, String suspFileText) throws IOException,
            Exception;

    protected List<QueryResult> submitQuery(String query, String queryId, Integer k)
            throws IOException, ParseException {

        List<QueryResult> results = new ArrayList<QueryResult>();

        results = retriever.searchAndReturnResults(query, queryId, k);

        return results;
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

    public Map<String, List<QueryResult>> getFinalResults() {
        return FinalResults;
    }

    public void setFinalResults(Map<String, List<QueryResult>> finalResults) {
        FinalResults = finalResults;
    }

}
