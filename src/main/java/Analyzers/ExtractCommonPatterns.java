package Analyzers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.xalan.xsltc.compiler.sym;
import org.bytedeco.javacpp.RealSense.context;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import AU.ALPACA.PreprocesorMain;
import GUI.ALPACAManager;

import java.util.Scanner;
import java.util.Set;

import NLP.NatureLanguageProcessor;
import NLP.StanfordNLPProcessing;
import TextNormalizer.TextNormalizer;
import Utils.Util;

public class ExtractCommonPatterns {
	private static final boolean DEBUG = false;
	private static final int THRESHOLD = 1;

	public static void extractPatterns(String directory, int sentenceThreshold, int levelOfStemming, String fileOutput,
			boolean strict, Set<String> patterns_expand, PrintWriter matchedSentences_expand) throws Throwable {
		Map<String, Integer> POSStat = new HashMap<>();
		Map<String, String> POSExample = new HashMap<>();
		Set<String> interestingWords = null;
		System.out.println("Start extracting templates from input: " + directory);
		TextNormalizer normalizer = TextNormalizer.getInstance();
		interestingWords = normalizer.getInterestingWords();
		StanfordNLPProcessing nlpStan = StanfordNLPProcessing.getInstance();
		NatureLanguageProcessor nlp = NatureLanguageProcessor.getInstance();
		String[] fileList = Util.listFilesForFolder(directory).toArray(new String[] {});
		Util.shuffleArray(fileList);
		// Set<String> stopWords = nlp.getStopWordSet();
		int processCount = 0, lineCount = 0, sentenceCount = 0;
		for (String fileInput : fileList) {
			// this kill switch is being planted everywhere
			if (ALPACAManager.Kill_Switch == true) {
				return;
			}
			if (sentenceCount >= sentenceThreshold)
				break;
			Scanner br = new Scanner(new FileReader(fileInput));
			while (br.hasNextLine()) {
				// this kill switch is being planted everywhere
				if (ALPACAManager.Kill_Switch == true) {
					br.close();
					return;
				}
				if (sentenceCount >= sentenceThreshold)
					break;
				String line = br.nextLine();
				List<List<String>> sentenceList = normalizer.normalize_SplitSentence(line,
						PreprocesorMain.LV1_SPELLING_CORRECTION, false);
				if (sentenceList == null)
					continue;
				for (List<String> sentence : sentenceList) {
					// apply stanford phrasal on LV1 normalized sentence.
					StringBuilder sentenceBld = new StringBuilder();
					for (String token : sentence) {							
						sentenceBld.append(token.split("_")[0]).append(" ");
					}
					sentenceBld.deleteCharAt(sentenceBld.length() - 1);

					List<String> phrases = nlpStan.getPhrasalTokens(sentenceBld.toString());
					for (String phrase : phrases) {
						// find POS tag for this phrase
						List<List<String>> tempList = normalizer.normalize_SplitSentence(phrase, levelOfStemming, true);
						if (tempList == null)
							continue;
						if (tempList.isEmpty())
							continue;
						String[] words = tempList.get(0).toArray(new String[] {});
						List<String> POS = new ArrayList<>();
						StringBuilder stemmedText = new StringBuilder();

						mergePOS_corefunction(interestingWords, nlp, words, POS, stemmedText);
						if (POS.size() == 0)
							continue;
						String[] compArr = buildComposition(POS.toArray(new String[] {}));

						// check if this is filterable
						if (isFilterable(compArr, interestingWords, normalizer.getDomainWords(), strict))
							continue;
						StringBuilder posstrBLD = new StringBuilder();
						for (String pos : compArr) {
							posstrBLD.append(pos).append(" ");
						}
						posstrBLD.deleteCharAt(posstrBLD.length() - 1);
						String posstr = posstrBLD.toString();
						if (patterns_expand != null) { // for expansion of
														// pattern
							if (patterns_expand.contains(posstr)) {
								matchedSentences_expand.println(phrase);
							}
						}
						// POS.deleteCharAt(POS.length() - 1);
						stemmedText.deleteCharAt(stemmedText.length() - 1);
						Integer count = POSStat.get(posstr);
						if (count == null) {
							POSStat.put(posstr, 1);
							POSExample.put(posstr, stemmedText + "," + phrase);
						} else
							POSStat.put(posstr, count + 1);
						if (DEBUG)
							System.out.println(POS.toString());
						processCount++;
						if (processCount % 10000 == 0)
							System.out.println("Extracted " + processCount + " phrases from " + sentenceCount
									+ " sentences from " + lineCount + " reviews.");
					}
					sentenceCount++;
				}
				lineCount++;
				if (lineCount % 10000 == 0)
					System.out.println("processed " + lineCount + " sentences");
				// if (lineCount % 50000 == 0) {
				// writeToFile(
				// fileOutput);
				// System.err.println("Backed up to file");
				// }
			}
			br.close();

			System.out.println("Looked for patterns through " + lineCount + " documents");
			if (patterns_expand == null)
				writeToFile(fileOutput, POSExample, POSStat);
			// if (sentenceCount == sentenceThreshold)
			// break;
		}

		System.err.println("Extracted " + processCount + " phrases from " + sentenceCount + " sentences from"
				+ lineCount + " documents.");

	}

	// nn
	// jj nn
	// cd jj nn
	// the jj nn
	// the cd nn
	// the nn
	//
	// vb
	// jj vb
	//
	// jj
	// the jj
	//
	public static String[] buildComposition(String[] POSArr) {
		// TODO Auto-generated method stub
		String[] compArr = buildNNCompositions(POSArr);
		compArr = buildVBCompositions(compArr);
		List<String> compJJlist = new ArrayList<>();
		for (int i = 0; i < compArr.length; i++) {

			if (compArr[i].equals("JJ")) {
				if ((i - 1) >= 0 && compArr[i - 1].equals("DT")) {
					compJJlist.remove(compJJlist.size() - 1);
					compJJlist.add("COMPJJ");
					continue;
				}
				compJJlist.add("COMPJJ");
			} else
				compJJlist.add(compArr[i]);

		}

		compArr = compJJlist.toArray(new String[] {});
		// if (compArr.length > 0){
		// if (compArr[0].equals("need"))
		// System.out.println();
		//
		// if (compArr[0].equals("please"))
		// System.out.println();
		//
		// if (compArr[0].equals("wish"))
		// System.out.println();
		//
		// if (compArr[0].equals("add"))
		// System.out.println();
		// }
		return compArr;
	}

	private static String[] buildVBCompositions(String[] compArr) {
		List<String> compVBlist = new ArrayList<>();
		for (int i = 0; i < compArr.length; i++) {

			if (compArr[i].equals("VB")) {
				if ((i - 1) >= 0 && compArr[i - 1].equals("JJ")) {
					compVBlist.remove(compVBlist.size() - 1);
					compVBlist.add("COMPVB");
					continue;
				}
				compVBlist.add("COMPVB");
			} else
				compVBlist.add(compArr[i]);

		}
		compArr = compVBlist.toArray(new String[] {});
		return compArr;
	}

	private static String[] buildNNCompositions(String[] POSArr) {
		List<String> compNNlist = new ArrayList<>();
		for (int i = 0; i < POSArr.length; i++) {
			if (POSArr[i].equals("NN")) {
				int iMinus1 = i - 1;
				int iMinus2 = i - 2;
				if (iMinus2 < 0) {
					if (iMinus1 < 0) {
						// nn
						compNNlist.add("COMPNN");
						continue;
					} else {
						// jj nn
						// the nn
						if (POSArr[iMinus1].equals("JJ") || POSArr[iMinus1].equals("DT")) {
							compNNlist.remove(compNNlist.size() - 1);
							compNNlist.add("COMPNN");
							continue;
						}
					}
				} else {
					if (POSArr[iMinus2].equals("CD")) {
						// cd jj nn
						if (POSArr[iMinus1].equals("JJ")) {
							compNNlist.remove(compNNlist.size() - 1);
							compNNlist.remove(compNNlist.size() - 1);
							compNNlist.add("COMPNN");
							continue;
						}
					}
					if (POSArr[iMinus2].equals("DT")) {
						// the jj nn
						// the cd nn
						if (POSArr[iMinus1].equals("JJ") || POSArr[iMinus1].equals("CD")) {
							compNNlist.remove(compNNlist.size() - 1);
							compNNlist.remove(compNNlist.size() - 1);
							compNNlist.add("COMPNN");
							continue;
						}
					}
					// jj nn
					// the nn
					if (POSArr[iMinus1].equals("JJ") || POSArr[iMinus1].equals("DT")) {
						compNNlist.remove(compNNlist.size() - 1);
						compNNlist.add("COMPNN");
						continue;
					}
				}
				compNNlist.add("COMPNN");
			} else {
				compNNlist.add(POSArr[i]);
			}
		}
		String[] compArr = compNNlist.toArray(new String[] {});
		return compArr;
	}

	private static boolean isFilterable(String[] compArr, Set<String> interestingWords, Set<String> domainWords,
			boolean strict) {
		// String[] lastWord = words.get(words.size() - 1).split("_");
		String lastWord = compArr[compArr.length - 1];
		if (strict)
			if (interestingWords.contains(lastWord))
				return true;
		// can't stop with a word that is not carrying main semantic
		if (strict)
			if (!PhraseAnalyzer.POSTAG_OF_VOCABULARY.contains(lastWord))
				return true;
		String firstWord = compArr[0];
		if (strict)
			if (interestingWords.contains(firstWord) && !domainWords.contains(firstWord))
				return true;
		// can't start with a word that is not carrying main semantic
		if (strict)
			if (!PhraseAnalyzer.POSTAG_OF_VOCABULARY.contains(firstWord) && !domainWords.contains(firstWord))
				return true;
		// at least has one POSTAG_OF_VOCABULARY word
		for (String word : compArr) {
			if (PhraseAnalyzer.POSTAG_OF_VOCABULARY.contains(word))
				return false;
		}
		return true;
	}

	public static void writeToFile(String output, Map<String, String> POSExample, Map<String, Integer> POSStat)
			throws FileNotFoundException {
		PrintWriter outputAll = new PrintWriter(output);
		outputAll.println("POS,count");
		for (Entry<String, Integer> entry : POSStat.entrySet()) {
			int count = entry.getValue();
			String example = POSExample.get(entry.getKey());
			if (count >= THRESHOLD)
				outputAll.println(entry.getKey() + "," + entry.getValue() + "," + example);
		}
		outputAll.close();
	}

	public static void MapPatterns(String sourcefile, int sentenceThreshold, int levelOfStemming, Set<String> patterns,
			PrintWriter matchedSentences_expand) throws Throwable {
		Set<String> matchedLine = new HashSet<>();
		Set<String> interestingWords = null;
		System.out.println("Start matching patterns from input: " + sourcefile);
		TextNormalizer normalizer = TextNormalizer.getInstance();
		interestingWords = normalizer.getInterestingWords();
		StanfordNLPProcessing nlpStan = StanfordNLPProcessing.getInstance();
		NatureLanguageProcessor nlp = NatureLanguageProcessor.getInstance();
		// Set<String> stopWords = nlp.getStopWordSet();
		int processCount = 0, lineCount = 0, sentenceCount = 0;
		CSVReader reader = new CSVReader(new FileReader(sourcefile), ',', CSVWriter.DEFAULT_ESCAPE_CHARACTER);
		String[] inLine = reader.readNext(); // read first line to get rid of title
		while ((inLine = reader.readNext()) != null) {
			if (sentenceCount >= sentenceThreshold)
				break;
			String line = inLine[1];
			List<List<String>> sentenceList = normalizer.normalize_SplitSentence(line,
					PreprocesorMain.LV1_SPELLING_CORRECTION, false);
			if (sentenceList == null)
				continue;
			for (List<String> sentence : sentenceList) {
				// apply stanford phrasal on LV1 normalized sentence.
				StringBuilder sentenceBld = new StringBuilder();
				for (String token : sentence) {
					sentenceBld.append(token).append(" ");
				}
				sentenceBld.deleteCharAt(sentenceBld.length() - 1);

				List<String> phrases = nlpStan.getPhrasalTokens(sentenceBld.toString());
				for (String phrase : phrases) {
					// find POS tag for this phrase
					List<List<String>> tempList = normalizer.normalize_SplitSentence(phrase, levelOfStemming, true);
					if (tempList == null)
						continue;
					if (tempList.isEmpty())
						continue;
					String[] words = tempList.get(0).toArray(new String[] {});
					List<String> POS = new ArrayList<>();
					StringBuilder stemmedText = new StringBuilder();
					mergePOS_corefunction(interestingWords, nlp, words, POS, stemmedText);
					///////////////////
					if (POS.size() == 0)
						continue;
					String[] compArr = buildComposition(POS.toArray(new String[] {}));

					// check if this is filterable
					if (isFilterable(compArr, interestingWords, normalizer.getDomainWords(), false))
						continue;
					StringBuilder posstrBLD = new StringBuilder();
					for (String pos : compArr) {
						posstrBLD.append(pos).append(" ");
					}
					posstrBLD.deleteCharAt(posstrBLD.length() - 1);
					String posstr = posstrBLD.toString();
					////////////////////
					if (patterns != null) { // for expansion of
											// patterncd
						if (patterns.contains(posstr)) {
							matchedLine.add(inLine[1]);
						}
					}

					if (DEBUG)
						System.out.println(POS.toString());
					processCount++;
					if (processCount % 10000 == 0)
						System.out.println("Processed " + processCount + " phrases from " + sentenceCount
								+ " sentences from " + lineCount + " reviews.");
				}
				sentenceCount++;
			}
			lineCount++;
			if (lineCount % 10000 == 0)
				System.out.println("processed " + lineCount + " sentences");
			// if (lineCount % 50000 == 0) {
			// writeToFile(
			// fileOutput);
			// System.err.println("Backed up to file");
			// }
		}
		reader.close();
		for (String line : matchedLine) {
			matchedSentences_expand.println(line);
		}
		System.out.println("Looked for patterns through " + lineCount + " documents");

		System.err.println("Extracted " + processCount + " phrases from " + sentenceCount + " sentences from"
				+ lineCount + " documents.");

	}

	private static void mergePOS_corefunction(Set<String> interestingWords, NatureLanguageProcessor nlp, String[] words,
			List<String> POS, StringBuilder stemmedText) {
		String mergedPOS = null;
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			String[] w = word.split("_");
			if (w.length == 4 && nlp.POSSET.contains(w[1])) {
				// check if this is the first POS/word in a row
				if (mergedPOS == null) {
					if (interestingWords.contains(w[0]))
						mergedPOS = w[0];
					else
						mergedPOS = w[1];
				} else {
					String POSorWord = null;
					if (interestingWords.contains(w[0])) {
						POSorWord = w[0];
					} else {
						POSorWord = w[1];
					}
					// check if this is a continuing POS/word
					if (mergedPOS.equals(POSorWord)) {
						// ehhh, doing nothing
					} else {
						// a new word, let's put the last
						// POS/word into the POS pattern
						POS.add(mergedPOS);
						// assign the new mergedPOS
						mergedPOS = POSorWord;
					}
				}
				// this is just for example phrase
				stemmedText.append(word).append(" ");
			}
		}
		// add the last POS
		if (mergedPOS != null)
			POS.add(mergedPOS);
	}

}
