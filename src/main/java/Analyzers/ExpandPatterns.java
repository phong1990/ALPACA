package Analyzers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import AU.ALPACA.PreprocesorMain;
import GUI.ALPACAManager;
import GUI.PatternChooserPanel;
import NLP.WordVec;
import TextNormalizer.TextNormalizer;
import Utils.POSTagConverter;
import Utils.Util;

public class ExpandPatterns {
	public static String[] noMeaningFunctionals = new String[] { "or", "and", "with", "on", "in", "of" };
	public static Set<String> uninterestedFunctionals = new HashSet<>(Arrays.asList(noMeaningFunctionals));

	// Sentence Semantic Similarity Measures
	// sum1 = sum(maxSim(w,s2)*weight(w))
	// sum2 = sum(maxSim(w,s1)*weight(w))
	// sim = 0.5* (sum1/sum(weight(w)) + sum2/sum(weight(w)))
	// cite: "Corpus-based and Knowledge-based Measures of Text Semantic
	// Similarity" - Mihalcea
	public static double sentenceSimilarity_Mihalcea(String[] s1, String[] s2, WordVec word2vec,
			Map<String, Double> wordScore) throws UnsupportedEncodingException, SQLException, IOException {

		double[][] similarityMatrix = calculateSimilarityMatrix(s1, s2, word2vec);
		double sum1 = 0d, sum2 = 0d, sumWeight1 = 0d, sumWeight2 = 0d;
		for (int i1 = 0; i1 < s1.length; i1++) {
			double max = 0d;
			for (int i2 = 0; i2 < s2.length; i2++) {
				if (similarityMatrix[i1][i2] > max)
					max = similarityMatrix[i1][i2];
			}

			Double weight = wordScore.get(s1[i1]);
			if (weight == null)
				weight = 0d;
			sum1 += max * weight;
			sumWeight1 += weight;
		}
		for (int i2 = 0; i2 < s2.length; i2++) {
			double max = 0d;
			for (int i1 = 0; i1 < s1.length; i1++) {
				if (similarityMatrix[i1][i2] > max)
					max = similarityMatrix[i1][i2];
			}
			Double weight = wordScore.get(s2[i2]);
			if (weight == null)
				weight = 0d;
			sum2 += max * weight;
			sumWeight2 += weight;
		}
		return 0.5 * (sum1 / sumWeight1 + sum2 / sumWeight2);
	}

	// public static double sentenceSimilarity_wordOrder(String[] s1, String[]
	// s2,
	// WordVec word2vec, double threshold) {
	// double[][] similarityMatrix = calculateSimilarityMatrix(s1, s2,
	// word2vec);
	// byte[] wo1 = new byte[s1.length];
	// byte[] wo2 = new byte[s2.length];
	// Map<String, Integer> indexMap = new HashMap<>();
	// byte increment = 1;
	// // build the word order vector for s1
	// for (int i = 0; i < s1.length; i++) {
	// Integer index = indexMap.get(s1[i]);
	// if (index == null) {
	// wo1[i] = increment;
	// indexMap.put(s1[i], (int) increment);
	// increment++;
	// } else {
	// wo1[i] = (byte) index.intValue(); // if a word repeat, use the
	// // last index
	// }
	// }
	// // build the world order vector for s2
	// for (int i = 0; i < s2.length; i++) {
	// Integer index = indexMap.get(s2[i]);
	// if (index == null) {
	// // search for most similar words that exceed a threshold, if
	// // not, assign 0
	// double maxSim = 0d;
	// int maxJ = -1;
	// for(int j =0 ; j < s1.length; j++){
	// if(similarityMatrix[j][i] > maxSim){
	// maxSim = similarityMatrix[j][i];
	// maxJ = j;
	// }
	// }
	// if (maxJ != -1 && maxSim >= threshold){
	// wo2[i] = (byte)indexMap.get(s1[maxJ]).intValue();
	// }else{
	// wo2[i] = 0;
	// }
	// } else {
	// wo2[i] = (byte) index.intValue(); // use the index of the word
	// // in s1
	// }
	// }
	// }

	// must check null before putting s1 and s2 in here
	private static double[][] calculateSimilarityMatrix(String[] s1, String[] s2, WordVec word2vec) {
		// java must have initialized every items of this matrix to 0d right?
		double[][] similarityMatrix = new double[s1.length][s2.length];
		for (int i1 = 0; i1 < s1.length; i1++) {
			for (int i2 = 0; i2 < s2.length; i2++) {
				// save some calculation cost
				if (s1[i1].equals(s2[i2])) {
					similarityMatrix[i1][i2] = 1d;
				} else {
					similarityMatrix[i1][i2] = word2vec.cosineSimilarityForWords(s1[i1], s2[i2], true);
				}
			}
		}
		return similarityMatrix;
	}

	public static void findSimilarSentences(String directory, WordVec word2vec, Map<String, Double> wordScore,
			List<String[]> baseSentenceSet, double threshold, PrintWriter expandedSentences, int maxNumberOfSentence)
			throws Exception {
		System.out.println(
				"Start looking for similar sentences (threshold = " + threshold + ") in directory: " + directory);
		TextNormalizer normalizer = TextNormalizer.getInstance();
		String[] fileList = Util.listFilesForFolder(directory).toArray(new String[] {});
		Util.shuffleArray(fileList);
		int sentenceCount = 0, similarCount = 0;
		for (String fileInput : fileList) {
			Scanner br = new Scanner(new FileReader(fileInput));
			while (br.hasNextLine()) {
				String line = br.nextLine();
				List<List<String>> sentenceList = normalizer.normalize_SplitSentence(line,
						PreprocesorMain.LV1_SPELLING_CORRECTION, false);
				if (sentenceList == null)
					continue;
				for (List<String> sentence : sentenceList) {
					String[] sen = sentence.toArray(new String[] {});
					if (sen == null)
						continue;
					for (String[] baseSen : baseSentenceSet) {
						double sim = sentenceSimilarity_Mihalcea(sen, baseSen, word2vec, wordScore);
						if (sim >= threshold) {
							// this sentence is similar to one of the sentences
							// we have, so print it out.
							StringBuilder sentenceBld = new StringBuilder();
							for (String token : sentence) {
								sentenceBld.append(token).append(" ");
							}
							sentenceBld.deleteCharAt(sentenceBld.length() - 1);
							expandedSentences.println(sentenceBld.toString());
							similarCount++;
							break;
						}
					}
					sentenceCount++;
					if (sentenceCount % 1000 == 0)
						System.out.println("Looked through " + sentenceCount + " sentences, found " + similarCount
								+ " similar to the original set");
					if (sentenceCount == maxNumberOfSentence)
						break;
				}
				if (sentenceCount == maxNumberOfSentence)
					break;
			}
			br.close();
			if (sentenceCount == maxNumberOfSentence)
				break;
		}
		System.out.println("Looked through " + sentenceCount + " sentences, found " + similarCount
				+ " similar to the original set");
	}

	public static void expand(String originalSentencesFile, String expandedSentenceDir, String directory,
			WordVec word2vec, Map<String, Double> wordScore, double threshold, int maxNumberOfSentence,
			int levelOfStemming, String patternFileOutput) throws Throwable {
		// read original sentences set
		TextNormalizer normalizer = TextNormalizer.getInstance();
		List<String[]> originalSet = new ArrayList<>();
		Scanner br = new Scanner(new FileReader(originalSentencesFile));
		while (br.hasNextLine()) {
			String line = br.nextLine();
			List<List<String>> sentenceList = normalizer.normalize_SplitSentence(line,
					PreprocesorMain.LV1_SPELLING_CORRECTION, false);
			for (List<String> sentence : sentenceList) {
				String[] sen = sentence.toArray(new String[] {});
				if (sen != null)
					originalSet.add(sen);
			}
		}
		br.close();
		// find similar sentences and write them out
		PrintWriter expandedSentences = new PrintWriter(new File(expandedSentenceDir + "/similarSentences.txt"));
		findSimilarSentences(directory, word2vec, wordScore, originalSet, threshold, expandedSentences,
				maxNumberOfSentence);
		expandedSentences.close();

		// extract patterns again, but on the expandedSentences only
		ExtractCommonPatterns.extractPatterns(expandedSentenceDir, Integer.MAX_VALUE, levelOfStemming,
				patternFileOutput, true, null, null);
	}

	public static void expandFromSubFunctionalSequences(String patternResourceFile, String patternSeedFile,
			double threshold, String outFile) throws FileNotFoundException {
		Set<String[]> patternResource = null;
		if (patternResourceFile.equals(PatternChooserPanel.BUG_PATH)
				|| patternResourceFile.equals(PatternChooserPanel.REQUEST_PATH))
			patternResource = getPOSpatternsInClass(patternResourceFile);
		else
			patternResource = getPOSpatterns(patternResourceFile);
		Set<String[]> patternSeed = null;
		if (patternSeedFile.equals(PatternChooserPanel.BUG_PATH)
				|| patternSeedFile.equals(PatternChooserPanel.REQUEST_PATH))
			patternSeed = getPOSpatternsInClass(patternSeedFile);
		else
			patternSeed = getPOSpatterns(patternSeedFile);
		Set<String[]> expandedPatterns = new HashSet<>();

		for (String[] source : patternResource) {
			// this kill switch is being planted everywhere
			if (ALPACAManager.Kill_Switch == true) {
				return;
			}
			Set<String> subSource = getSubsequences(source, false);
			for (String[] seed : patternSeed) {
				// this kill switch is being planted everywhere
				if (ALPACAManager.Kill_Switch == true) {
					return;
				}
				Set<String> subSeed = getSubsequences(seed, false);
				double sim = JaccardSimilarity(subSeed, subSource);
				if (sim >= threshold) {
					expandedPatterns.add(source);
					break;
				}
			}
		}
		// expandedPatterns.addAll(patternSeed);
		write2File(expandedPatterns, patternSeed, outFile, patternResourceFile);
	}

	public static double patternSimilarity(String[] s1, String[] s2, boolean allPattern) {
		Set<String> subSource = getSubsequences(s1, allPattern);
		Set<String> subSeed = getSubsequences(s2, allPattern);
		return JaccardSimilarity(subSeed, subSource);
	}

	private static void write2File(Set<String[]> expandedPatterns, Set<String[]> patternSeed, String outFile,
			String patternResourceFile) throws FileNotFoundException {
		Set<String> patterns = new HashSet<>();
		for (String[] pattArr : expandedPatterns) {
			StringBuilder patt = new StringBuilder();
			for (String pos : pattArr) {
				patt.append(pos).append(" ");
			}
			patt.deleteCharAt(patt.length() - 1);
			patterns.add(patt.toString());
		}
		// print the seeds
		Set<String> oldpatterns = new HashSet<>();
		for (String[] pattArr : patternSeed) {
			StringBuilder patt = new StringBuilder();
			for (String pos : pattArr) {
				patt.append(pos).append(" ");
			}
			patt.deleteCharAt(patt.length() - 1);
			oldpatterns.add(patt.toString());
		}
		PrintWriter pw = new PrintWriter(new File(outFile));
		Scanner br = new Scanner(new FileReader(patternResourceFile));
		br.nextLine();
		while (br.hasNextLine()) {
			String line = br.nextLine();
			String[] words = line.split(",");
			String patt = words[0];
			if (patterns.contains(patt)) {
				if (!oldpatterns.contains(patt))
					pw.println(line);
				// patterns.remove(patt);
			}
		}
		br.close();

		br = new Scanner(new FileReader(patternResourceFile));
		br.nextLine();
		while (br.hasNextLine()) {
			String line = br.nextLine();
			String[] words = line.split(",");
			String patt = words[0];
			if (oldpatterns.contains(patt)) {
				pw.println(line + ",OLD");
				oldpatterns.remove(patt);
			}
		}
		br.close();

		for (String patt : oldpatterns) {
			pw.println(patt);
		}
		pw.close();
	}

	public static double JaccardSimilarity(Set<String> set1, Set<String> set2) {
		if (set1.size() == 0 || set2.size() == 0)
			return 0; // rule out the empty sets
		Set<String> intersection = new HashSet<>(set1); // use the copy
														// constructor
		intersection.retainAll(set2);
		double intersection_size = intersection.size();
		return intersection_size / (set1.size() + set2.size() - intersection_size);
	}

	private static Set<String> getSubsequences(String[] seed, boolean fullPattern) {
		String[] functional_pattern = null;
		if (!fullPattern)
			functional_pattern = POSTagConverter.getInstance().int2String_onlyFunctional(seed);
		else
			functional_pattern = POSTagConverter.getInstance().int2String_NonFunctional(seed);
		Set<String> subsequences = new HashSet<>();
		for (int i = 0; i < functional_pattern.length; i++) {
			if (uninterestedFunctionals.contains(functional_pattern[i]))
				continue;
			subsequences.add(functional_pattern[i]);
			for (int j = i + 1; j < functional_pattern.length; j++) {
				if (uninterestedFunctionals.contains(functional_pattern[j]))
					continue;
				subsequences.add(functional_pattern[i] + "_" + functional_pattern[j]);
				for (int k = j + 1; k < functional_pattern.length; k++) {
					if (uninterestedFunctionals.contains(functional_pattern[k]))
						continue;
					subsequences.add(functional_pattern[i] + "_" + functional_pattern[j] + "_" + functional_pattern[k]);
				}
			}
		}
		return subsequences;
	}

	public static Set<String[]> getPOSpatterns(String infile) throws FileNotFoundException {
		Set<String[]> results = new HashSet<>();
		Scanner br = new Scanner(new FileReader(infile));
		br.nextLine();
		while (br.hasNextLine()) {
			String[] words = br.nextLine().split(",");
			String[] patternArr = words[0].split(" ");
			if (patternArr.length != 0)
				results.add(patternArr);
		}
		br.close();
		return results;
	}

	public static Set<String[]> getPOSpatternsInClass(String infile) throws FileNotFoundException {
		String test = Util.class.getResource(infile).getPath();
		Set<String[]> results = new HashSet<>();
		InputStream inputStream = null;
		try {
			//ClassLoader classLoader = ExpandPatterns.class.getClassLoader();
			inputStream = ExpandPatterns.class.getResourceAsStream(infile);
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			while ((line = br.readLine()) != null) {
				String[] words = line.split(",");
				String[] patternArr = words[0].split(" ");
				if (patternArr.length != 0)
					results.add(patternArr);

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return results;
	}
}
