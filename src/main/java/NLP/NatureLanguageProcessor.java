package NLP;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import TextNormalizer.TextNormalizer;
import Utils.Util;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class NatureLanguageProcessor {
	public static final String[] POSLIST = { "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$", "WRB",
			"$", "``", "NNPS", "NNS", "PDT", "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO", "CC", "CD",
			"DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNP" };
	public static final String[] POSLIST_STANDALONE = { "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP",
			"WP$", "WRB", "$", "``", "NNPS", "NNS", "PDT", "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO",
			"CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNP" };

	private static Pattern ALPHABETIC_PATTERN = Pattern.compile(".*[a-zA-Z].*");
	public static final Set<String> POSSET = new HashSet<>(Arrays.asList(POSLIST));
	private Set<String> stopWordSet;

	private Set<String> badWordSet;// less extensive.

	public Set<String> getBadWordSet() {
		return badWordSet;
	}

	public Set<String> getStopWordSet() {
		return stopWordSet;
	}

	public HashMap<String, String[]> getCorrectionMap() {
		return correctionMap;
	}

	private void readBadWordsFromFile() {
		badWordSet = new HashSet<>();
		System.err.println(">Read BadWords from file - bad.stop");
		Scanner br = null;
		try {
			br = new Scanner(new FileReader(getClass().getClassLoader()
					.getResource(TextNormalizer.getDictionaryDirectory() + "filtered\\bad.stop").getPath()));
			while (br.hasNext()) {
				badWordSet.add(br.nextLine());
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (br != null) {
				br.close();
			}
		}
	}

	private static NatureLanguageProcessor instance = null;
	MaxentTagger PoSTagger;
	private static final Map<String, String> POSCorrectionMap = new HashMap<>();
	private static final HashMap<String, Integer> realDictionary = new HashMap<>();
	private static final HashMap<String, String[]> correctionMap = new HashMap<>();

	public static synchronized NatureLanguageProcessor getInstance() {
		if (instance == null)
			instance = new NatureLanguageProcessor();
		return instance;
	}

	private static void loadCorrectionMap(File dictfile, File posFile) throws FileNotFoundException {
		// TODO Auto-generated method stub
		Scanner br = new Scanner(new FileReader(dictfile));
		while (br.hasNextLine()) {
			String[] pair = br.nextLine().split(",");
			if (pair.length == 2)
				correctionMap.put(pair[0], pair[1].split(" "));

		}
		br.close();

		br = new Scanner(new FileReader(posFile));
		while (br.hasNextLine()) {
			String[] pair = br.nextLine().split(",");
			if (pair.length == 2)
				POSCorrectionMap.put(pair[0], pair[1]);

		}
		br.close();

	}

	private static void loadDictionary(File[] fileLists) throws Exception {
		for (File file : fileLists) {
			Scanner br = new Scanner(new FileReader(file));
			while (br.hasNext())
				realDictionary.put(br.next(), 0);
			br.close();
		}
	}

	public HashMap<String, Integer> getRealDictionary() {
		return realDictionary;
	}

	private NatureLanguageProcessor() {
		readStopWordsFromFile();
		PoSTagger = new MaxentTagger(TextNormalizer.getDictionaryDirectory()
				+ "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger");
		try {
			loadCorrectionMap(new File(TextNormalizer.getDictionaryDirectory() + "Map/wordMapper.txt"),
					new File(TextNormalizer.getDictionaryDirectory() + "Map/posMapper.txt"));
			loadDictionary(new File(TextNormalizer.getDictionaryDirectory() + "improvised").listFiles());
			Porter2StemmerInit();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void readStopWordsFromFile() {
		stopWordSet = new HashSet<>();
		System.err.println(">Read StopWords from file - englishImprovised.stop");
		Scanner reader = null;
		try {
			reader = new Scanner(
					new FileReader(TextNormalizer.getDictionaryDirectory() + "stop/englishImprovised.stop"));
			while (reader.hasNextLine()) {

				stopWordSet.add(reader.nextLine());
			}

			// PrintWriter stop = new PrintWriter(
			// TextNormalizer.getDictionaryDirectory()
			// + "stop/englishImprovised.stop");
			// for (String w : stopWordSet)
			// stop.println(w);
			// stop.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (reader != null)
				reader.close();
		}
		stopWordSet.addAll(TextNormalizer.getInstance().getInterestingWords());
	}

	/**
	 * Standardize the text and then split it into sentences, separated by DOT
	 * Testing my nontrivial implementation of manipulating arrays without using
	 * advanced structures like List or Set
	 * 
	 * @param text
	 *            - a text
	 * @return a String array of all the sentences.
	 */
	public static String[] extractSentence(String text) {
		String[] originalSplit = text.split("\\.+\\s*");
		int[] indexHolder = new int[originalSplit.length];
		int countValidSentences = 0;
		for (int i = 0; i < originalSplit.length; i++) {
			if (originalSplit[i].length() > 1 && containAlphabet(originalSplit[i])) {
				indexHolder[countValidSentences++] = i;
			}
		}
		String[] results = new String[countValidSentences];
		for (int i = 0; i < countValidSentences; i++) {
			results[i] = originalSplit[indexHolder[i]];
		}
		return results;
	}

	public static boolean containAlphabet(String text) {
		// Pattern p = Pattern.compile("[a-zA-Z]");
		Matcher m = ALPHABETIC_PATTERN.matcher(text);

		if (m.find())
			return true;
		else {
			return false;
		}
	}

	/**
	 * Return the index of the corresponding PoS tag in the list provided by this
	 * Class. This provides a way to reduce the memory for String objects. Instead
	 * of storing the String of PoS tag, we can store its index.
	 * 
	 * @param PoS
	 *            - a PoS tag
	 * @return the index of that PoS tag or -1 if it is not in the list
	 */
	public boolean checkValidityOfPOS(String PoS) {
		return POSSET.contains(PoS);
	}

	public List<String> correctUsingMap(List<String> tokens) {
		List<String> tokenList = new ArrayList<>();
		for (String tok : tokens) {
			String[] wordarray = correctionMap.get(tok);
			if (wordarray != null)
				tokenList.addAll(Arrays.asList(wordarray));
			else
				tokenList.add(tok);
		}
		return tokenList;
	}

	// this function is for retaining the position information of the word
	// input words must have the form of: wordText_PositionNumber
	public List<String> correctUsingMapRetainPosition(List<String> tokens) {
		List<String> tokenList = new ArrayList<>();
		for (String tok : tokens) {
			String[] input = tok.split("_");
			String[] wordarray = correctionMap.get(input[0]);
			if (wordarray != null) {
				for (String word : wordarray) {
					tokenList.add(word + "_" + input[1] + "_" + input[2]);
					// tokenList.addAll(Arrays.asList(wordarray));
				}
			} else
				tokenList.add(tok);
		}
		return tokenList;
	}

	public static String mergeIntoText(List<String> tokens) {
		StringBuilder sb = new StringBuilder();
		for (String tok : tokens) {
			sb.append(tok).append(' ');
		}
		if (sb.length() <= 1)
			return null;
		return sb.deleteCharAt(sb.length() - 1).toString();
	}

	// return a string of new text and a string of the positions of each word
	public static String[] mergeIntoTextWithPosition(List<String> tokens) {
		StringBuilder sb = new StringBuilder();
		StringBuilder posSB = new StringBuilder();
		for (String tok : tokens) {
			String[] input = tok.split("_");
			sb.append(input[0]).append(' ');
			posSB.append(input[1]).append('_').append(input[2]).append(' ');
		}
		if (sb.length() <= 1)
			return null;
		return new String[] { sb.deleteCharAt(sb.length() - 1).toString(),
				posSB.deleteCharAt(posSB.length() - 1).toString() };
	}

	public static List<String> wordSplit(final CharSequence input) {
		List<String> tokenList = new ArrayList<>();
		StringBuilder sb = null;
		for (int i = 0; i < input.length(); i++) {
			final char c = input.charAt(i);
			if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '\'') {
				if (sb == null)
					sb = new StringBuilder();
				sb.append(c);
			} else {
				if (sb != null)
					tokenList.add(sb.toString());
				sb = null;
				if (c != ' ') {
					sb = new StringBuilder();
					sb.append(c);
					tokenList.add(sb.toString());
					sb = null;
				}
			}
		}
		if (sb != null)
			tokenList.add(sb.toString());
		sb = null;

		return tokenList;
	}

	public static List<String> wordSplitWithPositions(final CharSequence input) {
		List<String> tokenList = new ArrayList<>();
		StringBuilder sb = null;
		// i is also the lastest position of the word
		int lastestPosition = -1;
		int firstPosition = -1;
		for (int i = 0; i < input.length(); i++) {
			lastestPosition = i;
			final char c = input.charAt(i);
			if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') ) {
				if (sb == null) {
					sb = new StringBuilder();
					firstPosition = i;
				}
				sb.append(c);
			} else {
				if (sb != null)
					// (lastestPosition-1) because it's the last character before this one
					tokenList.add(sb.toString() + "_" + (lastestPosition - 1) + "_" + firstPosition);
				sb = null;
				if ( c == '\'' || c == '.'
						|| c == '?' || c == ';' || c == ':' || c == '!' || c == '(' || c == ')' || c == '"'|| c == '&') {
					sb = new StringBuilder();
					sb.append(c);
					// weird characters need to be counted
					tokenList.add(sb.toString() + "_"+i+"_"+i);
					sb = null;
				}else {
					// we don't add _ character because it messes with later annotations
					if (c != ' ' && c != '_') {
						sb = new StringBuilder();
						sb.append(c);
						// weird character don't need to be counted
						tokenList.add(sb.toString() + "_-1_-1");
						sb = null;
					}
				}
			}
		}
		if (sb != null) {
			// not -1 because it is already the last character of the string
			tokenList.add(sb.toString() + "_" + (lastestPosition) + "_" + firstPosition);
		}
		sb = null;

		return tokenList;
	}

	public static List<String> wordSplit_wordOnly(final CharSequence input) {
		List<String> tokenList = new ArrayList<>();
		StringBuilder sb = null;
		for (int i = 0; i < input.length(); i++) {
			final char c = input.charAt(i);
			if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c == '_')) {
				if (sb == null)
					sb = new StringBuilder();
				sb.append(c);
			} else {
				if (sb != null)
					tokenList.add(sb.toString());
				sb = null;
				// if (c != ' ') {
				// sb = new StringBuilder();
				// sb.append(c);
				// tokenList.add(sb.toString());
				// sb = null;
				// }
			}
		}
		if (sb != null)
			tokenList.add(sb.toString());
		sb = null;

		return tokenList;
	}

	public static List<String> extractWordsFromText(String text) {
		text = text.toLowerCase();
		String[] words = text.split("[^a-z0-9']+");
		// SymSpell symspell = SymSpell.getInstance();
		ArrayList<String> wordList = new ArrayList<>();
		for (String word : words) {
			if (word.equals("null") || word.length() == 0 || word.equals("'") || word.equals(""))
				continue;

			String[] wordarray = correctionMap.get(word);
			if (wordarray != null)
				wordList.addAll(Arrays.asList(wordarray));
			else
				wordList.add(word);
		}
		double totalScore = 0, bigramScore = 0, unigramScore = 0;
		boolean previousInDic = false;
		for (String word : wordList) {
			Integer wCount = realDictionary.get(word);
			double score = 1.0;
			if (wCount != null) {
				// score /= Math.log(wCount);
				unigramScore += score;
				if (previousInDic)
					bigramScore += score;
				previousInDic = true;
			} else
				previousInDic = false;

			totalScore += score;
		}
		double biproportion = bigramScore / totalScore;
		double uniproportion = unigramScore / totalScore;
		if (biproportion < 0.4 && uniproportion < 0.5)
			return null;

		return wordList;
	}

	/**
	 * This function will stem the words in the input List using Porter2/English
	 * stemmer and replace the String value of that word with the stemmed version.
	 * 
	 * @param wordList
	 *            - a List contains a String array of 2 elements: 0-word, 1-PoS
	 */
	// public List<String[]> stem(List<String[]> wordList) {
	// List<String[]> results = new ArrayList<>();
	// CustomStemmer stemmer = CustomStemmer.getInstance();
	// for (String[] pair : wordList) {
	// if (pair.length < 2)
	// continue;
	// // System.out.print(count++ + ": " + pair[0]);
	// // if (!stopWordSet.contains(pair[0]))
	// pair = stemmer.stem(pair,false);
	//
	// results.add(pair);
	// // System.out.println("-" + pair[0] + "<->" + pair[1]);
	// }
	// return results;
	// }

	private void Porter2StemmerInit() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class stemClass = Class.forName("org.tartarus.snowball.ext.englishStemmer");
	}

	public List<String[]> findPosTag(List<String> wordList) {
		if (wordList == null)
			return null;
		StringBuilder textForTag = new StringBuilder();
		String prefix = "";
		for (String word : wordList) {
			textForTag.append(prefix + word);
			prefix = " ";
		}
		// The tagged string
		String tagged = PoSTagger.tagString(textForTag.toString());
		// Output the result
		// System.out.println(tagged);

		String[] words = tagged.split(" ");
		// System.out.println("length = " + words.length);

		List<String[]> results = new ArrayList<>();
		for (int i = 0; i < words.length; i++) {
			String[] w = words[i].split("_");
			// if (!stopWordSet.contains(w[0]))
			if (w.length == 2 && POSSET.contains(w[1])) {
				String pos = POSCorrectionMap.get(w[0]);
				if (pos != null)
					w[1] = pos;
				results.add(w);
			}
		}
		return results;
	}

	public String findPosTag(String text) {
		String tagged = PoSTagger.tagString(text);
		return tagged;
	}

	public static void main(String[] args) {
		NatureLanguageProcessor nlp = NatureLanguageProcessor.getInstance();
		System.out.println(nlp.findPosTag("awesome lt 3. i really like this app because its save my battery life"));
		System.out.println(nlp.findPosTag(
				"can be convenient to use but. some of the power save setting profiles like bluetooth should have do n't change instead of just on and off."));
	}
}
