package TUDITPM.Kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class ProducerTwitterStreamingAPI {

	public static void main(String[] args) {

		// set configs
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
		try {
			producer = new KafkaProducer<>(props);
			Scanner sc = new Scanner(System.in);
			
			String line = null;
			while (true) {
				ArrayList<String> keywords = new ArrayList<>();
				System.out.println("Type Keyword to search Tweets");
				keywords.add(sc.nextLine());
				SearchTweetsStreamingAPI st = new SearchTweetsStreamingAPI();
				
				System.out.println("Another Keyword? (y or Anything to stop)");
				while((line = sc.nextLine().toLowerCase()).equals("y")){
					System.out.println("Type next keyword");
					keywords.add(sc.nextLine());
					System.out.println("Another Keyword? (y or Anything to stop)");
				}	
				System.out.println("starting search...");
				st.setKeywords(keywords.toArray(new String[keywords.size()]));
				
				List<String> tweets = st.searchTweets();
				for (String tweet : tweets) {
					producer.send(new ProducerRecord<String, String>("twitter", tweet));
					System.out.println(tweet);
				}
				System.out.println("finished");
			}
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			producer.close();
		}

	}

}
