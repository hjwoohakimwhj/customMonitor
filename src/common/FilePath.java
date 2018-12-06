package common;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class FilePath {
	public FilePath() throws IOException {
		File directory =  new File("");
		String course = directory.getCanonicalPath();
		System.out.println(course);
	}

/*	public static void main(String[] args) throws IOException {
		try {
			String dirString = "/tmp/";
			File dir = new File(dirString);
			String expression = "sh alarm.sh";
			Process ps = Runtime.getRuntime().exec(expression, null, dir);
			ps.waitFor();
			BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
			StringBuffer stringBr = new StringBuffer();
			String line;
			while((line = br.readLine()) != null) {
				stringBr.append(line);
			}
			System.out.println(stringBr);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}*/
}
