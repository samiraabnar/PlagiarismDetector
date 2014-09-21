///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package org.iis.plagiarismdetector.core.NamedEntityTagger;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
///**
// *
// * @author mosi
// */
//public class Annotator {
//	private double CONFIDENCE;
//	private DBPediaSpootlightAnnotator DBPS;
//	private Map<String, Set<Candidate>> annotation;
//
//	public Annotator(double confidence) {
//		this.CONFIDENCE = confidence;
//		DBPS = new DBPediaSpootlightAnnotator(confidence);
//	}
//
//	public List<String> anotateText(String inText) {
//		List<String> annotation = new ArrayList<String>();
//		try {
//			annotation = DBPS.evaluate(inText);
//		} catch (Exception ex) {
//			Logger.getLogger(Annotator.class.getName()).log(Level.SEVERE, null,
//					ex);
//		}
//		return annotation;
//	}
//
//	public static void main(String[] args) throws IOException, Exception {
//		Annotator ann = new Annotator(0.0);
//		String text = "Samira is in Amsterdam! Samira is sad. Samira is living with Mostafa.";
//		System.out.println(text);
//		List<String> annotation = ann.anotateText(text);
//		for (String e : annotation) {
//			System.out.println("----------------");
//			System.out.println(e);
//
//		}
//	}
//
//	public List<String> getNamedEntities(String text) {
//		List<String> results = new ArrayList<String>();
//		List<String> annotation = anotateText(text);
//		for (String e : annotation) {
//			results.add(e);
//		}
//
//		return results;
//	}
//
//}
