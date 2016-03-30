package com.cotrino.langnet.model;

import org.simmetrics.simplifiers.Soundex;
import org.simmetrics.metrics.Levenshtein;
import org.simmetrics.simplifiers.DoubleMetaphone;

public class WordComparator {

	String reference = "";
	static Soundex soundex = new Soundex();
	static Levenshtein levenshtein = new Levenshtein();
	static DoubleMetaphone metaphone = new DoubleMetaphone();

	public WordComparator(String reference) {
		this.reference = reference;
	}
	
	public void compareTo(String[] keys) {
		System.out.println("Comparison against: \""+reference+"\"");
		for( int i=0; i<keys.length; i++ ) {
			System.out.printf("- %s: %.2f %.2f\n", 
				keys[i], 
				getLevenshteinSimilarity(reference,keys[i]),
				getSoundexSimilarity(reference,keys[i])
			);
		}
	}

	public static double getLevenshteinSimilarity(String word1, String word2) {
		return (double)levenshtein.compare(word1,word2);
	}
	
	public static double getSoundexSimilarity(String word1, String word2) {
		return (double)levenshtein.compare(soundex.simplify(word1), soundex.simplify(word2));
	}
	
	public static double getMetaphoneSimilarity(String word1, String word2) {
		return (double)levenshtein.compare(metaphone.simplify(word1), metaphone.simplify(word2));
	}
	
}
