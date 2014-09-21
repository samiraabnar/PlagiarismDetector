/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.iis.plagiarismdetector.core.Settings;

import static org.iis.plagiarismdetector.core.Settings.Config.configFile;

import java.io.File;

import org.iis.plagiarismdetector.settings.TextAlignmentDatasetSettings;

/**
 * 
 * @author mosi
 */
public class FileManager {
	public static File enenDic;
	public static File stopwords;
	public static File index;

	static {
		index = new File(FileManager.class.getResource(
				configFile.getProperty("IndexPath")).getPath());
		enenDic = new File(FileManager.class.getResource(
				configFile.getProperty("Prob_lex_Parallel_en2en")).getPath());
		stopwords = new File(
				TextAlignmentDatasetSettings.stopwordsFiles
						.get(TextAlignmentDatasetSettings.LANGUAGE));
	}

}
