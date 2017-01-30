package TUDITPM.Kafka.Producer;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import TUDITPM.Kafka.LoggingWrapper;
import TUDITPM.Kafka.Connectors.MongoDBConnector;
import TUDITPM.Kafka.Connectors.Solr;
import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

/**
 * Abstract producer that includes all needed elements. It automates the
 * initialization and provides functions to implement a normal producers easliy.
 * 
 * @author Yannick Pferr
 * @author Tobias Mahncke
 * @version 6.0
 */
public abstract class AbstractProducer extends Thread {

	Producer<String, String> producer;
	Solr solr;
	MongoDBConnector config;
	private boolean reload = false;
	LinkedList<Document> companies = new LinkedList<>();

	/**
	 * Constructor that handles loading from configuration files. Creates the
	 * producer and needed connectors.
	 */
	public AbstractProducer() {
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

		// Init the 
		producer = new KafkaProducer<String, String>(props);

		// initialize solr and the config DB
		solr = new Solr();
		config = new MongoDBConnector(PropertyLoader.getPropertyValue(
				PropertyFile.database, "config.name"));
	}

	/**
	 * Here you should setup everything that only needs to be initialized after
	 * startup or o reload e.g the companies list
	 */
	public abstract void initializeNeededData();

	/**
	 * Here you specify what your producer should do, so for example check every
	 * rss message in a list (initialized in initializeNeededData())
	 * 
	 * DO NOT USE AN INFINITE LOOP BECAUSE THIS IS ALREADY HANDLED IN THE RUN
	 * METHOD OF THIS CLASS.
	 */
	public abstract void runRoutine();

	/**
	 * Sets the reload flag.
	 */
	public void reload() {
		reload = true;
	}
	
	/**
	 * Takes the entry, and checks it for companies. If the entry contains companies it is send via kafka 
	 * if not the solr entry is removed.
	 * @param id The id of the solr document
	 * @param source The source, e.g. "rss"
	 * @param link The link of the article.
	 * @param text The text of the article.
	 * @param date The publish date
	 * @param title The title
	 */
	public void checkForCompany(String id, String source, String link, String text, String date, String title){
		JSONObject json = new JSONObject();
		boolean companyFound = false;
		for (Document company : companies) {
			@SuppressWarnings("unchecked")
			ArrayList<String> searchTerms = (ArrayList<String>) company.get("searchTerms");
			String searchString = "\"" + company.getString("searchName") + "\"";
			if (searchTerms != null) {
				for (String term : searchTerms) {
					searchString += " \"" + term + "\"";
				}
			}
			if (solr.search(searchString, id)) {
				companyFound = true;
				json.put("searchName",
						company.getString("searchName"));
				json.put("companyKey", company.getString("key"));
				json.put("company", company.getString("name"));
				json.put("source", source);
				json.put("link", link);
				json.put("title", title);
				json.put("text", text);
				json.put("id", id);
				JSONArray JSONsearchTerms = new JSONArray();
				if (searchTerms != null) {
					for (String term : searchTerms) {
						JSONsearchTerms.put(term);
					}
				}
				json.put("searchTerms", JSONsearchTerms);
				json.put("date", date);
				LoggingWrapper.log(this.getClass().getName(), Level.INFO, json.toString());
				producer.send(new ProducerRecord<String, String>(source, json.toString()));
			}
		}
		if (!companyFound) {
			solr.delete(id);
		}
	}
	
	/**
	 * Takes the entry, and checks it for companies. If the entry contains companies it is send via kafka 
	 * if not the solr entry is removed.
	 * @param id The id of the solr document
	 * @param source The source, e.g. "rss"
	 * @param link The link of the article.
	 * @param text The text of the article.
	 * @param date The publish date
	 */
	public void checkForCompany(String id, String source, String link, String text, String date){
		checkForCompany(id, source, link, text, date, "");
	}
	
	/**
	 * Takes the entry, and checks it for companies. If the entry contains companies it is send via kafka 
	 * if not the solr entry is removed.
	 * @param id The id of the solr document
	 * @param source The source, e.g. "rss"
	 * @param link The link of the article.
	 * @param text The text of the article.
	 */
	public void checkForCompany(String id, String source, String link, String text){
		checkForCompany(id, source, link, text, "", "");
	}

	@Override
	public void run() {
		LoggingWrapper.log(this.getClass().getName(), Level.INFO,
				"Thread started");
		initializeNeededData();
		while (true) {
			runRoutine();
			// If the reload flag is set re-init the data and continue running
			if (reload) {
				LoggingWrapper.log(this.getClass().getName(), Level.INFO,
						"Reloading data.");
				initializeNeededData();
				reload = false;
			}
		}
	}
}