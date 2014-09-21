package org.iis.plagiarismdetector.core.sourceretrieval;

import java.util.List;
import java.util.Map;

public class EvaluationSummary {
	
	private Double microPrecision;
	private Double microRecall;
	
	
	private Double macroPrecision;
	private Double macroRecall;
	
	public Double getMicroPrecision() {
		return microPrecision;
	}
	public void setMicroPrecision(Double microPrecision) {
		this.microPrecision = microPrecision;
	}
	public Double getMicroRecall() {
		return microRecall;
	}
	public void setMicroRecall(Double microRecall) {
		this.microRecall = microRecall;
	}
	public Double getMacroPrecision() {
		return macroPrecision;
	}
	public void setMacroPrecision(Double macroPrecision) {
		this.macroPrecision = macroPrecision;
	}
	public Double getMacroRecall() {
		return macroRecall;
	}
	public void setMacroRecall(Double macroRecall) {
		this.macroRecall = macroRecall;
	}

	private String ExperimentDescription;
	private String ExperimentNumber;
	
	private Map<String, List<QueryResult>> detailedResult;
	private Map<String, Object> experimentOptions;
	
	
	public String getExperimentDescription() {
		return ExperimentDescription;
	}
	public void setExperimentDescription(String experimentDescription) {
		ExperimentDescription = experimentDescription;
	}
	public String getExperimentNumber() {
		return ExperimentNumber;
	}
	public void setExperimentNumber(String experimentNumber) {
		ExperimentNumber = experimentNumber;
	}
	public Map<String, List<QueryResult>> getDetailedResult() {
		return detailedResult;
	}
	public void setDetailedResult(Map<String, List<QueryResult>> detailedResult) {
		this.detailedResult = detailedResult;
	}
	
	public Map<String, Object> getExperimentOptions() {
		return experimentOptions;
	}
	public void setExperimentOptions(Map<String, Object> experimentOptions) {
		this.experimentOptions = experimentOptions;
	}
	
	public String toString()
	{
		StringBuilder str = new StringBuilder("");
		str.append("Experiment No. "+ExperimentNumber+": "+ExperimentDescription+"\n");
		
		
		for(String optionsKey: experimentOptions.keySet())
		{
			str.append(optionsKey+"-> "+experimentOptions.get(optionsKey).toString()+"\n");
		}
		
		str.append("\n___________\n");
		
		str.append("Micro Averaged Precision: "+microPrecision+" "+"Micro Averaged Recall: "+microRecall+"\n");
		str.append("Macro Averaged Precision: "+macroPrecision+" "+"Macro Averaged Recall: "+macroRecall+"\n");
		
		return str.toString();
	}
	
}
