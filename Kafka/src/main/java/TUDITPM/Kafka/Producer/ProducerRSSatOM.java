package TUDITPM.Kafka.Producer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.Level;

import org.bson.Document;

import TUDITPM.Kafka.LoggingWrapper;
import TUDITPM.Kafka.Topic;
import TUDITPM.Kafka.Connectors.MongoDBConnector;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

/**
 * Gets RSS and atOM feeds into a Kafka producer. a single feed of the specified
 * type.
 * 
 * @author Christian Zendo
 * @author Tobias Mahncke
 * @author Yannick Pferr
 * @version 6.0
 */
public class ProducerRSSatOM extends AbstractProducer {
	/** The database connector for the links of the checked articles */
	private MongoDBConnector mongo;
	/** The list of all feed URLs */
	private ArrayList<URL> allFeeds;
	/** The HashSet for fast reading of the links of the checked articles */
	private HashSet<String> visited = new HashSet<>();

	/**
	 * Constructor setting the database name.
	 * 
	 * @param env
	 *            the name of the environment to use for the collection of already
	 *            checked feed entries
	 */
	public ProducerRSSatOM(String env) {
		mongo = new MongoDBConnector("checkeddata_" + env);
		for (Document doc : mongo.getCollection("rss").find()) {
			visited.add(doc.getString("link"));
		}
	}

	/**
	 * Loads the feed URLs from the corresponding file.
	 * 
	 * @return An ArrayList of URLs containing all the feed sources.
	 */
	private ArrayList<URL> loadFeedSources() {
		ArrayList<URL> l = new ArrayList<>();
		try {
			FileInputStream in = new FileInputStream(new File("properties/feedsources"));
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.trim() != "") {
					try {
						l.add(new URL(line));
					} catch (MalformedURLException e) {
						// TODO Error Handling
						e.printStackTrace();
					}
				}
			}
			br.close();
			in.close();
		} catch (FileNotFoundException e) {
			// TODO Error Handling
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Error Handling
			e.printStackTrace();
		}
		LoggingWrapper.log(this.getClass().getName(), Level.INFO, "Loaded " + l.size() + " feed sources.");
		return l;
	}

	/**
	 * Reloads the feed sources.
	 */
	@Override
	public void initializeNeededData() {
		allFeeds = loadFeedSources();
	}

	/**
	 * Loops through all feeds and checks for new feed entries. If a new entry exists it is checked for the companies.
	 */
	@Override
	public void runRoutine() {
		for (int i = 0; i < allFeeds.size(); i++) {
			LoggingWrapper.log(this.getClass().getName(), Level.INFO, "Reading RSS: " + allFeeds.get(i));
			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = null;
			// Get the feed
			try {
				feed = input.build(new XmlReader(allFeeds.get(i)));
			} catch (IOException e) {
				LoggingWrapper.log(getName(), Level.WARNING,
						"Server returned HTTP response code: 403 for URL: "
								+ allFeeds.get(i).toExternalForm()
								+ ", continuing with next url");
				continue;
			} catch (IllegalArgumentException e) {
				// TODO Error Handling
				e.printStackTrace();
			} catch (FeedException e) {
				// TODO Error Handling
				e.printStackTrace();
			}

			// Counter for the found and skipped entries
			int found = 0;
			int skipped = 0;

			for (SyndEntry entry : feed.getEntries()) {
				String link = entry.getLink();
				// Articles that have the same link as an already checked are skippe
				if (!visited.contains(link) && entry.getDescription() != null) {
					String text = entry.getDescription().getValue();
					String title = entry.getTitle();

					if (entry.getPublishedDate() != null) {
						checkForCompany(Topic.rss, link, text, entry.getPublishedDate().toString(), title);
					} else {
						checkForCompany(Topic.rss, link, text, (new Date()).toString(), title);
					}

					visited.add(link);
					mongo.writeToDb(new Document("link", link), "rss");
					found++;
				} else {
					skipped++;
				}
			}
			LoggingWrapper.log(this.getClass().getName(), Level.INFO, "Scanned " + found + " entries, skipped " + skipped + " entries");
		}
	}
}
