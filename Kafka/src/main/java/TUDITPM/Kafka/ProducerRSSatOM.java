package TUDITPM.Kafka;

import java.net.URL;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

import java.util.ArrayList;

import com.google.common.collect.Multiset.Entry;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.SyndFeedOutput;
import com.rometools.rome.io.XmlReader;

import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

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
		props.put("acks", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "acks"));
		props.put("retries", Integer.parseInt(PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "retries")));
		props.put("batch.size", Integer.parseInt(PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "batch.size")));
		props.put("linger.ms", Integer.parseInt(PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "linger.ms")));
		props.put("buffer.memory", Integer.parseInt(PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "buffer.memory")));
		props.put("key.serializer", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "key.serializer"));
		props.put("value.serializer", PropertyLoader.getPropertyValue(
				PropertyFile.kafka, "value.serializer"));
		
		// Create the producer
		Producer<String, String> producer = null;

		ArrayList<String> allFeeds = loadFeedSources();

		try {
			producer = new KafkaProducer<>(props);
			List<SyndEntry> entries = new ArrayList<SyndEntry>();
			ArrayList<CloseableHttpClient> listClients = getClientList(allFeeds);

			// feed.setEntries(entries);
			// TODO: while schleife oder Timer drumherum fuer Dauerbetrieb..

			for (int i = 0; i < allFeeds.size(); i++) {
				try (CloseableHttpClient client = listClients.get(i)) {
					HttpUriRequest method = new HttpGet(allFeeds.get(i));
					try (CloseableHttpResponse response = client.execute(method);
							InputStream stream = response.getEntity().getContent()) {
						SyndFeedInput input = new SyndFeedInput();
						SyndFeed feed = input.build(new XmlReader(stream));
						entries.addAll(feed.getEntries());
						
						for(SyndEntry entry : entries){
							JSONObject json = new JSONObject();
							json.put("source", "rss");
							json.put("link", entry.getLink());
							json.put("title", entry.getTitle());
							json.put("text", entry.getDescription().getValue());
							if(entry.getPublishedDate() != null){
								json.put("date", entry.getPublishedDate());
							}
							producer.send(new ProducerRecord<String, String>("rss", json.toString()));
						}
					}
				}
			}
		} catch (Exception ex) {
			System.out.println("ERROR: " + ex.getMessage());
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

		return l;
	}

}
