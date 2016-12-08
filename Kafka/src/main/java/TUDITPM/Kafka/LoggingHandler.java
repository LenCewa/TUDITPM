package TUDITPM.Kafka;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;

public class LoggingHandler {
	private static FileHandler fileHandler;
	
	public static FileHandler getHandler() throws SecurityException, IOException{
		if(fileHandler == null){
			// Set up the file handler
			Date currentDate = new Date();
			DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");
			String strTodaysDate = dateFormat.format(currentDate);
			fileHandler = new FileHandler("logs/" + strTodaysDate + ".xml");
			// Set up logging
			System.setProperty( "java.util.logging.config.file", "properties/logging.properties" );
		}
		return fileHandler;
	}
}
