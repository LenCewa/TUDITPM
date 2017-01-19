package TUDITPM.Kafka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;

import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

/**
 * Gets RSS and atOM feeds into a Kafka producer. a single feed of the specified
 * type.
 * 
 * 
 * @author Christian Zendo
 * @author Tobias Mahncke
 * @version 5.0
 */
public class ProducerRSSatOM extends Thread {

	@Override
	public void run() {

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

		// Create the producer
		Producer<String, String> producer = null;

		Solr solr = new Solr();

		ArrayList<String> allFeeds = loadFeedSources();

		try {
			producer = new KafkaProducer<>(props);
			ArrayList<CloseableHttpClient> listClients = getClientList(allFeeds);

			// feed.setEntries(entries);
			// TODO: while schleife oder Timer drumherum fuer Dauerbetrieb..

			for (int i = 0; i < allFeeds.size(); i++) {
				try (CloseableHttpClient client = listClients.get(i)) {
					HttpUriRequest method = new HttpGet(allFeeds.get(i));
					try (CloseableHttpResponse response = client
							.execute(method);
							InputStream stream = response.getEntity()
									.getContent()) {
						System.out.println("Reading RSS: " + allFeeds.get(i));
						SyndFeedInput input = new SyndFeedInput();
						SyndFeed feed = input.build(new XmlReader(stream));

						for (SyndEntry entry : feed.getEntries()) {
							String title = entry.getTitle();
							System.out.println("Reading RSS " + i + ": " + title);
							if (entry.getDescription() != null) {
								String text = entry.getDescription().getValue();
								String id = solr.add(title + " " + text);
								JSONObject json = new JSONObject();
								boolean companyFound = false;
								for (String company : PropertyLoader
										.getCompanies()) {
									if (solr.search("\"" + company + "\"", id)) {
										json.put("company", company);
										companyFound = true;
										break;
									}
								}

								if (companyFound) {
									json.put("source", "rss");
									json.put("link", entry.getLink());
									json.put("title", title);
									json.put("text", text);
									json.put("id", id);
									if (entry.getPublishedDate() != null) {
										json.put("date",
												entry.getPublishedDate());
									} else {
										json.put("date", new Date().toString());
									}
									System.out.println("PRODUCER: " + json.toString());
									producer.send(new ProducerRecord<String, String>(
											"rss", json.toString()));
								} else {
									solr.delete(id);
								}
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private ArrayList<CloseableHttpClient> getClientList(ArrayList<String> feeds) {
		ArrayList<CloseableHttpClient> ret = new ArrayList<CloseableHttpClient>();
		for (int i = 0; i < feeds.size(); i++) {
			ret.add(HttpClients.createMinimal());
		}
		return ret;
	}

	private ArrayList<String> loadFeedSources() {
		ArrayList<String> l = new ArrayList<>();
		try {
			FileInputStream in = new FileInputStream(new File(
					"properties/feedsources"));
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String line = null;
			while ((line = br.readLine()) != null) {
				l.add(line);
			}
			br.close();
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Loaded " + l.size() + " feed sources.");

		return l;
	}

}
