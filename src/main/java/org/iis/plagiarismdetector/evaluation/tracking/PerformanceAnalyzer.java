package org.iis.plagiarismdetector.evaluation.tracking;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.iis.plagiarismdetector.settings.TextAlignmentDatasetSettings;
import org.iis.plagiarismdetector.tools.DetailsReader;

import com.sun.tools.javac.util.Pair;

import org.iis.plagiarismdetector.core.PlagiarismCase;

public class PerformanceAnalyzer {

	public static final Double F_MEATURE_ALPHA = 1D;

	private Long CaseCount = 0L;
	private Long DetectionsCount = 0L;

	private Long completely_not_detected_count = 0L;
	/**
	 * @param args
	 */

	// map from suspicious document to a src and its related plagiarism cases
	Map<String, Map<String, List<PlagiarismCase>>> detections = new HashMap<String, Map<String, List<PlagiarismCase>>>();
	Map<String, Map<String, List<PlagiarismCase>>> inverteddetections = new HashMap<String, Map<String, List<PlagiarismCase>>>();

	Map<String, Map<String, List<PlagiarismCase>>> cases = new HashMap<String, Map<String, List<PlagiarismCase>>>();
	Map<String, Map<String, List<PlagiarismCase>>> invertedcases = new HashMap<String, Map<String, List<PlagiarismCase>>>();;

	public DetailsComparator dc = new DetailsComparator();
	private Map<String, Map<String, List<PlagiarismCase>>> NotDetectedCases = new HashMap<String, Map<String, List<PlagiarismCase>>>();
	private List<Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>> SplitlyDetectedCases = new ArrayList<Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>>();
	private List<Pair<Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>, Double>> localRecalls = new ArrayList<Pair<Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>, Double>>();
	private List<Pair<Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>, Double>> localPrecisions = new ArrayList<Pair<Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>, Double>>();

	public static void main(String[] args) throws IOException {

		PerformanceAnalyzer pa = new PerformanceAnalyzer();

		/*
		 * try { pa.dc.validate_no_obfuscation_folder(); } catch (IOException e)
		 * { e.printStackTrace(); System.exit(0); } catch
		 * (FactoryConfigurationError e) { // TODO Auto-generated catch
		 * e.printStackTrace(); System.exit(0); } catch (XMLStreamException e) {
		 * e.printStackTrace(); System.exit(0); }
		 */

		try {
			pa.AnalyzePerformance();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}

	}

	public PerformanceAnalyzer(
			Long caseCount,
			Long completely_not_detected_count,
			Map<String, Map<String, List<PlagiarismCase>>> detections,
			Map<String, Map<String, List<PlagiarismCase>>> inverteddetections,
			Map<String, Map<String, List<PlagiarismCase>>> cases,
			Map<String, Map<String, List<PlagiarismCase>>> invertedcases,
			DetailsComparator dc,
			Map<String, Map<String, List<PlagiarismCase>>> notDetectedCases,
			List<Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>> splitlyDetectedCases,
			List<Pair<Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>, Double>> localRecalls,
			List<Pair<Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>, Double>> localPrecisions) {
		super();
		CaseCount = caseCount;
		this.completely_not_detected_count = completely_not_detected_count;
		this.detections = detections;
		this.inverteddetections = inverteddetections;
		this.cases = cases;
		this.invertedcases = invertedcases;
		this.dc = dc;
		NotDetectedCases = notDetectedCases;
		SplitlyDetectedCases = splitlyDetectedCases;
		this.localRecalls = localRecalls;
		this.localPrecisions = localPrecisions;

		TextAlignmentDatasetSettings.initialize();
	}

	public PerformanceAnalyzer() {
		TextAlignmentDatasetSettings.initialize();
	}

	public void AnalyzePerformance() throws CloneNotSupportedException,
			ParserConfigurationException, TransformerException, IOException {

		DetectionsCount = readDetails(
				TextAlignmentDatasetSettings.DETECTED_DETAILS_DIR, detections,
				inverteddetections);

		CaseCount = readDetails(
				TextAlignmentDatasetSettings.REALCASES_DETAILS_DIR, cases,
				invertedcases);

		Double micPrec = computeMicroAveragedPrecision("BOTH");
		Double macPrec = computeMacroAveragedPrecision("BOTH");

		Double micRec = computeMicroAveragedRecall("BOTH");
		Double macRec = computeMacroAveragedRecall("BOTH");

		Double gran = computeGranularity();

		Double macPrec_basedOnSusp = computeMacroAveragedPrecision("SUSP");
		Double micPrec_basedOnSusp = computeMicroAveragedPrecision("SUSP");

		Double macRec_basedOnSusp = computeMacroAveragedRecall("SUSP");
		Double micRec_basedOnSusp = computeMicroAveragedRecall("SUSP");

		Double gran_basedOnSusp = computeGranularity();

		Double macPrec_basedOnSrc = computeMacroAveragedPrecision("SOURCE");
		Double micPrec_basedOnSrc = computeMicroAveragedPrecision("SOURCE");

		Double macRec_basedOnSrc = computeMacroAveragedRecall("SOURCE");
		Double micRec_basedOnSrc = computeMicroAveragedRecall("SOURCE");

		Double gran_basedOnSrc = computeGranularity();

		System.out.println("Granularity: " + gran);

		System.out.println("Evaluation Results Based On Susp Passages:");
		System.out.println("Micro Averaged Precision: " + micPrec
				+ " Macro Averaged Precision: " + macPrec_basedOnSusp);
		System.out.println("Micro Averaged Recall:    " + micRec
				+ " Macro Averaged Recall:    " + macRec_basedOnSusp);
		System.out.println("Micro Plagdet Score:      "
				+ computePlagdetScore(micPrec_basedOnSusp, micRec_basedOnSusp,
						gran_basedOnSusp)
				+ " Macro Plagdet Score:      "
				+ computePlagdetScore(macPrec_basedOnSusp, macRec_basedOnSusp,
						gran_basedOnSusp));

		System.out.println("Evaluation Results Based On Source  Passages:");
		System.out.println("Micro Averaged Precision: " + micPrec_basedOnSrc
				+ " Macro Averaged Precision: " + macPrec_basedOnSrc);
		System.out.println("Micro Averaged Recall:    " + micRec_basedOnSrc
				+ " Macro Averaged Recall:    " + macRec_basedOnSrc);
		System.out.println("Micro Plagdet Score:      "
				+ computePlagdetScore(micPrec_basedOnSrc, micRec_basedOnSrc,
						gran_basedOnSrc)
				+ " Macro Plagdet Score:      "
				+ computePlagdetScore(macPrec_basedOnSrc, macRec_basedOnSrc,
						gran_basedOnSrc));

		System.out
				.println("Evaluation Results Based On both Source And Susp Passages:");
		System.out.println("Micro Averaged Precision: " + micPrec
				+ " Macro Averaged Precision: " + macPrec);
		System.out.println("Micro Averaged Recall:    " + micRec
				+ " Macro Averaged Recall:    " + macRec);
		System.out.println("Micro Plagdet Score:      "
				+ computePlagdetScore(micPrec, micRec, gran)
				+ " Macro Plagdet Score:      "
				+ computePlagdetScore(macPrec, macRec, gran));

		System.out.println("Number of Completely Not Detected Cases:"
				+ completely_not_detected_count + " Total Cases Count:"
				+ CaseCount);
		System.out
				.println("Number of Total  Detected Cases:" + DetectionsCount);
		printCasePairs(TextAlignmentDatasetSettings.SPLITLY_DETECTED_CASES_LOG,
				SplitlyDetectedCases);
		printEvaluatedCasesLog(
				TextAlignmentDatasetSettings.RECALL_BASED_EVALUATION_LOG,
				localRecalls);
		printEvaluatedCasesLog(
				TextAlignmentDatasetSettings.PRECISION_BASED_EVALUATION_LOG,
				localPrecisions);
		printCases(TextAlignmentDatasetSettings.NOT_DETECTED_EVALUATION_LOG,
				NotDetectedCases);
		System.out.println("Keep Trying :) There is Always a BETTER Solution!");
	}

	private void printCases(String resultsDir,
			Map<String, Map<String, List<PlagiarismCase>>> notDetectedCases2)
			throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.newDocument();
		// add elements to Document
		Element rootElement = doc.createElement("PlagiarismCases");
		// append root element to document
		doc.appendChild(rootElement);

		for (String suspFile : notDetectedCases2.keySet()) {
			for (String srcFile : notDetectedCases2.get(suspFile).keySet()) {
				for (PlagiarismCase pcase : notDetectedCases2.get(suspFile)
						.get(srcFile)) {
					Element plagiarismCaseElement = doc
							.createElement("PlagiarismCase");

					createPlagiarismCaseElement(doc, pcase,
							plagiarismCaseElement);
					rootElement.appendChild(plagiarismCaseElement);
				}
			}
		}

		// for output to file, console
		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		// for pretty print
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);

		// write to console or file
		// StreamResult console = new StreamResult(System.out);
		StreamResult file = new StreamResult((new File(resultsDir)).getPath());

		// write data
		// transformer.transform(source, console);
		transformer.transform(source, file);
	}

	private void printCasePairs(
			String resultsDir,
			List<Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>> splitedDetections)
			throws ParserConfigurationException, TransformerException,
			IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.newDocument();
		// add elements to Document
		Element rootElement = doc.createElement("EvaluatedCases");
		// append root element to document
		doc.appendChild(rootElement);
		for (Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>> splitlyDetected : splitedDetections) {
			// append first child element to root element
			rootElement.appendChild(getPairedCasesInfoXmlElement(doc,
					splitlyDetected));
		}

		// for output to file, console
		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		// for pretty print
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		DOMSource source = new DOMSource(doc);

		// write to console or file
		// StreamResult console = new StreamResult(System.out);

		StreamResult file = new StreamResult((new File(resultsDir)).getPath());

		// write data
		// transformer.transform(source, console);
		transformer.transform(source, file);
	}

	private Node getPairedCasesInfoXmlElement(
			Document doc,
			Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>> splitlyDetected) {

		Element caseInfo = doc.createElement("PairedCases");
		PlagiarismCase basePlagiarismCase = splitlyDetected.fst;
		Element basePlagiarismCaseElement = doc
				.createElement("basePlagiarismCase");

		createPlagiarismCaseElement(doc, basePlagiarismCase,
				basePlagiarismCaseElement);

		caseInfo.appendChild(basePlagiarismCaseElement);

		Element relatedPlagiarismCasesElement = doc
				.createElement("relatedPlagiarismCases");
		for (Pair<PlagiarismCase, PlagiarismCase> pairedRelatedPlagiarismCases : splitlyDetected.snd) {
			PlagiarismCase casePlagiarismCase = pairedRelatedPlagiarismCases.fst;
			PlagiarismCase truelyDetectedPlagiarismCase = pairedRelatedPlagiarismCases.snd;

			Element relatedPlagiarismCaseElement = doc
					.createElement("relatedPlagiarismCase");
			Element relatedCasePlagiarismCaseElement = doc
					.createElement("casePlagiarismCase");
			Element relatedDetectedPlagiarismCaseElement = doc
					.createElement("casePlagiarismCase");

			createPlagiarismCaseElement(doc, casePlagiarismCase,
					relatedCasePlagiarismCaseElement);
			createPlagiarismCaseElement(doc, truelyDetectedPlagiarismCase,
					relatedDetectedPlagiarismCaseElement);

			relatedPlagiarismCaseElement
					.appendChild(relatedCasePlagiarismCaseElement);
			relatedPlagiarismCaseElement
					.appendChild(relatedDetectedPlagiarismCaseElement);

			relatedPlagiarismCasesElement
					.appendChild(relatedPlagiarismCaseElement);

		}
		caseInfo.appendChild(relatedPlagiarismCasesElement);

		return caseInfo;

	}

	private void printEvaluatedCasesLog(
			String resultsDir,
			List<Pair<Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>, Double>> evaluatedPairs)
			throws ParserConfigurationException, TransformerException {
		if (evaluatedPairs != null) {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.newDocument();
			// add elements to Document
			Element rootElement = doc.createElement("EvaluatedCases");
			// append root element to document
			doc.appendChild(rootElement);
			for (Pair<Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>, Double> evaluatedCaseInfo : evaluatedPairs) {
				// append first child element to root element
				rootElement.appendChild(getEvaluatedCaseInfoXmlElement(doc,
						evaluatedCaseInfo));
			}

			// for output to file, console
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			// for pretty print
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);

			// write to console or file
			// StreamResult console = new StreamResult(System.out);
			StreamResult file = new StreamResult(
					(new File(resultsDir)).getPath());

			// write data
			// transformer.transform(source, console);
			transformer.transform(source, file);
		}
	}

	private Node getEvaluatedCaseInfoXmlElement(
			Document doc,
			Pair<Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>, Double> evaluatedCaseInfo) {
		Element caseInfo = doc.createElement("EvaluatedCase");
		caseInfo.setAttribute("score", evaluatedCaseInfo.snd.toString());
		PlagiarismCase basePlagiarismCase = evaluatedCaseInfo.fst.fst;
		Element basePlagiarismCaseElement = doc
				.createElement("basePlagiarismCase");

		createPlagiarismCaseElement(doc, basePlagiarismCase,
				basePlagiarismCaseElement);

		caseInfo.appendChild(basePlagiarismCaseElement);

		Element relatedPlagiarismCasesElement = doc
				.createElement("relatedPlagiarismCases");
		for (Pair<PlagiarismCase, PlagiarismCase> pairedRelatedPlagiarismCases : evaluatedCaseInfo.fst.snd) {
			PlagiarismCase casePlagiarismCase = pairedRelatedPlagiarismCases.fst;
			PlagiarismCase truelyDetectedPlagiarismCase = pairedRelatedPlagiarismCases.snd;

			Element relatedPlagiarismCaseElement = doc
					.createElement("relatedPlagiarismCase");
			Element relatedCasePlagiarismCaseElement = doc
					.createElement("casePlagiarismCase");
			Element relatedDetectedPlagiarismCaseElement = doc
					.createElement("truelyDetectedPlagiarismCase");

			createPlagiarismCaseElement(doc, casePlagiarismCase,
					relatedCasePlagiarismCaseElement);
			createPlagiarismCaseElement(doc, truelyDetectedPlagiarismCase,
					relatedDetectedPlagiarismCaseElement);

			relatedPlagiarismCaseElement
					.appendChild(relatedCasePlagiarismCaseElement);
			relatedPlagiarismCaseElement
					.appendChild(relatedDetectedPlagiarismCaseElement);

			relatedPlagiarismCasesElement
					.appendChild(relatedPlagiarismCaseElement);

		}
		caseInfo.appendChild(relatedPlagiarismCasesElement);

		return caseInfo;
	}

	public void createPlagiarismCaseElement(Document doc,
			PlagiarismCase basePlagiarismCase, Element basePlagiarismCaseElement)
			throws DOMException {
		Element basePlagiarismCaseInfo = doc
				.createElement("PlagiarismCaseInfo");
		basePlagiarismCaseInfo.appendChild(doc
				.createTextNode(basePlagiarismCase.toShortString()));

		Element basePlagiarismCaseValue = doc
				.createElement("PlagiarismCaseValue");

		Element basePlagiarismCaseValueSrc = doc.createElement("source");
		basePlagiarismCaseValueSrc.appendChild(doc.createTextNode(DetailsReader
				.getFeatureSrcText(
						TextAlignmentDatasetSettings.SOURCE_FILES_DIR
								+ basePlagiarismCase.getSourceDocument(),
						basePlagiarismCase)));

		Element basePlagiarismCaseValueSusp = doc.createElement("susp");
		basePlagiarismCaseValueSusp.appendChild(doc
				.createTextNode(DetailsReader.getFeatureSuspText(
						TextAlignmentDatasetSettings.SUSP_FILES_DIR
								+ basePlagiarismCase.getSuspDocument(),
						basePlagiarismCase)));

		basePlagiarismCaseValue.appendChild(basePlagiarismCaseValueSrc);
		basePlagiarismCaseValue.appendChild(basePlagiarismCaseValueSusp);

		basePlagiarismCaseElement.appendChild(basePlagiarismCaseInfo);
		basePlagiarismCaseElement.appendChild(basePlagiarismCaseValue);
	}

	private Long readDetails(String detailsDir,
			Map<String, Map<String, List<PlagiarismCase>>> suspBasedDetailsMap,
			Map<String, Map<String, List<PlagiarismCase>>> srcBasedDetailsMap) {
		Long count = 0L;
		try {
			File dir = new File(detailsDir);
			File[] detailfiles = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return !name.startsWith(".") && name.endsWith(".xml");
				}
			});
			HashMap<String, List<PlagiarismCase>> caseMap = new HashMap<String, List<PlagiarismCase>>();
			HashMap<String, List<PlagiarismCase>> invertedcaseMap = new HashMap<String, List<PlagiarismCase>>();

			for (File detailFile : detailfiles) {
				List<PlagiarismCase> perFilecases = dc.getCasesContent(
						detailsDir, detailFile.getName());
				/*
				 * List<PlagiarismCase> perFilecases2 = new
				 * ArrayList<PlagiarismCase>( perFilecases);
				 */
				if (perFilecases.isEmpty()) {
					String suspFileName = dc.getSuspFileName(detailsDir,
							detailFile.getName());
					String srcFileName = dc.getSrcFileName(suspFileName,
							detailFile.getName());
					if (!caseMap.containsKey(suspFileName)) {
						caseMap.put(suspFileName,
								new ArrayList<PlagiarismCase>());
					}

					if (!invertedcaseMap.containsKey(srcFileName)) {
						invertedcaseMap.put(srcFileName,
								new ArrayList<PlagiarismCase>());
					}
				} else {
					if (!caseMap.containsKey(perFilecases.get(0)
							.getSuspDocument())) {
						caseMap.put(perFilecases.get(0).getSuspDocument(),
								new ArrayList<PlagiarismCase>());
					}

					List<PlagiarismCase> perFilecases2 = new ArrayList<PlagiarismCase>(
							perFilecases);
					Collections.copy(perFilecases2, perFilecases);
					caseMap.get(perFilecases.get(0).getSuspDocument()).addAll(
							perFilecases2);

					if (!invertedcaseMap.containsKey(perFilecases2.get(0)
							.getFeature("source_reference"))) {
						invertedcaseMap.put(
								perFilecases2.get(0).getFeature(
										"source_reference"),
								new ArrayList<PlagiarismCase>());
					}
					List<PlagiarismCase> perFilecases3 = new ArrayList<PlagiarismCase>(
							perFilecases);
					Collections.copy(perFilecases3, perFilecases);
					invertedcaseMap
							.get(perFilecases2.get(0).getFeature(
									"source_reference")).addAll(perFilecases3);
				}

			}

			for (String suspFile : caseMap.keySet()) {
				suspBasedDetailsMap.put(suspFile,
						new HashMap<String, List<PlagiarismCase>>());
				for (PlagiarismCase pcase : caseMap.get(suspFile)) {
					count++;
					if (!suspBasedDetailsMap.get(suspFile).containsKey(
							pcase.getFeature("source_reference")))
						suspBasedDetailsMap.get(suspFile).put(
								pcase.getFeature("source_reference"),
								new ArrayList<PlagiarismCase>());
					suspBasedDetailsMap.get(suspFile)
							.get(pcase.getFeature("source_reference"))
							.add(pcase.clone());
				}
			}
			// System.out.println("susp based count: " + count);

			Long count2 = 0L;
			for (String srcFile : invertedcaseMap.keySet()) {
				srcBasedDetailsMap.put(srcFile,
						new HashMap<String, List<PlagiarismCase>>());
				for (PlagiarismCase pcase : invertedcaseMap.get(srcFile)) {
					count2++;
					if (!srcBasedDetailsMap.get(srcFile).containsKey(
							pcase.getSuspDocument()))
						srcBasedDetailsMap.get(srcFile).put(
								pcase.getSuspDocument(),
								new ArrayList<PlagiarismCase>());
					srcBasedDetailsMap.get(srcFile)
							.get(pcase.getSuspDocument()).add(pcase.clone());
				}
			}

			// System.out.println("src based count: " + count2);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		return count;
	}

	public void readDetections(String detectedDetailsDir) {
		try {
			File dir = new File(detectedDetailsDir);
			File[] detailfiles = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return !name.startsWith(".") && name.endsWith(".xml");
				}
			});
			// DetailsReader dreader = new DetailsReader();
			Map<String, List<PlagiarismCase>> caseMap = new HashMap<String, List<PlagiarismCase>>();
			for (File detailFile : detailfiles) {
				// dreader.readDetailsFile(caseMap, detailFile);
				List<PlagiarismCase> perFilecases = dc.getCasesContent(
						TextAlignmentDatasetSettings.DETECTED_DETAILS_DIR,
						detailFile.getName());
				if (perFilecases.size() > 0) {
					if (!caseMap.containsKey(perFilecases.get(0)
							.getSuspDocument())) {
						caseMap.put(perFilecases.get(0).getSuspDocument(),
								perFilecases);
					} else {
						caseMap.get(perFilecases.get(0).getSuspDocument())
								.addAll(perFilecases);

					}
				} else {
					// System.out.println(detailFile.getName() + " is empty");
				}
			}

			for (String suspFile : caseMap.keySet()) {
				detections.put(suspFile,
						new HashMap<String, List<PlagiarismCase>>());
				for (PlagiarismCase pcase : caseMap.get(suspFile)) {
					if (!detections.get(suspFile).containsKey(
							pcase.getFeature("source_reference")))
						detections.get(suspFile).put(
								pcase.getFeature("source_reference"),
								new ArrayList<PlagiarismCase>());
					detections.get(suspFile)
							.get(pcase.getFeature("source_reference"))
							.add(pcase.clone());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public List<PlagiarismCase> extractAllDetectedPlagiarismCases() {
		List<PlagiarismCase> detectedPlagiarismCases = new ArrayList<PlagiarismCase>();
		if (detections != null) {
			for (String suspFile : detections.keySet()) {
				for (String srcFile : detections.get(suspFile).keySet()) {
					for (PlagiarismCase detectedPlagiarismCase : detections
							.get(suspFile).get(srcFile)) {
						detectedPlagiarismCases.add(detectedPlagiarismCase);
					}
				}

			}
		}

		return detectedPlagiarismCases;
	}

	public List<PlagiarismCase> extractAllCasesPlagiarismCases() {
		List<PlagiarismCase> casePlagiarismCases = new ArrayList<PlagiarismCase>();
		if (detections != null) {
			Long sum = 0L;
			for (String suspFile : cases.keySet()) {
				for (String srcFile : cases.get(suspFile).keySet()) {
					for (PlagiarismCase detectedPlagiarismCase : cases.get(
							suspFile).get(srcFile)) {
						casePlagiarismCases.add(detectedPlagiarismCase);
						sum += Long.parseLong(detectedPlagiarismCase
								.getFeature("this_length"))
								+ Long.parseLong(detectedPlagiarismCase
										.getFeature("source_length"));
					}
				}

			}

			System.out.println("All cases Length: " + sum);
		}

		return casePlagiarismCases;
	}

	public List<PlagiarismCase> extractTruePositives()
			throws CloneNotSupportedException {
		List<PlagiarismCase> truePositives = new ArrayList<PlagiarismCase>();
		if (detections != null) {
			for (String suspFile : detections.keySet()) {
				if (cases.containsKey(suspFile)) {

					for (String srcFile : detections.get(suspFile).keySet()) {
						if (cases.get(suspFile).containsKey(srcFile)) {
							for (PlagiarismCase detectedPlagiarismCase : detections
									.get(suspFile).get(srcFile)) {
								for (PlagiarismCase realPlagiarismCase : cases
										.get(suspFile).get(srcFile)) {
									PlagiarismCase trulyDetectedPlagiarismCase = detectedPlagiarismCase
											.overlap(realPlagiarismCase);
									if (trulyDetectedPlagiarismCase != null)
										truePositives
												.add(trulyDetectedPlagiarismCase);
								}
							}
						}
					}
				}
			}
		}

		return truePositives;
	}

	public Double computeMicroAveragedPrecision(String basedOn)
			throws CloneNotSupportedException {
		Double microPrec = 0D;

		Map<String, Map<String, List<PlagiarismCase>>> trueDetections = filterDetailsCasesMap(
				detections, cases, "TRUE_DETECTIONS");
		Map<String, Map<String, List<PlagiarismCase>>> invertedTrueDetections = filterDetailsCasesMap(
				inverteddetections, invertedcases, "TRUE_DETECTIONS");
		Long trueLength = computeDetailsCasesLength(trueDetections,
				invertedTrueDetections, basedOn);

		Long totalLength = computeDetailsCasesLength(detections,
				inverteddetections, basedOn);

		if (CaseCount == 0) {
			if (totalLength == 0)
				return 1D;
		} else {
			// System.out.println("true detections length: " + trueLength);
			microPrec = (double) ((double) trueLength / (double) totalLength);
		}
		return microPrec;
	}

	public Double computeMicroAveragedRecall(String basedOn)
			throws CloneNotSupportedException {
		Double macroRec = 0D;

		Map<String, Map<String, List<PlagiarismCase>>> trueDetections = filterDetailsCasesMap(
				detections, cases, "TRUE_DETECTIONS");
		Map<String, Map<String, List<PlagiarismCase>>> invertedTrueDetections = filterDetailsCasesMap(
				inverteddetections, invertedcases, "TRUE_DETECTIONS");
		;
		Long trueLength = computeDetailsCasesLength(trueDetections,
				invertedTrueDetections, basedOn);

		Long totalLength = computeDetailsCasesLength(cases, invertedcases,
				basedOn);

		if (totalLength == 0) {
			if (trueLength == 0)
				return 1D;
			else
				return 0D;
		} else {
			// System.out.println("true detections length: " + trueLength);
			macroRec = (double) ((double) trueLength / (double) totalLength);
		}

		macroRec = (double) ((double) trueLength / (double) totalLength);

		return macroRec;
	}

	private Map<String, Map<String, List<PlagiarismCase>>> filterDetailsCasesMap(
			Map<String, Map<String, List<PlagiarismCase>>> inputMapfordetections,
			Map<String, Map<String, List<PlagiarismCase>>> inputMapforcases,
			String filterType) throws CloneNotSupportedException {
		Map<String, Map<String, List<PlagiarismCase>>> filteredMap = new HashMap<String, Map<String, List<PlagiarismCase>>>();
		if (filterType.equals("TRUE_DETECTIONS")) {
			for (String key1File : inputMapfordetections.keySet()) {
				if (inputMapforcases.containsKey(key1File)) {
					for (String key2File : inputMapfordetections.get(key1File)
							.keySet()) {
						if (inputMapforcases.get(key1File)
								.containsKey(key2File)) {
							for (PlagiarismCase detectedPlagiarismCase : inputMapfordetections
									.get(key1File).get(key2File)) {

								for (PlagiarismCase realPlagiarismCase : inputMapforcases
										.get(key1File).get(key2File)) {
									PlagiarismCase trulyDetectedPlagiarismCase = detectedPlagiarismCase
											.overlap(realPlagiarismCase);

									if (trulyDetectedPlagiarismCase != null) {
										if (!filteredMap.containsKey(key1File))
											filteredMap
													.put(key1File,
															new HashMap<String, List<PlagiarismCase>>());
										if (!filteredMap.get(key1File)
												.containsKey(key2File))
											filteredMap
													.get(key1File)
													.put(key2File,
															new ArrayList<PlagiarismCase>());

										filteredMap
												.get(key1File)
												.get(key2File)
												.add(trulyDetectedPlagiarismCase);
									}
								}
							}

						}
					}
				}
			}
		}

		return filteredMap;
	}

	private Long computeDetailsCasesLength(
			Map<String, Map<String, List<PlagiarismCase>>> suspBasedMap,
			Map<String, Map<String, List<PlagiarismCase>>> srcBasedMap,
			String basedOn) {
		Long totalLength_src = 0L;
		Long totalLength_susp = 0L;

		Integer count = 0;
		for (String suspFile : suspBasedMap.keySet()) {
			List<PlagiarismCase> pairedPlagiarismCasesList = new ArrayList<PlagiarismCase>();

			for (String srcFile : suspBasedMap.get(suspFile).keySet()) {

				for (PlagiarismCase detectedPlagiarismCase : suspBasedMap.get(
						suspFile).get(srcFile)) {
					count++;
					pairedPlagiarismCasesList.add(detectedPlagiarismCase);
				}
			}
			Collections.sort(pairedPlagiarismCasesList,
					new Comparator<PlagiarismCase>() {

						@Override
						public int compare(PlagiarismCase o1, PlagiarismCase o2) {
							// TODO Auto-generated method stub
							return (new Long(Long.parseLong(o1.getFeature(
									"this_offset").toString())))
									.compareTo(new Long(Long.parseLong(o2
											.getFeature("this_offset")
											.toString())));
						}
					});

			// forSusp
			Long lastEndOffset = 0L;
			for (int i = 0; i < pairedPlagiarismCasesList.size(); i++) {
				if (Long.parseLong(pairedPlagiarismCasesList.get(i)
						.getFeature("this_offset").toString()) <= lastEndOffset) {
					if (Long.parseLong(pairedPlagiarismCasesList.get(i)
							.getFeature("this_offset").toString())
							+ Long.parseLong(pairedPlagiarismCasesList.get(i)
									.getFeature("this_length").toString()) >= lastEndOffset) {
						totalLength_susp += Long
								.parseLong(pairedPlagiarismCasesList.get(i)
										.getFeature("this_offset").toString())
								+ Long.parseLong(pairedPlagiarismCasesList
										.get(i).getFeature("this_length")
										.toString()) - lastEndOffset;
						lastEndOffset = Long
								.parseLong(pairedPlagiarismCasesList.get(i)
										.getFeature("this_length").toString())
								+ Long.parseLong(pairedPlagiarismCasesList
										.get(i).getFeature("this_offset")
										.toString());
					}
				} else {
					totalLength_susp += Long
							.parseLong(pairedPlagiarismCasesList.get(i)
									.getFeature("this_length").toString());
					lastEndOffset = Long.parseLong(pairedPlagiarismCasesList
							.get(i).getFeature("this_length").toString())
							+ Long.parseLong(pairedPlagiarismCasesList.get(i)
									.getFeature("this_offset").toString());
				}
			}

		}

		count = 0;
		for (String srcFile : srcBasedMap.keySet()) {
			List<PlagiarismCase> pairedPlagiarismCasesList = new ArrayList<PlagiarismCase>();

			for (String suspFile : srcBasedMap.get(srcFile).keySet()) {

				for (PlagiarismCase casePlagiarismCase : srcBasedMap.get(
						srcFile).get(suspFile)) {
					count++;
					pairedPlagiarismCasesList.add(casePlagiarismCase);
				}
			}
			Collections.sort(pairedPlagiarismCasesList,
					new Comparator<PlagiarismCase>() {

						@Override
						public int compare(PlagiarismCase o1, PlagiarismCase o2) {
							// TODO Auto-generated method stub
							return (new Long(Long.parseLong(o1.getFeature(
									"source_offset").toString())))
									.compareTo(new Long(Long.parseLong(o2
											.getFeature("source_offset")
											.toString())));
						}
					});
			// forSrc
			Long lastEndOffset = 0L;
			for (int i = 0; i < pairedPlagiarismCasesList.size(); i++) {
				if (Long.parseLong(pairedPlagiarismCasesList.get(i)
						.getFeature("source_offset").toString()) <= lastEndOffset) {
					if (Long.parseLong(pairedPlagiarismCasesList.get(i)
							.getFeature("source_offset").toString())
							+ Long.parseLong(pairedPlagiarismCasesList.get(i)
									.getFeature("source_length").toString()) > lastEndOffset) {
						totalLength_src += Long
								.parseLong(pairedPlagiarismCasesList.get(i)
										.getFeature("source_offset").toString())
								+ Long.parseLong(pairedPlagiarismCasesList
										.get(i).getFeature("source_length")
										.toString()) - lastEndOffset;
						lastEndOffset = Long
								.parseLong(pairedPlagiarismCasesList.get(i)
										.getFeature("source_length").toString())
								+ Long.parseLong(pairedPlagiarismCasesList
										.get(i).getFeature("source_offset")
										.toString());
					}
				} else {
					totalLength_src += Long.parseLong(pairedPlagiarismCasesList
							.get(i).getFeature("source_length").toString());
					lastEndOffset = Long.parseLong(pairedPlagiarismCasesList
							.get(i).getFeature("source_length").toString())
							+ Long.parseLong(pairedPlagiarismCasesList.get(i)
									.getFeature("source_offset").toString());
				}
			}
		}

		Long totalLength = 0L;

		if (basedOn.equals("BOTH"))
			totalLength = totalLength_src + totalLength_susp;
		else if (basedOn.equals("SUSP"))
			totalLength = totalLength_susp;
		else if (basedOn.equals("SOURCE"))
			totalLength = totalLength_src;
		else
			totalLength = totalLength_src + totalLength_susp;

		return totalLength;
	}

	// public Double computeMacroAveragedPrecision(String basedOn)
	// throws CloneNotSupportedException {
	// Double macroPrec = 0D;
	// Double sumOfLocalPrec = 0D;
	// Integer countDetections = 0;
	// Long totalTrueDetectionLength = 0L;
	// for (String suspFile : detections.keySet()) {
	// if (cases.containsKey(suspFile)) {
	// for (String srcFile : detections.get(suspFile).keySet()) {
	// if (cases.get(suspFile).containsKey(srcFile)) {
	// Integer caseCount = 1;
	// for (PlagiarismCase detectedPlagiarismCase : detections
	// .get(suspFile).get(srcFile)) {
	// countDetections++;
	// Long truelyDetectedLength = 0L;
	// List<Pair<PlagiarismCase, PlagiarismCase>>
	// realPlagiarismCasesWithDetectedOverlaps = new
	// ArrayList<Pair<PlagiarismCase, PlagiarismCase>>();
	// for (PlagiarismCase realPlagiarismCase : cases.get(
	// suspFile).get(srcFile)) {
	// PlagiarismCase trulyDetectedPlagiarismCase = detectedPlagiarismCase
	// .overlap(realPlagiarismCase);
	// if (trulyDetectedPlagiarismCase != null) {
	// if (basedOn.equals("BOTH"))
	// truelyDetectedLength += Long
	// .parseLong(trulyDetectedPlagiarismCase
	// .getFeature("this_length"))
	// + Long.parseLong(trulyDetectedPlagiarismCase
	// .getFeature("source_length"));
	// else if (basedOn.equals("SOURCE"))
	// truelyDetectedLength += Long
	// .parseLong(trulyDetectedPlagiarismCase
	// .getFeature("source_length"));
	// else if (basedOn.equals("SUSP"))
	// truelyDetectedLength += Long
	// .parseLong(trulyDetectedPlagiarismCase
	// .getFeature("this_length"));
	// else
	// truelyDetectedLength += Long
	// .parseLong(trulyDetectedPlagiarismCase
	// .getFeature("this_length"))
	// + Long.parseLong(trulyDetectedPlagiarismCase
	// .getFeature("source_length"));
	//
	// totalTrueDetectionLength += truelyDetectedLength;
	// realPlagiarismCasesWithDetectedOverlaps
	// .add(new Pair<PlagiarismCase, PlagiarismCase>(
	// realPlagiarismCase,
	// trulyDetectedPlagiarismCase));
	// }
	//
	// }
	// Double localPrec = 0D;
	// if (basedOn.equals("BOTH"))
	// localPrec += (double) truelyDetectedLength
	// / (double) (Long
	// .parseLong(detectedPlagiarismCase
	// .getFeature("this_length")) + Long
	// .parseLong(detectedPlagiarismCase
	// .getFeature("source_length")));
	// else if (basedOn.equals("SOURCE"))
	// localPrec += (double) truelyDetectedLength
	// / (double) (Long
	// .parseLong(detectedPlagiarismCase
	// .getFeature("source_length")));
	// else if (basedOn.equals("SUSP"))
	// localPrec += (double) truelyDetectedLength
	// / (double) (Long
	// .parseLong(detectedPlagiarismCase
	// .getFeature("this_length")));
	// else
	// localPrec += (double) truelyDetectedLength
	// / (double) (Long
	// .parseLong(detectedPlagiarismCase
	// .getFeature("this_length")) + Long
	// .parseLong(detectedPlagiarismCase
	// .getFeature("source_length")));
	//
	// sumOfLocalPrec += localPrec;
	// caseCount++;
	// localPrecisions
	// .add(new Pair<Pair<PlagiarismCase, List<Pair<PlagiarismCase,
	// PlagiarismCase>>>, Double>(
	// new Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>(
	// detectedPlagiarismCase,
	// realPlagiarismCasesWithDetectedOverlaps),
	// localPrec));
	//
	// }
	// }
	// }
	// }
	// }
	//
	// macroPrec = (double) sumOfLocalPrec / (double) countDetections;//
	// (double)localPrecisions.size();
	// return macroPrec;
	// }

	public Double computeMacroAveragedRecall(String basedOn)
			throws CloneNotSupportedException {
		Double macroRecall = 0D;
		Double sumOfLocalRecalls = 0D;
		Long countCases = 0L;
		for (String suspFile : cases.keySet()) {
			if (detections.containsKey(suspFile)) {
				for (String srcFile : cases.get(suspFile).keySet()) {
					if (detections.get(suspFile).containsKey(srcFile)) {
						for (PlagiarismCase realPlagiarismCase : cases.get(
								suspFile).get(srcFile)) {
							countCases++;
							Map<String, Map<String, List<PlagiarismCase>>> srcBasedMap = new HashMap<String, Map<String, List<PlagiarismCase>>>();
							Map<String, Map<String, List<PlagiarismCase>>> suspBasedMap = new HashMap<String, Map<String, List<PlagiarismCase>>>();
							suspBasedMap
									.put(suspFile,
											new HashMap<String, List<PlagiarismCase>>());
							List<Pair<PlagiarismCase, PlagiarismCase>> DetectedPlagiarismCasesWithTrueOverlaps = new ArrayList<Pair<PlagiarismCase, PlagiarismCase>>();
							for (PlagiarismCase detectedPlagiarismCase : detections
									.get(suspFile).get(srcFile)) {
								PlagiarismCase trulyDetectedPlagiarismCase = realPlagiarismCase
										.overlap(detectedPlagiarismCase);

								if (trulyDetectedPlagiarismCase != null) {
									if (!srcBasedMap.containsKey(srcFile)) {
										srcBasedMap
												.put(srcFile,
														new HashMap<String, List<PlagiarismCase>>());
										srcBasedMap
												.get(srcFile)
												.put(suspFile,
														new ArrayList<PlagiarismCase>());
									}
									srcBasedMap.get(srcFile).get(suspFile)
											.add(trulyDetectedPlagiarismCase);

									if (!suspBasedMap.get(suspFile)
											.containsKey(srcFile)) {
										suspBasedMap
												.get(suspFile)
												.put(srcFile,
														new ArrayList<PlagiarismCase>());
									}
									suspBasedMap.get(suspFile).get(srcFile)
											.add(trulyDetectedPlagiarismCase);
									DetectedPlagiarismCasesWithTrueOverlaps
											.add(new Pair<PlagiarismCase, PlagiarismCase>(
													detectedPlagiarismCase,
													trulyDetectedPlagiarismCase));
								}

							}
							Long truelyDetectedLength = computeDetailsCasesLength(
									suspBasedMap, srcBasedMap, basedOn);

							Double localRec = 0D;

							if (basedOn.equals("BOTH")) {
								localRec = (double) (truelyDetectedLength)
										/ (double) (Long
												.parseLong(realPlagiarismCase
														.getFeature("this_length")) + Long
												.parseLong(realPlagiarismCase
														.getFeature("source_length")));
							} else if (basedOn.equals("SUSP")) {
								localRec = (double) (truelyDetectedLength)
										/ (double) (Long
												.parseLong(realPlagiarismCase
														.getFeature("this_length")));
							} else if (basedOn.equals("SOURCE")) {
								localRec = (double) (truelyDetectedLength)
										/ (double) (Long
												.parseLong(realPlagiarismCase
														.getFeature("source_length")));
							} else {
								localRec = (double) (truelyDetectedLength)
										/ (double) (Long
												.parseLong(realPlagiarismCase
														.getFeature("this_length")) + Long
												.parseLong(realPlagiarismCase
														.getFeature("source_length")));
							}
							if (localRec.isNaN()) {
								System.out.println("local recal is NaN!");
							}
							sumOfLocalRecalls += localRec;
							localRecalls
									.add(new Pair<Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>, Double>(
											new Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>(
													realPlagiarismCase,
													DetectedPlagiarismCasesWithTrueOverlaps),
											localRec));
						}
					}
				}
			}
		}

		if (CaseCount == 0) {
			return 1D;
		}
		macroRecall = (double) sumOfLocalRecalls / (double) CaseCount;// (double)localPrecisions.size();
		return macroRecall;
	}

	public Double computeMacroAveragedPrecision(String basedOn)
			throws CloneNotSupportedException {
		Double macroPrec = 0D;
		Double sumOfLocalPrecs = 0D;
		Long countCases = 0L;
		for (String suspFile : detections.keySet()) {
			if (cases.containsKey(suspFile)) {
				for (String srcFile : detections.get(suspFile).keySet()) {
					if (cases.get(suspFile).containsKey(srcFile)) {
						for (PlagiarismCase detectedPlagiarismCase : detections
								.get(suspFile).get(srcFile)) {
							countCases++;
							Map<String, Map<String, List<PlagiarismCase>>> srcBasedMap = new HashMap<String, Map<String, List<PlagiarismCase>>>();
							Map<String, Map<String, List<PlagiarismCase>>> suspBasedMap = new HashMap<String, Map<String, List<PlagiarismCase>>>();
							suspBasedMap
									.put(suspFile,
											new HashMap<String, List<PlagiarismCase>>());
							List<Pair<PlagiarismCase, PlagiarismCase>> truelyDetectedPlagiarismCases = new ArrayList<Pair<PlagiarismCase, PlagiarismCase>>();
							for (PlagiarismCase realPlagiarismCase : cases.get(
									suspFile).get(srcFile)) {
								PlagiarismCase trulyDetectedPlagiarismCase = detectedPlagiarismCase
										.overlap(realPlagiarismCase);

								if (trulyDetectedPlagiarismCase != null) {
									if (!srcBasedMap.containsKey(srcFile)) {
										srcBasedMap
												.put(srcFile,
														new HashMap<String, List<PlagiarismCase>>());
										srcBasedMap
												.get(srcFile)
												.put(suspFile,
														new ArrayList<PlagiarismCase>());
									}
									srcBasedMap.get(srcFile).get(suspFile)
											.add(trulyDetectedPlagiarismCase);

									if (!suspBasedMap.get(suspFile)
											.containsKey(srcFile)) {
										suspBasedMap
												.get(suspFile)
												.put(srcFile,
														new ArrayList<PlagiarismCase>());
									}
									suspBasedMap.get(suspFile).get(srcFile)
											.add(trulyDetectedPlagiarismCase);
									truelyDetectedPlagiarismCases
											.add(new Pair<PlagiarismCase, PlagiarismCase>(
													detectedPlagiarismCase,
													trulyDetectedPlagiarismCase));
								}

							}
							Long truelyDetectedLength = computeDetailsCasesLength(
									suspBasedMap, srcBasedMap, basedOn);

							Double localPrec = 0D;
							if (basedOn.equals("BOTH")) {
								localPrec = (double) (truelyDetectedLength)
										/ (double) (Long
												.parseLong(detectedPlagiarismCase
														.getFeature("this_length")) + Long
												.parseLong(detectedPlagiarismCase
														.getFeature("source_length")));
							} else if (basedOn.equals("SUSP")) {
								localPrec = (double) (truelyDetectedLength)
										/ (double) (Long
												.parseLong(detectedPlagiarismCase
														.getFeature("this_length")));
							} else if (basedOn.equals("SOURCE")) {
								localPrec = (double) (truelyDetectedLength)
										/ (double) (Long
												.parseLong(detectedPlagiarismCase
														.getFeature("source_length")));
							} else {
								localPrec = (double) (truelyDetectedLength)
										/ (double) (Long
												.parseLong(detectedPlagiarismCase
														.getFeature("this_length")) + Long
												.parseLong(detectedPlagiarismCase
														.getFeature("source_length")));
							}
							if (localPrec.isNaN()) {
								System.out.println("local prec is NaN!");
							}
							sumOfLocalPrecs += localPrec;
							localPrecisions
									.add(new Pair<Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>, Double>(
											new Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>(
													detectedPlagiarismCase,
													truelyDetectedPlagiarismCases),
											localPrec));
						}
					}
				}
			}
		}

		if (DetectionsCount == 0) {
			if (CaseCount == 0)
				return 1D;
			else
				return 0D;
		}
		macroPrec = (double) sumOfLocalPrecs / (double) DetectionsCount;// (double)localPrecisions.size();
		return macroPrec;
	}

	public Double computeGranularity() throws CloneNotSupportedException {
		Long trueDetections = 0L;
		Long detectedCases = 0L;
		NotDetectedCases = new HashMap<String, Map<String, List<PlagiarismCase>>>();
		SplitlyDetectedCases = new ArrayList<Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>>();

		for (String suspFile : cases.keySet()) {
			NotDetectedCases.put(suspFile,
					new HashMap<String, List<PlagiarismCase>>());
			for (String srcFile : cases.get(suspFile).keySet()) {
				NotDetectedCases.get(suspFile).put(srcFile,
						new ArrayList<PlagiarismCase>());
				NotDetectedCases.get(suspFile).get(srcFile)
						.addAll(cases.get(suspFile).get(srcFile));

			}
		}
		for (String suspFile : cases.keySet()) {
			if (detections.containsKey(suspFile)) {
				for (String srcFile : cases.get(suspFile).keySet()) {
					if (detections.get(suspFile).containsKey(srcFile)) {
						for (PlagiarismCase realPlagiarismCase : cases.get(
								suspFile).get(srcFile)) {
							Boolean detected = false;
							List<Pair<PlagiarismCase, PlagiarismCase>> pairedDetectionsList = new ArrayList<Pair<PlagiarismCase, PlagiarismCase>>();
							Long detectionsPerCase = 0L;
							for (PlagiarismCase detectedPlagiarismCase : detections
									.get(suspFile).get(srcFile)) {
								PlagiarismCase trulyDetectedPlagiarismCase = realPlagiarismCase
										.overlap(detectedPlagiarismCase);
								if (trulyDetectedPlagiarismCase != null) {
									detectionsPerCase++;
									detected = true;
									pairedDetectionsList
											.add(new Pair<PlagiarismCase, PlagiarismCase>(
													trulyDetectedPlagiarismCase,
													detectedPlagiarismCase));
								}

							}

							if (detected) {
								detectedCases += 1;
								NotDetectedCases.get(suspFile).get(srcFile)
										.remove(realPlagiarismCase);
							}
							trueDetections += detectionsPerCase;

							if (detectionsPerCase > 1) {

								SplitlyDetectedCases
										.add(new Pair<PlagiarismCase, List<Pair<PlagiarismCase, PlagiarismCase>>>(
												realPlagiarismCase,
												pairedDetectionsList));
							}

						}
					}
				}
			}
		}
		completely_not_detected_count = CaseCount - detectedCases;
		Double granularity = (double) trueDetections / (double) detectedCases;

		return granularity;
	}

	public double computePlagdetScore(Double prec, Double rec, Double gran) {
		Double plagdetScore = 0D;

		Double Falpha = computeF_ALPHA(prec, rec);

		plagdetScore = Falpha / (Math.log(1 + gran) / Math.log(2));
		return plagdetScore;
	}

	private Double computeF_ALPHA(Double prec, Double rec) {
		return (1 + F_MEATURE_ALPHA) * ((prec * rec) / (prec + rec));
	}

}
