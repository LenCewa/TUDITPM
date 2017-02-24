package TUDITPM.Kafka.Producer;

import java.io.IOException;
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
 * @version 6.1
 */
public class ProducerRSSatOM extends AbstractProducer {
	/** The database connector for the links of the checked articles */
	private MongoDBConnector mongo;
	/** The list of all feed URLs */
	private ArrayList<URL> allFeeds = new ArrayList<>();
	/** The HashSet for fast reading of the links of the checked articles */
	private HashSet<String> visited = new HashSet<>();

	/**
	 * Constructor setting the database name.
	 * 
	 * @param env
	 *            the name of the environment to use for the collection of
	 *            already checked feed entries
	 */
	public ProducerRSSatOM(String env) {
		super(env);
		mongo = new MongoDBConnector("checkeddata_" + env);
		for (Document doc : mongo.getCollection("rss").find()) {
			visited.add(doc.getString("link"));
		}
	}

	/**
	 * Reloads the feed sources.
	 */
	@Override
	public void initializeNeededData() {
		allFeeds = new ArrayList<URL>();
		for (Document doc : config.getCollection("rsslinks").find()) {
			try {
				allFeeds.add(new URL(doc.getString("link")));
			} catch (MalformedURLException e) {
				LoggingWrapper.log(getName(), Level.WARNING,
						"RSS URL " + doc.getString("link") + " is not a valid URL, consider removing it");
			}
		}
	}

	/**
	 * Loops through all feeds and checks for new feed entries. If a new entry
	 * exists it is checked for the companies.
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
				LoggingWrapper.log(this.getClass().getName(), Level.WARNING, "Server returned HTTP response code: 403 for URL: "
						+ allFeeds.get(i).toExternalForm() + ", continuing with next url");
				continue;
			} catch (IllegalArgumentException e) {
				LoggingWrapper.log(this.getClass().getName(), Level.WARNING,
						"Ivalid feed type for URL: " + allFeeds.get(i).toExternalForm() + ", continuing with next url");
				continue;
			} catch (FeedException e) {
				LoggingWrapper.log(this.getClass().getName(), Level.WARNING, "Couldn't get the feed for URL: "
						+ allFeeds.get(i).toExternalForm() + ", continuing with next url");
				continue;
			}

			// Counter for the found and skipped entries
			int found = 0;
			int skipped = 0;

			for (SyndEntry entry : feed.getEntries()) {
				String link = entry.getLink();
				// Articles that have the same link as an already checked are
				// skipped
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
			LoggingWrapper.log(this.getClass().getName(), Level.INFO,
					"Scanned " + found + " entries, skipped " + skipped + " entries");
		}
	}
}
