package TUDITPM.Kafka.Consumer;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.BasicDBObject;

import TUDITPM.DateChecker.DateChecker;
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
	private final int PROXIMITY = Integer.parseInt(PropertyLoader.getPropertyValue(PropertyFile.solr, "proximity"));
	private static final String groupId = "enhanced";
	private MongoDBConnector mongo;
	private LinkedList<KeywordCategory> categories;
	private RedisConnector redis;
	private Solr solr;
	private String env;

	private class KeywordCategory {
		public String name;
		public ArrayList<String> keywords;

		public KeywordCategory(String name, ArrayList<String> keywords) {
			this.name = name;
			this.keywords = keywords;
		}

		@Override
		public String toString() {
			return name + ":" + keywords.toString();
		}
	}

	/**
	 * Creates a new consumer for the given environment name.
	 * 
	 * @param env
	 *            the name of the environment to use for the database
	 */
	public Consumer(String env) {
		super(groupId);
		this.env = env;
		mongo = new MongoDBConnector("enhanceddata_" + env);
		redis = new RedisConnector();
		solr = new Solr();
	}

	/**
	 * Reloads the keywords.
	 */
	@SuppressWarnings("unchecked")
	@Override
	void initializeNeededData() {
		MongoDBConnector config = new MongoDBConnector(
				PropertyLoader.getPropertyValue(PropertyFile.database, "config.name") + "_" + env);
		categories = new LinkedList<>();
		for (Document doc : config.getCollection("keywords").find()) {
			categories.add(new KeywordCategory(doc.getString("category"), (ArrayList<String>) doc.get("keywords")));
		}
	}

	/**
	 * Consumes a single news object. The solr document is searched for the
	 * keywords and then written to the mongoDB and redis.
	 * @param json - the json object which contains the data 
	 */
	@Override
	public void consumeObject(JSONObject json) {
		String id = json.getString("id");

		// Only do this if unique company, link and keyword pair is not already
		// contained
		for (KeywordCategory category : categories) {
			for (String keyword : category.keywords) {
				boolean found = false;
				if (solr.search("\"" + json.getString("searchName") + " " + keyword + "\"" + "~" + PROXIMITY, id)) {
					found = true;
				}
				JSONArray searchTerms = json.getJSONArray("searchTerms");
				for (Object term : searchTerms.toList()) {
					if (solr.search("\"" + term + " " + keyword + "\"" + "~" + PROXIMITY, id)) {
						found = true;
						break;
					}
				}
				HashMap<String, String> query = new HashMap<>();
				query.put("company", json.getString("companyKey"));
				query.put("link", json.getString("link"));
				query.put("category", category.name);
				query.put("keyword", keyword);

				if (found && !mongo.contains(json.getString("companyKey"), query)) {
					// remove the id before writing to redis
					json.remove("id");
					json.append("category", category.name);
					json.append("keyword", keyword);

					// Create Date Object from String
					SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
					Date date = new Date();
					try {
						date = df.parse(json.getString("date"));
					} catch (ParseException e) {
						// If date is not correctly formatted, current time is used
					}
					// Create mongoDB document to store in mongoDB
					Document mongoDBdoc = new Document("text", json.getString("text"))
							.append("link", json.getString("link")).append("date", date)
							.append("company", json.getString("company")).append("category", category.name)
							.append("keyword", keyword);
					try {
						String title = json.getString("title");
						mongoDBdoc.append("title", title);
					} catch (JSONException e) {
						// title field is optional and not saved if not
						// available
					}

					// Write to database and redis
					String dbID = mongo.writeToDb(mongoDBdoc, json.getString("companyKey"));
					json.append("_id", dbID);
					redis.appendJSONToList(json.getString("companyKey"), json);

					if (DateChecker.isLastMonth(date)) {
						redis.appendJSONToList("monthList", json);
					}
				}
			}
		}
		// Remove the solr document to keep the solr instance clean
		solr.delete(id);
	}
}
