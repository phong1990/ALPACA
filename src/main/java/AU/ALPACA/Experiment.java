package AU.ALPACA;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import Analyzers.ExtractCommonPatterns;
import Analyzers.KeywordAnalyzer;
import Analyzers.KeywordExplorer;
import Analyzers.PhraseAnalyzer;
import Analyzers.SearchEngine;
import Analyzers.TrendAnalyzer;
import Datastores.Dataset;
import Datastores.Document;
import Datastores.FileDataAdapter;
import GUI.ALPACAManager;
import GUI.PatternChooserPanel;
import NLP.WordVec;
import TextNormalizer.TextNormalizer;
import Utils.Util;

// sorely for the experiment with our truthset
public class Experiment {
	// populate data
	private static boolean populateDataFolder(String csvFile, String outFolder) {
		// TODO Auto-generated method stub
		if (!outFolder.endsWith("/") && !outFolder.endsWith("\\"))
			outFolder = outFolder + "/";
		System.out.println("Reading experiment data for the first time");
		File directory = new File(outFolder);
		if (!directory.exists()) {
			directory.mkdirs();
			// If you require it to make the entire directory path including
			// parents,
			// use directory.mkdirs(); here instead.
		}
		String directoryReview = outFolder + "rawData/";
		directory = new File(directoryReview);
		if (!directory.exists()) {
			directory.mkdirs();
			// If you require it to make the entire directory path including
			// parents,
			// use directory.mkdirs(); here instead.
		}

		int counter = 0;
		PrintWriter pwMeta = null;

		CSVReader reader = null;
		try {
			reader = new CSVReader(new InputStreamReader(new FileInputStream(csvFile), "UTF-8"), ',',
					CSVWriter.DEFAULT_ESCAPE_CHARACTER);
			String[] line = reader.readNext(); // read first line to get
												// dataset
												// info
			while ((line = reader.readNext()) != null) {
				if (pwMeta == null) {
					pwMeta = new PrintWriter(new File(outFolder + "metadata.csv"));
					pwMeta.println("\"" + line[0]
							+ "\",\"This is the review dataset\",\"true\",\"true\",\"false\",\"no other metadata\"");
				}
				String text = line[0];
				String meta = "\"" + counter + ".txt\",\"0\",\"0\",\"" + line[1] + "\"";
				pwMeta.println(meta);
				PrintWriter pwRev = new PrintWriter(new File(directoryReview + counter + ".txt"));
				pwRev.println(text);
				pwRev.close();
				counter++;
				if (counter % 100 == 0)
					System.out.println("Retrieved " + counter + " reviews with content");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
		}
		if (pwMeta != null) {
			pwMeta.close();
			System.out.println("Retrieved " + counter + " reviews with content");
			return true;
		}
		return false;
	}

	private static boolean readCONFIG(String configFile) {
		TextNormalizer normalizer = TextNormalizer.getInstance();
		try {
			normalizer.readConfigINI(configFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.err.println("Cannot find the file specified.");
			return false;
		}
		return true;
	}

	public void classifyTypes(Dataset currentDataset, String dataDirectory, double optional_Similarity_Threshold,
			String intentPattern, String classtype, String destinationCSV) {
		try {
			if (currentDataset == null)
				currentDataset = Alpaca.readProcessedData(dataDirectory, PreprocesorMain.LV2_ROOTWORD_STEMMING);
			WordVec word2vec = new WordVec(FileDataAdapter.getLevelLocationDir("word2vec", dataDirectory,
					PreprocesorMain.LV2_ROOTWORD_STEMMING) + "vectors.txt");
			String pattFile = FileDataAdapter.getLevelLocationDir("POSpatterns/", currentDataset.getDirectory(),
					PreprocesorMain.LV2_ROOTWORD_STEMMING) + "rawPattern.csv";
			Set<String[]> POSpatternsOfInterest = null;
			POSpatternsOfInterest = PhraseAnalyzer.getPOSpatterns(intentPattern);
			Set<String> result = KeywordExplorer.extractOpinions(null, word2vec, currentDataset,
					optional_Similarity_Threshold, 0.6, null, pattFile, POSpatternsOfInterest);
			searchForClassification(currentDataset, dataDirectory, result, PreprocesorMain.LV2_ROOTWORD_STEMMING,
					destinationCSV, classtype);
			// print2Files(currentDataset, result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Map<String, Set<String>> readLabels(String directory, int level) throws Exception {
		System.out.println("Reading preprocessed data for the first time");

		String metaDataFileName = directory + "metadata.csv";

		File fcheckExist = new File(metaDataFileName);
		if (!fcheckExist.exists()) {
			throw new FileNotFoundException("This file can't be found: " + metaDataFileName);
		}

		Map<String, Set<String>> reviewAndLabels = new HashMap<>();
		CSVReader reader = null;
		int count = 0;
		try {
			reader = new CSVReader(new FileReader(metaDataFileName), ',', CSVWriter.DEFAULT_ESCAPE_CHARACTER);
			String[] line = reader.readNext(); // read first line to get dataset
												// info
			if (line != null) {
				while ((line = reader.readNext()) != null) {
					String rawtext_fileName = line[0];
					String label = line[3];
					if (label.length() > 0) {
						Set<String> labelSet = new HashSet<>();
						labelSet.addAll(Arrays.asList(label.split("-")));
						reviewAndLabels.put(rawtext_fileName, labelSet);
					}
					if (rawtext_fileName == null)
						throw new Exception("Line " + count + ": no raw text data file, aborting");

				}
				System.out.println("read in " + count + " documents");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		} finally {
			if (reader != null)
				reader.close();
		}
		return reviewAndLabels;
	}

	public static void searchForClassification(Dataset data, String datadirectory, Set<String> topicSequence, int level,
			String destinationCSV, String classType) throws Exception {
		Map<String, Set<String>> reviewAndLabels = readLabels(datadirectory, level);

		try (CSVWriter csvWriter = new CSVWriter(new FileWriter(destinationCSV), ',',
				CSVWriter.DEFAULT_ESCAPE_CHARACTER);) {
			String[] headerRecord = { "review", "manual label", "match", "classified Label" };
			csvWriter.writeNext(headerRecord);

			CSVReader reader = null;
			try {
				for (Document doc : data.getDocumentSet()) {
					int[][] sentences = doc.getSentences();
					// boolean countable = false;

					int[] highlightedPosition = TrendAnalyzer.containsTopicHTMLhighlight(topicSequence, sentences,
							data.getVocabulary());

					String rawText = doc.readRawTextFromDirectory(data.getDirectory());
					if (highlightedPosition != null) {
						// if(highlightedPosition[1]+1 == rawText.length())
						// System.err.println(highlightedPosition[1] +"_" +rawText.length());
						try {
							String highlighted = rawText.substring(0, highlightedPosition[0]) + "<mark>"
									+ rawText.substring(highlightedPosition[0], highlightedPosition[1] + 1) + "</mark>";
							if (highlightedPosition[1] + 1 < rawText.length())
								highlighted += rawText.substring(highlightedPosition[1] + 1);

							Set<String> labels = reviewAndLabels.get(doc.getRawTextFileName());
							if (labels != null) {
								if (labels.contains(classType))
									csvWriter.writeNext(
											new String[] { highlighted, labels.toString(), "matched", classType });
								else {
									csvWriter.writeNext(
											new String[] { highlighted, labels.toString(), "nah", classType });
								}
							} else {
								csvWriter.writeNext(new String[] { highlighted, "", "nah", classType });
							}
						} catch (java.lang.StringIndexOutOfBoundsException e) {
							// TODO: remember to fix this
							System.out.println("WARNING: can't map this sentence: " + rawText);
						}
						// pw.println(highlighted);
					} else {
						Set<String> labels = reviewAndLabels.get(doc.getRawTextFileName());
						if (labels == null)
							csvWriter.writeNext(new String[] { rawText, "", "nah", "nah" });
						else
							csvWriter.writeNext(new String[] { rawText, labels.toString(), "nah", "nah" });
					}
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				throw e;
			} finally {
				if (reader != null)
					reader.close();
			}

		}

		System.out.println();
		// pw.close();
	}

	public static void collectStatistic(String csvFile, String classType) throws Exception {

		File fcheckExist = new File(csvFile);
		if (!fcheckExist.exists()) {
			throw new FileNotFoundException("This file can't be found: " + csvFile);
		}

		CSVReader reader = null;
		int count = 0;
		int relevantDocument = 0, retrievedDocument = 0, correctMatch = 0;

		try {
			reader = new CSVReader(new FileReader(csvFile), ',', CSVWriter.DEFAULT_ESCAPE_CHARACTER);
			String[] line = reader.readNext(); // read first line to rid the labels
			if (line != null) {
				while ((line = reader.readNext()) != null) {
					count++;
					String original = line[1];
					String match = line[2];
					String classified = line[3];
					if (original.contains(classType))
						relevantDocument++;
					if (classified.contains(classType))
						retrievedDocument++;
					if (match.contains("matched"))
						correctMatch++;
				}
				System.out.println("Here are the statistics:");
				float precision = (float) correctMatch / retrievedDocument;
				float recall = (float) correctMatch / relevantDocument;
				float Fscore = 2 / (1 / recall + 1 / precision);
				System.out.println("Total Doc = " + count);
				System.out.println("Total " + classType + " = " + relevantDocument);
				System.out.println("Precision = " + precision);
				System.out.println("Recall = " + recall);
				System.out.println("F1 score = " + Fscore);

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	public static void getFunctionalWords(String file) throws FileNotFoundException {
		Scanner scn = new Scanner(new File(file));
		Set<String> functionalWords = new HashSet<>();
		while (scn.hasNextLine()) {
			String[] line = scn.nextLine().split(" ");
			for (int i = 0; i < line.length; i++)
				if (!Util.hasUpperCase(line[i]))
					functionalWords.add(line[i]);
		}
		scn.close();
		for (String w : functionalWords) {
			System.out.println(w);
		}
	}

	public static void main(String[] args) throws Exception {
		populateDataFolder("G:\\projects\\ALPACA\\fixTruthSet\\fixedTruthSet.csv", "G:\\projects\\ALPACA\\fixTruthSet");
		// getFunctionalWords("C:\\Users\\pmv0006\\Desktop\\ALPACA\\res\\seedPatterns\\bugreport.csv");
		readCONFIG("C:\\Users\\pmv0006\\Desktop\\ALPACARunningPackage\\dictionary\\config.INI");
		Experiment experiment = new Experiment();
		Dataset currentDataset = null;
		// we use ALPACA to pre-process this data so no pre-processing here.
		// expr1: classify intention
		experiment.classifyTypes(currentDataset, "G:\\projects\\ALPACA\\fixTruthSet\\", 0.5,
				"G:\\projects\\ALPACA\\fixTruthSet\\expandedPatterns\\complaintEx_loose.csv", "complaint",
				"G:\\projects\\ALPACA\\fixTruthSet\\experimentResults\\complaint.csv");
		collectStatistic("G:\\projects\\ALPACA\\fixTruthSet\\experimentResults\\complaint.csv", "complaint");
		// expr2: classify requests
	}
}
