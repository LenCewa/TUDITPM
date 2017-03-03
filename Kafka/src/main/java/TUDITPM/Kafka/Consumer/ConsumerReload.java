package TUDITPM.Kafka.Consumer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.bson.Document;
import org.json.JSONObject;

import TUDITPM.Kafka.LoggingWrapper;
import TUDITPM.Kafka.Topic;
import TUDITPM.Kafka.Connectors.MongoDBConnector;
import TUDITPM.Kafka.Connectors.Solr;
import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;
import TUDITPM.Kafka.Producer.AbstractProducer;
import TUDITPM.Kafka.Producer.ProducerRSSatOM;

/**
 * Listens to the reload topic and reloads producer and consumer respectively.
 * Extends Thread so that it can run asynchronously.
 * 
 * @author Yannick Pferr
 * @author Tobias Mahncke
 * 
 * @version 8.0
 */
public class ConsumerReload extends Thread {

	private String env;
	private final int PROXIMITY;
	private Consumer consumer;
	private LinkedList<AbstractProducer> producer = new LinkedList<>();

	public ConsumerReload(String env) {

		this.env = env;
		PROXIMITY = Integer.parseInt(PropertyLoader.getPropertyValue(PropertyFile.solr, "proximity"));
		consumer = new Consumer(env);
		consumer.start();
	}

	public void addProducer(AbstractProducer prod) {
		producer.add(prod);
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

		for (AbstractProducer prod : producer)
			prod.start();

		while (true) {
			ConsumerRecords<String, String> records = kafkaConsumer.poll(10);
			for (ConsumerRecord<String, String> record : records) {

				JSONObject json = new JSONObject(record.value());
				String msg = json.getString("msg");

				LoggingWrapper.log(this.getClass().getName(), Level.INFO, msg + ", reloading!");

				if (msg.equals("company added") || msg.equals("company removed")) {
					for (AbstractProducer prod : producer)
						prod.reload();
				} else if (msg.equals("keyword added") || msg.equals("keyword removed")
						|| msg.equals("category removed")) {
					consumer.reload();

					if (msg.equals("keyword added")){ //&& json.getBoolean("searchRaw")){
						LoggingWrapper.log(this.getClass().getName(), Level.INFO,
								"checking rawdata for keyword: " + json.getString("keyword") + " in category: " + json.getString("category"));
						checkRawDBForKeyword();
					}
				} else if (msg.equals("rss url added") || msg.equals("rss url removed")) {
					for (AbstractProducer prod : producer)
						if (prod instanceof ProducerRSSatOM)
							prod.reload();
				}
			}
		}
	}

	private void checkRawDBForKeyword() {

		MongoDBConnector mongoRaw = new MongoDBConnector("rawdata_" + env);
		
		for (String collection : mongoRaw.getAllCollectionNames()) {
			// Get every entry for the specific collection
			LinkedList<Document> data = mongoRaw.find(collection);

			for (Document entry : data) {
				if (!producer.isEmpty()) {
					String text = entry.getString("text");

					producer.getFirst().checkForCompany(Topic.rawdata, entry.getString("link"), text,
							entry.getString("date"), "");
				}
			}
		}
	}
}