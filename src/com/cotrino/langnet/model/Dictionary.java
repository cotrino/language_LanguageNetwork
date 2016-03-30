package com.cotrino.langnet.model;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.Vector;
import java.sql.ResultSet;

import net.sf.junidecode.Junidecode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cotrino.langnet.Constants;
import com.cotrino.langnet.io.DB;
import com.cotrino.langnet.io.DictionaryTSVParser;
import com.cotrino.langnet.io.IOUtil;

public class Dictionary extends DB {

	final static Logger logger = LoggerFactory.getLogger(Dictionary.class);

	int comparisonMode = Word.Levenshtein;
	long currentID;

	public Dictionary(String databaseName) {
		super(databaseName);
		enableOptimizations();
		currentID = 1;
	}

	class Comparison {
		String word1;
		String word2;
		double similarity;
		public Comparison(String word1, String word2, double similarity) {
			this.word1 = word1;
			this.word2 = word2;
			this.similarity = similarity;
		}
	}

	public String compareLanguages(Language lang1, Language lang2, String logFile) {
		long lang1ID = lang1.getId();
		long lang2ID = lang2.getId();
		double overall_similarity = 0.0;
		long very_similar = 0;
		long identical = 0;
		long comparisons = 0;
		String log = "WordA;WordB;Similarity;\n";

		Date date1 = new Date();
		List<List<Word[]>> words = getAllWordsForLanguages(lang1ID, lang2ID);
		List<Comparison> comparison = new LinkedList<Comparison>();
		Date date2 = new Date();
		long time = ((date2.getTime()-date1.getTime())/1000);
		//logger.debug(" "+lang1+" vs "+lang2+": "+words.size()+" word pairs found in "+time+" seconds");

		for(List<Word[]> wordPairs : words) {
			double lastSimilarity = 0.0;
			Comparison lastComparison = null; 
			for(Word[] wordPair : wordPairs) {
				
				double similarity = wordPair[0].compareTo(wordPair[1],comparisonMode);
				
				// from all the available translations, choose the closest one
				if( similarity >= lastSimilarity || lastComparison == null ) {

					lastComparison = new Comparison(
						wordPair[0].getAscii(),
						wordPair[1].getAscii(),
						similarity
						);
					lastSimilarity = similarity;
					
				}
			}
			if( wordPairs.size() > 0 ) {
				comparison.add(lastComparison);
				overall_similarity += lastComparison.similarity;
			}

		}

		for(Comparison comp : comparison) {
			log += comp.word1+";"+comp.word2+";"+comp.similarity+";\n";
			//logger.debug(comp.word1+";"+comp.word2+";"+comp.similarity+";");
			comparisons++;
			if( comp.similarity == 1.0 ) {
				identical++;
			} else if( comp.similarity > 0.9 ) {
				very_similar ++;
			}
		}
		overall_similarity /= comparisons;
		IOUtil.write(logFile, log);
		return lang1.toString()+";"+lang2.toString()+";"+overall_similarity+";"+comparisons+";"+identical+";"+very_similar+";\n";
	}

	public String compareLanguages(String lang1, String lang2, String logFile) {
		return compareLanguages(getLanguageByName(lang1), getLanguageByName(lang2), logFile);
	}

	public String findFalseFriends(Language lang1, Language lang2) {
		long lang1ID = lang1.getId();
		long lang2ID = lang2.getId();
		String content = "";

		logger.debug("Finding false friends of "+lang1+" in "+lang2+"...");
		List<Word> wordsA = getAllWordsForLanguage(lang1ID);
		List<Word> wordsB = getAllWordsForLanguage(lang2ID);
		for( int i=0; i<wordsA.size(); i++ ) {
			Word wordA = wordsA.get(i);
			for( int j=0; j<wordsB.size(); j++ ) {
				Word wordB = wordsB.get(j);
				// false friends must have different meaning...
				if( wordA.translation.equals(wordB.translation) ) {
					double similarity = wordA.compareTo(wordB,comparisonMode);
					// ...but be very similar
					if( similarity > 0.9 ) {
						WordGroup translationsA = wordA.getTranslation(lang2ID);
						WordGroup translationsB = wordB.getTranslation(lang1ID);
						String transA = "";
						String transB = "";
						for( int k = 0; k<translationsA.size(); k++ ) {
							if( k!= 0 ) {	transA += ", ";	}
							transA += translationsA.get(k);
						}
						for( int k = 0; k<translationsB.size(); k++ ) {
							if( k!= 0 ) {	transB += ", ";	}
							transB += translationsB.get(k);
						}
						// it may be that the link is wrong (no translations of the word B in the A language)
						// => avoid those cases
						if( translationsB.size() > 0 ) {
							content += lang1.toString()+";"+lang2.toString()+";"+								
									wordA+";"+wordB+";"+
									similarity+";"+transA+";"+transB+";\n";
						}
					}
				}
			}
		}
		return content;
	}

	public String findFalseFriends(String lang1, String lang2) {
		return findFalseFriends(getLanguageByName(lang1), getLanguageByName(lang2));
	}

	class ComparisonThread extends Thread {
		Dictionary dict;
		Language lang1;
		Language lang2;
		String file;
		String content;

		public ComparisonThread() {
			super();
			this.dict = new Dictionary(Constants.DATABASE_FILE);
		}
		public void set(Language lang1, Language lang2, String file) {
			this.lang1 = lang1;
			this.lang2 = lang2;
			this.file = file;
		}
		public void run() {
			content = dict.compareLanguages(lang1, lang2, file);
			dict.close();
			dict = null;
		}
	}

	public void printComparisonTable(String filename, int mode) {
		this.comparisonMode = mode;
		ComparisonThread[] threads = new ComparisonThread[Constants.MAX_THREADS];
		for(int i=0; i<threads.length; i++) {
			threads[i] = new ComparisonThread();
		}
		//String content = "LanguageA;LanguageB;Similarity;ExecutedComparisons;IdenticalWords;VerySimilarWords;\n";
		List<Language> langs = getAllLanguages();

		// calculate the total amount of comparisons
		double comparisons = 0.0;
		for(int i=0; i<langs.size()-1; i++) {
			for(int j=i+1; j<langs.size();j++) {
				if( i != j ) {
					comparisons++;
				}
			}
		}

		// execute the comparisons
		double executedComparisons = 0.0;
		for(int i=0; i<langs.size()-1; i++) {
			Language lang1 = langs.get(i); 
			for(int j=i+1; j<langs.size();j++) {
				String resultFile = filename+"_"+lang1.toString()+"_"+langs.get(j).toString()+".log";
				if( i != j ) {
					executedComparisons++;
					if( !IOUtil.fileExists(resultFile) ) {
						boolean freeThread = false;
						int k = 0;
						int done = (int)(10000.0*executedComparisons/comparisons);
						logger.debug("["+(done/100)+"%]"+langs.get(i).toString()+" vs "+langs.get(j).toString());

						while (!freeThread) {
							if( !threads[k].isAlive() ) {
								threads[k] = new ComparisonThread();
								threads[k].set(lang1, langs.get(j), resultFile );
								threads[k].start();
								freeThread = true;
							}
							k++;
							if( k >= threads.length ) {
								k = 0;
								try {this.wait(5000);} catch(Exception e) {}
							}
						}

					}
				}
			}
		}
		logger.debug("Printing comparison table to "+filename+".csv...");
		//WriteToFile.write(filename+".csv", content);
	}

	public void printComparisonTableForLanguage(String filename, String language, int mode) {
		this.comparisonMode = mode;
		String content = "LanguageA;LanguageB;Similarity;ExecutedComparisons;IdenticalWords;VerySimilarWords;\n";
		List<Language> langs = getAllLanguages();
		Language lang1 = getLanguageByName(language); 
		for(int j=0; j<1; j++) { //langs.size();j++) {
			if( !langs.get(j).toString().equals(lang1.toString()) ) {
				content += compareLanguages(lang1, langs.get(j),
						filename+"_"+lang1.toString()+"_"+langs.get(j).toString()+".log");
			}
		}
		logger.debug("Printing comparison table to "+filename+".csv...");
		IOUtil.write(filename+".csv", content);
	}

	public void printFalseFriendTable(String filename, int mode) {
		this.comparisonMode = mode;
		String content = "LanguageA;LanguageB;WordA;WordB;Similarity;RealTranslationA;RealTranslationB;\n";
		List<Language> langs = getAllLanguages();
		for(int i=0; i<langs.size()-1; i++) {
			Language lang1 = langs.get(i); 
			for(int j=i+1; j<langs.size();j++) {
				if( i!= j ) {
					content += findFalseFriends(lang1, langs.get(j));
				}
			}
		}
		logger.debug("Printing false friend table to "+filename+".csv...");
		IOUtil.write(filename+".csv", content);
	}

	public void printTranslations(String wordString) {
		WordGroup wordList = Word.getWords(this, wordString, 0);
		if( wordList.size() == 0 ) {
			logger.debug("Word '"+wordString+"' not found!");
		} else {
			for( Word word: wordList ) {
				logger.debug("'"+word+"' ("+word.getLanguage()+") translations:");
				WordGroup translations = word.getAllTranslations();
				for( int j=0; j<translations.size(); j++ ) {
					Word translation = translations.get(j);
					logger.debug("- '"+translation+"' ("+translation.getLanguage()+")");
				}
			}
		}
	}

	private int getValue(String query) {
		int value = 0;
		try {
			ResultSet rs = executeQuery(query);
			if(rs.next()) {
				value = rs.getInt(0);
			}
			rs.close();
		} catch(SQLException e) {
			logger.error("Query failed: "+query);
		}
		return value;
	}

	public void printStatistics() {

		logger.debug("Words without translations: "+
				getValue("SELECT COUNT(groupID) AS value FROM (SELECT COUNT(ID) AS c, groupID FROM tbl_words GROUP BY groupID)  WHERE c<2") );

	}

	public void printWordsPerLanguage() {
		List<Language> langs = getAllLanguages();
		for(int i=0; i<langs.size(); i++) {
			Language lang1 = langs.get(i);
			logger.debug(i+") "+lang1+"\t"+getWordAmountForLanguage(lang1.getId()));
		}
	}

	public void createTables() {
		try {
			executeUpdate("DROP TABLE IF EXISTS tbl_words;");
			executeUpdate(
					"CREATE TABLE tbl_words ("+
							//"ID INTEGER PRIMARY KEY,"+
							"ID INTEGER,"+
							"langID INTEGER,"+
							"translation TEXT, "+
							"word TEXT,"+
							"ascii TEXT, "+
							"soundex TEXT"+
							");"
					);
			executeUpdate("DROP TABLE IF EXISTS tbl_languages;");
			executeUpdate(
					"CREATE TABLE tbl_languages ("+
							"ID INTEGER PRIMARY KEY,"+
							"name TEXT"+
							");"
					);
		} catch(SQLException e) {
			logger.error("Table creation failed!");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void createWordMatrix() {

		long englishID = getLanguageId("English");
		List<Word> words = this.getAllWordsForLanguage(englishID);
		logger.debug("Found "+words.size()+" words for English");
		HashMap<String,Integer> wordIndex = new HashMap<String,Integer>();
		int i=0;
		for(Word word : words) {
			wordIndex.put(word.translation, i);
			i++;
		}
		List<Language> languages = this.getAllLanguages();
		String[][] matrix = new String[words.size()][languages.size()*2];
		
		StringJoiner columns = new StringJoiner(",");
		StringJoiner columnsTable = new StringJoiner(",");
		StringJoiner columnsPlaceholders = new StringJoiner(",");
		int langColumn = 0;
		for(Language language : languages) {
			List<Word> wordsForLanguage = this.getAllWordsForLanguage(language.ID);
			logger.debug("Found "+wordsForLanguage.size()+" words for "+language.name);
			for(Word word : wordsForLanguage) {
				int index = wordIndex.get(word.translation);
				if( matrix[index][langColumn*2] != null ) {
					matrix[index][langColumn*2] += "#" + word.word;
					matrix[index][langColumn*2+1] += "#" + word.ascii;
				} else {
					matrix[index][langColumn*2] = word.word;
					matrix[index][langColumn*2+1] = word.ascii;
				}
			}
			columns.add("W"+language.ID).add("A"+language.ID);
			columnsTable.add("W"+language.ID+" TEXT").add("A"+language.ID+" TEXT");
			columnsPlaceholders.add("?").add("?");
			langColumn++;
		}
		
		String query = "INSERT INTO tbl_matrix ("
				+ columns.toString() + ") VALUES ("
				+ columnsPlaceholders.toString() + ");";
		PreparedStatement prep;
		long p = 0;
		logger.debug("Creating word matrix for fast comparison");
		try {
			executeUpdate("DROP TABLE IF EXISTS tbl_matrix;");
			executeUpdate(
					"CREATE TABLE tbl_matrix ("
							+ columnsTable.toString()
							+ ");"
					);
			
			prep = massiveInsertPrepare(query);

			for( i=0; i<words.size(); i++ ) {
				
				for(int j=0; j<languages.size()*2; j++) {
					prep.setString(j+1, matrix[i][j]);
				}
				prep.addBatch();
				
				if( p > 5000 ) {
					logger.debug(i+"/"+words.size()+" entries imported");
					p = 0;
				}
				p++;
			}
			massiveInsertExecute(prep);

		} catch(SQLException e) {
			logger.error("Massive insert failed!");
		} finally {
			logger.debug("[OK]");
		}
		
	}
	
	public List<Language> getAllLanguages() {
		String query = "SELECT ID, name FROM tbl_languages ORDER BY name;";
		List<Language> langs = new LinkedList<Language>();
		try {
			ResultSet rs = executeQuery(query);
			while(rs.next()) {
				Language lang = new Language(this, rs.getLong("ID"), rs.getString("name"));
				langs.add(lang);
			}
			rs.close();
		} catch(SQLException e) {
			logger.error("Query failed: "+query);
		}
		return langs;
	}
	
	public List<Word> getAllWordsForLanguage(long langID) {
		String query = "SELECT "+Word.fields+" FROM tbl_words WHERE langID='"+langID+"' ORDER BY word;";
		List<Word> words = new LinkedList<Word>();
		try {
			ResultSet rs = executeQuery(query);
			while(rs.next()) {
				Word word = new Word(this, rs);
				words.add(word);
			}
			rs.close();
		} catch(SQLException e) {
			logger.error("Query failed: "+query);
		}
		return words;
	}

	public void cleanNouns() {
		String[] wordsToRemove = {
				"amper","brom","klor","kripton","krom","lantan","gadolinium","germanium","magnezium","palladium","foszfor","iridium","barium","helium","litium","nikkel","aluminium","cink","radio","stroncium","americium","berillium","itterbium","szkandium","kalcium","mangan","molekula","ittrium","tallium","millimeter","cezium","centimeter","laurencium","technecium","arzen","oxigen"
		};
		Language lang = this.getLanguageByName("Hungarian");
		for( String word: wordsToRemove ) {
			String query = "SELECT translation FROM tbl_words WHERE ascii='"+word+"' AND langID="+lang.ID+";";
			String translation = this.getString(query);
			if( !translation.equals("") ) {
				logger.debug(word+": "+translation+" found");
				try {
					executeUpdate("DELETE FROM tbl_words WHERE translation='"+translation+"';");
				} catch(SQLException e) {
					logger.error("Query failed: "+query);
				}
			}
		}


		/** Remove upper case words */
		/*String query = "SELECT translation, COUNT(*) AS amount FROM tbl_words GROUP BY translation;";
		Vector<String> list = new Vector<String>();
		long entries = 0;
		try {
			ResultSet rs = executeQuery(query);
			while(rs.next()) {
				String translation = rs.getString("translation");
				long amount = rs.getLong("amount");
				if( Character.isUpperCase(translation.charAt(0)) 
						&& !translation.startsWith("Very ")
						&& !translation.startsWith("By ") ) {
					//logger.debug(""+translation+": "+amount);
					list.add(translation);
					entries += amount;
				}
			}
			rs.close();
		} catch(SQLException e) {
			logger.error("Query failed: "+query);
		}
		logger.debug(list.size()+" words to be removed");
		logger.debug(entries+" entries to be removed");
		 */

		/*for(String word: list) {
			try {
				executeUpdate("DELETE FROM tbl_words WHERE translation='"+word+"';");
			} catch(SQLException e) {
				logger.error("Query failed: "+query);
			}
			Log.print(".");
		}
		logger.debug("Ready!");
		 */
	}	

	public List<List<Word[]>> getAllWordsForLanguages(long langID1, long langID2) {
		String query = "SELECT W"+langID1+" AS word1, A"+langID1+" AS ascii1, "
				+ "W"+langID2+" AS word2, A"+langID2+" AS ascii2 FROM tbl_matrix "
				+ "WHERE W"+langID1+"<>\"\" AND W"+langID2+"<>\"\";";
		//logger.debug(query);
		List<List<Word[]>> words = new LinkedList<List<Word[]>>();
		try {
			ResultSet rs = executeQuery(query);
			while(rs.next()) {
				String w1 = rs.getString("word1");
				String a1 = rs.getString("ascii1");
				String w2 = rs.getString("word2");
				String a2 = rs.getString("ascii2");
				List<Word> wordsForLang1 = new LinkedList<Word>();
				List<Word> wordsForLang2 = new LinkedList<Word>();
				if( w1.contains("#") ) {
					String[] wparts = w1.split("#");
					String[] aparts = a1.split("#");
					for(int i=0; i<wparts.length && i<aparts.length; i++) {
						wordsForLang1.add(new Word(this, wparts[i], aparts[i]));
					}
				} else {
					wordsForLang1.add(new Word(this, w1, a1));
				}
				if( w2.contains("#") ) {
					String[] wparts = w2.split("#");
					String[] aparts = a2.split("#");
					for(int i=0; i<wparts.length && i<aparts.length; i++) {
						wordsForLang2.add(new Word(this, wparts[i], aparts[i]));
					}
				} else {
					wordsForLang2.add(new Word(this, w2, a2));
				}
				List<Word[]> wordPairs = new LinkedList<Word[]>();
				for(Word word1 : wordsForLang1) {
					for(Word word2 : wordsForLang2) {
						wordPairs.add(new Word[]{word1, word2});
					}
				}
				words.add(wordPairs);
			}
			rs.close();
		} catch(SQLException e) {
			logger.error("Query failed: "+query);
			e.printStackTrace();
			this.close();
			System.exit(-1);
		}
		return words;
	}

	public long getWordAmountForLanguage(long langID) {
		String query = "SELECT COUNT(ID) FROM tbl_words WHERE langID='"+langID+"';";
		long amount = 0;
		try {
			ResultSet rs = executeQuery(query);
			if(rs.next()) {
				amount = rs.getLong(1);
			}
			rs.close();
		} catch(SQLException e) {
			logger.error("Query failed: "+query);
		}
		return amount;
	}

	public List<Word> getAllWords() {
		String query = "SELECT "+Word.fields+" FROM tbl_words ORDER BY word;";
		List<Word> words = new LinkedList<Word>();
		try {
			ResultSet rs = executeQuery(query);
			while(rs.next()) {
				Word word = new Word(this, rs);
				words.add(word);
			}
			rs.close();
		} catch(SQLException e) {
			logger.error("Query failed: "+query);
		}
		return words;
	}

	public Language getLanguage(long ID) {
		String query = "SELECT name FROM tbl_languages WHERE ID='"+ID+"' ORDER BY name;";
		Language lang = null;
		try {
			ResultSet rs = executeQuery(query);
			if(rs.next()) {
				lang = new Language(this, ID, rs.getString("name"));
			}
			rs.close();
		} catch(SQLException e) {
			logger.error("Query failed: "+query);
		}
		return lang;
	}

	public Language getLanguageByName(String name) {
		String query = "SELECT ID, name FROM tbl_languages WHERE name='"+name+"' ORDER BY name;";
		Language lang = null;
		try {
			ResultSet rs = executeQuery(query);
			if(rs.next()) {
				lang = new Language(this, rs.getLong("ID"), rs.getString("name"));
			}
			rs.close();
		} catch(SQLException e) {
			logger.error("Query failed: "+query);
		}
		return lang;
	}

	public long getLanguageId(String language) {
		return getLong("SELECT ID FROM tbl_languages WHERE name='"+language+"';");
	}

	public void importDictionaries(String path) {
		DictionaryTSVParser parser = new DictionaryTSVParser();
		parser.parse(path, this);
	}

	public void importDictionary(Vector<String[]> list, String lang1, String lang2) {
		long lang1ID = getLanguageId(lang1);
		if( lang1ID == 0 ) {
			lang1ID = insertLanguage(lang1);
		}
		long lang2ID = getLanguageId(lang2);
		if( lang2ID == 0 ) {
			lang2ID = insertLanguage(lang2);
		}
		importDictionary(list, lang1ID, lang2ID);
	}

	public void importDictionary(Vector<String[]> list, long lang1ID, long lang2ID) {

		String query = "INSERT INTO tbl_words (ID, langID, translation, word, ascii) VALUES (?, ?, ?, ?, ?);";
		PreparedStatement prep;
		long p = 0;
		logger.debug("Inserting into DB");
		try {
			prep = massiveInsertPrepare(query);

			for( int i=0; i<list.size(); i++ ) {
				String[] entry = list.get(i);

				String translation = Junidecode.unidecode(entry[1]).toLowerCase();
				String ascii = Junidecode.unidecode(entry[0]).toLowerCase(); 
				if( !translation.contains("[?]") && !ascii.contains("[?]") ) {
					// insert word
					prep.setLong(1, currentID);		// currentID
					prep.setLong(2, lang1ID);		// langID
					prep.setString(3, translation);	// translation
					prep.setString(4, entry[0]);	// word
					prep.setString(5, ascii);		// ascii
					prep.addBatch();
					currentID++;
					p++;
				}
				
				if( p > 5000 ) {
					massiveInsertExecute(prep);
					prep = massiveInsertPrepare(query);
					logger.debug(i+"/"+list.size()+" entries imported");
					p = 0;
				}
			}
			massiveInsertExecute(prep);

		} catch(SQLException e) {
			logger.error("Massive insert failed!");
		} finally {
			logger.debug("[OK]");
		}
	}

	public long insertLanguage(String name) {
		String query = "INSERT INTO tbl_languages (name) VALUES ('"+name+"');"; 
		return insert(query);
	}

	public LanguageHash loadLanguage(String lang) {
		return new LanguageHash(this, lang);
	}

	public void createEnglishDictionary() {
		String query = "SELECT DISTINCT translation FROM tbl_words translation;";
		Vector<String[]> list = new Vector<String[]>();
		try {
			ResultSet rs = executeQuery(query);
			while(rs.next()) {
				String word = rs.getString(1);
				list.add(new String[] {word, word});
			}
			rs.close();
		} catch(SQLException e) {
			logger.error("Query failed: "+query);
		}
		logger.debug("- English: "+list.size()+" definitions");
		importDictionary(list, "English", "English");
	}

	public void createIndex() {
		logger.debug("- Creating indexes ");
		try {
			executeUpdate("CREATE  INDEX 'main'.'group' ON 'tbl_words' ('translation' ASC)");
			executeUpdate("CREATE  INDEX 'main'.'ID' ON 'tbl_words' ('ID' DESC);");
			executeUpdate("CREATE  INDEX 'main'.'langID' ON 'tbl_words' ('langID' DESC);");
		} catch(SQLException e) {
			logger.error("Query failed");
		} finally {
			logger.debug("[OK]");
		}
	}
}
