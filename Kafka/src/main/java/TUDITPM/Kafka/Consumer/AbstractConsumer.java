package TUDITPM.Kafka.Consumer;

import java.util.Properties;
import java.util.logging.Level;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.json.JSONException;
import org.json.JSONObject;

import TUDITPM.Kafka.LoggingWrapper;
import TUDITPM.Kafka.Topic;
import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

/**
 * Abstract producer that includes all needed elements. It automates the
 * initialization and the topic listening.
 * 
 * @author Yannick Pferr
 * @author Tobias Mahncke
 * @version 6.0
 */
public abstract class AbstractConsumer extends Thread {
	/** The kafka consumer. */
	private KafkaConsumer<String, String> consumer;
	/** flag to indicate re-initializing before the next run */
	private boolean reload = false;

	/**
	 * Constructor that handles loading from configuration files. Creates the
	 * consumer and needed connectors.
	 */
	AbstractConsumer(String groupId) {
		// set configs for kafka
		Properties props = new Properties();
		props.put("bootstrap.servers", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "bootstrap.servers"));
		props.put("group.id", groupId);
		props.put("enable.auto.commit", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "enable.auto.commit"));
		props.put("auto.commit.interval.ms", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "auto.commit.interval.ms"));
		props.put("auto.offset.reset", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "auto.offset.reset"));
		props.put("session.timeout.ms", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "session.timeout.ms"));
		props.put("key.deserializer", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "key.deserializer"));
		props.put("value.deserializer", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "value.deserializer"));

		// Initialize the consumer
		consumer = new KafkaConsumer<String, String>(props);
		consumer.subscribe(Topic.toList());
	}

	/**
	 * Here you should setup everything that only needs to be initialized after
	 * startup or on reload e.g loading the keyword list.
	 */
	abstract void initializeNeededData();

	/**
	 * Works on a single data object.
	 * @param json The object containing the articles.
	 */
	abstract void consumeObject(JSONObject json);

	/**
	 * Sets the reload flag.
	 */
	public void reload() {
		reload = true;
	}

	@Override
	public void run() {
		LoggingWrapper.log(this.getClass().getName(), Level.INFO,
				"Thread started");
		initializeNeededData();
		while (true) {
			ConsumerRecords<String, String> records = consumer.poll(10);
			for (ConsumerRecord<String, String> record : records) {
				// Decode JSON String
				JSONObject json = null;
				try {
					json = new JSONObject(record.value());
				} catch (JSONException e) {
					LoggingWrapper.log(this.getClass().getName(), Level.SEVERE , "Kafka return no valid JSON, producer must be defect. " + record.value());
					continue;
				}
				LoggingWrapper.log(this.getClass().getName(), Level.INFO, json.toString());
				consumeObject(json);
			}
			// If the reload flag is set re-init the data and continue running
			if (reload) {
				LoggingWrapper.log(this.getClass().getName(), Level.INFO,
						"Reloading data.");
				initializeNeededData();
				reload = false;
			}
		}
	}
}