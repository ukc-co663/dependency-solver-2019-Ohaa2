package depsolver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Utils {

	public Utils() {
	}

	public static String readFile(String filename) throws IOException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filename));
			StringBuilder sb = new StringBuilder();
			br.lines().forEach(line -> sb.append(line));
			return sb.toString();
		} finally {
			if (br != null) {
				br.close();
			}
		}
	}

}
