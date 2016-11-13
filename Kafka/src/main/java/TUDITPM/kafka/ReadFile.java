package TUDITPM.kafka;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ReadFile {

	
	private static List<String> readLines(){
		
		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get("test.txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return lines;
	}
	
	public static String getMessage(String keyword){
		
		StringBuilder sb = new StringBuilder();
		for(String s : readLines()){
			sb.append(s);
		}
		return sb.toString();
	}
}
