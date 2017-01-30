package TUDITPM.Kafka.Consumer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Level;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import TUDITPM.Kafka.LoggingWrapper;
import TUDITPM.Kafka.Connectors.MongoDBConnector;
import TUDITPM.Kafka.Connectors.RedisConnector;
import TUDITPM.Kafka.Connectors.Solr;
import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

/**
 * Listening to all topics searches the entries for keywords and saves them to
 * the enhanced data database.
 * 
 * @author Yannick Pferr
 * @author Tobias Mahncke
 * @version 6.0
 */
public class Consumer extends AbstractConsumer {
	private final int PROXIMITY = Integer.parseInt(PropertyLoader
			.getPropertyValue(PropertyFile.solr, "proximity"));
	private static final String groupId = "enhanced";
	private MongoDBConnector mongo;
	private LinkedList<String> keywords;
	private RedisConnector redis;
	private Solr solr;

	/**
	 * Creates a new consumer for the given database name.
	 * 
	 * @param env
	 *            the name of the environment to use for the collection of
	 *            already checked feed entries
	 */
	public Consumer(String env) {
		super(groupId);
		mongo = new MongoDBConnector("enhanceddata_" + env);
	}

	@SuppressWarnings("unchecked")
	@Override
	void initializeNeededData() {
		MongoDBConnector config = new MongoDBConnector(
				PropertyLoader.getPropertyValue(PropertyFile.database,
						"config.name"));
		keywords = new LinkedList<>();
		for (Document doc : config.getCollection("keywords").find()) {
			keywords.addAll((ArrayList<String>) doc.get("keywords"));
		}
		redis = new RedisConnector();
		solr = new Solr();
	}

	/**
	 * Gets called on start of the Thread
	 */
	@Override
	public void runRoutine(JSONObject json) {
		String id = json.getString("id");

		for (String keyword : keywords) {
			boolean found = false;
			try {
				JSONArray searchTerms = json.getJSONArray("searchTerms");

				if (solr.search("\"" + json.getString("searchName") + " "
						+ keyword + "\"" + "~" + PROXIMITY, id)) {
					found = true;
				}
				for (Object term : searchTerms.toList()) {
					if (solr.search("\"" + term + " " + keyword + "\"" + "~"
							+ PROXIMITY, id)) {
						found = true;
						break;
					}
				}
			} catch (JSONException e) {
				System.out.println("skipped");
			}
			if (found) {
				String text = json.getString("text");
				String link = json.getString("link");
				String date = json.getString("date");
				String company = json.getString("company");

				// Create JSON object to store in redis
				JSONObject redisJson = new JSONObject();
				redisJson.append("id", id);
				redisJson.append("text", text);
				redisJson.append("link", link);
				redisJson.append("date", date);
				redisJson.append("company", company);
				redisJson.append("keyword", keyword);

				// Create mongoDB document to store in mongoDB
				Document mongoDBdoc = new Document("text", text)
						.append("link", link).append("date", date)
						.append("company", company).append("keyword", keyword);
				try {
					String title = json.getString("title");
					mongoDBdoc.append("title", title);
					redisJson.append("title", title);
				} catch (JSONException e) {
					// title field is optional and not saved if not
					// available
				}
				// Write to DB
				// Remove points for the collection name, because
				// they are not permitted in MongoDB
				mongo.writeToDb(mongoDBdoc, json.getString("companyKey"));
				redis.appendJSONToList(json.getString("companyKey"), redisJson);
			}
		}
		solr.delete(id);
	}
}
