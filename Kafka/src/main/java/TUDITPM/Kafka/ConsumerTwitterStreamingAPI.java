package TUDITPM.Kafka;

import java.util.Arrays;
import java.util.Properties;

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
 * spark. Extends Thread so that it can run asynchronously.
 * 
 * @author Yannick Pferr
 * @author Tobias Mahncke
 * @version 1.3
 */
public class ConsumerTwitterStreamingAPI extends Thread {

	private String dbname;

	/**
	 * Creates a new consumer for the given database name.
	 * 
	 * @param dbname
	 *            - the name of the database to wihc this consumer connects
	 */
	public ConsumerTwitterStreamingAPI(String dbname) {
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

		KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(props);
		kafkaConsumer.subscribe(Arrays.asList("twitter"));

		MongoDBConnector mongo = new MongoDBConnector(dbname);

		while (true) {
			ConsumerRecords<String, String> records = kafkaConsumer.poll(100);
			int missedTweets = 0;
			for (ConsumerRecord<String, String> record : records) {
				System.out.println(record.value());
				try {
					// decode JSON String
					JSONObject jObj = new JSONObject(record.value());
					String text = jObj.getString("text");
					String timeNdate = jObj.getString("created_at");

					JSONObject user = jObj.getJSONObject("user");
					String username = user.getString("screen_name");
					String location = (!user.get("location").toString()
							.equals("null")) ? user.getString("location") : "";
					
					// Write to DB
					mongo.writeToDb(
							new Document("username", username)
									.append("location", location)
									.append("timeNDate", timeNdate)
									.append("text", text), "testcollection");
				} catch (JSONException e) {
					e.printStackTrace();
					missedTweets++;
				}
			}
			if (missedTweets > 0) {
				System.out.println(missedTweets + " Tweets missed");
			}
		}
	}
}