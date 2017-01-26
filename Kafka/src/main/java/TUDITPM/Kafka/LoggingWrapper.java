package TUDITPM.Kafka;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggingWrapper {
	private static HashMap<String, Logger> loggers = new HashMap<>();

	private static void ensureFileExists(String date) {
		File logsDir = new File("logs/" + date);
		try {
			if (logsDir.mkdirs()) {
				System.out.println("Log directory Created");
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		}

	}

	public static void log(String classname, Level level, String msg) {
		Logger classLogger = null;
		FileHandler fh = null;
		try {
			String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
			ensureFileExists(date);
			// This block configure the logger with handler and formatter
			fh = new FileHandler("logs/" + date + "/" + classname + ".log", true);
			if (loggers.get(classname) == null) {
				classLogger = Logger.getLogger(classname);
				loggers.put(classname, classLogger);
			} else {
				classLogger = loggers.get(classname);
			}
			classLogger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
			classLogger.log(level, msg);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (classLogger != null && fh != null) {
				classLogger.removeHandler(fh);
				fh.close();
			}
		}
	}
}
