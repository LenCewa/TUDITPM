package TUDITPM.Kafka;

import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.simple.JSONObject;

import twitter4j.Status;

public class ProducerTwitter4j {

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
			SearchTweets st = new SearchTweets();
			while (true) {
				System.out.println("Type Keyword to search Tweets");
				String searchKeyword = sc.nextLine();
				List<Status> tweets = st.searchTweets(searchKeyword);

				for (Status tweet : tweets) {
					JSONObject obj = new JSONObject();
					obj.put("username", tweet.getUser().getScreenName());
					obj.put("text", tweet.getText());
					String json = obj.toJSONString();
					System.out.println(json);
					producer.send(new ProducerRecord<String, String>("twitter", json));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			producer.close();
		}

	}

}
