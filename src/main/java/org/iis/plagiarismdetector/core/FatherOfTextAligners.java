package org.iis.plagiarismdetector.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;

import org.iis.plagiarismdetector.settings.TextAlignmentDatasetSettings;
import org.iis.plagiarismdetector.textalignment.BaseLine;
import org.iis.plagiarismdetector.textalignment.Pairfilter;

public abstract class FatherOfTextAligners {

	public void readAndAlignPairs(String[] args) throws Exception {
		/*
		 * Process the commandline arguments. If there are two arguments we'll
		 * assume that those are a suspicious and a source document. If there is
		 * only one we have to decide if it points directly to a pairs file or
		 * to a directory. In the first case we break it down into pairs and
		 * compare them. In the latter case we scan the directory for all pairs
		 * file and proceed as before.
		 */
		if ((args == null) || (args.length == 0)) {
			File path = new File(TextAlignmentDatasetSettings.GOLD_RESULTS_DIR
					+ "pairs");
			ArrayList<File> pairs = new ArrayList<File>();

			if (path.isDirectory()) {

				pairs = BaseLine.walk(path, new Pairfilter());
			} else {
				pairs.add(path);
			}

			for (File p : pairs) {
				BufferedReader br = new BufferedReader(new FileReader(p));
				String line = null;
				while ((line = br.readLine()) != null) {
					String[] pair = line.split(" ");
					String susp = pair[0].trim();
					String src = pair[1].trim();
					alignTexts(TextAlignmentDatasetSettings.SUSP_FILES_DIR
							+ susp,
							TextAlignmentDatasetSettings.SOURCE_FILES_DIR + src);
				}
				br.close();
			}

			// evaluate();
		} else if (args.length == 2) {
			String susp = args[0];
			String src = args[1];
			alignTexts(TextAlignmentDatasetSettings.SUSP_FILES_DIR + susp,
					TextAlignmentDatasetSettings.SOURCE_FILES_DIR + src);
		} else {
			System.out.println("Unexpected number of commandline arguments.\n");
		}
	}

	private static void evaluate() throws IOException, InterruptedException {
		Runtime rt = Runtime.getRuntime();
		// System.out.println("Exited with error code "+exitVal);

		String[] paramsArray = {
				"/bin/sh",
				"-c",
				"python evaluations/pan_measures.py " + "--micro " + "-p "
						+ TextAlignmentDatasetSettings.GOLD_RESULTS_DIR + ""
						+ " -d " + TextAlignmentDatasetSettings.RESULTS_DIR
						+ " > " + TextAlignmentDatasetSettings.Evaluation_DIR };
		Process proc = rt.exec(paramsArray);// > HunTagger_Output.txt");
		BufferedReader input = new BufferedReader(new InputStreamReader(
				proc.getErrorStream()));

		String lline = null;

		while ((lline = input.readLine()) != null) {
			System.out.println(lline);
		}

		int exitVal = proc.waitFor();
	}

	public void alignTexts(String suspFileName, String srcFileName)
			throws Exception {

		try {
			ArrayList<Feature> features = computeFeatures(suspFileName,
					srcFileName);
			serializeFeatures(new File(suspFileName), new File(srcFileName),
					features);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	abstract protected ArrayList<Feature> computeFeatures(String suspFileName,
			String srcFileName) throws IOException, SQLException, Exception;

	protected void serializeFeatures(File susp, File src,
			ArrayList<Feature> features) throws IOException {
		String srcRef = src.getName();
		String suspRef = susp.getName();
		String srcID = srcRef.split("\\.")[0];
		srcID = srcID.replaceAll("-", "_");
		String suspID = suspRef.split("\\.")[0];

		PanDoc doc = new PanDoc(suspRef, srcRef);
		for (Feature f : features) {
			doc.addFeature(f);
		}
		doc.write(TextAlignmentDatasetSettings.RESULTS_DIR + suspID + "-"
				+ srcID + ".xml");
	}

	protected void serializeFeatures(File susp, File src,
			ArrayList<Feature> features, String resultDir)
			throws IOException {
		String srcRef = src.getName();
		String suspRef = susp.getName();
		String srcID = srcRef.split("\\.")[0];
		String suspID = suspRef.split("\\.")[0];

		PanDoc doc = new PanDoc(suspRef, srcRef);
		for (Feature f : features) {
			doc.addFeature(f);
		}
		doc.write(resultDir + "/" + suspID + "-" + srcID + ".xml");
	}
}
