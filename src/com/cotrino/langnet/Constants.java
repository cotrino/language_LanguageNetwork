package com.cotrino.langnet;

public class Constants {

	public final static String USER_DIR = System.getProperty("user.dir");
	public final static String DATA_FOLDER = USER_DIR+"/data";
	public final static String RESULTS_FOLDER = USER_DIR+"/results";
	public final static String WEB_FOLDER = USER_DIR+"/www";
	public final static String DATABASE_FILE = DATA_FOLDER+"/dictionary.sqlite";
	public final static String SUMMARY_FILE = RESULTS_FOLDER+"/Levenshtein.csv";
	public final static String LANGUAGES_FILE = RESULTS_FOLDER+"/Languages.csv";
	public final static String VISUALIZATION_FILE = WEB_FOLDER+"/Levenshtein.js";
	public final static String DICTIONARY_URL = "https://github.com/epitron/scripts/blob/master/wict";
	public final static int MAX_THREADS = 4;
	
}
