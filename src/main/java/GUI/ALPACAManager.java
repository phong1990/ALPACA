package GUI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import AU.ALPACA.Alpaca;
import AU.ALPACA.HTMLOutput;
import AU.ALPACA.PreprocesorMain;
import Analyzers.ExpandPatterns;
import Analyzers.ExtractCommonPatterns;
import Analyzers.KeywordAnalyzer;
import Analyzers.KeywordExplorer;
import Analyzers.PhraseAnalyzer;
import Analyzers.SearchEngine;
import Datastores.Dataset;
import Datastores.FileDataAdapter;
import NLP.WordVec;
import TextNormalizer.TextNormalizer;
import Utils.Util;

public class ALPACAManager {
	public static boolean Kill_Switch = false;
	private static ALPACAManager instance = null;

	private Dataset currentDataset = null;
	public String dataDirectory = null;

	public boolean doWord2Vec = false;

	public boolean doPatternExtraction = false;

	private MainGUI mainGUI = null;

	private ALPACAManager() {

	}

	public static ALPACAManager getInstance() {
		if (instance == null)
			instance = new ALPACAManager();
		return instance;
	}

	public boolean isThisLikely2beADatafolder(String dataFolder) {
		String metaDataFileName = dataFolder + "metadata.csv";
		File fcheckExist = new File(metaDataFileName);
		if (!fcheckExist.exists()) {
			return false;
		}
		return true;
	}

	public boolean isDatafolderPreprocessed(String datafolder) {
		String cleansedTextLocationDir = FileDataAdapter.getLevelLocationDir(FileDataAdapter.CLEANSED_SUBDIR,
				datafolder, PreprocesorMain.LV2_ROOTWORD_STEMMING);
		String keywordLocationDir = FileDataAdapter.getLevelLocationDir(FileDataAdapter.KEYWORD_SUBDIR, datafolder,
				PreprocesorMain.LV2_ROOTWORD_STEMMING);
		if (FileDataAdapter.isCompleted(keywordLocationDir) && FileDataAdapter.isCompleted(cleansedTextLocationDir)) {
			return true;
		}
		return false;
	}

	private void preprocessing(String additionalFile) {
		// synchronized block ensures only one thread
		// running at a time.
		synchronized (this) {
			if (dataDirectory == null) {
				System.out.println("Please set up you data folder first");
				return;
			}

			if (additionalFile == null) {
				System.out.println(
						"Notice: No additional text file chosen, we will train with just text from this dataset");
			} else {
				System.out.println("Notice: we will train with text from both this dataset and additional file at: "
						+ additionalFile);
			}
			try {
				System.out.println("Begin processing the dataset in: " + dataDirectory);
				currentDataset = PreprocesorMain.processDBData(dataDirectory, PreprocesorMain.LV2_ROOTWORD_STEMMING,
						additionalFile);
				// this kill switch is being planted everywhere
				if (ALPACAManager.Kill_Switch == true) {
					return;
				}
				if (doWord2Vec == true) {
					doWord2Vec = false;
					PreprocesorMain.trainWithWord2Vec(currentDataset);
					System.out.println(">>Done with word2vec.");
				}
				// this kill switch is being planted everywhere
				if (ALPACAManager.Kill_Switch == true) {
					return;
				}
				if (doPatternExtraction == true) {
					doPatternExtraction = false;
					String pattDir = FileDataAdapter.getLevelLocationDir("POSpatterns/", currentDataset.getDirectory(),
							PreprocesorMain.LV2_ROOTWORD_STEMMING);
					File fDirectory = new File(pattDir);
					if (!fDirectory.exists()) {
						fDirectory.mkdirs();
						// If you require it to make the entire directory path including
						// parents,
						// use directory.mkdirs(); here instead.
					}

					ExtractCommonPatterns.extractPatterns(dataDirectory, 100000, PreprocesorMain.LV2_ROOTWORD_STEMMING,
							pattDir + "rawPattern.csv", true, null, null);
					System.out.println(">>Done with template extraction.");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public void startPreprocessingThread(String additionalFile) {
		// Create a thread to run preprocessing in
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				mainGUI.enableAllFunction(false);
				preprocessing(additionalFile);
				mainGUI.enableAllFunction(true);
				if (Kill_Switch == true) {
					Kill_Switch = false;
					System.out.println(">>>>>>>>>>Preprocessing task was canceled successfully<<<<<<<<<<");
				}
			}
		});
		t1.start();

	}

	private void analyzeKeywords() {
		synchronized (this) {
			try {
				if (currentDataset == null)
					currentDataset = Alpaca.readProcessedData(dataDirectory, PreprocesorMain.LV2_ROOTWORD_STEMMING);
				KeywordAnalyzer analyzer = new KeywordAnalyzer();
				String outputFilename = FileDataAdapter.getLevelLocationDir("wordScore/", currentDataset.getDirectory(),
						PreprocesorMain.LV2_ROOTWORD_STEMMING);
				analyzer.calculateAndWriteKeywordScore(currentDataset, PreprocesorMain.LV2_ROOTWORD_STEMMING, true,
						outputFilename);
				Util.openFile(outputFilename + "score.csv");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void expandPattern(String patternResourceFile, String patternSeedFile, String outFile, double threshold) {
		try {
			System.out.println(">> Start expanding pattern...");
			ExpandPatterns.expandFromSubFunctionalSequences(patternResourceFile, patternSeedFile, threshold, outFile);
			System.out.println(">> Done expanding pattern!");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void startPatternExpansionThread(String seedFile, String outfile, double threshold) {
		// Create a thread to run preprocessing in
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (currentDataset == null)
						currentDataset = Alpaca.readProcessedData(dataDirectory, PreprocesorMain.LV2_ROOTWORD_STEMMING);
					String pattFile = FileDataAdapter.getLevelLocationDir("POSpatterns/", currentDataset.getDirectory(),
							PreprocesorMain.LV2_ROOTWORD_STEMMING) + "rawPattern.csv";
					mainGUI.enableAllFunction(false);
					expandPattern(pattFile, seedFile, outfile, threshold);
					mainGUI.enableAllFunction(true);
					if (Kill_Switch == true) {
						Kill_Switch = false;
						System.out.println(">>>>>>>>>>Pattern Expansion task was canceled successfully<<<<<<<<<<");
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		t1.start();
	}

	private void expandingTopic(Set<String> words, String outDir) {
		try {
			if (currentDataset == null)
				currentDataset = Alpaca.readProcessedData(dataDirectory, PreprocesorMain.LV2_ROOTWORD_STEMMING);
			WordVec word2vec = new WordVec(FileDataAdapter.getLevelLocationDir("word2vec", dataDirectory,
					PreprocesorMain.LV2_ROOTWORD_STEMMING) + "vectors.txt");
			// this kill switch is being planted everywhere
			if (ALPACAManager.Kill_Switch == true) {
				return;
			}
			String pattFile = FileDataAdapter.getLevelLocationDir("POSpatterns/", currentDataset.getDirectory(),
					PreprocesorMain.LV2_ROOTWORD_STEMMING) + "rawPattern.csv";
			// this kill switch is being planted everywhere
			if (ALPACAManager.Kill_Switch == true) {
				return;
			}
			String wordScoreFile = FileDataAdapter.getLevelLocationDir("wordScore/", currentDataset.getDirectory(),
					PreprocesorMain.LV2_ROOTWORD_STEMMING) + "score.csv";
			// this kill switch is being planted everywhere
			if (ALPACAManager.Kill_Switch == true) {
				return;
			}
			Map<String, Double> IDFWeights = readWordsSkewness(KeywordAnalyzer.WEIBULL_FREQUENCY, wordScoreFile);
			// this kill switch is being planted everywhere
			if (ALPACAManager.Kill_Switch == true) {
				return;
			}
			Set<String[]> POSpatternsOfInterest = PhraseAnalyzer.getPOSpatterns(pattFile);
			Set<String> result = KeywordExplorer.expand(words, word2vec, currentDataset, 0.7, 0.7, IDFWeights, pattFile,
					POSpatternsOfInterest, outDir);
			System.out.println();
			// open the folder
			Util.openFile(outDir);
		} catch (Throwable e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public Map<String, Double> readWordsSkewness(int typeOfScore, String fileName) throws Throwable {
		// wordScore = new HashMap<>();
		Map<String, Double> IDFWeights = new HashMap<>();
		CSVReader reader = new CSVReader(new FileReader(fileName), ',', CSVWriter.DEFAULT_ESCAPE_CHARACTER);
		String[] line = reader.readNext();
		while ((line = reader.readNext()) != null) {
			double score = Double.valueOf(line[typeOfScore]);
			double idf = Double.valueOf(line[KeywordAnalyzer.TFIDF]);
			// wordScore.put(line[0], score);
			IDFWeights.put(line[0], idf);
		}
		reader.close();
		return IDFWeights;
	}

	public void startKeywordExpandingThread(Set<String> words, String outDir) {
		// Create a thread to run preprocessing in
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				mainGUI.enableAllFunction(false);
				expandingTopic(words, outDir);
				mainGUI.enableAllFunction(true);
				if (Kill_Switch == true) {
					Kill_Switch = false;
					System.out.println(">>>>>>>>>>Topic Expansion task was canceled successfully<<<<<<<<<<");
				}
			}
		});
		t1.start();
	}

	public void startKeywordAnalysingThread() {
		// Create a thread to run preprocessing in
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				mainGUI.enableAllFunction(false);
				analyzeKeywords();
				mainGUI.enableAllFunction(true);
				if (Kill_Switch == true) {
					Kill_Switch = false;
					System.out.println(">>>>>>>>>>Keyword Analyzing task was canceled successfully<<<<<<<<<<");
				}
			}
		});
		t1.start();
	}

	private boolean readCONFIG(String configFile) {
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

	// public void expandKeyword() {
	// WordVec word2vec = new WordVec(vectorFile);
	// if (word2vec == null) {
	// System.out.println("you have to set up a vector file first!");
	// return;
	// }
	// if (args.length == 2)
	// setupThreshold(Double.parseDouble(args[1]));
	// readWordsSkewness(scoringScheme, wordScoreFile);
	// if (currentDataset == null)
	// currentDataset = Alpaca.readProcessedData(dataDirectory,
	// currentLevel);
	// Set<String> result = KeywordExplorer.expand(words, word2vec,
	// currentDataset, optional_Similarity_Threshold, 0.8, IDFWeights,
	// "D:/projects/NewReview/Patterns/PosPatternsPanichella.csv");
	// print2Files(currentDataset, result);
	// System.out.println();
	// }
	public boolean startReadingConfigINIThread(String configFile) {
		return readCONFIG(configFile);
	}

	public boolean setProgressbarValue(int value) {
		if (mainGUI != null) {
			mainGUI.setProgressbarValue(value);
			return true;
		}
		return false;
	}

	private void startGUIThread() {
		// Schedule a job for the event dispatching thread:
		// creating and showing this application's GUI.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Turn off metal's use of bold fonts
				UIManager.put("swing.boldMetal", Boolean.FALSE);
				mainGUI = MainGUI.createAndShowGUI();
			}
		});
	}

	public void startOpinionExtractionThread(Collection<String> words, double optional_Similarity_Threshold,
			String outDirectory, String intentPattern) {
		// Create a thread to run preprocessing in
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				mainGUI.enableAllFunction(false);
				findOpinions(words, optional_Similarity_Threshold, outDirectory, intentPattern);
				mainGUI.enableAllFunction(true);
				if (Kill_Switch == true) {
					Kill_Switch = false;
					System.out.println(">>>>>>>>>>Opinion Extraction task was canceled successfully<<<<<<<<<<");
				}
			}
		});
		t1.start();
	}

	public void findOpinions(Collection<String> words, double optional_Similarity_Threshold, String outDirectory,
			String intentPattern) {
		try {
			if (currentDataset == null)
				currentDataset = Alpaca.readProcessedData(dataDirectory, PreprocesorMain.LV2_ROOTWORD_STEMMING);
			WordVec word2vec = new WordVec(FileDataAdapter.getLevelLocationDir("word2vec", dataDirectory,
					PreprocesorMain.LV2_ROOTWORD_STEMMING) + "vectors.txt");
			// this kill switch is being planted everywhere
			if (ALPACAManager.Kill_Switch == true) {
				return;
			}
			String pattFile = FileDataAdapter.getLevelLocationDir("POSpatterns/", currentDataset.getDirectory(),
					PreprocesorMain.LV2_ROOTWORD_STEMMING) + "rawPattern.csv";
			// this kill switch is being planted everywhere
			if (ALPACAManager.Kill_Switch == true) {
				return;
			}
			String wordScoreFile = FileDataAdapter.getLevelLocationDir("wordScore/", currentDataset.getDirectory(),
					PreprocesorMain.LV2_ROOTWORD_STEMMING) + "score.csv";
			// this kill switch is being planted everywhere
			if (ALPACAManager.Kill_Switch == true) {
				return;
			}
			Map<String, Double> IDFWeights = readWordsSkewness(KeywordAnalyzer.WEIBULL_FREQUENCY, wordScoreFile);
			// this kill switch is being planted everywhere
			if (ALPACAManager.Kill_Switch == true) {
				return;
			}
			Set<String[]> POSpatternsOfInterest = null;
			if (intentPattern.equals(PatternChooserPanel.BUG_PATH)
					|| intentPattern.equals(PatternChooserPanel.REQUEST_PATH))
				POSpatternsOfInterest = PhraseAnalyzer.getPOSpatternsInClass(intentPattern);
			else
				POSpatternsOfInterest = PhraseAnalyzer.getPOSpatterns(intentPattern);
			Set<String> result = KeywordExplorer.extractOpinions(words, word2vec, currentDataset,
					optional_Similarity_Threshold, 0.6, IDFWeights, pattFile, POSpatternsOfInterest);
			searchAndOutput2HTML(currentDataset, result, outDirectory + "/opinions.html", words, intentPattern,
					optional_Similarity_Threshold);
			// print2Files(currentDataset, result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void searchAndOutput2HTML(Dataset dts, Set<String> termset, String htmlFile, Collection<String> words,
			String intentPattern, double threshold) {
		try {

			List<HTMLOutput.FinalResult> printableResults = SearchEngine.search(dts, termset);
			HTMLOutput.printToHTML(printableResults, htmlFile, words, intentPattern, threshold);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		ALPACAManager.getInstance().startGUIThread();
		// testPatternExtraction();

	}

	private static void testPatternExtraction() {
		try {
			ALPACAManager manager = ALPACAManager.getInstance();
			manager.readCONFIG("C:\\Users\\pmv0006\\Desktop\\ALPACA\\dictionary\\config.ini");
			manager.dataDirectory = "C:\\Users\\pmv0006\\Documents\\Magictiles\\";
			manager.currentDataset = Alpaca.readProcessedData(manager.dataDirectory,
					PreprocesorMain.LV2_ROOTWORD_STEMMING);
			String pattDir = FileDataAdapter.getLevelLocationDir("POSpatterns/", manager.currentDataset.getDirectory(),
					PreprocesorMain.LV2_ROOTWORD_STEMMING);
			File fDirectory = new File(pattDir);
			if (!fDirectory.exists()) {
				fDirectory.mkdirs();
				// If you require it to make the entire directory path including
				// parents,
				// use directory.mkdirs(); here instead.
			}

			ExtractCommonPatterns.extractPatterns(manager.dataDirectory, 100000, PreprocesorMain.LV2_ROOTWORD_STEMMING,
					pattDir + "rawPattern.csv", true, null, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void startDATAFolderMakerThread(String csvFile, String outFolder) {
		// TODO Auto-generated method stub
		// Create a thread to run preprocessing in
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				mainGUI.enableAllFunction(false);
				boolean successful = populateDataFolder(csvFile, outFolder);
				if (successful) {
					//mainGUI.enableAllFunction(true);
					mainGUI.dataFolderTextField.setText(outFolder);
					mainGUI.enableBasicFunctions(true);
				}
				if (Kill_Switch == true) {
					Kill_Switch = false;
					System.out.println(">>>>>>>>>>Opinion Extraction task was canceled successfully<<<<<<<<<<");
				}
			}

		});
		t1.start();
	}

	private boolean populateDataFolder(String csvFile, String outFolder) {
		// TODO Auto-generated method stub
		if (!outFolder.endsWith("/") && !outFolder.endsWith("\\"))
			outFolder = outFolder + "/";
		System.out.println("Reading reviews data for the first time");
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
		String directoryReply = outFolder + "reply/";
		directory = new File(directoryReply);
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
				long time = Long.parseLong(line[0]);
				int rating = Integer.parseInt(line[1]);
				String title = line[2];
				String text = line[3];
				String reply = line[4];
				String meta = "\"" + counter + ".txt\",\"" + rating + "\",\"" + time + "\"";
				pwMeta.println(meta);
				PrintWriter pwRev = new PrintWriter(new File(directoryReview + counter + ".txt"));
				if (title.isEmpty())
					pwRev.println(text);
				else
					pwRev.println(title + ". " + text);
				pwRev.close();
				if (!reply.isEmpty()) {
					PrintWriter pwReply = new PrintWriter(new File(directoryReply + counter + ".txt"));
					pwReply.println(reply);
					pwReply.close();
				}
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
}
