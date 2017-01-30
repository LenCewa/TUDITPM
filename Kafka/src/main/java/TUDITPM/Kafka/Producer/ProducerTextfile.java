package TUDITPM.Kafka.Producer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

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
		Producer<String, String> producer_test = new KafkaProducer<>(props);
		Producer<String, String> producer_empty_text = new KafkaProducer<>(props);
		Producer<String, String> producer_140_chars = new KafkaProducer<>(props);

		List<String> lines_test = null;
		List<String> lines_empty_text = null;
		List<String> lines_140_chars = null;
		
		try {
			lines_test = Files.readAllLines(Paths.get("test.txt"));
			
			// Added two more test text files for testing purposes by Len
			lines_140_chars = Files.readAllLines(Paths.get("test_140_chars.txt"));
			lines_empty_text = Files.readAllLines(Paths.get("test_empty_text.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		StringBuilder sb_test = new StringBuilder();
		StringBuilder sb_empty_text = new StringBuilder();
		StringBuilder sb_140_chars = new StringBuilder();
		
		String m_lines_test = appendLinesFromTextFileToStringbuilder(sb_test, lines_test);
		String m_lines_empty_text = appendLinesFromTextFileToStringbuilder(sb_empty_text, lines_empty_text);
		String m_lines_140_chars = appendLinesFromTextFileToStringbuilder(sb_140_chars, lines_140_chars);
		
		producer_test.send(new ProducerRecord<String, String>("twitter", m_lines_test));
		producer_test.close();
		producer_empty_text.send(new ProducerRecord<String, String>("twitter", m_lines_empty_text));
		producer_empty_text.close();
		producer_140_chars.send(new ProducerRecord<String, String>("twitter", m_lines_140_chars));
		producer_140_chars.close();
	}
	
	/**
	 * Appends the lines from red txt-File to the StringBuilder
	 * @param sb
	 * 			- the String Builder that is needed
	 * @param list
	 * 			- a list that contains all lines from the red txt-File
	 * @return
	 * 			- a String that contains the red lines from the given list
	 */
	public String appendLinesFromTextFileToStringbuilder(StringBuilder sb, List<String> list) {
		for (String s : list) {
		sb.append(s);
		}
		return sb.toString();
	}
}