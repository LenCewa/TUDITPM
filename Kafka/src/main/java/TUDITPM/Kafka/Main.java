package TUDITPM.Kafka;

import java.io.IOException;
import java.util.logging.Level;

import TUDITPM.Kafka.Consumer.ConsumerMongoDB;
import TUDITPM.Kafka.Consumer.ConsumerReload;
import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

/**
 * Main class to start all necessary consumers and producers. Each consumer and
 * producer should contain all necessary startup functions in its constructor.
 * 
 * @author Tobias Mahncke
 * @author Yannick Pferr
 * 
 * @version 6.0
 */
public class Main {
	public static void main(String[] args) {
		// Load the property files
		try {
			new PropertyLoader();
		} catch (IOException e) {
			System.err.println("Could not load property files.");
			e.printStackTrace();
			System.exit(1);
		}
		// Define the environment from the args parameter
		String env = "";
		switch (args.length) {
		case 0:
			// If no parameter was given assume development mode
			System.out.println("No environment set, starting in development mode.");
			System.out.println("To specifiy an environment start the system with parameter. Valid environments are 'development'/'dev', 'producition'/'prod' oder 'test'.");
			env = "dev";
			break;
		case 1:
			switch (args[0]) {
			case "dev":
			case "development":
				env = "dev";
				break;
			case "prod":
			case "production":
				env = "prod";
				break;
			case "test":
				env = "test";
				break;
			default:
				// If no valid parameter was given abort
				System.err.println("No existing environment set, aborting.");
				System.err.println("Valid environments are 'development'/'dev', 'producition'/'prod' oder 'test'.");
				System.exit(1);
			}
		default:
			// If more than one parameter was given abort
			System.err.println("No environment set, aborting.");
			System.err.println("To specifiy an environment start the system with only one parameter. Valid environments are 'development'/'dev', 'producition'/'prod' oder 'test'.");
			System.exit(1);
		}
		
		System.out.println("Starting in environment '" + env + "'. Logs can be found at logs/<currentDate>.");
		LoggingWrapper.log(Main.class.getName(), Level.INFO, "Starting in environment '" + env + "'.");
		
		// Enable rawdata database
		if (Boolean.valueOf(PropertyLoader.getPropertyValue(PropertyFile.database, "rawdata"))) {
			LoggingWrapper.log(Main.class.getName(), Level.INFO, "Logging rawdata enabled.");
			new ConsumerMongoDB(env).start();
		} else {
			LoggingWrapper.log(Main.class.getName(), Level.INFO, "Logging rawdata disabled.");
		}
		// Start the service
		new ConsumerReload(env).start();
	}
}