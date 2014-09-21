//package org.iis.plagiarismdetector.core.NamedEntityTagger;
//
//import java.io.*;
//import java.net.URLEncoder;
//import java.text.ParseException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
//import org.apache.commons.httpclient.Header;
//import org.apache.commons.httpclient.HttpClient;
//import org.apache.commons.httpclient.HttpException;
//import org.apache.commons.httpclient.HttpMethod;
//import org.apache.commons.httpclient.HttpStatus;
//import org.apache.commons.httpclient.URI;
//import org.apache.commons.httpclient.URIException;
//import org.apache.commons.httpclient.methods.GetMethod;
//import org.apache.commons.httpclient.params.HttpMethodParams;
//import org.dbpedia.spotlight.exceptions.AnnotationException;
//import org.dbpedia.spotlight.model.DBpediaResource;
//import org.dbpedia.spotlight.model.Text;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.jsoup.Connection;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//
///**
// * @author Mosi
// */
//
//public class DBPediaSpootlightAnnotator {
//
//	private static String API_URL = "http://spotlight.dbpedia.org:80/";
//	private static double CONFIDENCE = 0.0;
//	private static int SUPPORT = 0;
//	private static String powered_by = "non";
//	private static String spotter = "CoOccurrenceBasedSelector";// "LingPipeSpotter"=Annotate
//																// all spots
//	// AtLeastOneNounSelector"=No verbs and adjs.
//	// "CoOccurrenceBasedSelector" =No 'common words'
//	// "NESpotter"=Only Per.,Org.,Loc.
//	private static String disambiguator = "Default";// Default
//													// ;Occurrences=Occurrence-centric;Document=Document-centric
//	private static String showScores = "yes";
//
//	private Map<String, String[]> dbpParsedInfo;
//
//	public DBPediaSpootlightAnnotator(double confidence) {
//		this.CONFIDENCE = confidence;
//	}
//
//	// Create an instance of HttpClient.
//	private static HttpClient client = new HttpClient();
//
//	public String request(HttpMethod method) throws AnnotationException {
//
//		String response = null;
//
//		// Provide custom retry handler is necessary
//		method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
//				new DefaultHttpMethodRetryHandler(3, false));
//
//		try {
//			// Execute the method.
//			int statusCode = client.executeMethod(method);
//
//			if (statusCode != HttpStatus.SC_OK) {
//				System.err.println("Method failed: " + method.getStatusLine());
//			}
//
//			// Read the response body.
//			byte[] responseBody = method.getResponseBody(); // TODO Going to
//															// buffer response
//															// body of large or
//															// unknown size.
//															// Using
//															// getResponseBodyAsStream
//															// instead is
//															// recommended.
//
//			// Deal with the response.
//			// Use caution: ensure correct character encoding and is not binary
//			// data
//			response = new String(responseBody);
//
//		} catch (HttpException e) {
//			System.err.println("Fatal protocol violation: " + e.getMessage());
//			throw new AnnotationException(
//					"Protocol error executing HTTP request.", e);
//		} catch (IOException e) {
//			System.err.println("Fatal transport error: " + e.getMessage());
//			System.err.println(method.getQueryString());
//			throw new AnnotationException(
//					"Transport error executing HTTP request.", e);
//		} finally {
//			// Release the connection.
//			method.releaseConnection();
//		}
//		return response;
//
//	}
//
//	static abstract class LineParser {
//
//		public abstract String parse(String s) throws ParseException;
//
//		static class ManualDatasetLineParser extends LineParser {
//			public String parse(String s) throws ParseException {
//				return s.trim();
//			}
//		}
//
//		static class OccTSVLineParser extends LineParser {
//			public String parse(String s) throws ParseException {
//				String result = s;
//				try {
//					result = s.trim().split("\t")[3];
//				} catch (ArrayIndexOutOfBoundsException e) {
//					throw new ParseException(e.getMessage(), 3);
//				}
//				return result;
//			}
//		}
//	}
//
//	private List<String> getExtractedEntitiesSet(String text, LineParser parser,
//			int restartFrom) {
//		List<String> annotations = new ArrayList<String>();
//		this.dbpParsedInfo = new HashMap<String, String[]>();
//		{
//			try {
//				String s = parser.parse(text);
//				if (s != null && !s.equals("")) {
//
//					List<DBpediaResource> entities = new ArrayList<DBpediaResource>();
//					if (text.split(" ").length > 500) {
//						String[] tempText = text.split(" ");
//						int reuqestLength = 500;
//						int requestNum = tempText.length / reuqestLength;
//						for (int j = 0; j < requestNum; j++) {
//							String temp = "";
//							for (int k = j * reuqestLength; k <= (j + 1)
//									* reuqestLength; k++)
//								temp += tempText[k] + " ";
//							annotations.addAll(extract(new Text(temp.replaceAll("\\s+", " "))));
//						}
//						String temp = "";
//						for (int k = reuqestLength * requestNum + 1; k <= tempText.length - 1; k++)
//							temp += tempText[k] + " ";
//						annotations.addAll(extract(new Text(temp.replaceAll("\\s+", " "))));
//					} else
//						annotations.addAll(extract(new Text(text.replaceAll("\\s+", " "))));
//					for (DBpediaResource e : entities) {
//						System.out.println(e.uri());
//					}
//				}
//			} catch (ParseException ex) {
//				Logger.getLogger(DBPediaSpootlightAnnotator.class.getName())
//						.log(Level.SEVERE, null, ex);
//			}
//		}
//		
//		return annotations;
//	}
//
//	private List<String> extract(Text text) {
//		try {
//			// Extract_Candidate(text);
//			return Extract_Annotate(text);
//			// Extract_Candidate_First(text);
//		} catch (AnnotationException ex) {
//			Logger.getLogger(DBPediaSpootlightAnnotator.class.getName()).log(
//					Level.SEVERE, null, ex);
//			return new ArrayList<String>();
//		}
//	}
//
//	private List<String> Extract_Annotate(Text text) throws AnnotationException {
//
//		String spotlightResponse;
//		try {
//			String Query = API_URL + "rest/annotate/?" + "confidence="
//					+ CONFIDENCE + "&support=" + SUPPORT + "&spotter="
//					+ spotter + "&disambiguator=" + disambiguator
//					+ "&showScores=" + showScores + "&powered_by=" + powered_by
//					+ "&text=" + URLEncoder.encode(text.text(), "utf-8");
//			GetMethod getMethod = new GetMethod(Query);
//			getMethod
//					.addRequestHeader(new Header("Accept", "application/json"));
//			spotlightResponse = request(getMethod);
//		} catch (UnsupportedEncodingException e) {
//			throw new AnnotationException("Could not encode text.", e);
//		}
//
//		assert spotlightResponse != null;
//
//		JSONObject resultJSON = null;
//		JSONArray entities = null;
//
//		try {
//			resultJSON = new JSONObject(spotlightResponse);
//			entities = resultJSON.getJSONArray("Resources");
//		} catch (JSONException e) {
//			throw new AnnotationException(
//					"Received invalid response from DBpedia Spotlight API.");
//		}
//		List<String> annotation = new ArrayList<String>();
//		for (int i = 0; i < entities.length(); i++) {
//			try {
//				JSONObject entity = entities.getJSONObject(i);
//				String entityName = entity.getString("@surfaceForm");
//
//				annotation.add(entityName);
//
//			} catch (Exception e) {
//				try {
//					System.err.println("ERROR in " + entities.getString(i));
//				} catch (JSONException ex) {
//					Logger.getLogger(DBPediaSpootlightAnnotator.class.getName())
//							.log(Level.SEVERE, null, ex);
//				}
//				continue;
//			}
//		}
//		
//		return annotation;
//	}
//
//
//	public List<String> evaluate(String text) throws Exception {
//		List<String> annotation = evaluateManual(text, 0);
//		return annotation;
//	}
//
//	private List<String> evaluateManual(String text, int restartFrom) {
//		List<String> annotations = getExtractedEntitiesSet(text,
//				new LineParser.ManualDatasetLineParser(), restartFrom);
//		return annotations;
//	}
//}
