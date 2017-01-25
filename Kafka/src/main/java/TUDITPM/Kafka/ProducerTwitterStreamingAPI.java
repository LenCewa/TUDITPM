package TUDITPM.Kafka;

import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import TUDITPM.Kafka.DBConnectors.MongoDBConnector;
import TUDITPM.Kafka.Loading.LegalFormHelper;
import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

/**
 * Producer that listens to the twitter streaming API for given keywords and
 * pushes them to the kafka topic "twitter". Extends Thread so that it can run
 * asynchronously.
 * 
 * @author Yannick Pferr
 * @author Tobias Mahncke
 * @version 5.0
 */
public class ProducerTwitterStreamingAPI extends Thread {
	/**
	 * Gets called on start of the Thread
	 */
	@Override
	public void run() {
		LoggingWrapper.log(this.getClass().getName(), Level.INFO,
				"Thread started");

		// set configs for kafka
		Properties props = new Properties();
		props.put("bootstrap.servers", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "bootstrap.servers"));
		props.put("acks",
				PropertyLoader.getPropertyValue(PropertyFile.kafka, "acks"));
		props.put("retries", Integer.parseInt(PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "retries")));
		props.put("batch.size", Integer.parseInt(PropertyLoader
				.getPropertyValue(PropertyFile.kafka, "batch.size")));
		props.put("linger.ms", Integer.parseInt(PropertyLoader
				.getPropertyValue(PropertyFile.kafka, "linger.ms")));
		props.put("buffer.memory", Integer.parseInt(PropertyLoader
				.getPropertyValue(PropertyFile.kafka, "buffer.memory")));
		props.put("key.serializer", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "key.serializer"));
		props.put("value.serializer", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "value.serializer"));

		// Create the producer
		Producer<String, String> producer = null;

		// Load configuration for hbc from config files
		Authentication auth = null;
		auth = new OAuth1(PropertyLoader.getPropertyValue(
				PropertyFile.credentials, "OAUTHCONSUMERKEY"),
				PropertyLoader.getPropertyValue(PropertyFile.credentials,
						"OAUTHCONSUMERSECRET"),
				PropertyLoader.getPropertyValue(PropertyFile.credentials,
						"OAUTHACCESSTOKEN"), PropertyLoader.getPropertyValue(
						PropertyFile.credentials, "OAUTHACCESSTOKENSECRET"));

		BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);

		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

		// Fetches Tweets from specific Users with their User Id
		// hosebirdEndpoint.followings(followings);
		Client builder = new ClientBuilder().hosts(Constants.STREAM_HOST)
				.authentication(auth).endpoint(hosebirdEndpoint)
				.processor(new StringDelimitedProcessor(msgQueue)).build();
		Solr solr = new Solr();

		try {
			producer = new KafkaProducer<String, String>(props);

			MongoDBConnector config = new MongoDBConnector(
					PropertyLoader.getPropertyValue(PropertyFile.database,
							"config.name"));
			
			LinkedList<String> companiesWithLegalForms = new LinkedList<>();
			for (Document doc : config.getCollection("companies").find()) {
				companiesWithLegalForms.add(doc.getString("company"));
			}
			
			LinkedList<String> legalForms = PropertyLoader.getLegalForms();
			LinkedList<String[]> companies = LegalFormHelper.removeLegalForms(
					companiesWithLegalForms, legalForms);
			
			LinkedList<String> strippedCompanies = new LinkedList<String>();
			for(String[] company : companies){
				strippedCompanies.add(company[1]);
			}

			// Fetches Tweets that contain specified keywords
			hosebirdEndpoint.trackTerms(strippedCompanies);
			builder.connect();

			final int abortSize = Integer.parseInt(PropertyLoader
					.getPropertyValue(PropertyFile.kafka, "abort.size"));

			// Stop at abort size
			for (int i = 0; i < abortSize; i++) {

				try {
					String tweet = msgQueue.take().trim();
					JSONObject JSONrawdata = new JSONObject(tweet);
					JSONObject json = new JSONObject();
					String text = JSONrawdata.getString("text");
					String id = solr.add(text);
					boolean companyFound = false;
					for (String[] company : companies) {
						if (solr.search("\"" + company[1] + "\"", id)) {
							companyFound = true;
							json.put("company", company[0]);
							json.put("companyStripped", company[1]);
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
					System.out.println("Tweet limit reached.");
				}
			}

			System.out.println("finished");

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			producer.close();
			builder.stop();
		}
	}
}