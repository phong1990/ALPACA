package Analyzers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
	private static final Set<String> specificNounConnectors = new HashSet<>(
			Arrays.asList(new String[] { "and", "or", "of" }));

	public static void extractPatterns(String directory, int sentenceThreshold, int levelOfStemming, String fileOutput,
			boolean strict, Set<String> patterns_expand, PrintWriter matchedSentences_expand) throws Throwable {
		Map<String, Integer> POSStat = new HashMap<>();
		Map<String, String> POSExample = new HashMap<>();
		Set<String> interestingWords = null;
		System.out.println("Start extracting templates from input: " + directory + "/rawData");
		TextNormalizer normalizer = TextNormalizer.getInstance();
		interestingWords = normalizer.getInterestingWords();
		StanfordNLPProcessing nlpStan = StanfordNLPProcessing.getInstance();
		NatureLanguageProcessor nlp = NatureLanguageProcessor.getInstance();
		String[] fileList = Util.listFilesForFolder(directory + "/rawData").toArray(new String[] {});
		Util.shuffleArray(fileList);
		// Set<String> stopWords = nlp.getStopWordSet();
		int processCount = 0, revCount = 0, sentenceCount = 0;
		for (String fileInput : fileList) {
			// this kill switch is being planted everywhere
			if (ALPACAManager.Kill_Switch == true) {
				return;
			}
			if (sentenceThreshold > 0)
				if (sentenceCount >= sentenceThreshold)
					break;
			Scanner br = new Scanner(new FileReader(fileInput));
			while (br.hasNextLine()) {
				// this kill switch is being planted everywhere
				if (ALPACAManager.Kill_Switch == true) {
					br.close();
					return;
				}
				if (sentenceThreshold > 0)
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
					phrases.add(sentenceBld.toString()); // add this sentence as well
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
									+ " sentences from " + revCount + " reviews.");
					}
					sentenceCount++;
				}
				// if (lineCount % 50000 == 0) {
				// writeToFile(
				// fileOutput);
				// System.err.println("Backed up to file");
				// }
			}
			br.close();
			revCount++;
			if (revCount % 10000 == 0)
				System.out.println("processed " + revCount + " reviews");

			System.out.println("Looked for patterns through " + revCount + " reviews");
			// if (sentenceCount == sentenceThreshold)
			// break;
		}
		if (patterns_expand == null)
			writeToFile(fileOutput, POSExample, POSStat);

		System.err.println("Extracted " + processCount + " phrases from " + sentenceCount + " sentences from" + revCount
				+ " reviews.");

	}

	// the order is important for these rules:
	// jj
	// the jj
	// <intensifier> jj
	//
	// nn
	// prp$ nn
	// jj nn
	// cd jj nn
	// the jj nn
	// the cd nn
	// the nn
	//
	// vb
	// jj vb
	//
	//
	// the order is not important for connector rules
	public static String[] buildComposition(String[] POSArr) {
		// TODO Auto-generated method stub
		String[] compArr = buildCOMPJJ(POSArr);
		compArr = combineCOMPJJ(compArr);
		compArr = buildOBJCompositions(compArr);
		compArr = combineCOMPOBJ(compArr);
		compArr = buildVBCompositions(compArr);
		compArr = combineCOMPACT(compArr);
		// extra DT now are not necessary
		compArr = removeExtraDT(compArr);
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

	// deal with COMPACT <preposition> COMPOBJ/PRP and COMPACT COMPOBJ and arrays of
	// COMPACT
	// also a sequence of COMPACT COMPOBJ starting with COMPACT will count as one
	// COMPACT too
	private static String[] combineCOMPACT(String[] compArr) {
		List<String> COMPACTlist = new ArrayList<>();
		int i = 0;
		boolean isConnectingCOMPACT = false;
		Set<String> prepositions = TextNormalizer.getInstance().getPrepositions();
		while (i < compArr.length) {
			if (compArr[i].equals("COMPVB") && !isConnectingCOMPACT) {
				// start the COMPOBJ connecting
				isConnectingCOMPACT = true;
			} else {
				// is in COMPACT connecting sequence
				if (isConnectingCOMPACT) {
					// check if this should be obviously the end of the connecting sequence
					if (!compArr[i].equals("COMPVB") && !compArr[i].equals("COMPNN")
							&& !prepositions.contains(compArr[i])) {
						COMPACTlist.add("COMPVB");
						COMPACTlist.add(compArr[i]);
						isConnectingCOMPACT = false;
					} else {
						// check if this is a preposition, but no next COMPOBJ after it
						if (prepositions.contains(compArr[i])) {
							if (i + 1 < compArr.length) {// there is a next item
								if (!compArr[i + 1].equals("COMPNN") && !compArr[i + 1].equals("PRP")) {
									// the next item isn't COMPOBJ/PRP
									COMPACTlist.add("COMPVB");
									COMPACTlist.add(compArr[i]);
									isConnectingCOMPACT = false;
								} else {
									// this case means next item is a COMPOBJ so continue
								}
							} else {// there is no next item
								COMPACTlist.add("COMPVB");
								COMPACTlist.add(compArr[i]);
								isConnectingCOMPACT = false;
							}
						} else {
							// this case means it is a COMPACT/COMPOBJ so continue
						}
					}
				} else {
					// not part of the connecting sequence
					COMPACTlist.add(compArr[i]);
				}
			}
			i++;
		}
		if (isConnectingCOMPACT) { // in case the last conneting sequence hasn't finished
			COMPACTlist.add("COMPVB");
			isConnectingCOMPACT = false;
		}
		compArr = COMPACTlist.toArray(new String[] {});
		return compArr;
	}

	// those DT are special cases, because they can also be DT and also be a
	// reference to an entity. At this point, since they are not connected to a
	// COMPNN/COMPJJ as the role of a DT, they will be treated as COMPNN.
	// The other possible leftover DT are: a, an, the will be removed since they
	// don't help our generalization process.
	private static String[] removeExtraDT(String[] compArr) {
		List<String> complist = new ArrayList<>();
		for (int i = 0; i < compArr.length; i++) {
			if (!compArr[i].equals("DT") && !compArr[i].equals("PRP$") ) {
				if (compArr[i].equals("this") || compArr[i].equals("that") || compArr[i].equals("these")
						|| compArr[i].equals("those"))
					complist.add("COMPNN");
				else
					complist.add(compArr[i]);
			}

		}

		compArr = complist.toArray(new String[] {});
		return compArr;
	}

	private static String[] buildCOMPJJ(String[] compArr) {
		List<String> compJJlist = new ArrayList<>();
		Set<String> intensifiers = TextNormalizer.getInstance().getIntensifiers();
		for (int i = 0; i < compArr.length; i++) {

			if (compArr[i].equals("JJ")) {
				if ((i - 1) >= 0 && (compArr[i - 1].equals("DT") || compArr[i - 1].equals("this")
						|| compArr[i - 1].equals("that") || compArr[i - 1].equals("these")
						|| compArr[i - 1].equals("those"))) {
					compJJlist.remove(compJJlist.size() - 1);
					compJJlist.add("COMPJJ");
					continue;
				}
				if ((i - 1) >= 0 && intensifiers.contains(compArr[i - 1])) {
					compJJlist.remove(compJJlist.size() - 1);
					compJJlist.add("COMPJJ");
					continue;
				}
				compJJlist.add("COMPJJ");
			} else
				compJJlist.add(compArr[i]);

		}

		compArr = compJJlist.toArray(new String[] {});
		return compArr;
	}

	// add connectors
	private static String[] combineCOMPJJ(String[] compArr) {
		List<String> compJJlist = new ArrayList<>();
		int i = 0;
		boolean isConnectingCOMPJJ = false;
		while (i < compArr.length) {
			if (compArr[i].equals("COMPJJ") && !isConnectingCOMPJJ) {
				// start the COMPOBJ connecting
				isConnectingCOMPJJ = true;
			} else {
				// is in COMPOBJ connecting sequence
				if (isConnectingCOMPJJ) {
					// check if this should be obviously the end of the connecting sequence
					if (!compArr[i].equals("COMPJJ") && !specificNounConnectors.contains(compArr[i])) {
						compJJlist.add("COMPJJ");
						compJJlist.add(compArr[i]);
						isConnectingCOMPJJ = false;
					} else {
						// check if this is a connector, but no next COMPOBJ after it
						if (specificNounConnectors.contains(compArr[i])) {
							if (i + 1 < compArr.length) {// there is a next item
								if (!compArr[i + 1].equals("COMPJJ")) {
									// the next item isn't COMPOBJ
									compJJlist.add("COMPJJ");
									compJJlist.add(compArr[i]);
									isConnectingCOMPJJ = false;
								} else {
									// this case means next item is a COMPOBJ so continue
								}
							} else {// there is no next item
								compJJlist.add("COMPJJ");
								compJJlist.add(compArr[i]);
								isConnectingCOMPJJ = false;
							}
						} else {
							// this case means it is a COMPOBJ so continue
						}
					}
				} else {
					// not part of the connecting sequence
					compJJlist.add(compArr[i]);
				}
			}
			i++;
		}
		if (isConnectingCOMPJJ) { // in case the last conneting sequence hasn't finished
			compJJlist.add("COMPJJ");
			isConnectingCOMPJJ = false;
		}
		compArr = compJJlist.toArray(new String[] {});
		return compArr;
	}

	// add connectors AND prepositions to connect the COMPOBJ
	private static String[] combineCOMPOBJ(String[] compArr) {
		List<String> COMPOBJlist = new ArrayList<>();
		int i = 0;
		boolean isConnectingCOMPOBJ = false;
		Set<String> prepositions = TextNormalizer.getInstance().getPrepositions();
		while (i < compArr.length) {
			if (compArr[i].equals("COMPNN") && !isConnectingCOMPOBJ) {
				// start the COMPOBJ connecting
				isConnectingCOMPOBJ = true;
			} else {
				// is in COMPOBJ connecting sequence
				if (isConnectingCOMPOBJ) {
					// check if this should be obviously the end of the connecting sequence
					if (!compArr[i].equals("COMPNN") && !specificNounConnectors.contains(compArr[i])
							&& !prepositions.contains(compArr[i])) {
						COMPOBJlist.add("COMPNN");
						COMPOBJlist.add(compArr[i]);
						isConnectingCOMPOBJ = false;
					} else {
						// check if this is a connector/preposition, but no next COMPOBJ after it
						if (specificNounConnectors.contains(compArr[i]) || prepositions.contains(compArr[i])) {
							if (i + 1 < compArr.length) {// there is a next item
								if (!compArr[i + 1].equals("COMPNN")) {
									// the next item isn't COMPOBJ
									COMPOBJlist.add("COMPNN");
									COMPOBJlist.add(compArr[i]);
									isConnectingCOMPOBJ = false;
								} else {
									// this case means next item is a COMPOBJ so continue
								}
							} else {// there is no next item
								COMPOBJlist.add("COMPNN");
								COMPOBJlist.add(compArr[i]);
								isConnectingCOMPOBJ = false;
							}
						} else {
							// this case means it is a COMPOBJ so continue
						}
					}
				} else {
					// not part of the connecting sequence
					COMPOBJlist.add(compArr[i]);
				}
			}
			i++;
		}
		if (isConnectingCOMPOBJ) { // in case the last conneting sequence hasn't finished
			COMPOBJlist.add("COMPNN");
			isConnectingCOMPOBJ = false;
		}
		compArr = COMPOBJlist.toArray(new String[] {});
		return compArr;
	}

	private static String[] buildVBCompositions(String[] compArr) {
		List<String> COMPACTlist = new ArrayList<>();
		for (int i = 0; i < compArr.length; i++) {

			if (compArr[i].equals("VB") || compArr[i].equals("VBP")) {
				if ((i - 1) >= 0 && compArr[i - 1].equals("COMPJJ")) {
					COMPACTlist.remove(COMPACTlist.size() - 1);
					COMPACTlist.add("COMPVB");
					continue;
				}
				COMPACTlist.add("COMPVB");
			} else
				COMPACTlist.add(compArr[i]);

		}
		compArr = COMPACTlist.toArray(new String[] {});
		return compArr;
	}

	private static String[] buildOBJCompositions(String[] POSArr) {
		List<String> COMPOBJlist = new ArrayList<>();
		for (int i = 0; i < POSArr.length; i++) {
			if (POSArr[i].equals("PRP")) // replace all PRP as NN to create object
				POSArr[i] = "NN";
			if (POSArr[i].equals("CD")) // replace all CD as NN to create object
				POSArr[i] = "NN";
			if (POSArr[i].equals("LS")) // replace all LS as NN to create object
				POSArr[i] = "NN";
			if (POSArr[i].equals("FW")) // replace all FW as NN to create object
				POSArr[i] = "NN";
			if (POSArr[i].equals("NN")) {
				int iMinus1 = i - 1;
				int iMinus2 = i - 2;
				if (iMinus2 < 0) {
					if (iMinus1 < 0) {
						// nn
						COMPOBJlist.add("COMPNN");
						continue;
					} else {
						// jj nn
						// the nn
						// PRP$ NN
						if (POSArr[iMinus1].equals("COMPJJ") || POSArr[iMinus1].equals("DT")
								|| POSArr[iMinus1].equals("PRP$")) {
							COMPOBJlist.remove(COMPOBJlist.size() - 1);
							COMPOBJlist.add("COMPNN");
							continue;
						}
					}
				} else {
					if (POSArr[iMinus2].equals("CD")) {
						// cd jj nn
						if (POSArr[iMinus1].equals("COMPJJ")) {
							COMPOBJlist.remove(COMPOBJlist.size() - 1);
							COMPOBJlist.remove(COMPOBJlist.size() - 1);
							COMPOBJlist.add("COMPNN");
							continue;
						}
					}
					if (POSArr[iMinus2].equals("DT") || POSArr[iMinus2].equals("this") || POSArr[iMinus2].equals("that")
							|| POSArr[iMinus2].equals("these") || POSArr[iMinus2].equals("those")) {
						// the jj nn
						// the cd nn
						// this/that/these/those/a/an
						if (POSArr[iMinus1].equals("COMPJJ") || POSArr[iMinus1].equals("CD")) {
							COMPOBJlist.remove(COMPOBJlist.size() - 1);
							COMPOBJlist.remove(COMPOBJlist.size() - 1);
							COMPOBJlist.add("COMPNN");
							continue;
						}
					}
					// jj nn
					// the nn
					// PRP$ NN
					if (POSArr[iMinus1].equals("COMPJJ") || POSArr[iMinus1].equals("DT")
							|| POSArr[iMinus1].equals("PRP$") || POSArr[iMinus1].equals("this")
							|| POSArr[iMinus1].equals("that") || POSArr[iMinus1].equals("these")
							|| POSArr[iMinus1].equals("those")) {
						COMPOBJlist.remove(COMPOBJlist.size() - 1);
						COMPOBJlist.add("COMPNN");
						continue;
					}
				}
				COMPOBJlist.add("COMPNN");
			} else {
				COMPOBJlist.add(POSArr[i]);
			}
		}
		String[] compArr = COMPOBJlist.toArray(new String[] {});
		return compArr;
	}

	private static boolean isFilterable(String[] compArr, Set<String> interestingWords, Set<String> domainWords,
			boolean strict) {
		if (compArr.length <= 1)
			return true;
		// String[] lastWord = words.get(words.size() - 1).split("_");
		String lastWord = compArr[compArr.length - 1];
		if (strict)
			if (interestingWords.contains(lastWord))
				return true;
		// can't stop with a word that is not carrying main semantic
		if (strict)
			if (!PhraseAnalyzer.POSTAG_OF_VOCABULARY.contains(lastWord))
				return true;
		// String firstWord = compArr[0];
		// if (strict)
		// if (interestingWords.contains(firstWord) && !domainWords.contains(firstWord))
		// return true;
		// can't start with a word that is not carrying main semantic
		// if (strict)
		// if (!PhraseAnalyzer.POSTAG_OF_VOCABULARY.contains(firstWord) &&
		// !domainWords.contains(firstWord))
		// return true;
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

	// test
	public static void main(String[] args) {
		String[] POSArr = new String[] { "PRP", "hate", "FW" };
		String[] result = buildComposition(POSArr);
		for (String pos : result) {
			System.out.print(pos + " ");
		}
	}
}
