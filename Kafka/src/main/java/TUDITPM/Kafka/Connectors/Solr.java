package TUDITPM.Kafka.Connectors;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import TUDITPM.Kafka.LoggingWrapper;
import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

/**
 * 
 * @author Yannick Pferr
 * @author Ludwig Koch
 * 
 * @version 5.0
 *
 */
public class Solr {
	// Temporaer, dann in Config
	private final String urlString = PropertyLoader.getPropertyValue(
			PropertyFile.solr, "core.url");
	private static SolrClient solr = null;

	public Solr() {
		if (solr == null) {
			solr = new HttpSolrClient.Builder(urlString).build();
		}
	}

	/**
	 * Searches the solr documents for specified keyword
	 * 
	 * @param queryText - the text to be searched for
	 * @param id - the id of the doc to be searched
	 * @return true if doc contains keyword else false
	 */
	public boolean search(String queryText, String id) {
		SolrQuery query = new SolrQuery();
		query.setFilterQueries(id);
		query.setQuery(queryText);
		QueryResponse response = null;
		try {
			response = solr.query(query);
		} catch (SolrServerException e) {
			LoggingWrapper.log(getClass().getName(), Level.WARNING, "Solr server error, it might not be running or solr url: " + urlString + " is incorrect. Exiting...");
			System.exit(1);
		} catch (IOException e) {
			LoggingWrapper.log(getClass().getName(), Level.WARNING, "Communication error with Solr server: " + urlString + ". Exiting...");
			System.exit(1);
		}
		SolrDocumentList results = response.getResults();
		if (!results.isEmpty()) {
			return true;
		}

		return false;
	}

	/**
	 * Adds a text to solr documents and returns the id
	 * @param text - text to be added to solr
	 * @return - returns the id if successful null false
	 */
	public String add(String text) {
		SolrInputDocument doc = new SolrInputDocument();
		String id = Long.toString(new Date().getTime())
				+ Integer.toString((int) (Math.random() * 10));
		doc.addField("id", id);
		doc.addField("_text_", text);
		try {
			solr.add(doc);
			solr.commit();
			return id;
		} catch (SolrServerException e) {
			LoggingWrapper.log(getClass().getName(), Level.WARNING, "Solr server error, it might not be running or solr url: " + urlString + " is incorrect. Exiting...");
			System.exit(1);
		} catch (IOException e) {
			LoggingWrapper.log(getClass().getName(), Level.WARNING, "Communication error with Solr server: " + urlString + ". Exiting...");
			System.exit(1);
		}
		return null;
	}

	/**
	 * Deletes the created document by id
	 *  @param id - the id of the doc to be deleted
	 */
	public void delete(String id) {
		try {
			solr.deleteById(id);
			solr.commit();
		} catch (SolrServerException e) {
			LoggingWrapper.log(getClass().getName(), Level.WARNING, "Solr server error, it might not be running or solr url: " + urlString + " is incorrect. Exiting...");
			System.exit(1);
		} catch (IOException e) {
			LoggingWrapper.log(getClass().getName(), Level.WARNING, "Communication error with Solr server: " + urlString + ". Exiting...");
			System.exit(1);
		}
	}

	/**
	 * Closes the server connection
	 */
	public void close() {
		try {
			solr.close();
		} catch (IOException e) {
			LoggingWrapper.log(getClass().getName(), Level.WARNING, "Communication error with Solr server: " + urlString + ". Exiting...");
			System.exit(1);
		}
	}
}