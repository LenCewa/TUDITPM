package TUDITPM.Kafka;

import java.io.IOException;
import java.util.Arrays;
import java.util.Observable;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

/**
 * Listening to the twitter Stream and converting the given data to stream it to
 * spark. Implements Runnable so that it can run asynchronously.
 * 
 * @author Yannick Pferr
 * @author Tobias Mahncke
 * 
 * @version 3.2
 */
public class ConsumerTwitterStreamingAPI extends Observable implements Runnable {
	private final Logger log = Logger.getLogger(this.getClass().getName());

	private MongoDBConnector mongo;

	/**
	 * Creates a new consumer for the given database name.
	 * 
	 * @param dbname
	 *            - the name of the database to which this consumer connects
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	public ConsumerTwitterStreamingAPI(String dbname) throws SecurityException, IOException {
		log.addHandler(LoggingHandler.getHandler());
		mongo = new MongoDBConnector(dbname);
		log.info("Consumer connected to database");
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

		KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(props);
		kafkaConsumer.subscribe(Arrays.asList("twitter"));
		log.info("Consumer connected to topic TWITTER");

		while (true) {
			ConsumerRecords<String, String> records = kafkaConsumer.poll(100);
			int missedTweets = 0;
			for (ConsumerRecord<String, String> record : records) {
				try {
					writeToDatabase(new JSONObject(record.value()));
				} catch (JSONException e) {
					e.printStackTrace();
					missedTweets++;
				}
			}
			if (missedTweets > 0) {
				log.warning(missedTweets + " Tweets missed");
			}
		}
	}

	/**
	 * Writes the given JSONObject to the MongoDB and notifies the observers.
	 * 
	 * @param jObj
	 *            - The JSONObject to save.
	 */
	private void writeToDatabase(JSONObject jObj) {
		// decode JSON String
		String text = jObj.getString("text");
		String timeNdate = jObj.getString("created_at");

		JSONObject user = jObj.getJSONObject("user");
		String username = user.getString("screen_name");
		String location = (!user.get("location").toString().equals("null")) ? user
				.getString("location") : "";

		// Write to DB
		Document document = new Document("username", username)
				.append("location", location).append("timeNDate", timeNdate)
				.append("text", text);
		mongo.writeToDb(document, "rawdata_twitter");
		log.info("Twitter record " + jObj.getString("id_str")
				+ " written to database");
		setChanged();
		notifyObservers(document);
	}
}