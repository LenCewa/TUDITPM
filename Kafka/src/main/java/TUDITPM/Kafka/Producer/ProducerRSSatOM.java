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
import java.util.HashSet;
import java.util.logging.Level;

import org.bson.Document;

import TUDITPM.Kafka.LoggingWrapper;
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

	private String dbname;
	private MongoDBConnector mongo;
	private ArrayList<URL> allFeeds;
	private HashSet<String> visited = new HashSet<>();

	/**
	 * Constructor setting the database name.
	 * 
	 * @param dbname
	 *            the name of the database to use for the collection of already
	 *            checked feed entries
	 */
	public ProducerRSSatOM(String dbname) {
		this.dbname = dbname;
	}

	/**
	 * Loads the feed URLs from the corresponding file.
	 * 
	 * @return An ArrayList of URLs containing all the feed sources.
	 */
	private ArrayList<URL> loadFeedSources() {
		ArrayList<URL> l = new ArrayList<>();
		try {
			FileInputStream in = new FileInputStream(new File(
					"properties/feedsources"));
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.trim() != "") {
					try {
						l.add(new URL(line));
					} catch (MalformedURLException e) {
						// TODO
						e.printStackTrace();
					}
				}
			}
			br.close();
			in.close();
		} catch (FileNotFoundException e) {
			// TODO
			e.printStackTrace();
		} catch (IOException e) {
			// TODO
			e.printStackTrace();
		}
		LoggingWrapper.log(this.getClass().getName(), Level.INFO,
				"Loaded " + l.size() + " feed sources.");
		return l;
	}

	@Override
	public void initializeNeededData() {
		mongo = new MongoDBConnector(dbname);
		allFeeds = loadFeedSources();

		for (Document doc : config.getCollection("companies").find()) {
			companies.add(doc);
		}
	}

	@Override
	public void runRoutine() {
		for (Document doc : mongo.getCollection("rss").find()) {
			visited.add(doc.getString("link"));
		}
		for (int i = 0; i < allFeeds.size(); i++) {
			LoggingWrapper.log(this.getClass().getName(), Level.INFO,
					"Reading RSS: " + allFeeds.get(i));
			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = null;
			try {
				feed = input.build(new XmlReader(allFeeds.get(i)));
			} catch (IOException e) {
				LoggingWrapper.log(getName(), Level.WARNING,
						"Server returned HTTP response code: 403 for URL: "
								+ allFeeds.get(i).toExternalForm()
								+ ", continuing with next url");
				continue;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (FeedException e) {
				e.printStackTrace();
			}

			int found = 0;
			int skipped = 0;

			for (SyndEntry entry : feed.getEntries()) {
				String link = entry.getLink();
				if (!visited.contains(link) && entry.getDescription() != null) {
					found++;
					String text = entry.getDescription().getValue();
					String title = entry.getTitle();
						
					// Checked here because of performance
					if ((text.trim().equals("") || text == null)
							&& (title.trim().equals("") || title == null)) {
						break;
					} else if (text.trim().equals("") || text == null) {
						text = title;
					}

					String id = solr.add(title + " " + text);

					if (entry.getPublishedDate() != null) {
						checkForCompany(id, "rss", link, text, entry.getPublishedDate().toString());
					} else {
						checkForCompany(id, "rss", link, text);
					}
					
					visited.add(link);
					mongo.writeToDb(new Document("link", link), "rss");
				} else {
					skipped++;
				}
			}
			LoggingWrapper.log(this.getClass().getName(), Level.INFO,
					"Scanned " + found + " entries, skipped " + skipped
							+ " entries");
		}
	}
}
