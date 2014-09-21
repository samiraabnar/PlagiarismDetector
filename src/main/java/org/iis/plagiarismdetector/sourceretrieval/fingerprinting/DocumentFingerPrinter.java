package org.iis.plagiarismdetector.sourceretrieval.fingerprinting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import org.iis.plagiarismdetector.core.sourceretrieval.irengine.LuceneIndex;

public class DocumentFingerPrinter {

	private static final String SOURCE_DIR = null;
	private static final String SUSP_DIR = null;
	private static final String FINGERPRINTED_SOURCE_DIR = null;
	private static final String FINGERPRINTED_SUSP_DIR = null;
	private static final String LANG = null;
	private static final String FINGERPRINTED_SOURCE_INDEX = null;
	Integer k_threshold = 10;
	Integer window_size = 100;
	Integer Base = 8;

	Map<Long, List<String>> documentFingerPrint;
	Pattern pattern = Pattern.compile("[^\\p{L}]");

	public DocumentFingerPrinter() {
		documentFingerPrint = new HashMap<Long, List<String>>();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DocumentFingerPrinter documentFingerPrinter = new DocumentFingerPrinter();

		try {
			boolean ifCreateFingerprints = true;
			if (ifCreateFingerprints) {
				File fsource = new File(FINGERPRINTED_SOURCE_DIR);
				if (fsource.exists())
					fsource.delete();
				fsource.createNewFile();

				File fsusp = new File(FINGERPRINTED_SUSP_DIR);
				if (fsusp.exists())
					fsusp.delete();
				fsusp.createNewFile();

				documentFingerPrinter.readFiles(SOURCE_DIR,
						FINGERPRINTED_SOURCE_DIR);
				documentFingerPrinter.readFiles(SUSP_DIR,
						FINGERPRINTED_SUSP_DIR);
			}

			File fngindpath = new File(FINGERPRINTED_SOURCE_INDEX);
			if (fngindpath.exists())
				fngindpath.delete();
			fngindpath.createNewFile();

			try {
				LuceneIndex fIndex = new LuceneIndex(SOURCE_DIR, LANG,
						FINGERPRINTED_SOURCE_INDEX, false, false);
			} catch (SQLException e) {
				e.printStackTrace();
			}

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void readFiles(String folderName, String newDirPath)
			throws ParserConfigurationException, SAXException, IOException {
		File dir = new File(folderName);
		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".txt");
			}
		});

		for (File file : files) {
			fetchDocuments(file, newDirPath);
		}

	}

	public void fetchDocuments(File file, String newDirPath)
			throws ParserConfigurationException, SAXException, IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(file), "UTF8"));

		String str;

		String fileString = "";
		while ((str = in.readLine()) != null) {
			fileString += str + "\n";
		}
		in.close();
		fingerprintingComputation(fileString, file.getName(), newDirPath);
	}

	public void fingerprintingComputation(String text, String fileName,
			String newDirPath) throws IOException {
		documentFingerPrint.clear();

		for (int i = 0; i < (text.length() - window_size); i = i + window_size)
			windowKGramsHashList(text.substring(i, i + window_size), fileName);
		printHashKeys(fileName, newDirPath);
	}

	public void windowKGramsHashList(String windowText, String docName) {
		List<Long> hashList = new ArrayList<Long>();
		long lastHash = 0L;
		long minHash = 0L;
		// String minHashKGram = "";

		for (int i = 0; i < (windowText.length() - k_threshold); i++) {
			if (i > 0)
				lastHash = getHash(lastHash, windowText.charAt(i - 1),
						windowText.charAt(i + k_threshold - 1));
			else {
				lastHash = 0L;
				for (int k = 0; k < k_threshold; k++)
					lastHash = lastHash * Base
							+ (windowText.charAt(i + k) % Base) * Base;

				minHash = lastHash;
				// minHashKGram = windowText.substring(i,i+k_threshold);
			}

			if (lastHash < minHash) {
				minHash = lastHash;
				// minHashKGram = windowText.substring(i,i+k_threshold);
			}
			// System.out.println("K-gram: "+windowText.substring(i,i+k_threshold)+" Hash: "+lastHash);
			hashList.add(new Long(lastHash));
		}

		/*
		 * HashInfo hashInfo = new HashInfo();
		 * hashInfo.setDocumentId(documentId); hashInfo.setKgram(minHashKGram);
		 */

		if (!documentFingerPrint.containsKey(minHash))
			documentFingerPrint.put(minHash, new ArrayList<String>());
		documentFingerPrint.get(minHash).add(docName);
	}

	public Long getHash(Long lastHash, Character lastChar, Character newChar) {
		Long hashValue = 0L;
		hashValue = (long) (((lastHash - (lastChar.charValue() % Base)
				* Math.pow(Base, k_threshold)) + (newChar % Base)) * Base);

		return hashValue;
	}

	public void printHashKeys(String docName, String newPathDir)
			throws IOException {
		File outFile = new File(newPathDir + "/" + docName);

		FileOutputStream out = new FileOutputStream(outFile);
		String writeString = "";
		for (Long key : documentFingerPrint.keySet()) {
			writeString += key + " ";
		}
		out.write(writeString.getBytes());
		out.close();
	}

}
