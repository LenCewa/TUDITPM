package TUDITPM.Kafka;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import org.apache.kafka.clients.producer.ProducerRecord;
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

import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

/**
 * Producer that listens to the twitter streaming API for given keywords and
 * pushes them to the kafka topic "twitter". Extends Thread so that it can run
 * asynchronously.
 * 
 * @author Yannick Pferr
 * @author Tobias Mahncke
 * @version 5.1
 */
public class ProducerTwitterStreamingAPI extends ProducerKafka {
	
	private BlockingQueue<String> msgQueue;
	
	@SuppressWarnings("unchecked")
	@Override
	public void initializeNeededData() {
		// TODO Auto-generated method stub
		Authentication auth = null;
		auth = new OAuth1(PropertyLoader.getPropertyValue(
				PropertyFile.credentials, "OAUTHCONSUMERKEY"),
				PropertyLoader.getPropertyValue(PropertyFile.credentials,
						"OAUTHCONSUMERSECRET"),
				PropertyLoader.getPropertyValue(PropertyFile.credentials,
						"OAUTHACCESSTOKEN"), PropertyLoader.getPropertyValue(
						PropertyFile.credentials, "OAUTHACCESSTOKENSECRET"));

		msgQueue = new LinkedBlockingQueue<String>(100000);

		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

		// Fetches Tweets from specific Users with their User Id
		// hosebirdEndpoint.followings(followings);
		Client builder = new ClientBuilder().hosts(Constants.STREAM_HOST)
				.authentication(auth).endpoint(hosebirdEndpoint)
				.processor(new StringDelimitedProcessor(msgQueue)).build();
		

			LinkedList<String> searchNames = new LinkedList<>();
			for (Document doc : config.getCollection("companies").find()) {
				companies.add(doc);
				ArrayList<String> searchTerms = (ArrayList<String>) doc
						.get("searchTerms");
				searchNames.add(doc.getString("searchName"));
				if (searchTerms != null) {
					searchNames.addAll(searchTerms);
				}
			}

			// Fetches Tweets that contain specified keywords
			hosebirdEndpoint.trackTerms(searchNames);
			LoggingWrapper.log(this.getClass().getName(), Level.INFO,
					"Started tracking terms: " + searchNames.toString());
			builder.connect();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void runRoutine() {
		
		String tweet = null;
		try {
			tweet = msgQueue.take().trim();
			JSONObject JSONrawdata = new JSONObject(tweet);
			JSONObject json = new JSONObject();
			String text = JSONrawdata.getString("text");
			String id = solr.add(text);
			boolean companyFound = false;
			for (Document company : companies) {
				ArrayList<String> searchTerms = (ArrayList<String>) company
						.get("searchTerms");
				String searchString = "\""
						+ company.getString("searchName") + "\"";
				if (searchTerms != null) {
					for (String term : searchTerms) {
						searchString += " \"" + term + "\"";
					}
				}
				if (solr.search(searchString, id)) {
					companyFound = true;
					json.put("searchName",
							company.getString("searchName"));
					json.put("companyKey", company.getString("key"));
					json.put("company", company.getString("name"));
					json.put("source", "twitter");
					json.put("text", text);
					json.put("date",
							JSONrawdata.getString("created_at"));
					json.put("link", "https://twitter.com/statuses/"
							+ JSONrawdata.getString("id_str"));
					json.put("id", id);

					LoggingWrapper.log(this.getClass().getName(),
							Level.INFO, json.toString());

					producer.send(new ProducerRecord<String, String>(
							"twitter", json.toString()));
				}
			}
			if (!companyFound)
				solr.delete(id);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.out.println("Couldnt fetch tweets.");
		} catch (JSONException e) {
			LoggingWrapper.log(this.getClass().getName(), Level.WARNING, tweet);
		}
	}
}