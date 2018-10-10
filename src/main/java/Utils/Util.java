package Utils;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.swing.ImageIcon;

import AU.ALPACA.Alpaca;
import GUI.ALPACAManager;
import GUI.MainGUI;

public class Util {
	public static List<String> listFilesForFolder(final String folderName) throws IOException {
		List<String> filePaths = new ArrayList<>();

		Files.walk(Paths.get(folderName)).forEach(filePath -> {
			if (Files.isRegularFile(filePath)) {
				filePaths.add(filePath.toString());
			}
		});
		return filePaths;
	}

	/**
	 * Export a resource embedded into a Jar file to the local file path.
	 *
	 * @param resourceName
	 *            ie.: "/SmartLibrary.dll"
	 * @return The path to the exported resource
	 * @throws Exception
	 */
	static public String ExportResource(String resourceName, String outFolder) throws Exception {
		InputStream stream = null;
		OutputStream resStreamOut = null;
		try {
			stream = Util.class.getResourceAsStream(resourceName);// note that each / is a directory down in the "jar
																	// tree" been the jar the root of the tree
			if (stream == null) {
				throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
			}

			int readBytes;
			byte[] buffer = new byte[4096];
			resStreamOut = new FileOutputStream(outFolder +"/" + resourceName);
			while ((readBytes = stream.read(buffer)) > 0) {
				resStreamOut.write(buffer, 0, readBytes);
			}
		} catch (Exception ex) {
			throw ex;
		} finally {
			stream.close();
			resStreamOut.close();
		}

		return outFolder +"/" + resourceName;
	}

	public static Map<String, Double> sortByComparator(Map<String, Double> unsortMap, final boolean order) {

		List<Entry<String, Double>> list = new LinkedList<Entry<String, Double>>(unsortMap.entrySet());

		// Sorting the list based on values
		Collections.sort(list, new Comparator<Entry<String, Double>>() {
			public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
				if (order) {
					return o1.getValue().compareTo(o2.getValue());
				} else {
					return o2.getValue().compareTo(o1.getValue());

				}
			}
		});
		// Maintaining insertion order with the help of LinkedList
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Entry<String, Double> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

	/** Returns an ImageIcon, or null if the path was invalid. */
	public static ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = Util.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	public static boolean openFile(String fileName) {
		// text file, should be opening in default text editor
		File file = new File(fileName);
		if (file == null)
			return false;
		// first check if Desktop is supported by Platform or not
		if (!Desktop.isDesktopSupported()) {
			System.out.println("Desktop is not supported");
			return false;
		}

		Desktop desktop = Desktop.getDesktop();
		if (file.exists())
			try {
				desktop.open(file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				System.out.println("WARNING: This OS doesn't support opening the file, please open it yourself!");
				return false;
			}
		return true;
	}

	// Fisher–Yates shuffle array
	public static void shuffleArray(int[] array) {
		int index;
		Random random = new Random();
		for (int i = array.length - 1; i > 0; i--) {
			index = random.nextInt(i + 1);
			if (index != i) {
				array[index] ^= array[i];
				array[i] ^= array[index];
				array[index] ^= array[i];
			}
		}
	}

	// Fisher–Yates shuffle array
	public static void shuffleArray(String[] array) {
		int index;
		String temp = null;
		Random random = new Random();
		for (int i = array.length - 1; i > 0; i--) {
			index = random.nextInt(i + 1);
			if (index != i) {
				temp = array[index];
				array[index] = array[i];
				array[i] = temp;
			}
		}
	}

	// uncomment these on release to get the console progress
	public static void printProgress(double percentage) {
		if (Alpaca.PRINT_PROGRESS) {
			if (percentage == 0) {
				if (!ALPACAManager.getInstance().setProgressbarValue(0))
					System.out.print("(Progress) Percent completed:   0.00 %");
			} else {
				if (!ALPACAManager.getInstance().setProgressbarValue((int) percentage))
					System.out.printf("\b\b\b\b\b\b\b\b%3.2f %%", percentage);
			}
		}
	}

	public static String convertTime(long time) {
		Date date = new Date(time);
		SimpleDateFormat df2 = new SimpleDateFormat("dd/MMM/yyyy");
		return df2.format(date);
	}

	public static String convertTimeDetail(long time) {
		Date date = new Date(time);
		SimpleDateFormat df2 = new SimpleDateFormat("hh:mm dd/MMM/yyyy");
		return df2.format(date);
	}

	public static <T> List<T> deepCopyList(List<T> input) {
		List<T> output = new ArrayList<>();
		for (T item : input)
			output.add(item);
		return output;
	}

	public static <T> String collectionToCSVprintable(Collection<T> list) {
		StringBuilder printable = new StringBuilder();
		for (T item : list) {
			printable.append("[").append(item.toString()).append("]");
		}
		return printable.toString();
	}

	public static String replaceSomeWords(final String text) {
		return text.replace("n't", "not").replace("'s", "is");
	}

	public static String StripDot(final CharSequence input) {
		final StringBuilder sb = new StringBuilder(input.length());
		for (int i = 0; i < input.length(); i++) {
			final char c = input.charAt(i);
			if (c != 46) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	static public double log(double x, int base) {
		return (Math.log(x) / Math.log(base));
	}

	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	public static String ReplaceNonInterestingChars(final CharSequence input) {
		final StringBuilder sb = new StringBuilder(input.length());
		for (int i = 0; i < input.length(); i++) {
			final char c = input.charAt(i);
			if ((c > 32 && c < 48) || (c > 57 && c < 65) || (c > 90 && c < 97) || (c > 122)) {
				sb.append(". ");
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static long normalizeDate(long date) throws ParseException {
		Date d = new Date(date);
		SimpleDateFormat df2 = new SimpleDateFormat("dd-MM-yy");
		String dateText = df2.format(d);
		return df2.parse(dateText).getTime();
	}

	public static String getMonthYear(long date) throws ParseException {
		Date d = new Date(date);
		SimpleDateFormat df2 = new SimpleDateFormat("MMM-yyyy");
		return df2.format(d);
	}

	public static int getMonth(long date) throws ParseException {
		Date d = new Date(date);
		SimpleDateFormat df2 = new SimpleDateFormat("MMM");
		String month = df2.format(d);
		switch (month) {
		case "Jan":
			return 1;
		case "Feb":
			return 2;
		case "Mar":
			return 3;
		case "Apr":
			return 4;
		case "May":
			return 5;
		case "Jun":
			return 6;
		case "Jul":
			return 7;
		case "Aug":
			return 8;
		case "Sep":
			return 9;
		case "Nov":
			return 11;
		case "Dec":
			return 12;
		default:
			return -1;
		}
	}

	public static int[] toIntArray(List<Integer> list) {
		int[] ret = new int[list.size()];
		for (int i = 0; i < ret.length; i++)
			ret[i] = list.get(i);
		return ret;
	}

	public static boolean isNumeric(String str) {
		try {
			double d = Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	public static String removeNonChars(final CharSequence input) {
		final StringBuilder sb = new StringBuilder(input.length());
		for (int i = 0; i < input.length(); i++) {
			final char c = input.charAt(i);
			if ((c > 64 && c < 91) || (c > 96 && c < 123) || (c == ' ')) {
				sb.append(c);
			} else {
				if (c == '.' || c == ',')
					sb.append(' ');
				else
					sb.append(" ");
			}

		}
		return sb.toString();
	}

	public static boolean hasNumeric(final CharSequence input) {
		// TODO Auto-generated method stub
		for (int i = 0; i < input.length(); i++) {
			final char c = input.charAt(i);
			if (c >= '0' && c <= '9') {
				return true;
			}

		}
		return false;
	}
	public static boolean hasUpperCase(final CharSequence input) {
		// TODO Auto-generated method stub
		for (int i = 0; i < input.length(); i++) {
			final char c = input.charAt(i);
			if (c >= 'A' && c <= 'Z') {
				return true;
			}

		}
		return false;
	}
	public static boolean hasSpecialCharacters(final CharSequence input) {
		for (int i = 0; i < input.length(); i++) {
			final char c = input.charAt(i);
			if (!(c >= '0' && c <= '9') && !(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z')) {
				return true;
			}

		}
		return false;
	}

	public static boolean isKeywordInput(final CharSequence input) {
		for (int i = 0; i < input.length(); i++) {
			final char charEntered = input.charAt(i);
			if ((charEntered >= 'A' && charEntered <= 'Z') || (charEntered >= 'a' && charEntered <= 'z')
					|| (charEntered >= '0' && charEntered <= '9') || charEntered == ',' || charEntered == ' ') {

			} else {
				return false;
			}

		}
		return true;
	}

	public static boolean isAscii(char ch) {
		return ch < 128;
	}

	public static boolean isContainingNonASCII(String word) {
		for (int i = 0; i < word.length(); i++) {
			if (word.charAt(i) >= 128)
				return true;
		}
		return false;
	}

	public static boolean isSpecialCharacter(String word) {
		switch (word) {
		case ",":
		case "<":
		case ".":
		case ">":
		case "?":
		case "/":
		case ":":
		case ";":
		case "\"":
		case "'":
		case "{":
		case "[":
		case "}":
		case "]":
		case "+":
		case "=":
		case "_":
		case "-":
		case "~":
		case "`":
		case "!":
		case "@":
		case "#":
		case "$":
		case "%":
		case "^":
		case "&":
		case "*":
		case "(":
		case ")":
		case "|":
		case "\\":
			return true;
		}
		return false;
	}
}
