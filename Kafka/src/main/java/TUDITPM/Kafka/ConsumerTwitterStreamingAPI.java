package TUDITPM.Kafka;

import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

public class ConsumerTwitterStreamingAPI {

	public static void main(String[] args) {
		Properties props = new Properties();
		props.put("bootstrap.servers", "localhost:9092");
		props.put("group.id", "group-1");
		props.put("enable.auto.commit", "true");
		props.put("auto.commit.interval.ms", "1000");
		props.put("auto.offset.reset", "earliest");
		props.put("session.timeout.ms", "30000");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

		KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(props);
		kafkaConsumer.subscribe(Arrays.asList("twitter"));
		
		MongoDBWriter mongo = new MongoDBWriter("localhost", 27017, "dbtest", "testcollection");
		
		while(true){
			ConsumerRecords<String, String> records = kafkaConsumer.poll(100);
			int missedTweets = 0;
			for (ConsumerRecord<String, String> record : records) {
				
				System.out.println(record.value());
				try{
				//decode JSON String
				JSONObject jObj = new JSONObject(record.value());
				String text = jObj.getString("text");
				
				JSONObject user = jObj.getJSONObject("user");
				String name = user.getString("name");
				String username = user.getString("screen_name");
				String location = (!user.get("location").toString().equals("null")) ? user.getString("location") : "";
				
				//Write to DB
				mongo.writetoDb(new Document("name", name)
						.append("username", username)
						.append("location", location)
						.append("text", text));
				}catch(JSONException e){
					missedTweets++;
				}
			}
			if(missedTweets > 0)
				System.out.println(missedTweets + " Tweets missed");
		}
	}

}
