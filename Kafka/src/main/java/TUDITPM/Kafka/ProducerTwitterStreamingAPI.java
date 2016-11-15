package TUDITPM.Kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

public class ProducerTwitterStreamingAPI {

	public static void main(String[] args) {

		// set configs for kafka
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
		
		//set configs for hbc
		Authentication auth = new OAuth1(ApplicationCredentials.OAUTHCONSUMERKEY, 
				ApplicationCredentials.OAUTHCONSUMERSECRET, 
				ApplicationCredentials.OAUTHACCESSTOKEN,
				ApplicationCredentials.OAUTHACCESSTOKENSECRET);

		BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);

		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
		
		//Fetches Tweets from specific Users with their User Id
		//hosebirdEndpoint.followings(followings);
		
		Client builder = new ClientBuilder()
				.hosts(Constants.STREAM_HOST)
				.authentication(auth)
				.endpoint(hosebirdEndpoint)
				.processor(new StringDelimitedProcessor(msgQueue))
				.build();																			
		
		try {
			producer = new KafkaProducer<>(props);
			Scanner sc = new Scanner(System.in);
			
			String line = null;
			while (true) {
				ArrayList<String> keywords = new ArrayList<>();
				System.out.println("Type Keyword to search Tweets");
				keywords.add(sc.nextLine());
				
				System.out.println("Another Keyword? (y or Anything to stop)");
				while((line = sc.nextLine().toLowerCase()).equals("y")){
					System.out.println("Type next keyword");
					keywords.add(sc.nextLine());
					System.out.println("Another Keyword? (y or Anything to stop)");
				}	
				System.out.println("starting search...");
				
				
				//Fetches Tweets that contain specified keywords
				hosebirdEndpoint.trackTerms(keywords);
				builder.connect();
	
				for (int i = 0; i < 100; i++){
					try {
						String tweet = msgQueue.take();
						producer.send(new ProducerRecord<String, String>("twitter", tweet));
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
		}

	}

}
