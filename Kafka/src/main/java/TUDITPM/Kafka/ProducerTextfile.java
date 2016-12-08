package TUDITPM.Kafka;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

/**
 * Producer that reads a text file and pushes them to the kafka topic "twitter".
 * Extends Thread so that it can run asynchronously.
 * 
 * @author Tobias Mahncke
 * @author Len Williamson
 * @version 1.2
 */
public class ProducerTextfile extends Thread {
	private final Logger log = Logger.getLogger(this.getClass().getName());

	private String filename;

	public ProducerTextfile(String filename) throws SecurityException,
			IOException {
		log.addHandler(LoggingHandler.getHandler());
		log.setLevel(Level.ALL);
		this.filename = "src/test/data/" + filename;
	}

	/**
	 * Gets called on start of the Thread
	 */
	@Override
	public void run() {
		// set configs for kafka
		Properties props = new Properties();
		props.put("bootstrap.servers", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "bootstrap.servers"));
		props.put("acks",
				PropertyLoader.getPropertyValue(PropertyFile.kafka, "acks"));
		props.put("retries", Integer.parseInt(PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "retries")));
		props.put("batch.size", Integer.parseInt(PropertyLoader
				.getPropertyValue(PropertyFile.kafka, "batch.size")));
		props.put("linger.ms", Integer.parseInt(PropertyLoader
				.getPropertyValue(PropertyFile.kafka, "linger.ms")));
		props.put("buffer.memory", Integer.parseInt(PropertyLoader
				.getPropertyValue(PropertyFile.kafka, "buffer.memory")));
		props.put("key.serializer", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "key.serializer"));
		props.put("value.serializer", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "value.serializer"));

		// Create the producer
		Producer<String, String> producer = new KafkaProducer<>(props);

		log.info("Producer set up.");

		List<String> lines = null;
		
		try {
			lines = Files.readAllLines(Paths.get(filename));
			log.fine("Read tweet from file " + lines);
		} catch (IOException e) {
			e.printStackTrace();
			log.warning("File not found: " + e.getMessage());
		}
		StringBuilder sb = new StringBuilder();

		String m_lines = appendLinesFromTextFileToStringbuilder(sb, lines);

		log.fine("Read tweet from file " + m_lines);

		producer.send(new ProducerRecord<String, String>("twitter", m_lines));

		log.info("Send tweet to topic TWITTER");

		producer.close();
	}

	/**
	 * Appends the lines from red txt-File to the StringBuilder
	 * 
	 * @param sb
	 *            - the String Builder that is needed
	 * @param list
	 *            - a list that contains all lines from the red txt-File
	 * @return - a String that contains the red lines from the given list
	 */
	public String appendLinesFromTextFileToStringbuilder(StringBuilder sb,
			List<String> list) {
		for (String s : list) {
			log.fine("Append Line: " + s);
			sb.append(s);
		}
		return sb.toString();
	}
}