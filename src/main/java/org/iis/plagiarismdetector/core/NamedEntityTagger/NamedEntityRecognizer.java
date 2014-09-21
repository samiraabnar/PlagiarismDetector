/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.iis.plagiarismdetector.core.NamedEntityTagger;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.iis.plagiarismdetector.settings.TextAlignmentDatasetSettings;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

/**
 * 
 ** **
 * 
 * @author Mostafa Dehghani
 * 
 **/
public class NamedEntityRecognizer {

	public static String Classifieradd[] = {
			"stanford-ner/classifiers/english.all.3class.distsim.crf.ser.gz",
			"stanford-ner/classifiers/english.conll.4class.distsim.crf.ser.gz",
			"stanford-ner/classifiers/english.muc.7class.distsim.crf.ser.gz" };
	public static final AbstractSequenceClassifier<CoreLabel> Classifier[] = new AbstractSequenceClassifier[Classifieradd.length];

	static {
		for (int i = 0; i < Classifieradd.length; i++) {
			Classifier[i] = CRFClassifier
					.getClassifierNoExceptions(TextAlignmentDatasetSettings.JAR_FILE_PATH
							+ Classifieradd[i]);
		}
	}

	public static List<Entry<String, String>> NER(String Input)
			throws IOException {
		List<Entry<String, String>> Output = new ArrayList<Entry<String, String>>();
		for (int i = 0; i < Classifier.length; i++) {
			String Text_NER;
			try {
				Text_NER = Classifier[i].classifyWithInlineXML(Input);
				// System.out.println(Classifier[1].classifyWithInlineXML(Input));
				// System.out.println(Classifier[1].classifyToString(Input,
				// "xml", true));
				// System.out.println(Classifier[1].classifyToString(Input));
			} catch (Exception e) {
				continue;
			}
			Matcher m = Pattern.compile(
					"<([A-Za-z0-9]+?)>(.*?)<(/[A-Za-z0-9]+?)>").matcher(
					Text_NER);
			while (m.find()) {
				Entry<String, String> entry = new AbstractMap.SimpleEntry<String, String>(
						m.group(2), m.group(1));
				Output.add(entry);
			}
		}
		return Output;
	}
}
