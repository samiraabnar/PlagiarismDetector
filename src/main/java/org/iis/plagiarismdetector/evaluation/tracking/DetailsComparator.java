package org.iis.plagiarismdetector.evaluation.tracking;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.iis.plagiarismdetector.settings.TextAlignmentDatasetSettings;
import org.iis.plagiarismdetector.tools.DetailsReader;
import org.iis.plagiarismdetector.core.PlagiarismCase;
import org.iis.plagiarismdetector.core.TextProcessor;

public class DetailsComparator {

	private static final String eol = System.getProperty("line.separator");

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		DetailsComparator dc = new DetailsComparator();
		BufferedReader br;
		try {

			br = new BufferedReader(new FileReader(new File(
					TextAlignmentDatasetSettings.PAIRS_FILE_ADDR)));

			String line = null;
			while ((line = br.readLine()) != null) {
				String[] pair = line.split(" ");
				String susp = pair[0].trim().substring(0,
						pair[0].trim().indexOf("."));
				String src = pair[1].trim().substring(0,
						pair[1].trim().indexOf("."));
				dc.getCasesContent(TextAlignmentDatasetSettings.RESULTS_DIR,
						susp + "-" + src + ".xml");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void validate_no_obfuscation_folder() throws IOException,
			FactoryConfigurationError, XMLStreamException {
		BufferedReader br;
		br = new BufferedReader(new FileReader(new File(
				TextAlignmentDatasetSettings.PAIRS_FILE_ADDR)));

		String line = null;
		while ((line = br.readLine()) != null) {
			String[] pair = line.split(" ");
			String susp = pair[0].trim().substring(0,
					pair[0].trim().lastIndexOf("."));
			String src = pair[1].trim().substring(0,
					pair[1].trim().lastIndexOf("."));
			List<PlagiarismCase> cases = getCasesContent(
					TextAlignmentDatasetSettings.GOLD_RESULTS_DIR, susp + "-"
							+ src + ".xml");

			for (PlagiarismCase pcase : cases) {
				if (pcase.getPlagiarizedText() == null) {
					System.out
							.println("Susp Text is Null Error in Details Files!");
					System.out.println(pcase);
				}

				if (pcase.getSourceText() == null) {
					System.out
							.println("Source Text is Null Error in Details Files!");
					System.out.println(pcase);
				}

				if (!pcase.getSourceText().equals(pcase.getPlagiarizedText())) {
					System.out.println("Error in Details Files!");
					System.out.println(pcase);
				}
				if (Long.parseLong(pcase.getFeature("this_length").toString()) == 0) {
					System.out.println("Zero Length Case!");
					System.out.println(pcase);
				}
			}
		}
		br.close();

	}

	public List<PlagiarismCase> getCasesContent(String detailsFilePath,
			String detailsFileName) throws FactoryConfigurationError,
			XMLStreamException {

		DetailsReader dreader = new DetailsReader();
		List<PlagiarismCase> cases = dreader.readDetailsFile(null, new File(
				detailsFilePath + detailsFileName));

		for (PlagiarismCase pcase : cases) {
			String srcDoc = pcase.getFeature("source_reference");
			String suspDoc = pcase.getSuspDocument();

			String srcFileText = TextProcessor.getMatn(new File(
					TextAlignmentDatasetSettings.SOURCE_FILES_DIR + srcDoc));
			String suspFileText = TextProcessor.getMatn(new File(
					TextAlignmentDatasetSettings.SUSP_FILES_DIR + suspDoc));

			String srcText = "";
			String plagiarizedText = "";
			try {
				srcText = srcFileText.substring(
						Integer.parseInt(pcase.getFeature("source_offset")),
						Integer.parseInt(pcase.getFeature("source_offset"))
								+ Integer.parseInt(pcase
										.getFeature("source_length")));
				plagiarizedText = suspFileText.substring(
						Integer.parseInt(pcase.getFeature("this_offset")),
						Integer.parseInt(pcase.getFeature("this_offset"))
								+ Integer.parseInt(pcase
										.getFeature("this_length")));

			} catch (Exception e) {

				e.printStackTrace();
				System.out.println("SourceTextLength: " + srcFileText.length()
						+ "\n" + "SuspTextLength: " + suspFileText.length()
						+ "\n" + pcase);
				System.exit(0);
			}
			pcase.setSourceText(srcText);
			pcase.setPlagiarizedText(plagiarizedText);

			// System.out.println(pcase);

		}

		return cases;

	}

	public String getSuspFileName(String detailsDir, String name)
			throws XMLStreamException {
		DetailsReader dreader = new DetailsReader();
		return dreader.getSuspFileName(detailsDir, name);
	}

	public String getSrcFileName(String suspFileName, String name) {
		return name
				.replace(
						suspFileName.substring(0, suspFileName.indexOf("."))
								+ "-", "")
				.replace(".xml", TextAlignmentDatasetSettings.DOCUMENTS_POSTFIX);
	}

}
