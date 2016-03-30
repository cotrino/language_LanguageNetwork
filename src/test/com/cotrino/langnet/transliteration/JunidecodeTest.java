package test.com.cotrino.langnet.transliteration;

import net.sf.junidecode.Junidecode;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JunidecodeTest {

	final static Logger logger = LoggerFactory.getLogger(JunidecodeTest.class);

	@Test
	public void testEncoding() {
		
		String[] words = {
			" ",
			"đŠćšŽžČč",
			"ïüíúéèçÇò·óà",
			"đŠćšŽžČčǌǉ",
			"ěŽžřŘťŠšůďňČč",
			"ĳ",
			"ĵĥŭĈŝŜĉĝĜ",
			"İăŠšşŏž",
			"œŒïîíÎÉëêéèçæÇäâáàÂºüûùôñ",
			"ó·µ",
			"μνξοθικλδεζηαβγέάίήΩΨΥΤΧΦΡΠΣΞΟΜΝΚΛΘΙΖΗΔΕΒΓΐΑΎΌΊΉΈΆϜϝϋϊωψώύόσςρπχφυτ",
			" Ἄὐἱ",
			"–",
			"ÍíéæÆÁáþÞýúÚöÖóðÓ",
			"ẛ",
			"ïªíìêéèçâàºûùöò",
			"ńłąż",
			" ‌",
			"Ↄↄō",
			"ēĒėķžļņĀāģŠšĪīČčū",
			"ŠėšųąŽęžČčįū",
			"‘",
			"шфхцчрстуќџљјњѕѐѓЈЊИЛКНМПОБАГВЕДЗШТУРСЦФХлкипонмгвбазжед",
			"ĠġĦħĊċż",
			"ėȝȜþð",
			"ńłŁąį",
			"ǫ ́ʼ",
			"đŧŠšžŋČč",
			"ī",
			"ᚅᚐᚋíÉúé·óá",
			"ńłćĄŁążęśŚŻź",
			"ÍíêéçÇãÁâÃáàºüúôõó",
			"ăŢţş",
			"²×"
		};
		
		for(int i=0; i<words.length; i++) {
			
			logger.debug(words[i]+" => "+Junidecode.unidecode(words[i]));
			
		}
	}
	
	
}
