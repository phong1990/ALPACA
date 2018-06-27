package Analyzers;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.distribution.WeibullDistribution;

import Datastores.Dataset;
import Datastores.Document;
import Datastores.FileDataAdapter;
import GUI.ALPACAManager;
import NLP.NatureLanguageProcessor;
import Utils.Util;
import Vocabulary.Vocabulary;

public class KeywordAnalyzer {
//	public static void main(String[] args) throws Exception {
//		KeywordAnalyzer analyzer = new KeywordAnalyzer();
//		Dataset data = Alpaca.readProcessedData("D:/projects/ALPACA/NSF/",
//				PreprocesorMain.LV2_ROOTWORD_STEMMING);
//		analyzer.calculateAndWriteKeywordScore(data,
//				PreprocesorMain.LV2_ROOTWORD_STEMMING, false);
//	}

	public static final int TFIDF = 3; // tf * log(1/N+1)
	public static final int MEAN = 2; // average score of a term based on doc
										// rating/score
	public static final int FREQUENCY = 1;
	public static final int WEIBULL_FREQUENCY = 4;
	public static final int CONTRAST_SCORE = 5; // refer to our publication with
												// MARK, this is just for app
												// reviews

	public void calculateAndWriteKeywordScore(Dataset dataset, int level,
			boolean isContrast, String outputFilename) throws Exception {

		Vocabulary voc = dataset.getVocabulary();
//		String outputFilename = FileDataAdapter.getLevelLocationDir(
//				"wordScore/", dataset.getDirectory(), level);
		File fDirectory = new File(outputFilename);
		if (!fDirectory.exists()) {
			fDirectory.mkdirs();
			// If you require it to make the entire directory path including
			// parents,
			// use directory.mkdirs(); here instead.
		}
		outputFilename += "score.csv";
		Map<String, Integer> termCount = new HashMap<>(); // total time a term
															// appeared
		Map<String, Integer> documentCount = new HashMap<>(); // count the
																// number of doc
																// a term
																// appeared in
		Map<String, Integer> termTotalScore = new HashMap<>(); // total score
																// of the docs
																// contain this
																// term
		Map<String, Double> frequencyPercentile = new HashMap<>();
		Map<String, int[]> data4ContrastScore = null;
		if (isContrast)
			data4ContrastScore = new HashMap<>();
		// List<Application> appList = appMan.getAppList();
		// counting statistic
		double percentageCompleted = 0, docCompleted = 0;
		int totalDoc = dataset.getDocumentSet().size();
		Util.printProgress(percentageCompleted);
		for (Document doc : dataset.getDocumentSet()) {
			// this kill switch is being planted everywhere
			if (ALPACAManager.Kill_Switch == true) {
				return;
			}
			// doc.populatePreprocessedDataFromDB(level, dataset);
			if (!doc.isEnglish())
				continue;
			// int docID = doc.getDocumentID();
			int[][] sentences = doc.getSentences();
			String[][] sentencesSTR = new String[sentences.length][];
			for(int i=0; i< sentences.length; i++) {
				sentencesSTR[i] = new String[sentences[i].length];
				for(int j = 0; j < sentences[i].length; j++ )
					sentencesSTR[i][j] = voc.getWordFromDB(sentences[i][j]).getText();
			}
			int rating = doc.getRating();
			countWord(termTotalScore, rating, termCount, documentCount,
					sentencesSTR, data4ContrastScore);

			docCompleted++;
			double newPercentage = Util.round(100*docCompleted/totalDoc, 2);
			if(newPercentage > percentageCompleted){
				percentageCompleted=newPercentage;
				Util.printProgress(percentageCompleted);
			}
		}
		System.out.println();
		// computing and printing to file
		Set<String> stopwords = NatureLanguageProcessor.getInstance()
				.getStopWordSet();
		countFrequencyPercentile(frequencyPercentile, termCount);
		// this kill switch is being planted everywhere
		if (ALPACAManager.Kill_Switch == true) {
			return;
		}
		PrintWriter outputFile = new PrintWriter(new File(outputFilename));
		int totalNumberOfDoc = dataset.getDocumentSet().size();
		outputFile.println(
				"\"term\",\"frequency\",\"mean score\",\"idf\",\"weibull(freq)\",\"contrast\"");
		for (Entry<String, Integer> totalScoreEntry : termTotalScore
				.entrySet()) {
			String term = totalScoreEntry.getKey();
			//String word = voc.getWordFromDB(term).getText();
			if (Util.isNumeric(term))
				continue;
//			if (stopwords.contains(word))
//				continue;
			Integer freq = termCount.get(term);
			Integer docFreq = documentCount.get(term);
			Integer totalScore = totalScoreEntry.getValue();
			double idf = Util.log(totalNumberOfDoc, 10) - Util.log(docFreq, 10);
			if (isContrast) {
				int[] ratingCounts = data4ContrastScore.get(term);
				outputFile.println("\"" + term + "\"," + "\"" + freq + "\","
						+ "\"" + (double) totalScore / docFreq + "\"," + "\""
						+ idf + "\"," + "\"" + frequencyPercentile.get(term)
						+ "\",\""
						+ contrast(ratingCounts[0], ratingCounts[1],
								ratingCounts[2], ratingCounts[3],
								ratingCounts[4], true)[1]
						+ "\"");
			} else {
				outputFile.println("\"" + term + "\"," + "\"" + freq + "\","
						+ "\"" + (double) totalScore / docFreq + "\"," + "\""
						+ idf + "\"," + "\"" + frequencyPercentile.get(term)
						+ "\",\"" + "null" + "\"");
			}
		}
		outputFile.close();
		System.out.println("Done writing to file: " + outputFilename);
		// System.out.println("done writing, exiting program.");
	}

	private static double[] contrast(int x1, int x2, int x3, int x4, int x5,
			boolean negative) {
		double bad = x1 + x2 + 1;
		double good = x4 + x5 + 1;
		if (negative) {
			double score = bad * (bad - good) / good;
			double ratio = bad / good;
			return new double[] { ratio, score };
		} else {
			double score = good * (good - bad) / bad;
			double ratio = good / bad;
			return new double[] { ratio, score };

		}

	}

	private static void countFrequencyPercentile(
			Map<String, Double> frequencyPercentile,
			Map<String, Integer> termCount) {
		List<Integer> sortedFreq = new ArrayList<>(termCount.values());
		Collections.sort(sortedFreq); // ascending
																	// order

		// number of percentile is determined by the number of different
		// frequencies. Ensure the smoothest scoring possible
		Set<Integer> setOfDifferentFreq = new HashSet<>();
		setOfDifferentFreq.addAll(sortedFreq);
		Integer[] rangeList = setOfDifferentFreq.toArray(new Integer[0]);
		Arrays.sort(rangeList);
		
		double smallestPercentile = 1f / rangeList.length;
		for (Entry<String, Integer> entry : termCount.entrySet()) {
			int freq = entry.getValue();
			String word = entry.getKey();
			for (int i = 0; i < rangeList.length; i++) {
				int value = rangeList[i];
				if (freq == value) {
					frequencyPercentile.put(word, smallestPercentile * i);
					break;
				}
			}
		}
		
		WeibullDistribution distribution = new WeibullDistribution(1.5, 0.78);

		for (Entry<String, Double> entry : frequencyPercentile.entrySet()) {
			double percentile = entry.getValue();
			double density = distribution.density(percentile);
			entry.setValue(density);
		}
	}

	private static void countWord(Map<String, Integer> termMeanScore,
			int score, Map<String, Integer> termCount,
			Map<String, Integer> documentCount, String[][] sentences,
			Map<String, int[]> fiveRatingWordCount) throws Exception {
		Set<String> terms = new HashSet<>();
		for (int s = 0; s < sentences.length; s++) {
			for (int w = 0; w < sentences[s].length; w++) {
				Integer count = termCount.get(sentences[s][w]);
				if (count == null) {
					termCount.put(sentences[s][w], 1);
				} else {
					termCount.put(sentences[s][w], count + 1);
				}
				terms.add(sentences[s][w]);
				// only for 5 rating scheme
				if (fiveRatingWordCount != null) {
					if (score > 5 || score <= 0)
						throw new Exception(
								"not a 5 ratings scheme, value = " + score);
					int[] ratingCount = fiveRatingWordCount
							.get(sentences[s][w]);
					if (ratingCount == null) {
						ratingCount = new int[5];
						ratingCount[score - 1] = 1;
					} else {
						ratingCount[score - 1] += 1;
					}
					fiveRatingWordCount.put(sentences[s][w], ratingCount);
				}
			}
		}

		for (String term : terms) {
			Integer count = documentCount.get(term);
			if (count == null) {
				documentCount.put(term, 1);
			} else {
				documentCount.put(term, count + 1);
			}
			Integer scoreTotal = termMeanScore.get(term);
			if (scoreTotal == null) {
				termMeanScore.put(term, score);
			} else {
				termMeanScore.put(term, scoreTotal + score);
			}
		}
	}
}
