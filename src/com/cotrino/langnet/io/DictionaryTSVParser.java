package com.cotrino.langnet.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.*;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cotrino.langnet.model.Dictionary;

public class DictionaryTSVParser {

	final static Logger logger = LoggerFactory.getLogger(DictionaryTSVParser.class);

	Pattern definitionLine = Pattern.compile("^([^\t]+)[\\t]([^\t]+).+#\\s+(.+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	String lang1 = "";
	String lang2 = "";
	Vector<String[]> list;

	public DictionaryTSVParser() {
		list = new Vector<String[]>();
	}
 
	public void parse(String file, Dictionary dict) {
		
		// Load English
		/*Runtime rt = Runtime.getRuntime();
		long mem1 = rt.freeMemory();
		LanguageHash english = dict.loadLanguage("English");
		long mem2 = rt.freeMemory();
		logger.debug("Object size: " + ((mem2-mem1)/(1024.0)) + " kB");
		*/
		int amountLanguages = 0;
		
		try {
			logger.debug("Importing "+file+"...");
			FileInputStream fis = new FileInputStream(file); 
			InputStreamReader in = new InputStreamReader(fis, "UTF-8");
			BufferedReader input =  new BufferedReader(in);
			String line = "";
			String lastLanguage = "";
			
			long n = 0;
			boolean skipLanguages = false;
			while ( (line = input.readLine()) != null ) {
				Matcher definition = definitionLine.matcher(line);
				if (definition.find()) {
					String language = definition.group(1);
					
					if( skipLanguages && !language.equals("Ladino") ) {
						continue;
					} else {
						skipLanguages = false;
					}
					if( language.equals("American Sign Language") 
							|| language.equals("English")
							|| language.equals("Translingual")
							|| language.equals("Cantonese")
							|| language.equals("Japanese")
							|| language.equals("Korean")
							|| language.equals("Lao")
							|| language.equals("Mandarin")
							|| language.equals("Min Nan")
							|| language.equals("Thai")
							|| language.equals("Vietnamese") ) {
						continue;
					}
					if( !language.equals(lastLanguage) && !lastLanguage.equals("") ) {
						long defs = list.size();
						if( defs > 500 ) {
							logger.debug("\n- "+lastLanguage+": "+defs+" definitions");
							dict.importDictionary(list, lastLanguage, "English");
							amountLanguages++;
						}
						list.clear();
					}
					String word = cleanWord(definition.group(2));
					for( String def:  splitWords(definition.group(3)) ) {
						addDefinition( word, def );
					}
					lastLanguage = language;
				}
				n++;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public String getFirstLanguage() {
		return lang1;
	}

	public String getSecondLanguage() {
		return lang2;
	}

	private void addDefinition(String word1, String word2) {
		if( !word1.equals("") && !word2.equals("") ) {
			/*
			 * We allow nouns by now, because it's all messed up:
			 * Saturday, Turkish, ID card, Miss, West, ...
			if( word2.matches("^[A-Z].+") ) {
				logger.debug(word1+" = "+word2);
			}
			*/
			list.add(new String[] { word1, word2.toLowerCase() });
		}
	}

	private Vector<String> splitWords(String word) {
		String[] wordList = word.split("[,;]");
		Vector<String> list = new Vector<String>(); 
		for( int i=0; i<wordList.length; i++ ) {
			String singleWord = cleanWord(wordList[i]);
			// split by " or "
			String[] moreWords = singleWord.split("\\sor\\s");
			if( moreWords.length > 1 ) {
				for(String w: moreWords) {
					list.add(w);
				}
			} else {
				list.add(singleWord);
			}
		}

		return list;
	}

	private String cleanWord(String word) {
		// skip comments
		word = word.replaceAll("\\{\\{[^\\s]+", "");
		word = word.replaceAll("\\}", "");
		word = word.replaceAll("\\([^\\)]+\\)", "");
		word = word.replaceAll("\\[", "");
		word = word.replaceAll("\\]", "");
		word = word.replaceAll("\\/.+", "");
		// substitute ' with `
		word = word.replaceAll("\\'", "");
		word = word.replaceAll("\\\"", "");
		// subtitute weird characters
		word = word.replaceAll(" ́", "");
		String newWord = "";
		for(Character c: word.toCharArray()) {
			// discard "Combining Diacritical Marks"
			if( !(c >= 0x300 && c <= 0x36F)
				// discard "Arabic Presentation Forms-A"
				&& !(c >= 0xFB50 && c <= 0xFDFF)
				// discard "Arabic Presentation Forms-B"
				&& !(c >= 0xFE70 && c <= 0xFEFF)
				// discard "General Punctuation"
				&& !(c >= 0x2000 && c <= 0x206F)
				// discard "C1 controls and Latin-1 supplement"
				&& !(c >= 0x0080 && c <= 0x00BF) ) {
				newWord += c;
			}
		}
		word = newWord;
		// skip +
		//word = word.replaceAll("\\+", "");
		// skip initial or final spaces
		word = word.replaceAll("^\\s+", "");
		word = word.replaceAll("\\s+$", "");
		// skip dot
		word = word.replaceAll("\\.$", "");
		// skip "I ..."
		word = word.replaceAll("^I\\s+", "");
		word = word.replaceAll("^am\\s+", "I am ");
		// skip "a ..."
		word = word.replaceAll("^(a|A|an|An)\\s+", "");
		// skip "the ..."
		word = word.replaceAll("^(the|The)\\s+", "");
		// skip "to ..."
		word = word.replaceAll("^(to|To)\\s+", "");
		// skip end of line
		word = word.replaceAll("\\n", "");

		// discard long definitions
		if( word.contains(":") || word.contains("|") || word.equals("!") ||
				word.contains("(") ||
				word.contains(")") || word.split("\\s+").length > 3 ) {
			word = "";
		}
		return word;
	}
}
