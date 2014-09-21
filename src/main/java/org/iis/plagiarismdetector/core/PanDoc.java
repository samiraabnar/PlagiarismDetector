package org.iis.plagiarismdetector.core;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Simple class to handle the translation of the features into the proper XML format.
 * The is no need for a full fledge XML-solution so we resort to a simple StringBuilder.
 */
public class PanDoc {
	StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
	String srcRef;
	
	public PanDoc(String ref, String srcRef) {
		this.srcRef= srcRef;
		this.sb.append("<document reference=\"" + ref + "\">");		
	}
	
	public void addFeature(Feature f) {
		this.sb.append("<feature name=\"detected-plagiarism\" source_length=\"" + f.srcLength +
				"\" source_offset=\"" + f.srcOffset + 
				"\" source_reference=\"" + this.srcRef + 
				"\" this_length=\"" + f.getLength() + "\" this_offset=\"" + f.getOffset() + "\"/>");
	}
	
	public void write(String path) throws IOException {
		this.sb.append("</document>");
		FileWriter fw = new FileWriter(path);
		fw.write(sb.toString());
		fw.close();
	}
}