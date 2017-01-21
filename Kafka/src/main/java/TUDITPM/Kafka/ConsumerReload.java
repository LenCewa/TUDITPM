package TUDITPM.Kafka;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

/**
 * Listening to the twitter Stream and converting the given data to stream it to
 * spark. Extends Thread so that it can run asynchronously.
 * 
 * @author Yannick Pferr
 * @author Tobias Mahncke
 * 
 * @version 5.0
 */
public class ConsumerReload extends Thread {

	String dbname;
	Consumer consumer;
	ProducerTwitterStreamingAPI producerTwitter;
	ProducerRSSatOM producerRss;
	
	
	public ConsumerReload(String dbname) {
		
		this.dbname = dbname;
				
		consumer = new Consumer(dbname);
		consumer.start();
		
		producerTwitter = new ProducerTwitterStreamingAPI();
		producerTwitter.start();
		
		producerRss = new ProducerRSSatOM();
		producerRss.start();
	}
	
	/**
	 * Gets called on start of the Thread
	 */
	@Override
	public void run() {
		Properties props = new Properties();
		props.put("bootstrap.servers", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "bootstrap.servers"));
		props.put("group.id", "group-1");
		props.put("enable.auto.commit", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "enable.auto.commit"));
		props.put("auto.commit.interval.ms", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "auto.commit.interval.ms"));
		props.put("auto.offset.reset", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "auto.offset.reset"));
		props.put("session.timeout.ms", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "session.timeout.ms"));
		props.put("key.deserializer", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "key.deserializer"));
		props.put("value.deserializer", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "value.deserializer"));

		KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<String, String>(
				props);
		kafkaConsumer.subscribe(Arrays.asList("reload"));


		while (true) {
			ConsumerRecords<String, String> records = kafkaConsumer.poll(10);
			for (ConsumerRecord<String, String> record : records) {
				if(record.value().equals("company added")){
					
					try {
						new PropertyLoader();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					producerTwitter.interrupt();
					producerRss.interrupt();
					
					producerTwitter = new ProducerTwitterStreamingAPI();
					producerRss = new ProducerRSSatOM();
					
					producerTwitter.start();
					producerRss.start();
				}
				else if(record.value().equals("keyword added")){
					consumer.interrupt();
					
					consumer = new Consumer(dbname);
					
					consumer.start();
				}
				else if(record.value().equals("rss url added")){
					producerRss.interrupt();
					
					producerRss = new ProducerRSSatOM();
					
					producerRss.start();
				}
				System.err.println(record.value() + ", reloading");
			}
		}
	}
}