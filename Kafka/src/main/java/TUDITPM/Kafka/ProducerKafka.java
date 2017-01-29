package TUDITPM.Kafka;

import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.bson.Document;

import TUDITPM.Kafka.DBConnectors.MongoDBConnector;
import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

public abstract class ProducerKafka extends Thread {

	Producer<String, String> producer;
	Solr solr;
	MongoDBConnector config;
	private boolean reload = false;
	LinkedList<Document> companies = new LinkedList<>();

	public ProducerKafka() {

		// set configs for kafka
		Properties props = new Properties();
		props.put("bootstrap.servers", PropertyLoader.getPropertyValue(PropertyFile.kafka, "bootstrap.servers"));
		props.put("acks", PropertyLoader.getPropertyValue(PropertyFile.kafka, "acks"));
		props.put("retries", Integer.parseInt(PropertyLoader.getPropertyValue(PropertyFile.kafka, "retries")));
		props.put("batch.size", Integer.parseInt(PropertyLoader.getPropertyValue(PropertyFile.kafka, "batch.size")));
		props.put("linger.ms", Integer.parseInt(PropertyLoader.getPropertyValue(PropertyFile.kafka, "linger.ms")));
		props.put("buffer.memory",
				Integer.parseInt(PropertyLoader.getPropertyValue(PropertyFile.kafka, "buffer.memory")));
		props.put("key.serializer", PropertyLoader.getPropertyValue(PropertyFile.kafka, "key.serializer"));
		props.put("value.serializer", PropertyLoader.getPropertyValue(PropertyFile.kafka, "value.serializer"));

		producer = new KafkaProducer<String, String>(props);

		// initialize solr and the config DB
		solr = new Solr();
		config = new MongoDBConnector(PropertyLoader.getPropertyValue(PropertyFile.database, "config.name"));
	}

	/**
	 * Here you should setup everything that only needs to be initialized once
	 * e.g the companies list
	 */
	public abstract void initializeNeededData();

	/**
	 * Here you specify what your producer should do, so for example check every
	 * rss message in a list (initialized in initializeNeededData())
	 * 
	 * DO NOT USE AN INFINITE LOOP BECAUSE THIS IS ALREADY HANDLED IN THE RUN
	 * METHOD
	 */
	public abstract void runRoutine();

	public void reload() {
		reload = true;
	}

	@Override
	public void run() {
		LoggingWrapper.log(this.getClass().getName(), Level.INFO, "Thread started");
		initializeNeededData();

		while (true) {
			runRoutine();
			if (reload) {
				initializeNeededData();
				reload = false;
			}
		}
	}

}
