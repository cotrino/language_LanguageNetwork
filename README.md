# Language Network
Language Network is an approach to show language similarity based on morphological distance.
- Execution results can be seen at: http://languagenetwork.cotrino.com/
- An explanation (in Spanish) can be found at: http://www.cotrino.com/2012/11/language-network/

Dictionary database:
- This tool uses Wiktionary content to compare languages.
	https://www.wiktionary.org/ 
- Unfortunately, Wiktionary's export files are XML and SQL with full articles, which difficult content parsing.
	http://dumps.wikimedia.org/enwiki/latest/ 
- However, it is possible to download a TSV file with the latest content from a different source with this script:
	https://github.com/epitron/scripts/blob/master/wict
- This TSV file has to be stored in the ./data/ directory.
- Execute com.cotrino.langnet.GenerateDictionary.

Language comparison:
- Execute com.cotrino.langnet.GenerateComparison.

Visualization generation:
- Execute com.cotrino.langnet.GenerateVisualization.

