package AU.ALPACA;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;

import Analyzers.PhraseAnalyzer;
import GUI.PatternChooserPanel;
import Utils.Util;

public class HTMLOutput {

	public static void printToHTML(List<FinalResult> results, String outFile, Collection<String> words,
			String patternFileLocation, double threshold) {
		
		
		InputStream inputStream = null;
		PrintWriter pw = null;
		try {
			inputStream = PhraseAnalyzer.class.getResourceAsStream("/html/reportTemplate.html");
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			pw = new PrintWriter(new File(outFile));
			while ((line = br.readLine()) != null) {
				if (line.contains("_placeTopicHere_")) {
					StringBuilder strBdr = new StringBuilder();
					for (String w : words) {
						strBdr.append(w).append(", ");
					}
					strBdr.delete(strBdr.length() - 2, strBdr.length() - 1);
					// line.replace("_placeTopicHere_", strBdr.toString());
					pw.println(strBdr.toString());
				} else {
					if (line.contains("_placePatternAddressHere_")) {
						// line.replace("_placePatternAddressHere_", patternFileLocation);
						if (patternFileLocation.equals(PatternChooserPanel.BUG_PATH))
							pw.println("Complaints (Bug reports, dislikes, negative remarks, etc)");
						else if (patternFileLocation.equals(PatternChooserPanel.REQUEST_PATH))
							pw.println("Requests (any type of request)");
						else
							pw.println(patternFileLocation);
					} else {
						if (line.contains("_placeThresholdHere_")) {
							if (threshold == 0.99)
								pw.println("Exact Match");
							if (threshold == 0.8)
								pw.println("Strictly Match");
							if (threshold == 0.7)
								pw.println("Moderately Match");
							if (threshold == 0.6)
								pw.println("Flexible Match");
							if (threshold == 0.45)
								pw.println("Loosely Match");

						} else {
							if (line.contains("_placeReviewsHere_")) {
								for (FinalResult res : results) {
									pw.println("<tr>");
									pw.println("<td>" + res.time + "</td>");
									pw.println("<td>" + res.rating + "</td>");
									pw.println("<td>" + res.reviewText + "</td>");
									pw.println("</tr>");
								}
							} else {
								pw.println(line);
							}
						}
					}
				}
				

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
				if (pw != null)
					pw.close();
			}
		}
		
		

		Util.openFile(outFile);

	}

	public static class FinalResult {

		public String reviewText;
		public String time;
		public int rating;

		public FinalResult(String review, String time, int rating) {
			this.reviewText = review;
			this.time = time;
			this.rating = rating;
		}
	}
}
