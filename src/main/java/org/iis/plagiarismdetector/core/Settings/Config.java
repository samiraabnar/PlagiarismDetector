/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iis.plagiarismdetector.core.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 
 * @author Mosi
 */
public class Config {
	public static Properties configFile = new Properties();
	static {
		try {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			InputStream stream = loader
					.getResourceAsStream("ir/ac/ut/iscisc/Settings/Config.properties");
			configFile.load(stream);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
