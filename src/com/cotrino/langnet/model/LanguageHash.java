package com.cotrino.langnet.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class LanguageHash extends HashMap<String, Word> {

	private static final long serialVersionUID = 1L;
	long langID;
	Dictionary dict;

	public LanguageHash(Dictionary dict, String lang) {
		this.langID = dict.getLanguageId(lang);
		this.dict = dict;
		this.load();
		System.out.println(this.size()+" words found for "+lang);
	}
	
	private void load() {
		String query = "SELECT * FROM tbl_words WHERE langID="+this.langID+";";
		try {
			ResultSet rs = dict.executeQuery(query);
			while(rs.next()) {
				Word word = new Word(this.dict,	rs);
				this.put(word.ascii, word);
			}
			rs.close();
		} catch(SQLException e) {
			System.err.println("Query failed: "+query);
		}
	}

}
