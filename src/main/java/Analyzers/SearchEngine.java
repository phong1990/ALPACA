package Analyzers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import AU.ALPACA.HTMLOutput;
import Datastores.Dataset;
import Datastores.Document;
import Utils.Util;
import Vocabulary.Vocabulary;

public class SearchEngine {
	public static List<HTMLOutput.FinalResult> search(Dataset data, Set<String> topicSequence)
			throws UnsupportedEncodingException, SQLException, IOException {

		double percentageCompleted = 0, docCompleted = 0;
		int totalDoc = data.getDocumentSet().size();
		// PrintWriter pw = new PrintWriter(new File(outputfile));
		Util.printProgress(percentageCompleted);

		List<HTMLOutput.FinalResult> printableResults = new ArrayList<>();
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		for (Document doc : data.getDocumentSet()) {
			int[][] sentences = doc.getSentences();
			// boolean countable = false;

			int[] highlightedPosition = TrendAnalyzer.containsTopicHTMLhighlight(topicSequence, sentences,
					data.getVocabulary());

			docCompleted++;

			if (highlightedPosition != null) {
				String rawText = doc.readRawTextFromDirectory(data.getDirectory());
				// if(highlightedPosition[1]+1 == rawText.length())
				// System.err.println(highlightedPosition[1] +"_" +rawText.length());
				try {
					String highlighted = rawText.substring(0, highlightedPosition[0]) + "<mark>"
							+ rawText.substring(highlightedPosition[0], highlightedPosition[1] + 1) + "</mark>";
					if (highlightedPosition[1] + 1 < rawText.length())
						highlighted += rawText.substring(highlightedPosition[1] + 1);
					String reply = doc.readReplyFromDirectory(data.getDirectory());
					HTMLOutput.FinalResult res = new HTMLOutput.FinalResult(highlighted,reply, df.format(doc.getTime()),
							doc.getRating());
					printableResults.add(res);
				} catch (java.lang.StringIndexOutOfBoundsException e) {
					// TODO: remember to fix this
					System.out.println("WARNING: can't map this sentence: " +rawText);
				}
				// pw.println(highlighted);
				double newPercentage = Util.round(100 * docCompleted / totalDoc, 2);
				if (newPercentage > percentageCompleted) {
					percentageCompleted = newPercentage;
					Util.printProgress(percentageCompleted);
				}
			}
		}

		System.out.println();
		return printableResults;
		// pw.close();
	}

	// for experimenting purpose
	public static void splitRequestBugIntoProperDataDocuments(String directory) throws FileNotFoundException {
		PrintWriter meta = new PrintWriter(new File(directory) + "/metadata.csv");
		meta.println(
				"\"Emitza Gutzman\",\"This is the dataset for DECA\",\"false\",\"false\",\"false\",\"no other metadata\"");
		Scanner scn = new Scanner(new File(directory + "/1.txt"));
		int count = 0;
		while (scn.hasNextLine()) {
			String line = scn.nextLine();
			meta.println("\"" + count + ".txt\",\"\",\"no timestamp data\"");
			PrintWriter doc = new PrintWriter(new File(directory + "/rawData/" + count + ".txt"));
			doc.print(line);
			doc.close();
			System.out.println(count);
			count++;
		}
		scn.close();
		meta.close();
	}

	public static void main(String[] args) throws FileNotFoundException {
		splitRequestBugIntoProperDataDocuments("D:/projects/ALPACA/DECATruthSet");
	}
}
