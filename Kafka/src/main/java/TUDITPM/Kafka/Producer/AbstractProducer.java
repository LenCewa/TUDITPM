package TUDITPM.Kafka.Producer;

import java.util.ArrayList;
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
import TUDITPM.Kafka.Topic;
import TUDITPM.Kafka.Connectors.MongoDBConnector;
import TUDITPM.Kafka.Connectors.Solr;
import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

/**
 * Abstract producer that includes all needed elements. It automates the
 * initialization and provides functions to implement a normal producers easily.
 * 
 * @author Yannick Pferr
 * @author Tobias Mahncke
 * @version 6.1
 */
public abstract class AbstractProducer extends Thread {
	/** The kafka producer. */
	private Producer<String, String> producer;
	/** The solr connector instance */
	private Solr solr;
	/** The MongoDB connector instance for the configuration database */
	MongoDBConnector config;
	/** The list of all companies from the configuration database. Initialized in {@link initialize} */
	LinkedList<Document> companies = new LinkedList<>();
	/** flag to indicate re-initializing before the next run */
	private boolean reload = false;

	/**
	 * Constructor that handles loading from configuration files. Creates the
	 * producer and needed connectors.
	 */
	AbstractProducer(String env) {
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

		// Initialize the producer
		producer = new KafkaProducer<String, String>(props);

		// Initialize the solr connector and the connector for the configuration
		// database
		solr = new Solr();
		config = new MongoDBConnector(PropertyLoader.getPropertyValue(
				PropertyFile.database, "config.name") + "_" + env);
	}

	/**
	 * Here you should setup everything that only needs to be initialized after
	 * startup or on reload e.g loading the companies list.
	 */
	abstract void initializeNeededData();
	
	/**
	 * Loads the company list from the config database and calls the {@link initializeNeededData} afterwards.
	 */
	private void initialize(){
		for (Document doc : config.getCollection("companies").find()) {
			companies.add(doc);
		}
		initializeNeededData();
	}

	/**
	 * <p>
	 * Here you specify what your producer should do, so for example check every
	 * rss message in a list (initialized in initializeNeededData()).
	 * </p>
	 * 
	 * <p>
	 * DO NOT USE AN INFINITE LOOP BECAUSE THIS IS ALREADY HANDLED IN THE RUN
	 * METHOD OF THIS CLASS.
	 * </p
	 */
	abstract void runRoutine();

	/**
	 * Sets the reload flag.
	 */
	public void reload() {
		reload = true;
	}

	/**
	 * Takes the entry, and checks it for companies. If the entry contains
	 * companies it is send via kafka if not the solr entry is removed.
	 * 
	 * This method MUST be used in the {@link runRoutine} for every entry.
	 * 
	 * @param topic
	 *            The topic to receive the message
	 * @param link
	 *            The link of the article.
	 * @param text
	 *            The text of the article.
	 * @param date
	 *            The publish date
	 * @param title
	 *            The title
	 */
	public void checkForCompany(Topic topic, String link, String text, String date,
			String title) {
		String id;
		// Check for empty title and text. If the title is set but no text, the
		// title is automatically assumed as text.
		if ((text.trim().equals("") || text == null)
				&& (title.trim().equals("") || title == null)) {
			return;
		} else if (text.trim().equals("") || text == null) {
			text = title;
			title = "";
			id = solr.add(text);
		} else {
			// Add the document to solr
			id = solr.add(title + " " + text);
		}

		JSONObject json = new JSONObject();
		boolean companyFound = false;
		for (Document company : companies) {
			// Combine the company search name and the alternative search terms for the solr query
			@SuppressWarnings("unchecked")
			ArrayList<String> searchTerms = (ArrayList<String>) company
					.get("searchTerms");
			String searchString = "\"" + company.getString("searchName") + "\"";
			if (searchTerms != null) {
				for (String term : searchTerms) {
					searchString += " \"" + term + "\"";
				}
			}
			// Search the added document for the company
			if (solr.search(searchString, id)) {
				// If the company is found create a json object and send it via kafka
				companyFound = true;
				json.put("searchName", company.getString("searchName"));
				json.put("companyKey", company.getString("key"));
				json.put("company", company.getString("name"));
				json.put("source", topic.name());
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
				LoggingWrapper.log(this.getClass().getName(), Level.INFO,
						json.toString());
				producer.send(new ProducerRecord<String, String>(topic.name(), json
						.toString()));
			}
		}
		// If no company was found delete the solr document
		if (!companyFound) {
			solr.delete(id);
		}
	}

	@Override
	public void run() {
		LoggingWrapper.log(this.getClass().getName(), Level.INFO,
				"Thread started");
		initialize();
		while (true) {
			runRoutine();
			// If the reload flag is set, re-init the data and continue running
			if (reload) {
				LoggingWrapper.log(this.getClass().getName(), Level.INFO,
						"Reloading data.");
				initialize();
				reload = false;
			}
		}
	}
}