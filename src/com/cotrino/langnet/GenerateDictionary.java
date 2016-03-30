package com.cotrino.langnet;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cotrino.langnet.model.Dictionary;

public class GenerateDictionary {

	final static Logger logger = LoggerFactory.getLogger(GenerateDictionary.class);

	static Dictionary dict; 
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		long start = System.currentTimeMillis();
	
		// look for TSV files in data directory
		File folder = new File(Constants.DATA_FOLDER);
		File[] listOfFiles = folder.listFiles();
		List<File> foundExports = new LinkedList<File>();
		for(File tsvFile : listOfFiles) {
			if( tsvFile.getName().matches("TEMP-S\\d+\\.tsv") ) {
				foundExports.add(tsvFile);
			}
		}
		Collections.sort(foundExports);

		if( foundExports.size() > 0 ) {
			
			// import the latest found TSV file into an SQLite database 
			File tsvFile = foundExports.get(foundExports.size()-1);
			logger.debug("Using dictionary dump "+tsvFile);
			dict = new Dictionary(Constants.DATABASE_FILE);
			dict.createTables();
			dict.importDictionaries(tsvFile.getAbsolutePath());
			dict.createEnglishDictionary();
			dict.createIndex();
			dict.cleanNouns();
			dict.createWordMatrix();
			dict.compact();
			dict.close();
			
		} else {
			
			logger.error("No dictionary dump found at "+Constants.DATA_FOLDER
					+ ". Please look for a valid dump at "+Constants.DICTIONARY_URL);
			
		}
		
		long end = System.currentTimeMillis();
		double duration = ((double)(end-start))/1000.0;
		logger.debug( String.format("%.2f seconds = %.2f minutes = %.2f hours", duration, duration/60.0, duration/3600.0) );
		
	}
	
}
