package org.iis.plagiarismdetector.core.wordnet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.util.BytesRef;

import com.sun.tools.javac.util.Pair;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;
import java.util.HashSet;
import java.util.Set;
import org.iis.plagiarismdetector.core.lucene.IndexInfo;

/**
 * Displays word forms and definitions for synsets containing the word form
 * specified on the command line. To use this application, specify the word form
 * that you wish to view synsets for, as in the following example which displays
 * all synsets containing the word form "airplane": <br>
 * java TestJAWS airplane
 */
public class WordNetUtil {

	private WordNetDatabase database = WordNetDatabase.getFileInstance();

	public WordNetUtil() {
		System.setProperty("wordnet.database.dir",
				"../PlagiarismDetection/WordNet-3.0/dict/");
	}

	public void testJAWS(String[] args) {

		String wordForm = "Spring";
		// Get the synsets containing the wrod form
		Synset[] synsets = database.getSynsets(wordForm);
		// Display the word forms and definitions for synsets retrieved
		if (synsets.length > 0) {
			System.out.println("The following synsets contain '" + wordForm
					+ "' or a possible base form " + "of that text:");
			for (int i = 0; i < synsets.length; i++) {
				System.out.println("");
				String[] wordForms = synsets[i].getWordForms();

				for (int j = 0; j < wordForms.length; j++) {
					System.out.print((j > 0 ? ", " : "") + wordForms[j]);
				}
				System.out.println(": " + synsets[i].getDefinition());
			}
		} else {
			System.err.println("No synsets exist that contain "
					+ "the word form '" + wordForm + "'");
		}

	}

	public Pair<String, Double> getWordMostFrequentSense(String word,
			String tag, IndexInfo bgIndxInfo) {
		List<Pair<String, Double>> senseFrequencies = new ArrayList<Pair<String, Double>>();

		Synset[] synsets = database.getSynsets(word);
		for (int i = 0; i < synsets.length; i++) {
			for (String wordForm : synsets[i].getWordForms()) {
				Double DF = bgIndxInfo.getDF("TEXT", new BytesRef(wordForm))
						.doubleValue();
				if (DF > 0) {
					senseFrequencies.add(new Pair<String, Double>(wordForm,
							bgIndxInfo.getTotalTF_PerField("TEXT",
									new BytesRef(wordForm)).doubleValue()
									/ DF));
				}
			}

		}
		Collections.sort(senseFrequencies,
				new Comparator<Pair<String, Double>>() {

					@Override
					public int compare(Pair<String, Double> o1,
							Pair<String, Double> o2) {
						// TODO Auto-generated method stub
						return o1.snd.compareTo(o2.snd);
					}
				});

		return senseFrequencies.size() > 0 ? senseFrequencies.get(0) : null;

	}

	public List<Pair<String, Double>> getAllWordSenses(String word, String tag,
			IndexInfo bgIndxInfo) {
		List<Pair<String, Double>> senseFrequencies = new ArrayList<Pair<String, Double>>();

		Synset[] synsets = database.getSynsets(word);
		for (int i = 0; i < synsets.length; i++) {
			for (String wordForm : synsets[i].getWordForms()) {
				Double DF = bgIndxInfo.getDF("TEXT", new BytesRef(wordForm))
						.doubleValue();
				if ((DF > 0) && (!word.equals(wordForm))) {
					senseFrequencies.add(new Pair<String, Double>(wordForm,
							bgIndxInfo.getTotalTF_PerField("TEXT",
									new BytesRef(wordForm)).doubleValue()
									/ DF));
				}
			}

		}

		return senseFrequencies;
	}

    

    public Set<Long> getAllSynsets(String word) {
                Synset[] synsets = database.getSynsets(word);
            HashSet<Long> synsetIdz = new HashSet<Long>();
		for (int i = 0; i < synsets.length; i++) {
			synsetIdz.add((long)synsets[i].hashCode());

		}

		return synsetIdz;    
    }
}