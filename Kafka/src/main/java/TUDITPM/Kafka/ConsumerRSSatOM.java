package TUDITPM.Kafka;

import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.bson.Document;
import org.json.JSONException;

import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

/**
 * Listening to the twitter Stream and converting the given data to stream it to
 * spark. Extends Thread so that it can run asynchronously.
 * 
 * @author Christian Zendo
 * 
 * @version 3.1
 */
public class ConsumerRSSatOM extends Thread {

	private String dbname;

	/**
	 * Creates a new consumer for the given database name.
	 * 
	 * @param dbname
	 *            - the name of the database to which this consumer connects
	 */
	public ConsumerRSSatOM(String dbname) {
		this.dbname = dbname;
	}

	/**
	 * Gets called on start of the Thread
	 */
	@Override
	public void run() {
		Properties props = new Properties();
		props.put("bootstrap.servers", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "bootstrap.servers"));
		props.put("group.id", "group-1");
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

		KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<String, String>(
				props);
		kafkaConsumer.subscribe(Arrays.asList("rss"));

		MongoDBConnector mongo = new MongoDBConnector(dbname);

		while (true) {
			ConsumerRecords<String, String> records = kafkaConsumer.poll(100);
			int missedFeeds = 0;
			for (ConsumerRecord<String, String> record : records) {
				System.out.println(record.value());
				try {

					String entry = record.value();
					// Write to DB
					mongo.writeToDb(new Document("content", entry),
							"rawdata_rss");
				} catch (JSONException e) {
					e.printStackTrace();
					missedFeeds++;
				}
			}
			if (missedFeeds > 0) {
				System.out.println(missedFeeds + " Feeds missed");
			}
		}
	}
}