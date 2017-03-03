package TUDITPM.Kafka.Consumer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import TUDITPM.Kafka.Topic;
import TUDITPM.Kafka.Connectors.MongoDBConnector;

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
	private MongoDBConnector mongo;

	/**
	 * Creates a new consumer for the given environment name.
	 * 
	 * @param env
	 *            the name of the environment to use for the database
	 */
	public ConsumerMongoDB(String env) {
		super(groupId);
		
		//Subscribe to every topic except for rawdata
		Collection<String> topics = Topic.toList(); 
		topics.remove(Topic.rawdata.name());
		subscribeToList(topics);
		
		mongo = new MongoDBConnector("rawdata_" + env);
	}

	@Override
	void initializeNeededData() {}

	/**
	 * Consumes a single news object. The solr document is searched for the
	 * keywords and then written to the mongoDB and redis.
	 * @param json - the json object which contains the data 
	 */
	@Override
	void consumeObject(JSONObject json) {
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
		HashMap<String, String> query = new HashMap<>();
		query.put("company", json.getString("companyKey"));
		query.put("link", json.getString("link"));
		
		// Write to DB
		if(!mongo.contains(json.getString("companyKey"), query))
			mongo.writeToDb(mongoDBdoc, json.getString("source"));
	}
}