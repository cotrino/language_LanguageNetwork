/*************************************************************************************
 * Copyright (C) 2009-2015 José Miguel Cotrino. All rights reserved.
 *
 * This software may be distributed and modified under the terms of the GNU
 * General Public License version 2 (GPL2) as published by the Free Software
 * Foundation and appearing in the file LICENSE included in the packaging of
 * this file. Please note that GPL2 Section 2[b] requires that all works based
 * on this software must also be made publicly available under the terms of
 * the GPL2 ("Copyleft").
 *
 * Contact information
 * -------------------
 *
 * José Miguel Cotrino
 * Web : http://www.cotrino.com/
 *
 ************************************************************************************/
package com.cotrino.langnet;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cotrino.langnet.io.IOUtil;

/**
 * 
 * @author <a href="http://www.cotrino.com/">José Miguel Cotrino</a>
 *
 */
public class GenerateVisualization {

	final static Logger logger = LoggerFactory.getLogger(GenerateVisualization.class);

	private final static double MIN_SIMILARITY = 0.30;
	private CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(';').withIgnoreEmptyLines(true).withHeader().withQuote('#');
	private HashMap<String,List<LanguageSimilarity>> languageSimilarities;
	private HashMap<String,Integer> amountWordsPerLanguage;

	public GenerateVisualization() throws IOException {
		
		generateSummary(Constants.RESULTS_FOLDER, Constants.SUMMARY_FILE);

		generateLanguages(Constants.SUMMARY_FILE, Constants.LANGUAGES_FILE);

		getLanguages(Constants.LANGUAGES_FILE);

		generateVisualization(Constants.VISUALIZATION_FILE);
		
	}
	
	private void generateSummary(String path, String summaryFile) {

		languageSimilarities = new HashMap<String,List<LanguageSimilarity>>();

		String content = "LanguageA;LanguageB;Similarity;ExecutedComparisons;IdenticalWords;VerySimilarWords;\n";
		File directory = new File(path);
		File[] logFiles = directory.listFiles();
		for(File logFile : logFiles) {
			if( logFile.getName().contains(".log") ) {
				try {
					//logger.debug("Including "+logFile.getName());
					String[] parts = logFile.getName().split("[\\_\\.]");
					String langA = parts[1];
					String langB = parts[2];
					double[] similarity = calculateSimilarity(logFile);
					content += langA+";"+langB+";"+similarity[0]+";"+(int)similarity[1]+";0;0;\n";
					
					if( similarity[0] >= MIN_SIMILARITY ) {
						if( !languageSimilarities.containsKey(langA) ) {
							languageSimilarities.put(langA, new LinkedList<LanguageSimilarity>());
						}
						if( !languageSimilarities.containsKey(langB) ) {
							languageSimilarities.put(langB, new LinkedList<LanguageSimilarity>());
						}
						languageSimilarities.get(langA).add(new LanguageSimilarity(langB, similarity[0]));
						languageSimilarities.get(langB).add(new LanguageSimilarity(langA, similarity[0]));
					}
					
				} catch(IOException e) {
					logger.error("At "+logFile.getName()+", error "+e.getMessage());
				}
			}
		}
		IOUtil.write(summaryFile, content);
		
	}

	private double[] calculateSimilarity(File logFile) throws IOException {
		
		int i = 0;
		double total = 0.0;
		Reader reader = new FileReader(logFile);
		CSVParser parser = new CSVParser(reader, csvFormat);
		for (CSVRecord record : parser) {
			try {
			    double similarity = Double.parseDouble( record.get("Similarity") );
			    total += similarity;
			    i++;
			} catch(NumberFormatException e) {
				logger.error("At "+logFile.getName()+", failed line: "+record.toString());
			}
		}
		parser.close();
		reader.close();
		return new double[]{ (total/i), i };
		
	}
	
	private void generateLanguages(String summaryFile, String languagesFile) throws IOException {
		
		HashMap<String,Integer> list = new HashMap<String,Integer>(); 
		Reader reader = new FileReader(summaryFile);
		CSVParser parser = new CSVParser(reader, csvFormat);
		for (CSVRecord record : parser) {
		    String languageA = record.get("LanguageA");
		    String languageB = record.get("LanguageB");
		    int words = Integer.parseInt(record.get("ExecutedComparisons"));
		    list.put(languageA, Math.max(words, list.getOrDefault(languageA, 0) ));
		    list.put(languageB, Math.max(words, list.getOrDefault(languageB, 0) ));
		}
		parser.close();
		reader.close();
		
		String content = "Language;Words;Family;\n";
		for(String language : list.keySet()) {
			content += language+";"+list.get(language)+";Romance;\n";
		}
		IOUtil.write(languagesFile, content);
	}

	private void getLanguages(String file) throws IOException {

		amountWordsPerLanguage = new HashMap<String,Integer>();

		Reader reader = new FileReader(file);
		CSVParser parser = new CSVParser(reader, csvFormat);
		for (CSVRecord record : parser) {
		    String language = record.get("Language");
		    int words = Integer.parseInt(record.get("Words"));
		    amountWordsPerLanguage.put(language, words);
		}
		parser.close();
		reader.close();

	}

	/**
	 * based on http://bl.ocks.org/1377729
	 * @param jsFile
	 */
	private void generateVisualization(String jsFile) {
		
		String content = "";
		int i = 0;
		double max = 0.0, min = 100.0;
		StringJoiner sj = new StringJoiner(",");
		StringJoiner nodesHash = new StringJoiner(" ");
		StringJoiner linkList = new StringJoiner(",");
		HashMap<String,Integer> languageIds = new HashMap<String,Integer>(); 
		
		for(String language : amountWordsPerLanguage.keySet()) {

			// language bubble color & size
			int red = (int)(Math.random()*127.0);
			int green = (int)(Math.random()*127.0);
			int blue = (int)(Math.random()*127.0);
			int color = ((red+128)<<16) | ((green+128)<<8) | ((blue+128)); 
			int textcolor = ((red+64)<<16) | ((green+64)<<8) | ((blue+64)); 
			int size = amountWordsPerLanguage.get(language)/2000;
			if( size < 5 ) {
				size = 5;
			}

			// language information
			String description = "Language: " + getWikiURL(language) + "<br/><br/>";
			if( languageSimilarities.containsKey(language) ) {
				List<LanguageSimilarity> similarLanguages = languageSimilarities.get(language);
				Collections.sort(similarLanguages);
				description += "Similar to:<ul>";
				// look for most similar languages
				for(LanguageSimilarity languageB : similarLanguages) {
					description += "<li>"+getWikiURL(languageB.language)
							+" at "+String.format("%d", (int)(languageB.similarity*100))+"%</li>";
					max = Math.max(max, languageB.similarity);
					min = Math.min(min, languageB.similarity);
				}
				description += "</ul>";
			} else {
				description += "No similar languages found.";
			}
			
			sj.add("{ label : \""+language+"\", "
				+"id : "+i+", "
				+"color : \"#"+String.format("%6x",color)+"\", "
				+"textcolor : \"#"+String.format("%6x",textcolor)+"\", "
				+"size : "+size+", desc : \""+description+"\" }");
			
			nodesHash.add("nodesHash[\""+language+"\"] = "+i+";");
			languageIds.put(language, i);
			i++;
		}
		
		content += "var nodesArray = [\n"
			+sj.toString()
			+"\n];\n\n";
		
		content += "var nodesHash = [];\n"
			+nodesHash.toString()
			+"\n\n";

		for(String languageA : languageSimilarities.keySet()) {
			for(LanguageSimilarity languageB : languageSimilarities.get(languageA)) {
				int color = (int) ( 240 * (1.0 + MIN_SIMILARITY - languageB.similarity) );
				if( color > 240 ) {
					color = 240;
				}
				if( languageIds.containsKey(languageA) && languageIds.containsKey(languageB.language) ) {
					linkList.add("{ desc : \""+languageA+" -- "+languageB.language+"\", "
						+ "source : "+languageIds.get(languageA)+", "
						+ "target : "+languageIds.get(languageB.language)+", "
						+ "weight : "+languageB.similarity+", "
						+ "color : \"#"+String.format("%02x%02xff",color,color)+"\" }");
				}
			}
		}
		content += "var linksArray = [\n"
			+ linkList.toString()
			+ "\n];\n";

		IOUtil.write(jsFile, content);
		
	}

	/*
	private void cleanName
	{
		my $name = shift;
		#$name =~ s/\s//igs;
		$name =~ s/-/ /igs;
		while($name =~ m/(.+[^ ]+)([A-Z])(.+)/) {
			$name = "$1 $2$3";
		}
		return $name;
	}

	*/

	private String getWikiURL (String language) {
		String url = "http://en.wikipedia.org/wiki/"+language+"_language";
		url.replaceAll(" ", "_");
		return "<a href='"+url+"' target='_blank'>"+language+"</a>";
	}

	class LanguageSimilarity implements Comparable<LanguageSimilarity> {
		
		String language;
		double similarity;
		
		public LanguageSimilarity(String language, double similarity) {
			this.language = language;
			this.similarity = similarity;
		}

		@Override
		public int compareTo(LanguageSimilarity o) {
			if( this.similarity > o.similarity ) {
				return -1;
			} else if( this.similarity < o.similarity ) {
				return 1;
			} else {
				return 0;
			}
		}
		
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		long start = System.currentTimeMillis();
	
		// generate visualization
		new GenerateVisualization();

		long end = System.currentTimeMillis();
		double duration = ((double)(end-start))/1000.0;
		logger.debug( String.format("%.2f seconds = %.2f minutes = %.2f hours", duration, duration/60.0, duration/3600.0) );
					
	}
	
}
