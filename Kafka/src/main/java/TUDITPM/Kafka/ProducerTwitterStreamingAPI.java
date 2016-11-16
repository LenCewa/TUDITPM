package TUDITPM.Kafka;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

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
 * pushes them to the kafka topic "twitter".
 * 
 * @author Yannick Pferr
 * @author Tobias Mahncke
 * @version 1.2
 */
public class ProducerTwitterStreamingAPI {

	public ProducerTwitterStreamingAPI() {
		// set configs for kafka
		Properties props = new Properties();
		props.put("bootstrap.servers", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "bootstrap.servers"));
		props.put("acks",
				PropertyLoader.getPropertyValue(PropertyFile.kafka, "acks"));
		props.put("retires", Integer.parseInt(PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "retires")));
		props.put("batch.size", Integer.parseInt(PropertyLoader
				.getPropertyValue(PropertyFile.kafka, "batch.size")));
		props.put("linger.ms", Integer.parseInt(PropertyLoader
				.getPropertyValue(PropertyFile.kafka, "linger.ms")));
		props.put("buffer.memory", Integer.parseInt(PropertyLoader
				.getPropertyValue(PropertyFile.kafka, "buffer.memory")));
		props.put("key.deserializer", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "key.deserializer"));
		props.put("value.deserializer", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "value.deserializer"));

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
		// Create a scanner to interact with the user
		Scanner sc = null;

		try {
			producer = new KafkaProducer<>(props);
			// TODO: replace with keywords from file
			sc = new Scanner(System.in);
			while (true) {
				ArrayList<String> keywords = new ArrayList<>();
				System.out.println("Type Keyword to search Tweets");
				keywords.add(sc.nextLine());

				System.out.println("Another Keyword? (y or Anything to stop)");
				while (sc.nextLine().toLowerCase().equals("y")) {
					System.out.println("Type next keyword");
					keywords.add(sc.nextLine());
					System.out
							.println("Another Keyword? (y or Anything to stop)");
				}
				System.out.println("starting search...");

				// Fetches Tweets that contain specified keywords
				hosebirdEndpoint.trackTerms(keywords);
				builder.connect();

				// Stop at 100 tweets
				for (int i = 0; i < 100; i++) {
					try {
						String tweet = msgQueue.take().trim();
						producer.send(new ProducerRecord<String, String>(
								"twitter", tweet));
						System.out.println(tweet);
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.out.println("Couldnt fetch tweets");
					}
				}

				builder.stop();
				System.out.println("finished");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			producer.close();
			sc.close();
		}
	}
}