package TUDITPM.Kafka.Consumer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import TUDITPM.Kafka.LoggingWrapper;
import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;
import TUDITPM.Kafka.Producer.ProducerRSSatOM;
import TUDITPM.Kafka.Producer.ProducerTwitterStreamingAPI;

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

	Consumer consumer;
	ProducerTwitterStreamingAPI producerTwitter;
	ProducerRSSatOM producerRss;

	public ConsumerReload(String env) {
		consumer = new Consumer(env);
		consumer.start();

		producerTwitter = new ProducerTwitterStreamingAPI(env);
		producerTwitter.start();

		producerRss = new ProducerRSSatOM(env);
		producerRss.start();
	}

	/**
	 * Gets called on start of the Thread
	 */
	@Override
	public void run() {
		Properties props = new Properties();
		props.put("bootstrap.servers", PropertyLoader.getPropertyValue(PropertyFile.kafka, "bootstrap.servers"));
		props.put("group.id", "group-1");
		props.put("enable.auto.commit", PropertyLoader.getPropertyValue(PropertyFile.kafka, "enable.auto.commit"));
		props.put("auto.commit.interval.ms",
				PropertyLoader.getPropertyValue(PropertyFile.kafka, "auto.commit.interval.ms"));
		props.put("auto.offset.reset", PropertyLoader.getPropertyValue(PropertyFile.kafka, "auto.offset.reset"));
		props.put("session.timeout.ms", PropertyLoader.getPropertyValue(PropertyFile.kafka, "session.timeout.ms"));
		props.put("key.deserializer", PropertyLoader.getPropertyValue(PropertyFile.kafka, "key.deserializer"));
		props.put("value.deserializer", PropertyLoader.getPropertyValue(PropertyFile.kafka, "value.deserializer"));

		KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<String, String>(props);
		kafkaConsumer.subscribe(Arrays.asList("reload"), new ConsumerRebalanceListener() {

			@Override
			public void onPartitionsRevoked(Collection<TopicPartition> arg0) {

			}

			@Override
			public void onPartitionsAssigned(Collection<TopicPartition> arg0) {
				// Lets consumer jump to the latest offset
				// so it doesnt consume messages published while it wasnt
				// running
				kafkaConsumer.seekToEnd(arg0);
			}
		});

		while (true) {
			ConsumerRecords<String, String> records = kafkaConsumer.poll(10);
			for (ConsumerRecord<String, String> record : records) {

				LoggingWrapper.log(this.getClass().getName(), Level.INFO, record.value() + ", reloading!");

				if (record.value().equals("company added") || record.value().equals("company removed")) {

					try {
						new PropertyLoader();
					} catch (IOException e) {
						LoggingWrapper.log(getName(), Level.WARNING, "Property files couldn't be reloaded, exiting...");
						System.exit(1);
					}

					producerTwitter.reload();
					producerRss.reload();
				} else if (record.value().equals("keyword added") || record.value().equals("keyword removed")
						|| record.value().equals("category removed")) {
					
					consumer.reload();
				} else if (record.value().equals("rss url added") || record.value().equals("rss url removed")) {
					producerRss.reload();
				}
			}
		}
	}
}