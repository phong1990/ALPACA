package Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class POSTagConverter {
	private static String[] POSLIST = { "EOS", "UH", "VB", "VBD", "VBG", "VBN",
			"VBP", "VBZ", "WDT", "WP", "WP$", "WRB", "NNPS", "NNS", "PDT",
			"PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO", "CC", "CD",
			"DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNP",
			"UNKNOWN", "ADJP", "@VP", "VP", "NP", "POS", ",", "COMPNN",
			"COMPVB", "COMPJJ" };
	private static Set<String> originalPOSlist = new HashSet<>();
	// COMPJJ: DT_JJ, JJ
	// COMPNN: CD/DT/COMPJJ_NN , NN
	// COMPVB: COMPJJ_VB , VB
	private static Map<String, Integer> tag2code = new HashMap<String, Integer>();
	// private static Map<Byte, String> code2tag = new HashMap<Byte, String>();
	private static POSTagConverter instance = null;

	public static String[] getPOSLIST() {
		return POSLIST;
	}

	public static POSTagConverter getInstance() {
		if (instance == null)
			instance = new POSTagConverter();
		return instance;
	}

	private POSTagConverter() {
		for (int i = 0; i < POSLIST.length; i++) {
			originalPOSlist.add(POSLIST[i]);
		}
		for (int i = 0; i < POSLIST.length; i++) {
			tag2code.put(POSLIST[i], i);
		}
	}

	public void extendPOSLIST(Set<String> extendedList) throws Exception {
		String[] temp = new String[POSLIST.length + extendedList.size()];
		for (int i = 0; i < POSLIST.length; i++) {
			temp[i] = POSLIST[i];
		}
		int index = POSLIST.length;
		for (String newpos : extendedList) {
			temp[index++] = newpos;
		}
		POSLIST = temp;

		for (int i = 0; i < POSLIST.length; i++) {
			tag2code.put(POSLIST[i], i);
			// if (tagIndex >= 256)
			// throw new Exception("Too many functional words, currently the
			// system only handles 200 words");
		}
	}

	public long string2long(String posSeq) {
		String[] patternArr = posSeq.split(" ");
		long tagsequence = 0l;
		for (int i = 0; i < patternArr.length; i++) {
			int code = getCode(patternArr[i]);
			tagsequence = setTagAt(tagsequence, i, code);
		}
		return tagsequence;
	}

	public int[] string2int(String posSeq) {
		String[] patternArr = posSeq.split(" ");
		int[] result = new int[patternArr.length];
		// long tagsequence = 0l;
		for (int i = 0; i < patternArr.length; i++) {
			int code = getCode(patternArr[i]);
			result[i] = code;
			// tagsequence = setTagAt(tagsequence, i, code);
		}
		return result;
	}

	public String[] int2String_onlyFunctional(String[] posSeq) {
		List<String> results = new ArrayList<>();

		for (int i = 0; i < posSeq.length; i++) {
			if (!originalPOSlist.contains(posSeq[i])) {
				results.add(posSeq[i]);
			}
		}
		return results.toArray(new String[] {});
	}

	public String[] int2String_NonFunctional(String[] posSeq) {
		List<String> results = new ArrayList<>();

		for (int i = 0; i < posSeq.length; i++) {
			if (originalPOSlist.contains(posSeq[i])) {
				results.add(posSeq[i]);
			}
		}
		return results.toArray(new String[] {});
	}

	public int getCode(String tag) {

		Integer code = tag2code.get(tag);
		if (code != null)
			return code;
		else
			return -1; // error!!!
	}

	public String getTag(int code) {

		if (code < 0 || code >= POSLIST.length)
			return null;
		return POSLIST[code];
	}

	public long setTagAt(long tagseq, int pos, int code) {
		pos *= 8; // convert position from byte to bit
		return (tagseq | (0xFF << pos)) & (code << pos);
	}

	public byte getTagAt(long tagseq, int pos) {
		pos *= 8; // convert position from byte to bit
		return (byte) (tagseq >> pos);
	}
}