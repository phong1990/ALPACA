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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.Map.Entry;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import AU.ALPACA.HTMLOutput;
import AU.ALPACA.HTMLOutput.FinalResult;
import Datastores.Dataset;
import Datastores.Document;
import GUI.PatternChooserPanel;
import Utils.Util;

public class TimeAnalyzer {

	public static final long MONTHMILIS = 2592000000L;
	public static final long HOURMILIS = 3600000L;
	public static final long DAYMILIS = 86400000L;

	public static void writeTimeSeriesForClusters(String outFolder, List<List<String>> clusters, String metadataFile,
			Set<String> words, Dataset dataset) throws UnsupportedEncodingException, SQLException, IOException {
		String[] listOfDays = getListOfDaysFromStartToEnd(metadataFile);
		Map<String, Map<String, Integer>> dateNcountOfWords = countTheWords(metadataFile, words, dataset, listOfDays);
		for (List<String> cluster : clusters) {
			Set<String> setOfWords = new HashSet<String>(cluster);
			StringBuilder chartData = getTimeserries(setOfWords, dateNcountOfWords, listOfDays);
			List<HTMLOutput.FinalResult> results = SearchEngine.search(dataset, setOfWords);
			writeToFile(outFolder, cluster, chartData, results);
		}

		Util.openFile(outFolder);
	}

	private static void writeToFile(String outFolder, List<String> words, StringBuilder chartData,
			List<HTMLOutput.FinalResult> results) {
		// copy the required amchart files into this folder
		File fDirectory = new File(outFolder + "/amcharts/images");
		if (!fDirectory.exists()) {
			fDirectory.mkdirs();
			// If you require it to make the entire directory path including
			// parents,
			// use directory.mkdirs(); here instead.
			try {
				Util.ExportResource("/amcharts/serial.js", outFolder);
				Util.ExportResource("/amcharts/amcharts.js", outFolder);
				Util.ExportResource("/amcharts/images/dragIconRoundBig.png", outFolder);
				Util.ExportResource("/amcharts/images/lens.png", outFolder);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// write the htlm file
		InputStream inputStream = null;
		PrintWriter pw = null;
		try {
			inputStream = PhraseAnalyzer.class.getResourceAsStream("/html/trendTemplate.html");
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			pw = new PrintWriter(new File(outFolder + "/" + words.get(0) + ".html"));
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
					if (line.contains("_placeChartDataHere_")) {
						pw.println(chartData.toString());
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


	}

	public static StringBuilder getTimeSeriesForWordSet(String metadataFile, Set<String> words, Dataset dataset) {
		String[] listOfDays = getListOfDaysFromStartToEnd(metadataFile);
		Map<String, Map<String, Integer>> dateNcountOfWords = countTheWords(metadataFile, words, dataset, listOfDays);
		return getTimeserries(words, dateNcountOfWords, listOfDays);
	}

	private static StringBuilder getTimeserries(Set<String> words, Map<String, Map<String, Integer>> dateNcountOfWords,
			String[] listOfDays) {
		// populate the timeseries
		int[] timeSerries = new int[listOfDays.length];
		for (Entry<String, Map<String, Integer>> entry : dateNcountOfWords.entrySet()) {
			if (words.contains(entry.getKey())) {
				Map<String, Integer> dateCountMap = entry.getValue();
				for (int i = 0; i < listOfDays.length; i++) {
					timeSerries[i] += dateCountMap.get(listOfDays[i]);
				}
			}
		}
		// calc moving average
		float[] movingAvr = calcMovingAverage(timeSerries, 20);
		// calc standard deviation
		double[] ratios = new double[timeSerries.length];
		double sigma = calcStandardDeviation(timeSerries, movingAvr);
		for (int k = 0; k < timeSerries.length; k++) {
			double error = timeSerries[k] - movingAvr[k];
			if (sigma != 0) {
				ratios[k] = error / sigma;
			}
		}
		return makeJSONResult(timeSerries, movingAvr, ratios, listOfDays);
	}

	private static Map<String, Map<String, Integer>> countTheWords(String metadataFile, Set<String> words,
			Dataset dataset, String[] listOfDays) {
		// get the list of days from start to end of this dataset
		// init the map of count for each word in each date
		Map<String, Map<String, Integer>> dateNcountOfWords = new HashMap<>();
		for (String w : words) {
			Map<String, Integer> dateCountMap = new HashMap<>();
			for (String date : listOfDays) {
				dateCountMap.put(date, 0);
			}
			dateNcountOfWords.put(w, dateCountMap);
		}
		// count the occurrence of each word in each date (an occurrence is 1 for each
		// review containing it)
		Set<Document> documentSet = dataset.getDocumentSet();
		Vocabulary.Vocabulary voc = dataset.getVocabulary();
		for (Document doc : documentSet) {
			int[][] IDtext = doc.getSentences();
			if (IDtext == null)
				continue;
			String date = convertTime(doc.getTime());
			for (int[] IDsentence : IDtext) {
				for (int ID : IDsentence) {
					try {
						String wordText = voc.getWordFromDB(ID).getText();
						if (words.contains(wordText)) {
							Map<String, Integer> dateCountMap = dateNcountOfWords.get(wordText);
							int count = dateCountMap.get(date) + 1;
							dateCountMap.put(date, count);
						}
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
			}
		}
		return dateNcountOfWords;
	}

	private static String[] getListOfDaysFromStartToEnd(String metadataFile) {
		CSVReader reader = null;
		String[] listOfDays = null;
		try {
			reader = new CSVReader(new FileReader(metadataFile), ',', CSVWriter.DEFAULT_ESCAPE_CHARACTER);
			String[] line = reader.readNext(); // read first line to get dataset
			// info
			long minTime = Long.MAX_VALUE, maxtime = 0;
			while ((line = reader.readNext()) != null) {
				long time = Long.parseLong(line[2]);
				if (time < minTime)
					minTime = time;
				if (time > maxtime)
					maxtime = time;
			}
			long duration = maxtime - minTime;
			int days = (int) (duration / DAYMILIS) + 1; // +1 to make sure we always have an extra day for extra hours
			listOfDays = new String[days];
			int i = 0;
			for (long currentTime = minTime; currentTime <= maxtime; currentTime += DAYMILIS) {
				listOfDays[i] = convertTime(currentTime);
				i++;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		return listOfDays;
	}

	private static StringBuilder makeJSONResult(int[] timeSerries, float[] movingAvr, double[] ratios, String[] dates) {
		if (timeSerries == null || movingAvr == null || ratios == null || dates == null) {
			return null;
		}
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < dates.length; i++) {
			res.append("{\"date\": \"").append(dates[i]).append("\",\r\n" + "				\"count\": ")
					.append(timeSerries[i]).append(",\r\n" + "				\"movingavr\": ").append(movingAvr[i])
					.append(",\r\n" + "				\"ratio\": ").append(ratios[i]).append("}");
			if (i != dates.length - 1)
				res.append(",\r\n");
		}
		return res;
	}

	private static StringBuilder makeXMLResult(long allRating[], float[] movingAvr, double[] ratios, String firstDay) {
		if (allRating == null || movingAvr == null || ratios == null) {
			return null;
		}
		StringBuilder res = new StringBuilder();
		res.append("<timeseries>");
		res.append("<startdate>" + firstDay + "</startdate>");
		res.append("<data>");
		int loop = allRating.length;
		for (int i = 0; i < loop; i++) {
			res.append("<day>");
			res.append("<count>" + allRating[i] + "</count>");
			res.append("<movingavr>" + movingAvr[i] + "</movingavr>");
			res.append("<ratio>" + ratios[i] + "</ratio>");
			res.append("</day>");
		}
		res.append("</data>");
		res.append("</timeseries>");
		return res;
	}

	private static String convertTime(long milis) {
		return new SimpleDateFormat("yyyy-MM-dd").format(new Date(milis));
	}

	private static long convertTime(String time) throws Throwable {
		SimpleDateFormat f = new SimpleDateFormat("MMM dd,yyyy");
		Date date = (Date) f.parse(time);
		return date.getTime();
	}

	public static double calcStandardDeviation(int[] timeseries, float[] means) {
		double sigma = 0;
		for (int i = 0; i < timeseries.length; i++) {
			sigma += (timeseries[i] - means[i]) * (timeseries[i] - means[i]);
		}
		sigma = Math.sqrt(sigma / timeseries.length);
		return sigma;
	}

	public static float[] calcMovingAverage(int[] timeseries, int period) {
		float[] avr = new float[timeseries.length];
		avr[0] = timeseries[0];
		for (int i = 1; i < timeseries.length; i++) {
			int k = Math.max(0, i - period);
			int sum = 0;
			for (int j = k; j < i; j++) {
				sum += timeseries[j];
			}
			avr[i] = sum / (i - k);
		}

		return avr;
	}

}
