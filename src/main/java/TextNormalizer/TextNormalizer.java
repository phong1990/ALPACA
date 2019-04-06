package TextNormalizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.print.Doc;

import org.bytedeco.javacpp.RealSense.context;
import org.tartarus.snowball.SnowballStemmer;

import AU.ALPACA.ExperimentReponse;
import AU.ALPACA.PreprocesorMain;
import NLP.CustomStemmer;
import NLP.NatureLanguageProcessor;
import NLP.SymSpell;
import Utils.Util;

public class TextNormalizer {
	private static TextNormalizer instance = null;
	private static String DICTIONARY_DIRECTORY = "dictionary/";
	private static String TRIGRAM_TRAINING_DIRECTORY = "dictionary/trigramTraning/";
	private static boolean DEBUG = true;
	private static boolean needCorrection = true;// this is a hidden feature for splitting sentence without corrections
	private Set<String> interestingWords;
	private Set<String> structuralWords;
	private Set<String> intensifiers;
	private Set<String> domainWords;
	private Set<String> prepositions;
	private Set<String> DT_WHs;

	public void setNeedCorrection(boolean need) {
		needCorrection = need;
	}

	public Set<String> getPrepositions() {
		return prepositions;
	}

	public Set<String> getIntensifiers() {
		return intensifiers;
	}

	private TextNormalizer() {
		// TODO Auto-generated constructor stub

	}

	public Set<String> getInterestingWords() {
		return interestingWords;
	}

	public Set<String> getStructuralWords() {
		return structuralWords;
	}

	public Set<String> getDomainWords() {
		return domainWords;
	}

	private static void debug_println(String msg) {
		if (DEBUG)
			System.out.println(msg);
	}

	public void readConfigINI(String fileName) throws FileNotFoundException {
		System.out.println("Reading configuration file at " + fileName);
		Scanner br = new Scanner(new File(fileName));
		while (br.hasNextLine()) {
			String item = br.nextLine();
			if (item.charAt(0) == '%')
				continue;
			String[] tokens = item.split("=");

			if (tokens.length == 2) {
				String variable = tokens[0].replace(" ", "");
				if (variable.equals("DICTIONARY_DIRECTORY")) {
					String value = tokens[1].replace(" ", "");
					DICTIONARY_DIRECTORY = value;
				}
				if (variable.equals("TRIGRAM_TRAINING_DIRECTORY")) {
					String value = tokens[1].replace(" ", "");
					TRIGRAM_TRAINING_DIRECTORY = value;
				}
				if (variable.equals("DEBUG")) {
					String value = tokens[1].replace(" ", "");
					if (value.equals("0"))
						DEBUG = false;
					if (value.equals("1"))
						DEBUG = true;
				}
			}
		}
		br.close();
		interestingWords = new HashSet<>();
		structuralWords = new HashSet<>();
		domainWords = new HashSet<>();
		intensifiers = new HashSet<>();
		prepositions = new HashSet<>();
		DT_WHs = new HashSet<>();
		DT_WHs.add("this");
		DT_WHs.add("that");
		prepositions.addAll(
				loadWordsSet(new File(TextNormalizer.getDictionaryDirectory() + "baseword/misc/prepositions.txt")));
		intensifiers.addAll(
				loadWordsSet(new File(TextNormalizer.getDictionaryDirectory() + "baseword/misc/intensifiers.txt")));
		interestingWords.addAll(prepositions);
		interestingWords.addAll(intensifiers);
		interestingWords.addAll(
				loadWordsSet(new File(TextNormalizer.getDictionaryDirectory() + "baseword/misc/connectors.txt")));
		interestingWords.addAll(
				loadWordsSet(new File(TextNormalizer.getDictionaryDirectory() + "baseword/misc/negations.txt")));
		DT_WHs.addAll(loadWordsSet(new File(TextNormalizer.getDictionaryDirectory() + "baseword/misc/wh.txt")));
		interestingWords.addAll(DT_WHs);
		domainWords
				.addAll(loadWordsSet(new File(TextNormalizer.getDictionaryDirectory() + "baseword/misc/domain.txt")));
		interestingWords.addAll(domainWords);
		structuralWords.addAll(interestingWords);
		interestingWords
				.addAll(loadWordsSet(new File(TextNormalizer.getDictionaryDirectory() + "baseword/misc/others.txt")));

		System.out.println("DONE Reading configuration file");
	}

	public Set<String> getDT_WHs() {
		return DT_WHs;
	}

	private static Set<String> loadWordsSet(File testDataFile) throws FileNotFoundException {
		// TODO Auto-generated method stub
		Set<String> results = new HashSet<>();
		Scanner br = new Scanner(new FileReader(testDataFile));
		while (br.hasNextLine()) {
			String[] words = br.nextLine().split(",");
			results.add(words[0]);
		}
		br.close();
		return results;
	}

	public static String getTrigramTrainingDirectory() {
		return TRIGRAM_TRAINING_DIRECTORY;
	}

	public static String getDictionaryDirectory() {
		return DICTIONARY_DIRECTORY;
	}

	public static TextNormalizer getInstance() {
		if (instance == null)
			instance = new TextNormalizer();
		return instance;
	}

	// will return null if the text is not english
	// public String normalize(String input) {
	// String[] taggedTokens = preprocessAndSplitToTaggedTokens(input);
	// if (taggedTokens == null)
	// return null;
	// List<String> correctedTaggedTokens = new ArrayList<>();
	// CustomStemmer stemmer = CustomStemmer.getInstance();
	// for (String taggedTok : taggedTokens) {
	// String[] pair = taggedTok.split("_");
	// pair[0] = pair[0].toLowerCase();
	// pair = stemmer.stem(pair,false);
	// correctedTaggedTokens.add(pair[0] + "_" + pair[1]);
	// }
	// String correctedTaggedText = NatureLanguageProcessor
	// .mergeIntoText(correctedTaggedTokens);
	// if (correctedTaggedText == null)
	// return null;
	// debug_println(correctedTaggedText);
	// return correctedTaggedText;
	// }

	// check for non-english text using the method proposed in our publication
	// biproportionThreshold: ratio of pair of english words to all words,
	// suggest using 0.4
	// uniproportionThreshold: ratio of single english words to all words,
	// suggest using 0.5
	public boolean isNonEnglish(List<String> wordList, double biproportionThreshold, double uniproportionThreshold,
			double smallWordThreshold) {
		Set<String> realDictionary = SymSpell.getInstance().getFullDictionary();
		double totalScore = 0, bigramScore = 0, unigramScore = 0, smallWord = 0;
		// smallWords are word that has less than 3 char. if smallWord is
		// propotionally large
		// in a text then maybe the text doesnt make any sense.
		boolean previousInDic = false;
		for (String word : wordList) {
			// ignore special characters: , < . > ? / : ; " ' { [ } ] + = _ - ~
			// ` ! @ # $ % ^ & * ( ) | \
			if (Util.isSpecialCharacter(word))
				continue;
			if (Util.isContainingNonASCII(word))
				continue;
			if (word.length() <= 2)
				smallWord++;
			double score = 1.0;
			if (realDictionary.contains(word)) {
				// score /= Math.log(wCount);
				unigramScore += score;
				if (previousInDic)
					bigramScore += score;
				previousInDic = true;
			} else
				previousInDic = false;

			totalScore += score;
		}
		if (totalScore == 0)
			return true;
		double biproportion = bigramScore / totalScore;
		double uniproportion = unigramScore / totalScore;
		if (biproportion < biproportionThreshold && uniproportion < uniproportionThreshold)
			return true;
		if (smallWord / wordList.size() > smallWordThreshold)
			return true;
		return false;
	}

	public boolean isNonEnglishWithPosition(List<String> wordList, double biproportionThreshold,
			double uniproportionThreshold, double smallWordThreshold) {
		Set<String> realDictionary = SymSpell.getInstance().getFullDictionary();
		double totalScore = 0, bigramScore = 0, unigramScore = 0, smallWord = 0;
		// smallWords are word that has less than 3 char. if smallWord is
		// propotionally large
		// in a text then maybe the text doesnt make any sense.
		boolean previousInDic = false;
		for (String word : wordList) {
			String[] input = word.split("_");
			// ignore special characters: , < . > ? / : ; " ' { [ } ] + = _ - ~
			// ` ! @ # $ % ^ & * ( ) | \
			if (Util.isSpecialCharacter(input[0]))
				continue;
			if (Util.isContainingNonASCII(input[0]))
				continue;
			if (input[0].length() <= 2)
				smallWord++;
			double score = 1.0;
			if (realDictionary.contains(input[0])) {
				// score /= Math.log(wCount);
				unigramScore += score;
				if (previousInDic)
					bigramScore += score;
				previousInDic = true;
			} else
				previousInDic = false;

			totalScore += score;
		}
		if (totalScore == 0)
			return true;
		double biproportion = bigramScore / totalScore;
		double uniproportion = unigramScore / totalScore;
		if (biproportion < biproportionThreshold && uniproportion < uniproportionThreshold)
			return true;
		if (smallWord / wordList.size() > smallWordThreshold)
			return true;
		return false;
	}

	// split text into sentence using end-of-sentence indicator: . ; ! ?
	// The text inside parentheses is accounted as new, separated sentences.
	// The following example shows how it works:
	// Original: Angry birds I love the new levels they (the new level. I meant
	// the new levels) are very challenging.
	// Ordered output:
	// 1. the_NN new_JJ level_NN
	// 2. i_PRP mean_VB the_NN new_JJ level_NN
	// 3. angry_JJ bird_VB i_PRP love_VB the_NN new_JJ level_NN they_PRP be_VB
	// very_NN challenging_JJ
	// will return null if the text is not english
	public List<List<String>> normalize_SplitSentence(String input, int level, boolean needPOS) throws Exception {
		if (level < 0 && level > 3)
			throw new Exception("Level is not right, not expecting level = " + level);
		if (input == null)
			throw new Exception("input is null ");
		String[] taggedTokens = preprocessAndSplitToTaggedTokens(input);
		if (taggedTokens == null)
			return null;
		List<List<String>> correctedTaggedSentences = new ArrayList<>();
		CustomStemmer customStemmer = CustomStemmer.getInstance();
		Class stemClass = Class.forName("org.tartarus.snowball.ext.porterStemmer");
		SnowballStemmer stemmer = (SnowballStemmer) stemClass.newInstance();
		List<String> sentence = null;
		boolean inParentheses = false;
		List<String> inParenthesesSentence = null;
		for (String taggedTok : taggedTokens) {
			// if (!isPositionable(taggedTok)) // this is a wrongly tagged one
			// continue;
			String[] pair = taggedTok.split("_");
			if (!isPositionable(pair[0])) // this is a wrongly tagged one
				continue;
			// if (pair.length == 2) // sometime special characters make splitting
			// // not working
			// continue;
			pair[0] = pair[0].toLowerCase();
			if (pair[0].equals(".") || pair[0].equals(";") || pair[1].equals(".") || pair[1].equals(":")
					|| pair[0].equals("!") || pair[0].equals("?") || pair[1].equals("-RRB-")) {
				// end sentence
				if (inParentheses) {
					if (inParenthesesSentence != null) {
						correctedTaggedSentences.add(Util.deepCopyList(inParenthesesSentence));
						inParenthesesSentence = null;
					}
					if (pair[1].equals("-RRB-"))
						inParentheses = false;
				} else {
					if (sentence != null) {
						correctedTaggedSentences.add(Util.deepCopyList(sentence));
						sentence = null;
					}
				}
			} else {
				if (pair[1].equals(",") || pair[1].equals("``") || pair[1].equals("''") || pair[1].equals("--")
						|| pair[1].equals("$") || pair[1].equals("#") || pair[1].equals("SYM"))
					continue; // ignore all of these special chars and symbols
				if (pair[1].equals("-LRB-")) {
					inParentheses = true;
				} else {
					List<String> senOI = null;
					if (inParentheses) {

						if (inParenthesesSentence == null)
							inParenthesesSentence = new ArrayList<>();
						senOI = inParenthesesSentence;
					} else {
						if (sentence == null)
							sentence = new ArrayList<>();
						senOI = sentence;
					}
					switch (level) {
					case PreprocesorMain.LV0_NONE:
						break;
					case PreprocesorMain.LV1_SPELLING_CORRECTION:
						break;
					case PreprocesorMain.LV2_ROOTWORD_STEMMING:
						pair = customStemmer.stem(pair, false);
						break;
					case PreprocesorMain.LV3_OVER_STEMMING:
						// porter stemmer
						stemmer.setCurrent(pair[0]);
						stemmer.stem();
						pair[0] = stemmer.getCurrent();
						break;
					case PreprocesorMain.LV4_ROOTWORD_STEMMING_LITE:
						pair = customStemmer.stem(pair, true);
						break;
					}

					// if (pair.length != 4) {
					// System.out.println("Can't normalize this text, will ignore it: " + input);
					// return null;
					// }
					if (needPOS) {
						String taggedWord = null;
						try {

							// if (pair[2].equals("-1"))
							// System.out.println();
							// this one has position
							taggedWord = pair[0] + "_" + pair[1] + "_" + pair[2] + "_" + pair[3];

							senOI.add(taggedWord);
						} catch (Exception e) {
							System.err.println("====================================");
							System.err.println(
									"WARNING: An error I have never met before, please send the log back to me.");
							System.err.println("This review will be ignored from our analysis.");
							System.err.println(pair[0]);
							System.err.println(pair[1]);
							System.err.println(input);
							System.err.println("====================================");
						}
					} else {
						if (needCorrection) {
							// this one has position
							senOI.add(pair[0] + "_" + pair[2] + "_" + pair[3]);
						} else {
							senOI.add(pair[0]);
						}

					}
				}

			}
		}
		if (inParenthesesSentence != null) {
			correctedTaggedSentences.add(Util.deepCopyList(inParenthesesSentence));
			sentence = null;
		}
		if (sentence != null) {
			correctedTaggedSentences.add(Util.deepCopyList(sentence));
			sentence = null;
		}
		for (List<String> sen : correctedTaggedSentences)
			debug_println(sen.toString());
		return correctedTaggedSentences;
	}

	// split but no normalize nor POS tagging
	public List<List<String>> splitSentence(String input) throws Exception {
		if (input == null)
			throw new Exception("input is null ");
		String[] taggedTokens = preprocessAndSplitToTaggedTokens(input);
		if (taggedTokens == null)
			return null;
		List<List<String>> correctedTaggedSentences = new ArrayList<>();
		List<String> sentence = null;
		boolean inParentheses = false;
		List<String> inParenthesesSentence = null;
		for (String taggedTok : taggedTokens) {
			// if (!isPositionable(taggedTok)) // this is a wrongly tagged one
			// continue;
			String[] pair = taggedTok.split("_");
			if (!isPositionable(pair[0])) // this is a wrongly tagged one
				continue;
			// if (pair.length == 2) // sometime special characters make splitting
			// // not working
			// continue;
			pair[0] = pair[0].toLowerCase();
			if (pair[0].equals(".") || pair[0].equals(";") || pair[1].equals(".") || pair[1].equals(":")
					|| pair[0].equals("!") || pair[0].equals("?") || pair[1].equals("-RRB-")) {
				// end sentence
				if (inParentheses) {
					if (inParenthesesSentence != null) {
						correctedTaggedSentences.add(Util.deepCopyList(inParenthesesSentence));
						inParenthesesSentence = null;
					}
					if (pair[1].equals("-RRB-"))
						inParentheses = false;
				} else {
					if (sentence != null) {
						correctedTaggedSentences.add(Util.deepCopyList(sentence));
						sentence = null;
					}
				}
			} else {
				if (pair[1].equals(",") || pair[1].equals("``") || pair[1].equals("''") || pair[1].equals("--")
						|| pair[1].equals("$") || pair[1].equals("#") || pair[1].equals("SYM"))
					continue; // ignore all of these special chars and symbols
				if (pair[1].equals("-LRB-")) {
					inParentheses = true;
				} else {
					List<String> senOI = null;
					if (inParentheses) {

						if (inParenthesesSentence == null)
							inParenthesesSentence = new ArrayList<>();
						senOI = inParenthesesSentence;
					} else {
						if (sentence == null)
							sentence = new ArrayList<>();
						senOI = sentence;
					}

					// if (pair.length != 4) {
					// System.out.println("Can't normalize this text, will ignore it: " + input);
					// return null;
					// }
					String taggedWord = null;
					try {

						// if (pair[2].equals("-1"))
						// System.out.println();
						// this one has position
						senOI.add(pair[0]);
					} catch (Exception e) {
						System.err.println("====================================");
						System.err
								.println("WARNING: An error I have never met before, please send the log back to me.");
						System.err.println("This review will be ignored from our analysis.");
						System.err.println(pair[0]);
						System.err.println(pair[1]);
						System.err.println(input);
						System.err.println("====================================");
					}

				}

			}
		}
		if (inParenthesesSentence != null) {
			correctedTaggedSentences.add(Util.deepCopyList(inParenthesesSentence));
			sentence = null;
		}
		if (sentence != null) {
			correctedTaggedSentences.add(Util.deepCopyList(sentence));
			sentence = null;
		}
		for (List<String> sen : correctedTaggedSentences) {
			debug_println(sen.toString());
		}

		return correctedTaggedSentences;
	}

	// will return null if this text is not english
	private String[] preprocessAndSplitToTaggedTokens(String input) {
		NatureLanguageProcessor nlp = NatureLanguageProcessor.getInstance();
		// 0th step: lower case
		input = input.toLowerCase();
		String[] text = null;
		// input = fixORiginalCommonMistake(input);
		// 1st step: replace words with a mapper, keep the whole format
		if (needCorrection) {
			List<String> tokens = NatureLanguageProcessor.wordSplitWithPositions(input);
			List<String> correctedTokens = nlp.correctUsingMapRetainPosition(tokens);
			text = NatureLanguageProcessor.mergeIntoTextWithPosition(correctedTokens);
			if (text == null)
				return null;
			// 2nd step: check if this is a non-English text, if yes then
			// discontinue
			if (isNonEnglishWithPosition(correctedTokens, 0.4, 0.5, 0.6))
				return null;
		} else {
			List<String> tokens = NatureLanguageProcessor.wordSplitWithPositions(input);
			text = NatureLanguageProcessor.mergeIntoTextWithPosition(tokens);
			if (text == null)
				return null;
		}
		// System.out.println(text);
		// 3rd step: tag the whole thing
		String taggedText = nlp.findPosTag(text[0]);
		debug_println(taggedText);
		debug_println(text[1]);
		// 4th step: stem and correct every words.
		String[] taggedTokens = taggedText.split("\\s+");
		// int len = text[1].split("\\s+").length;
		// if(len!= taggedTokens.length) {
		// System.err.println(taggedTokens.length +"!=" + len);
		// System.err.println(text[1]);
		// System.err.println(text[0]);
		// System.err.println(taggedText);
		// //System.err.println(input);
		// }
		// return taggedTokens;
		return addPositionForTaggedTokens(taggedTokens, text[1].split("\\s+"), text[1], text[0], taggedText, input);
	}

	public String[] addPositionForTaggedTokens(String[] taggedTokens, String[] positions, String text1, String text0,
			String taggedText, String original) {

		// this array shoukd be the same size with positions, else we can't map them
		// together
		String[] beforeTagged = text0.split("\\s+");
		if (beforeTagged.length != positions.length)
			return null;
		// removing all the -1 pos (special chars)
		StringBuilder testString = new StringBuilder();
		int arrayID = 0;
		int arrayIDNOMATCH = -1;
		for (int i = 0; i < taggedTokens.length; i++) {
			// we don't care about these
			if (!isPositionable(taggedTokens[i])) {
				if (arrayID != -1)
					if (arrayID < positions.length)
						while (positions[arrayID].equals("-1_-1")) {

							arrayID++;
							if (arrayID == positions.length) {
								arrayID = -1; // signal ending
								break;
							}
						}
					else
						arrayID = -1; // signal ending
				continue;// doens't matter what tag, if it does not cointain valid characters, it will
							// not match with any position
			}
			String[] pair = taggedTokens[i].split("_");
			if (arrayID == -1 && arrayIDNOMATCH != -1) {
				// System.err.print(" " + pair[0] + "_" + positions[arrayIDNOMATCH]);
				testString.append(taggedTokens[i] + "_" + positions[arrayIDNOMATCH] + " ");
				taggedTokens[i] = taggedTokens[i] + "_" + positions[arrayIDNOMATCH];
				break;
			}
			if (arrayID == -1) {
				// this means we mismatched and some text token were left.
				System.err.println("WARNING: Cannot match this processed text back to the original document.");
				System.err.println("====================================");
				System.err.println(text1);
				System.err.println(original);
				System.err.println(taggedText);
				System.err.println(testString.toString());
				System.err.println("====================================");
				return null;
			}
			if (pair[0].equals(beforeTagged[arrayID])) {
				// System.err.print(" " + pair[0] + "_" + positions[arrayID]);
				testString.append(taggedTokens[i] + "_" + positions[arrayID] + " ");
				taggedTokens[i] = taggedTokens[i] + "_" + positions[arrayID];
				// advance array ID to find new match
				arrayID++;
				if (arrayID < positions.length)
					while (arrayID < positions.length && positions[arrayID].equals("-1_-1")) {
						arrayID++;
						if (arrayID == positions.length) {
							arrayID = -1; // signal ending
							break;
						}
					}
				else
					arrayID = -1; // signal ending
				if (arrayIDNOMATCH != -1) {
					arrayIDNOMATCH = -1;
				}
			} else {
				if (arrayIDNOMATCH == -1) {
					// first time not matching
					arrayIDNOMATCH = arrayID;
					// System.err.print(" " + pair[0] + "_" + positions[arrayIDNOMATCH]);
					testString.append(taggedTokens[i] + "_" + positions[arrayIDNOMATCH] + " ");
					taggedTokens[i] = taggedTokens[i] + "_" + positions[arrayIDNOMATCH];
					// advance array ID to find new match
					arrayID++;
					// in case it's a dot sequence
					if (pair[0].equals("...")) {
						while (arrayID < beforeTagged.length && beforeTagged[arrayID].equals("."))
							arrayID++;
						if (arrayID == beforeTagged.length)
							arrayID = -1; // signal ending
					}
					if (arrayID >= 0 && arrayID < positions.length) {
						while (arrayID < positions.length && positions[arrayID].equals("-1_-1")) {
							arrayID++;
							if (arrayID == positions.length) {
								arrayID = -1; // signal ending
								break;
							}
						}
					} else
						arrayID = -1; // signal ending
				} else {
					// not the first time not matching
					// System.err.print(" " + pair[0] + "_" + positions[arrayIDNOMATCH]);
					testString.append(taggedTokens[i] + "_" + positions[arrayIDNOMATCH] + " ");
					taggedTokens[i] = taggedTokens[i] + "_" + positions[arrayIDNOMATCH];
				}
			}
			// pair[0] = pair[0].toLowerCase();

			// if (pair[0].equals(".") || pair[0].equals(";") || pair[1].equals(".") ||
			// pair[1].equals(":")
			// || pair[0].equals("!") || pair[0].equals("?") || pair[1].equals("-RRB-") ||
			// pair[1].equals("-LRB-")
			// || pair[1].equals(",") || pair[1].equals("``") || pair[1].equals("''") ||
			// pair[1].equals("--")
			// || pair[1].equals("$") || pair[1].equals("#") || pair[1].equals("SYM")) {
			// }else {
			// lenCount++;
			// }
		}
		return taggedTokens;
	}

	// has at least 1 valid character, stop at _ because input is a POS tagged token
	// (text_tag)
	private boolean isPositionable(final CharSequence input) {
		if (((String) input).contains("-LRB-") || ((String) input).contains("-RRB-") || ((String) input).contains("SYM")
				|| ((String) input).contains("IN"))
			return true;
		for (int i = 0; i < input.length(); i++) {
			final char c = input.charAt(i);
			if (c >= 'A' && c <= 'Z')
				return false; // things get to this point must be lower cased
			if (c == '_')
				return false; // should not have any '_' in the text anymore
			if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))
				return true;
			if (c == '\'' || c == '?' || c == '(' || c == ')' || c == '.' || c == ';' || c == '!' || c == '`')
				return true;

		}
		return false;
	}

	public String fixORiginalCommonMistake(String input) {
		return input.replace(" havn't ", " haven't ").replace(" can't ", " cann't ").replace(" can.t ", " cann.t ")
				.replace("n.t ", " not ").replace("n't ", " not ");
	}

	// will return null if this text is not english
	public String[] preprocessAndSplitToTokens(String input) {
		NatureLanguageProcessor nlp = NatureLanguageProcessor.getInstance();
		// 0th step: lower case
		input = input.toLowerCase();
		// 1st step: replace words with a mapper, keep the whole format
		List<String> tokens = NatureLanguageProcessor.wordSplit(input);
		List<String> correctedTokens = nlp.correctUsingMap(tokens);
		String text = NatureLanguageProcessor.mergeIntoText(correctedTokens);
		if (text == null)
			return null;
		// 2nd step: check if this is a non-English text, if yes then
		// discontinue
		if (isNonEnglish(correctedTokens, 0.4, 0.5, 0.6))
			return null;
		// System.out.println(text);
		// 3rd step: tag the whole thing
		// String taggedText = nlp.findPosTag(text);
		// debug_println(taggedText);
		// 4th step: stem and correct every words.
		String[] Tokens = text.split("\\s+");
		return Tokens;
	}

	public boolean isEnglish(String input) {
		NatureLanguageProcessor nlp = NatureLanguageProcessor.getInstance();
		// 0th step: lower case
		input = input.toLowerCase();
		// 1st step: replace words with a mapper, keep the whole format
		List<String> tokens = NatureLanguageProcessor.wordSplit(input);
		List<String> correctedTokens = nlp.correctUsingMap(tokens);
		String text = NatureLanguageProcessor.mergeIntoText(correctedTokens);
		if (text == null)
			return false;
		if (isNonEnglish(correctedTokens, 0.4, 0.5, 0.6))
			return false;
		return true;
	}

	// An optional output. Good for some task, the added computational cost is
	// not too much.
	public CleansedText preprocessText(String rawText) throws Exception {
		Utils.POSTagConverter POSconverter = Utils.POSTagConverter.getInstance();
		TextNormalizer normalizer = TextNormalizer.getInstance();
		List<List<String>> processedTaggedSentences = normalizer.normalize_SplitSentence(rawText,
				PreprocesorMain.LV1_SPELLING_CORRECTION, true);
		if (processedTaggedSentences == null)
			return null;
		int[][] POSTag = new int[processedTaggedSentences.size()][];
		String[][] words = new String[processedTaggedSentences.size()][];
		for (int i = 0; i < processedTaggedSentences.size(); i++) {
			List<String> wordList = processedTaggedSentences.get(i);
			POSTag[i] = new int[wordList.size()];
			words[i] = new String[wordList.size()];
			for (int pairIndex = 0; pairIndex < wordList.size(); pairIndex++) {
				String[] pair = wordList.get(pairIndex).split("_");
				if (pair.length != 2)
					continue;
				words[i][pairIndex] = pair[0];
				POSTag[i][pairIndex] = POSconverter.getCode(pair[1]);
			}
		}

		return new CleansedText(POSTag, words);

	}

	public static class CleansedText {
		public String[][] mSentencesOfWords = null;
		public int[][] mSentencesOfPOS = null;

		public CleansedText(int[][] POSTag, String[][] words) {
			mSentencesOfPOS = POSTag;
			mSentencesOfWords = words;
			// TODO Auto-generated constructor stub
		}

		public String toProperString() {
			StringBuilder properString = new StringBuilder();
			for (int i = 0; i < mSentencesOfPOS.length; i++) {
				for (int j = 0; j < mSentencesOfPOS[i].length; j++) {
					properString.append(mSentencesOfWords[i][j]).append(" ");
				}
				properString.deleteCharAt(properString.length() - 1);
				properString.append(".");
			}
			return properString.toString();
		}
	}

	public static void main(String[] args) throws FileNotFoundException {
		TextNormalizer normalizer = TextNormalizer.getInstance();
		normalizer.readConfigINI("C:\\Users\\pmv0006\\Desktop\\ALPACARunningPackage\\dictionary\\config.INI");
		try {
			String test = "Angry birds I am loving the new levels they (the new level. I'd  meant the"
					+ " new levels that is going well) are very challenging. You should make  more levels . (random things) I"
					+ "love angry birds.And you should sign with sponge bob squarepants for"
					+ " an app .And you should youse \"Billy Joel\" music for your background"
					+ " sound. He is running????"
					+ "After this new update I couldnt open the game...... am using redmi note 5!!! pls help me fix this."
					+ "PUBGMOBILE_CS@tencentgames.com with the exact details of this issue and we will gladly assist you as soon as we can."
					+ "Your latest update often goes to a black screen and only displays a blank black screen ....";
			String test2 = "***   this app is a scam!!!**** do not save your credit info in this app!!, "
					+ "it will make you pay for an expensive subscription if you click wrong once. their "
					+ "business plan is to obtain payment details, and then trick their customers into making payments. "
					+ "they do this by providing “oneminus1click payments” defaulting to large sums of money.the app is "
					+ "maliciously designed to make you pay by accident. they will not return any accidental payments. "
					+ "they will not even reply to your messages. also, most of the positive reviews here are fakes.**** scam warning!!****";
			String test3 = "My rating is 4/5 or 7/10 for its usefulness. 8,5/10 in its category, as it is. "
					+ "Meaning it's a free app, so might not be perfect but it serves its purpose. "
					+ "|| Virus definition update is fixed. See edit history. Thanks. || Ps. Google is slow w/ notifications ^^";
			// String test = "I want a other player";
			List<List<String>> results = normalizer.splitSentence(ExperimentReponse.transformCommonLinks(test3, false));
			for (List<String> sentence : results) {
				for (String word : sentence) {
					System.out.print(ExperimentReponse.transformCommonLinks(word, true) + " ");
				}
				System.out.println();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// normalizer.preprocessAndSplitToTaggedTokens("c");

	}
}
