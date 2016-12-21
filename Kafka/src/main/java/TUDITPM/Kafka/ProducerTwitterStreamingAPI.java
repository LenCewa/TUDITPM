package TUDITPM.Kafka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Properties;
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
 * pushes them to the kafka topic "twitter". Extends Thread so that it can run
 * asynchronously.
 * 
 * @author Yannick Pferr
 * @author Tobias Mahncke
 * @version 3.1
 */
public class ProducerTwitterStreamingAPI extends Thread {

	/**
	 * Gets called on start of the Thread
	 */
	@Override
	public void run() {
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

		try {
			producer = new KafkaProducer<String, String>(props);
			// TODO: replace with keywords from file

			LinkedList<String> companies = loadCompanies();
			if (companies.isEmpty()) {
				System.out.println("No companies added, aborting...");
				producer.close();
				builder.stop();
				return;
			}

			System.out.println("starting search...");

			// Fetches Tweets that contain specified keywords
			hosebirdEndpoint.trackTerms(companies);
			builder.connect();
			
			final int abortSize = Integer.parseInt(PropertyLoader
					.getPropertyValue(PropertyFile.kafka, "abort.size"));

			// Stop at 100 tweets
			for (int i = 0; i < abortSize; i++) {
				try {
					String tweet = msgQueue.take().trim();
					producer.send(new ProducerRecord<String, String>("twitter",
							tweet));
					System.out.println(tweet);
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.out.println("Couldnt fetch tweets");
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

	private LinkedList<String> loadCompanies() {
		LinkedList<String> l = new LinkedList<String>();
		try {
			FileInputStream in = new FileInputStream(new File(
					"properties/companies"));
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String line = null;
			while ((line = br.readLine()) != null) {
				l.add(line);
			}
			br.close();
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return l;
	}
}