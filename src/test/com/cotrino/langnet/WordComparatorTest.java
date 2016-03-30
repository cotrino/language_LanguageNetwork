package test.com.cotrino.langnet;

import static org.junit.Assert.*;

import org.junit.Test;

import com.cotrino.langnet.model.WordComparator;

public class WordComparatorTest {

	@Test
	public void comparatorTest() {
		
		String[] words = {
				"precipitado", "precripitado",
				"precipitado", "precipotado",
				"paladio", "paladio",
				"peras", "manzanas",
				"Smith", "Schmidt"
		};
		double[][] similarityResults = {
				{ 0.92, 0.91, 1.0, 0.25, 0.43 },
				{ 0.75, 1.0, 1.0, 0.25, 1.0 },
				{ 0.5, 1.0, 1.0, 0.25, 0.33 }
		};

		System.out.println("Using Levenshtein...");
		for( int i=0; i<words.length-1; i+=2 ) {
			double similarity = WordComparator.getLevenshteinSimilarity(words[i], words[i+1]);
			System.out.println("- Comparing '"+words[i]+"' & '"+words[i+1]+"' = "+
					similarity);
			assertEquals(similarityResults[0][i/2], similarity, 0.05);
		}
		System.out.println("Using Soundex...");
		for( int i=0; i<words.length-1; i+=2 ) {
			double similarity = WordComparator.getSoundexSimilarity(words[i], words[i+1]);
			System.out.println("- Comparing '"+words[i]+"' & '"+words[i+1]+"' = "+
					similarity);
			assertEquals(similarityResults[1][i/2], similarity, 0.05);
		}
		System.out.println("Using Metaphone...");
		for( int i=0; i<words.length-1; i+=2 ) {
			double similarity = WordComparator.getMetaphoneSimilarity(words[i], words[i+1]); 
			System.out.println("- Comparing '"+words[i]+"' & '"+words[i+1]+"' = "+
					similarity);
			assertEquals(similarityResults[2][i/2], similarity, 0.05);
		}
		
	}
	
	
}
