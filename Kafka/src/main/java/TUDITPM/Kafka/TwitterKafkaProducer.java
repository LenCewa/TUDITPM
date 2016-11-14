package TUDITPM.Kafka;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

public class TwitterKafkaProducer {

	private static final String topic = "twitter";

	public static void run(String consumerKey, String consumerSecret, String token, String secret)
			throws InterruptedException {

		Properties props = new Properties();
		props.put("bootstrap.servers", "localhost:9092");
		props.put("acks", "all");
		props.put("retries", 0);
		props.put("batch.size", 16384);
		props.put("linger.ms", 1);
		props.put("buffer.memory", 33554432);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		Producer<String, String> producer = null;

		Authentication auth = new OAuth1(consumerKey, consumerSecret, token, secret);

		/**
		 * Set up your blocking queues: Be sure to size these properly based on
		 * expected TPS of your stream
		 */
		BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);
		BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>(1000);

		/**
		 * Declare the host you want to connect to, the endpoint, and
		 * authentication (basic auth or oauth)
		 */
		Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
		// Optional: set up some followings and track terms
		List<Long> followings = Lists.newArrayList(798176734965276673L);
		List<String> terms = Lists.newArrayList("trump");
		hosebirdEndpoint.followings(followings);
		// hosebirdEndpoint.trackTerms(terms);

		ClientBuilder builder = new ClientBuilder().name("Hosebird-Client-01") // optional:
																				// mainly
																				// for
																				// the
																				// logs
				.hosts(hosebirdHosts).authentication(auth).endpoint(hosebirdEndpoint)
				.processor(new StringDelimitedProcessor(msgQueue)).eventMessageQueue(eventQueue); // optional:
																									// use
																									// this
																									// if
																									// you
																									// want
																									// to
																									// process
																									// client
																									// events

		Client hosebirdClient = builder.build();
		// Attempts to establish a connection.
		hosebirdClient.connect();

		// producer
		producer = new KafkaProducer<>(props);

		// Do whatever needs to be done with messages
		for (int msgRead = 0; msgRead < 1000; msgRead++) {
			String message = null;
			try {
				message = msgQueue.take();
				System.out.println(message);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			producer.send(new ProducerRecord<String, String>(topic, message));
		}
		producer.close();
		hosebirdClient.stop();

	}

	public static void main(String[] args) {
		try {
			TwitterKafkaProducer.run("59CTodxzVORljR7sSyCEKIvwD", "AvksbDOKhyNLPbxLWlpbgs0oi4nKes2KlAdzr2ysgKCJIYfQW8",
					"798176734965276673-3itftrppVUnMKcZsYQIR912LCcvm1rF",
					"jnO0Q5oLUwgNqqtUlRNfwtjzORpcLBWcReqIXSB4LzLdC");
		} catch (InterruptedException e) {
			System.out.println(e);
		}
	}
}
