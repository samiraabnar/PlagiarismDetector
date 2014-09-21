package org.iis.plagiarismdetector.textalignment;

import java.io.File;
import java.io.FilenameFilter;

/**
 * FilenameFilter for pairs-Files.
 */
public class Pairfilter implements FilenameFilter {
	@Override
	public boolean accept(File dir, String name) {
		if(name.equalsIgnoreCase("pairs")) {
			return true;
		}
		return false;
	}
}