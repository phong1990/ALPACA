package Analyzers;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import Datastores.Dataset;
import NLP.WordVec;
import Utils.Util;

public class MARKClusterAnalyzer {

	public static void clusterByWordsList(Set<String> wordList, WordVec word2vec, String outputFolderName,
			Dataset dataset, String metadataFile) {
		try {
			// cluster
			List<List<String>> clusters = clusterWords(wordList, word2vec);
			// write keyword files
			for (List<String> cluster : clusters) {
				if (cluster.isEmpty())
					continue;
				// create a file with the name of a word in the list
				PrintWriter pw = new PrintWriter(new File(outputFolderName + "/" + cluster.get(0) + ".txt"));
				for (String word : cluster) {
					// write the word into that file
					pw.print(word + ",");
				}
				pw.close();
			}
			// write html files
			TimeAnalyzer.writeTimeSeriesForClusters(outputFolderName, clusters, metadataFile, wordList, dataset);
			System.out.println(">> Done Clustering");
			Util.openFile(outputFolderName);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static List<List<String>> clusterWords(Set<String> wordList, WordVec word2vec) throws Throwable {
		// System.out.println("> Read data from test files");
		ArrayList<Item> words = loadTestSet(wordList, word2vec);
		List<List<String>> resultClusters = new ArrayList<>();
		for (List<Clusterable> cluster : cluster(words)) {
			List<String> res = new ArrayList<>();
			for (Clusterable item : cluster) {
				Item it = (Item) item;
				res.add(it.toString());
			}
			if (!res.isEmpty()) {
				resultClusters.add(res);
			}
		}
		return resultClusters;
	}

	private static List<List<Clusterable>> cluster(ArrayList<Item> words) throws Throwable {

		List<Clusterable> itemList = new ArrayList<>();
		if (words.isEmpty()) {
			return null;
		}
		for (Item word : words) {
			itemList.add(word);
		}
		List<List<Clusterable>> clusters = KMeanClustering.clusterBySimilarity(100, itemList,
				(int) Math.round(Math.sqrt(words.size() / 2)), KMeanClustering.COSINE_SIM);
		return clusters;
		// List<List<Clusterable>> clusters = KMeanClustering.clusterBySimilarity(
		// 100, itemList, 0.4, KMeanClustering.COSINE_SIM);
		// System.out.println("> Write clusters to file");
		// writeClustersToFile(clusters, file);
	}

	private static ArrayList<Item> loadTestSet(Set<String> wordList, WordVec word2vec) {
		// TODO Auto-generated method stub
		ArrayList<Item> words = new ArrayList<>();
		for (String w : wordList) {
			Item item = new Item(w, 0, word2vec);
			if (item.getVector() != null) {
				words.add(item);
			} else {
				System.out.println(">> WARNING: The word '" + w
						+ "' is not a valid keyword for this app, please check spelling for it.");
			}
		}
		return words;
	}

	private static class Item extends Clusterable {

		double[] vector = null;
		int frequency;
		String word;
		boolean change = false;
		private WordVec word2vec;

		public Item(String str, int freq, WordVec word2vec) {
			// TODO Auto-generated constructor stub
			this.word2vec = word2vec;
			frequency = freq;
			word = str.intern();
			float[] tempVector = word2vec.getVectorForWord(word);
			if (tempVector != null) {
				vector = new double[WordVec.VECTOR_SIZE];
				for (int i = 0; i < WordVec.VECTOR_SIZE; i++) {
					vector[i] = tempVector[i];
				}
			}
		}

		public String toString() {
			return word;
		}

		@Override
		public double[] getVector() {
			// TODO Auto-generated method stub
			return vector;
		}

		@Override
		public int getFrequency() {
			// TODO Auto-generated method stub
			return frequency;
		}

		@Override
		public void setChange(boolean isChanged) {
			// TODO Auto-generated method stub
			change = isChanged;
		}

		@Override
		public boolean isChanged() {
			// TODO Auto-generated method stub
			return change;
		}

	}
}