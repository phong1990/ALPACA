package NLP;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;


import Utils.Util;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.parser.lexparser.EnglishTreebankParserParams;
import edu.stanford.nlp.parser.lexparser.Options;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

public class StanfordNLPProcessing {
	private static StanfordNLPProcessing instance = null;
	// adding a couple extra terms to standard lucene list to test against
	// private static String customStopWordList = "";
	private Properties customProps;
	private static StanfordCoreNLP customPipeline;
	// private static Set<?> stopWords;
	private static final int PHRASE_MAX_SIZE = 30;
	private static final String[] SENTIMENT_MEANING = { "Very Negative",
			"Negative", "Neutral", "Positive", "Very Positive" };
	public static final HashSet<String> POSTAG_OF_NOUN = new HashSet<>(
			Arrays.asList(new String[] { "NN", "NNS", "NP" }));
	public static final HashSet<String> POSTAG_OF_ADJECTIVE = new HashSet<>(
			Arrays.asList(new String[] { "ADJP", "JJ" }));
	public static final HashSet<String> POSTAG_OF_VERB = new HashSet<>(
			Arrays.asList(new String[] { "VBG", "VBP", "VB", "VP", "@VP", "VBZ",
					"VBN" }));
	private static final HashSet<String> POSTAG_OF_VERB_COMPLEMENT = new HashSet<>(
			Arrays.asList(new String[] { "ADVP", "PP", "PRT", "ADJP", "S" }));
	private static final HashSet<String> POSTAG_OF_VOCABULARY = new HashSet<>(
			Arrays.asList(new String[] { "NN", "ADJP", "JJ", "VBG", "VBP",
					"VBN", "VBZ", "VB", "NP", "VP", "NNS" }));
	private Options mOp;

	// private static void readStopWordsFromFile() {
	// System.out.println("Read StopWords from file - english.stop");
	// StringBuilder text = new StringBuilder();
	// CSVReader reader = null;
	// try {
	// reader = new CSVReader(new FileReader("english.stop"));
	// String[] row = null;
	// String prefix = "";
	// while ((row = reader.readNext()) != null) {
	// text.append(prefix);
	// prefix = ",";
	// text.append(row[0]);
	// }
	// customStopWordList = text.toString();
	// } catch (FileNotFoundException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } finally {
	// if (reader != null)
	// try {
	// reader.close();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	// }

	protected StanfordNLPProcessing() {
		// Exists only to defeat instantiation.
		// readStopWordsFromFile();
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
		// NER, parsing, and coreference resolution
		customProps = new Properties();

		mOp = new Options(new EnglishTreebankParserParams());
		customProps.put("annotators",
				"tokenize, ssplit, pos, parse, sentiment");
		customProps.setProperty("customAnnotatorClass.stopword",
				"StanfordNLP.StopwordAnnotator");
		customPipeline = new StanfordCoreNLP(customProps);

	}

	public static StanfordNLPProcessing getInstance() {
		if (instance == null) {
			instance = new StanfordNLPProcessing();
		}
		return instance;
	}



	public static String normalizePhrase(Tree input) {
		StringBuilder fullPhrase = new StringBuilder();
		List<Tree> leaves = input.getLeaves();
		for (Tree leaf : leaves) {
			fullPhrase.append(leaf.nodeString() + " ");
		}

		String str = fullPhrase.deleteCharAt(fullPhrase.length() - 1)
				.toString();
		// lemmatize and remove stopwords
		// List<String> words = tokenize(fullPhrase.toString());
		// fullPhrase = new StringBuilder();
		// for (String word : words) {
		// if (word.length() > 1) // dont care about single char
		// fullPhrase.append(word + " ");
		// }
		return str;
	}

	private List<CoreMap> extractSentences(String text) {
		text = text.toLowerCase();
		Annotation document = new Annotation(
				Util.ReplaceNonInterestingChars(text));
		// run all Annotators on this text
		customPipeline.annotate(document);
		List<CoreMap> sentences = document
				.get(CoreAnnotations.SentencesAnnotation.class);
		return sentences;

	}




	public List<String> getPhrasalTokens(String text) {
		List<String> voc = new ArrayList<>();
		text = text.toLowerCase();
		List<CoreMap> sentences = extractSentences(text);

		for (CoreMap sentence : sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			String stc = sentence.toString().toLowerCase();
			if (stc.length() > 3) {
				Tree tree = sentence
						.get(SentimentCoreAnnotations.AnnotatedTree.class);
				if (tree.getLeaves().size() <= PHRASE_MAX_SIZE) {
					String phrase = normalizePhrase(tree);
					if (phrase.length() > 0) {
						voc.add(phrase);
					}
				}
				voc.addAll(travelForPhrasalVocabulary(tree));
				// vocabulary.addAll(travelForPhrasalVocabulary(tree));
				// tree.pennPrint();
			}
		}
		return voc;
	}




	private List<String> travelForPhrasalVocabulary(Tree input) {
		List<String> voc = new ArrayList<>();
		List<Tree> leaves = input.getLeaves();
		if (input.isPhrasal() && leaves.size() <= PHRASE_MAX_SIZE/* 20*/) {
			String phrase = normalizePhrase(input);
			if (phrase.length() > 0) {
				voc.add(phrase);
			}
		}
		for (Tree node : input.getChildrenAsList()) {
			if (!node.isLeaf()) {
				voc.addAll(travelForPhrasalVocabulary(node));
			}
		}
		return voc;
	}

}
