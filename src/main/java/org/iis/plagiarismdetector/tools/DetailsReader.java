package org.iis.plagiarismdetector.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;


import org.iis.plagiarismdetector.core.PlagiarismCase;
import org.iis.plagiarismdetector.core.TextProcessor;

public class DetailsReader {

	public static final String detailsPath = "/Users/MacBookPro/Documents/Uni-MS/FinalProject/Code/artificial_plagiarism/dataset/details-replace/";

	public static void main(String[] args) throws XMLStreamException {
		DetailsReader dreader = new DetailsReader();
		File dir = new File(detailsPath);
		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return !name.startsWith(".") && name.endsWith(".xml");
			}
		});
		HashMap<String, List<PlagiarismCase>> caseMap = new HashMap<String, List<PlagiarismCase>>();
		for (File file : files) {
			dreader.readDetailsFile(caseMap, file);

		}

		System.out.println("Number of Cases:"
				+ dreader.numberOfCases(caseMap.values()));
		System.out.println(dreader.averageOnFeature("obfuscaton_degree",
				caseMap.values()));
		dreader.convertToCSV(caseMap.values());
	}

	public List<PlagiarismCase> readDetailsFile(
			HashMap<String, List<PlagiarismCase>> caseMap, File file)
			throws FactoryConfigurationError, XMLStreamException {
		List<PlagiarismCase> caseList = new ArrayList<PlagiarismCase>();
		PlagiarismCase currentCase = new PlagiarismCase();
		String suspFile = "";

		XMLInputFactory factory = XMLInputFactory.newInstance();

		InputStream in = null;
		in = read(file.getPath());
		XMLEventReader reader = factory.createXMLEventReader(in);

		while (reader.hasNext()) {
			XMLEvent event = reader.nextEvent();
			if (event.isStartElement()) {
				String tagName = event.asStartElement().getName()
						.getLocalPart();
				if (tagName.equals("document")) {
					Attribute reference = event.asStartElement()
							.getAttributeByName(new QName("reference"));
					suspFile = reference.getValue();
				}
				if (tagName.equals("feature")) {
					Iterator itr = event.asStartElement().getAttributes();
					while (itr.hasNext()) {
						Attribute element = (Attribute) itr.next();
						currentCase.setFeature(element.getName().toString(),
								element.getValue());
					}
					currentCase.setSuspDocument(suspFile);
					currentCase.setFeature("this_reference", suspFile);

					if (currentCase.getFeature("this_length").equals(0)) {
						System.out.println("caseLength Is Zero");
					}
					caseList.add(currentCase);
					currentCase = new PlagiarismCase();
				}

			}

		}

		if (caseMap != null)
			caseMap.put(suspFile, caseList);

		/*
		 * if (caseList.size() == 0) { System.out.println(" Empty!" +
		 * file.getPath()); }
		 */
		return caseList;
	}

	private void convertToCSV(Collection<List<PlagiarismCase>> collection) {
		List<String> featurNames = new ArrayList<String>();
		StringBuilder csvString = new StringBuilder("");
		for (List<PlagiarismCase> pcases : collection) {
			for (PlagiarismCase pcase : pcases) {
				if (featurNames.isEmpty()) {
					featurNames.addAll(pcase.getFeatures().keySet());
					// Write Header
					for (String feature : featurNames)
						csvString.append(feature + " ");

					csvString.append("\n");
				}

				for (String feature : featurNames)
					csvString.append(pcase.getFeature(feature) + " ");
				csvString.append("\n");
			}
		}

		PrintWriter writer;
		try {
			writer = new PrintWriter("detailsCSV.csv", "UTF-8");
			writer.write(csvString.toString());
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	}

	private Integer numberOfCases(Collection<List<PlagiarismCase>> collection) {
		Integer count = 0;
		for (List<PlagiarismCase> pcases : collection) {
			count += pcases.size();
		}

		return count;
	}

	private Double averageOnFeature(String fname,
			Collection<List<PlagiarismCase>> collection) {
		Double value = 0D;
		double count = 0;
		for (List<PlagiarismCase> pcases : collection) {
			for (PlagiarismCase pcase : pcases) {
				value += Double.parseDouble(pcase.getFeature(fname));
				count++;
			}
		}

		return value / count;
	}

	

	private InputStream read(String url) {
		try {
			return new FileInputStream(url);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getFeatureSuspText(String suspFilesDir,
			PlagiarismCase basePlagiarismCase) {
		String suspFileText = TextProcessor.getMatn(new File(suspFilesDir));

		String plagiarizedText = basePlagiarismCase.getPlagiarizedText();
		if (plagiarizedText == null)
			plagiarizedText = suspFileText.substring(
					Integer.parseInt(basePlagiarismCase
							.getFeature("this_offset")),
					Integer.parseInt(basePlagiarismCase
							.getFeature("this_offset"))
							+ Integer.parseInt(basePlagiarismCase
									.getFeature("this_length")));

		return plagiarizedText;
	}

	public static String getFeatureSrcText(String srcFilesDir,
			PlagiarismCase basePlagiarismCase) {
		String srcFileText = TextProcessor.getMatn(new File(srcFilesDir));

		String srcText = null;
		if (srcText == null)
			srcText = srcFileText.substring(
					Integer.parseInt(basePlagiarismCase
							.getFeature("source_offset")),
					Integer.parseInt(basePlagiarismCase
							.getFeature("source_offset"))
							+ Integer.parseInt(basePlagiarismCase
									.getFeature("source_length")));

		return srcText;
	}

	public String getSuspFileName(String detailsDir, String name)
			throws XMLStreamException {
		List<PlagiarismCase> caseList = new ArrayList<PlagiarismCase>();
		PlagiarismCase currentCase = new PlagiarismCase();
		String suspFile = "";

		XMLInputFactory factory = XMLInputFactory.newInstance();

		InputStream in = null;
		in = read(detailsDir + name);
		XMLEventReader reader = factory.createXMLEventReader(in);

		while (reader.hasNext()) {
			XMLEvent event = reader.nextEvent();
			if (event.isStartElement()) {
				String tagName = event.asStartElement().getName()
						.getLocalPart();
				if (tagName.equals("document")) {
					Attribute reference = event.asStartElement()
							.getAttributeByName(new QName("reference"));
					suspFile = reference.getValue();
					break;
				}
			}
		}

		return suspFile;
	}
}
