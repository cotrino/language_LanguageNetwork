package com.cotrino.langnet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cotrino.langnet.model.Dictionary;
import com.cotrino.langnet.model.Word;

public class GenerateComparison {

	final static Logger logger = LoggerFactory.getLogger(GenerateComparison.class);

	static Dictionary dict; 
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		long start = System.currentTimeMillis();
	
		// check that dictionary exists
		dict = new Dictionary(Constants.DATABASE_FILE);
		dict.printWordsPerLanguage();

		// compare all languages with each other
		dict.printComparisonTable(Constants.RESULTS_FOLDER+"/Levenshtein", Word.Levenshtein);
		dict.close();
		
		long end = System.currentTimeMillis();
		double duration = ((double)(end-start))/1000.0;
		logger.debug( String.format("%.2f seconds = %.2f minutes = %.2f hours", duration, duration/60.0, duration/3600.0) );

	}
	
}
