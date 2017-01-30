package TUDITPM.Kafka.Consumer;

import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import TUDITPM.Kafka.LoggingWrapper;
import TUDITPM.Kafka.Connectors.MongoDBConnector;
import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

/**
 * Listening to the twitter Stream and converting the given data to stream it to
 * spark. Extends Thread so that it can run asynchronously.
 * 
 * @author Yannick Pferr
 * @author Tobias Mahncke
 * 
 * @version 5.0
 */
public class ConsumerMongoDB extends Thread {

	private String dbname;

	/**
	 * Creates a new consumer for the given database name.
	 * 
	 * @param dbname
	 *            - the name of the database to which this consumer connects
	 */
	public ConsumerMongoDB(String dbname) {
		this.dbname = "rawdata_" + dbname;
	}

	/**
	 * Gets called on start of the Thread
	 */
	@Override
	public void run() {
		LoggingWrapper.log(this.getClass().getName(), Level.INFO, "Thread started");

		Properties props = new Properties();
		props.put("bootstrap.servers", PropertyLoader.getPropertyValue(PropertyFile.kafka, "bootstrap.servers"));
		props.put("group.id", "group-1");
		props.put("enable.auto.commit", PropertyLoader.getPropertyValue(PropertyFile.kafka, "enable.auto.commit"));
		props.put("auto.commit.interval.ms",
				PropertyLoader.getPropertyValue(PropertyFile.kafka, "auto.commit.interval.ms"));
		props.put("auto.offset.reset", PropertyLoader.getPropertyValue(PropertyFile.kafka, "auto.offset.reset"));
		props.put("session.timeout.ms", PropertyLoader.getPropertyValue(PropertyFile.kafka, "session.timeout.ms"));
		props.put("key.deserializer", PropertyLoader.getPropertyValue(PropertyFile.kafka, "key.deserializer"));
		props.put("value.deserializer", PropertyLoader.getPropertyValue(PropertyFile.kafka, "value.deserializer"));

		KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<String, String>(props);
		kafkaConsumer.subscribe(Arrays.asList("twitter", "rss"));

		MongoDBConnector mongo = new MongoDBConnector(dbname);

		while (true) {
			ConsumerRecords<String, String> records = kafkaConsumer.poll(10);
			for (ConsumerRecord<String, String> record : records) {
				System.out.println("CONSUMER_RAWDATA: " + record.value());
				// decode JSON String
				JSONObject json = null;
				try {
					json = new JSONObject(record.value());
				} catch (JSONException e) {
					System.err.println("Not a valid JSON Object, continuing...");
					continue;
				}

				Document mongoDBdoc = new Document("text", json.getString("text"))
						.append("link", json.getString("link")).append("company", json.getString("company"))
						.append("date", json.get("date"));
				try {
					String title = json.getString("title");
					mongoDBdoc.append("title", title);
				} catch (JSONException e) {
					// title field is optional and not saved if not
					// available
				}
				// Write to DB
				mongo.writeToDb(mongoDBdoc, json.getString("source"));
			}
		}
	}
}