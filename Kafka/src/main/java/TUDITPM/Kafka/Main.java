package TUDITPM.Kafka;

import java.io.IOException;

import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

/**
 * Main class to start all necessary consumers and producers. Each consumer and
 * producer should contain all necessary startup functions in its constructor.
 * 
 * @author Tobias Mahncke
 * @author Yannick Pferr
 * 
 * @version 5.0
 */
public class Main {
	public static void main(String[] args) {
		try {
			new PropertyLoader();
		} catch (IOException e) {
			System.err.println("Could not load property files.");
			e.printStackTrace();
			System.exit(1);
		}
		if(Boolean.valueOf(PropertyLoader.getPropertyValue(PropertyFile.database, "rawdata")))
				new ConsumerMongoDB("rawdata_dev").start();
		new ConsumerReload("enhanceddata_dev").start();
	}
}