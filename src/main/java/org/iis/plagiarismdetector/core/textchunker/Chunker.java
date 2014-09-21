package org.iis.plagiarismdetector.core.textchunker;

import java.util.List;

public abstract class Chunker {
	abstract public List<String> chunk(String chunk);

	abstract public List<String> chunk(String chunk, String language);

}
