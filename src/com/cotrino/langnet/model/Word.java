package com.cotrino.langnet.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Word {

	public static final int Levenshtein = 1;
	public static final int Soundex = 2;
	public static final int Metaphone = 3;
	
	static String fields = "ID, word, ascii, soundex, translation, langID";
	long ID;
	long langID;
	String translation;
	String word;
	String ascii;
	String soundex;
	Dictionary dict;
	
	public Word(Dictionary dict, long ID, String word, String ascii, String soundex, String translation, long langID) {
		 this.ID = ID;
		 this.word = word;
		 this.ascii = ascii;
		 this.soundex = soundex;
		 this.langID = langID;
		 this.translation = translation;
		 this.dict = dict;
	}
	
	// just for comparison with other languages
	public Word(Dictionary dict, String word, String ascii) {
		 this.word = word;
		 this.ascii = ascii;
		 this.dict = dict;
	}
	
	public Word(Dictionary dict, ResultSet rs) throws SQLException {
		 this.ID = rs.getLong("ID");
		 this.word = rs.getString("word");
		 this.ascii = rs.getString("ascii");
		 this.soundex = rs.getString("soundex");
		 this.langID = rs.getLong("langID");
		 this.translation = rs.getString("translation");
		 this.dict = dict;
	}
	
	public static Word insertWord(Dictionary dict, String word, String ascii, String soundex, String translation, long language) {
		String query = "INSERT INTO tbl_words (langID, translation, word, ascii, soundex) VALUES "+
			"('"+language+"', '"+translation+"', '"+word+"', '"+ascii+"', '"+soundex+"');"; 
		long ID = dict.insert(query);
		return Word.getWord(dict, ID);
	}
	
	public static Word getWord(Dictionary dict, long ID) {
		String query = "SELECT "+Word.fields+" FROM tbl_words WHERE ID='"+ID+"' ORDER BY word;";
		Word word = null;
		try {
			ResultSet rs = dict.executeQuery(query);
			if(rs.next()) {
				word = new Word(dict, rs);
			}
			rs.close();
		} catch(SQLException e) {
			System.err.println("Query failed: "+query);
		}
		return word;
	}
	
	public static WordGroup getWords(Dictionary dict, String wordString, long lang) {
		String query = "SELECT "+Word.fields+" FROM tbl_words "+
		"WHERE word='"+wordString+"'";
		if( lang != 0 ) {
			query += " AND langID='"+lang+"'";
		}
		WordGroup list = new WordGroup(); 
		try {
			ResultSet rs = dict.executeQuery(query);
			while(rs.next()) {
				list.add(new Word(dict, rs));
			}
			rs.close();
		} catch(SQLException e) {
			System.err.println("Query failed: "+query);
		}
		return list;
	}
	
	public static Word getWord(Dictionary dict, String wordString, long lang) {
		String query = "SELECT "+Word.fields+" FROM tbl_words "+
			"WHERE word='"+wordString+"'";
		if( lang != 0 ) {
			query += " AND langID='"+lang+"'";
		}
		Word word = null;
		try {
			ResultSet rs = dict.executeQuery(query);
			if(rs.next()) {
				word = new Word(dict, rs);
			}
			rs.close();
		} catch(SQLException e) {
			System.err.println("Query failed: "+query);
		}
		return word;
	}
	
	public double compareTo(Word word2,int mode) {
		switch(mode) {
		case Levenshtein:	return WordComparator.getLevenshteinSimilarity(ascii.toLowerCase(), word2.getAscii().toLowerCase()); 
		case Soundex:		return WordComparator.getSoundexSimilarity(ascii, word2.getAscii());
		case Metaphone:		return WordComparator.getMetaphoneSimilarity(ascii, word2.getAscii());
		}
		return 0.0;
	}
	 
	public String getAscii() {
		 return ascii;
	}
	
	public String getLanguage() {
		 return dict.getLanguage(langID).toString();
	}
	
	public WordGroup getTranslation(long lang2ID) {
		WordGroup result = new WordGroup();
		try {
			ResultSet rs = dict.executeQuery("SELECT "+Word.fields+
					" FROM tbl_words "+
					"WHERE translation='"+translation+"' AND langID="+lang2ID);
			while(rs.next()) {
				result.add(new Word(dict, rs));
			}
			rs.close();
		} catch(SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return result;
	}

	public WordGroup getAllTranslations() {
		WordGroup result = new WordGroup();
		try {
			ResultSet rs = dict.executeQuery("SELECT "+Word.fields+
					" FROM tbl_words "+
					"WHERE translation='"+translation+"';");
			while(rs.next()) {
				result.add(new Word(dict, rs));
			}
			rs.close();
		} catch(SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return result;
	}
	
	public long getID() {
		return ID;
	}
	public String getTranslation() {
		return translation;
	}
	public String toString() {
		 return word;
	}
}
