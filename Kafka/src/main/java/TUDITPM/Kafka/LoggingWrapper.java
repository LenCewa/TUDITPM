package TUDITPM.Kafka;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggingWrapper {
	private static Logger logger = Logger.getLogger("Logger");

	private static void ensureFileExists(String date) {
		File logsDir = new File("logs/" + date);
		try {
			if (logsDir.mkdirs()) {
				System.out.println("Log directory Created");
			} else {
				System.out.println("Log directory is not created");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void log(String classname, Level level, String msg) {
		FileHandler fh;
		try {
			String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
			ensureFileExists(date);
			// This block configure the logger with handler and formatter
			fh = new FileHandler("logs/" + date + "/" + classname + ".log");
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
			logger.log(level, msg);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
