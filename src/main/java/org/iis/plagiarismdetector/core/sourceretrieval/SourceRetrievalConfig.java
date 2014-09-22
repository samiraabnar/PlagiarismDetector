package org.iis.plagiarismdetector.core.sourceretrieval;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.iis.plagiarismdetector.core.SimilarityFunction;
import org.iis.plagiarismdetector.settings.PARAMETER;


public class SourceRetrievalConfig {
	public static Properties configFile = new Properties();

	static {
		try {
			File cConf = new File("sourceRetrievalConfig.properties");
			System.out.println("Config File:"+ cConf.getAbsolutePath());
			InputStream stream = new FileInputStream(cConf);
			configFile.load(stream);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getJudgePath() {
		if (configFile.getProperty("phase").equals("test"))
			return configFile.getProperty("testJudgePath");
		else
			return configFile.getProperty("judgePath");
	}

	
	public static String getSrcIndexPath() {
		if (configFile.getProperty("phase").equals("test"))
			return configFile.getProperty("testSrcIndexPath");
		else
			return configFile.getProperty("srcIndexPath");
	}

	public static String getStopWordsPath() {
		if (getLanguage().equals("FA"))
			return configFile.getProperty("FA_stopword");
		else
			return configFile.getProperty("EN_stopword");
	}

	public static String getSuspIndexPath() {
		if (configFile.getProperty("phase").equals("test"))
			return configFile.getProperty("testSuspIndexPath");
		else
			return configFile.getProperty("suspIndexPath");
	}



	public static String getCorpusPath() {
		if (configFile.getProperty("phase").equals("test")) {
		
				return configFile.getProperty("testCorpusPath");
		
		} else {
			
				return configFile.getProperty("corpusPath");
		}
	}

	public static String getLanguage() {
		return configFile.getProperty("lang");
	}

	public static String getSuspMapPath() {
		if (configFile.getProperty("phase").equals("test"))
			return configFile.getProperty("testSuspDocMapPath");
		else
			return configFile.getProperty("suspDocMapPath");
	}

	public static String getSrcMapPath() {
		if (configFile.getProperty("phase").equals("test"))
			return configFile.getProperty("testSrcDocMapPath");
		else
			return configFile.getProperty("srcDocMapPath");
	}

	public static String getMapPath(String selector) {
		if (configFile.getProperty("phase").equals("test")) {
			if (selector.equals("src"))
				return configFile.getProperty("testSrcDocMapPath");
			else if (selector.equals("susp"))
				return configFile.getProperty("testSuspDocMapPath");
			else
				return configFile.getProperty("testSuspDocMapPath");
		} else {
			if (selector.equals("src"))
				return configFile.getProperty("srcDocMapPath");
			else if (selector.equals("susp"))
				return configFile.getProperty("suspDocMapPath");
			else
				return configFile.getProperty("suspDocMapPath");
		}
	}

	public static String getCandidatesMapPath() {
		if (configFile.getProperty("phase").equals("test"))
			return configFile.getProperty("testCandidatesMapPath");
		else
			return configFile.getProperty("candidatesMapPath");
	}

	public static String getPairsPath() {
		if (configFile.getProperty("phase").equals("test"))
			return configFile.getProperty("testPairsPath");
		else
			return configFile.getProperty("pairsPath");
	}

	public static Integer getK() {
		return  Integer.parseInt(configFile.getProperty("K"));
	}
	
	public static String getSuspCorpusPath()
	{
		if (configFile.getProperty("phase").equals("test")) {
			
			return configFile.getProperty("testSuspCorpusPath");
	
	} else {
		
			return configFile.getProperty("suspCorpusPath");
	}		
	}
	
	
	public static String getSrcCorpusPath()
	{
		if (configFile.getProperty("phase").equals("test")) {
			
			return configFile.getProperty("testSrcCorpusPath");
	
	} else {
		
			return configFile.getProperty("srcCorpusPath");
	}		
	}

	public static Boolean get(String string) {
		return Boolean.parseBoolean(configFile.getProperty(string));
	}

	public static String getSearchResultPath() {
		if (configFile.getProperty("phase").equals("test")) {
		return configFile.getProperty("testResultsFolderPath");
		}
		else
		{
			return configFile.getProperty("resultsFolderPath");
		}
	}

	public static SimilarityFunction getSimilarityFunction() {
		for(SimilarityFunction sf: SimilarityFunction.values())
		{
			if (configFile.getProperty("similarityFunction").equals(sf.toString())) {
				return sf;
			}
		}
		
		return SimilarityFunction.LMDirichlet;
	}
	
	public static String getCorpusMainDir()
	{
		return configFile.getProperty("corpusMainDir");
	}

	public static Float getParameter(PARAMETER param) {
		return Float.parseFloat(configFile.getProperty(param.toString()));
	}


	public static String getSrcTrainCorpusPath() {
		return configFile.getProperty("srcCorpusPath");

	}


	public static String getSrcTrainIndexPath() {
		return configFile.getProperty("srcIndexPath");
	}


	public static String getDictionaryPath() {
		return configFile.getProperty("dictionaryPath");
	}


	public static String getFeaturedIndexPath() {
		if (configFile.getProperty("phase").equals("test")) {
			return configFile.getProperty("testFeaturedIndexPath");
			}
			else
			{
				return configFile.getProperty("featuredIndexPath");
			}
	}
}
