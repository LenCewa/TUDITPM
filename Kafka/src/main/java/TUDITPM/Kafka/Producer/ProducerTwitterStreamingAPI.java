package TUDITPM.Kafka.Producer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import org.apache.solr.common.util.Hash;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import TUDITPM.Kafka.LoggingWrapper;
import TUDITPM.Kafka.Topic;
import TUDITPM.Kafka.Connectors.MongoDBConnector;
import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

/**
 * Producer that listens to the twitter streaming API for given keywords and
 * pushes them to the kafka topic "twitter". Extends Thread so that it can run
 * asynchronously.
 * 
 * @author Yannick Pferr
 * @author Tobias Mahncke
 * @version 6.1
 */
public class ProducerTwitterStreamingAPI extends AbstractProducer {
	private BlockingQueue<String> msgQueue;
	private Authentication auth;
	private MongoDBConnector mongo;
	
	public ProducerTwitterStreamingAPI(String env) {
		super(env);
		auth = new OAuth1(PropertyLoader.getPropertyValue(PropertyFile.credentials, "OAUTHCONSUMERKEY"),
				PropertyLoader.getPropertyValue(PropertyFile.credentials, "OAUTHCONSUMERSECRET"),
				PropertyLoader.getPropertyValue(PropertyFile.credentials, "OAUTHACCESSTOKEN"),
				PropertyLoader.getPropertyValue(PropertyFile.credentials, "OAUTHACCESSTOKENSECRET"));
		msgQueue = new LinkedBlockingQueue<String>(100000);
		mongo = new MongoDBConnector("enhanceddata_" + env);
	}


	@SuppressWarnings("unchecked")
	@Override
	public void initializeNeededData() {
		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

		// Fetches Tweets from specific Users with their User Id
		// hosebirdEndpoint.followings(followings);
		Client builder = new ClientBuilder().hosts(Constants.STREAM_HOST).authentication(auth)
				.endpoint(hosebirdEndpoint).processor(new StringDelimitedProcessor(msgQueue)).build();

		LinkedList<String> searchNames = new LinkedList<>();
		for (Document doc : companies) {
			ArrayList<String> searchTerms = (ArrayList<String>) doc.get("searchTerms");
			searchNames.add(doc.getString("searchName"));
			if (searchTerms != null) {
				searchNames.addAll(searchTerms);
			}
		}

		// Fetches Tweets that contain specified keywords
		hosebirdEndpoint.trackTerms(searchNames);
		LoggingWrapper.log(this.getClass().getName(), Level.INFO, "Started tracking terms: " + searchNames.toString());
		builder.connect();
	}

	@Override
	public void runRoutine() {
		String tweet = null;
		try {
			tweet = msgQueue.take().trim();
			JSONObject json = new JSONObject(tweet);
			String text = json.getString("text").replaceAll("RT @.*?\\s+", "");
			
			// Check if tweet is a retweet and original tweet is already in DB
			for (Document company : companies) {
				String searchName = company.getString("searchName");
				
				HashMap<String, String> query = new HashMap<>();
				query.put("text", text);
				if(mongo.find(searchName, query))
					return;
			}
			
			checkForCompany(Topic.twitter, "https://twitter.com/statuses/" + json.getString("id_str"), text, json.getString("created_at"), "");
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.out.println("Couldnt fetch tweets.");
		} catch (JSONException e) {
			LoggingWrapper.log(this.getClass().getName(), Level.WARNING, tweet);
		}
	}
}