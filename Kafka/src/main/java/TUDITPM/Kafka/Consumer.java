package TUDITPM.Kafka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
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
 * spark.
 * 
 * @author Yannick Pferr
 * @author Tobias Mahncke
 * @version 1.2
 */
public class Consumer extends Thread {

	private String dbname;
	private final int PROXIMITY = Integer.parseInt(PropertyLoader
			.getPropertyValue(PropertyFile.solr, "proximity"));

	/**
	 * Creates a new consumer for the given database name.
	 * 
	 * @param dbname
	 *            - the name of the database to which this consumer connects
	 */
	public Consumer(String dbname) {
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
		kafkaConsumer.subscribe(Arrays.asList("twitter", "rss"));

		MongoDBConnector mongo = new MongoDBConnector(dbname);
		LinkedList<String> keywords = readKeywords();
		Solr solr = new Solr();

		while (true) {
			ConsumerRecords<String, String> records = kafkaConsumer.poll(10);
			for (ConsumerRecord<String, String> record : records) {
				System.out.println("CONSUMER: " + record.value());
				// decode JSON String
				JSONObject json = new JSONObject(record.value());
				String id = json.getString("id");

				for (String keyword : keywords) {
					if (solr.search("\"" + json.getString("company") + " "
							+ keyword + "\"" + "~" + PROXIMITY, id)) {
						Document mongoDBdoc = new Document("text",
								json.getString("text"))
								.append("link", json.getString("link"))
								.append("date", json.getString("date"))
								.append("company", json.getString("company"))
								.append("keyword", keyword);
						try {
							String title = json.getString("title");
							mongoDBdoc.append("title", title);
						} catch (JSONException e) {
							// title field is optional and not saved if not
							// available
						}
						// Write to DB
						mongo.writeToDb(mongoDBdoc, json.getString("company"));
					}
				}
				solr.delete(id);
			}
		}
	}

	public LinkedList<String> readKeywords() {
		String keywordList = "properties/keywords";
		File file = new File(Paths.get(keywordList).toString());
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(file), "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return null;
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return null;
		}

		String line = null;
		LinkedList<String> list = new LinkedList<String>();

		try {
			while ((line = reader.readLine()) != null) {
				list.add(line);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return list;
	}
}