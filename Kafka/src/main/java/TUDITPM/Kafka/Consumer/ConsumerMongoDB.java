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
public class ConsumerMongoDB extends AbstractConsumer {

	private static final String groupId = "rawdata";
	private String dbname;
	private MongoDBConnector mongo;

	/**
	 * Creates a new consumer for the given environment name.
	 * 
	 * @param env
	 *            the name of the environment to use for the database
	 */
	public ConsumerMongoDB(String env) {
		super(groupId);
		this.dbname = "rawdata_" + env;
	}

	@Override
	void initializeNeededData() {
		
		mongo = new MongoDBConnector(dbname);
	}

	@Override
	void runRoutine(JSONObject json) {
		// decode JSON String
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