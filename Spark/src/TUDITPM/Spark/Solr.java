package TUDITPM.Spark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.LinkedList;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.bson.Document;
import org.json.JSONObject;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import TUDITPM.Spark.Loading.PropertyFile;
import TUDITPM.Spark.Loading.PropertyLoader;

/**
 * 
 * @author Yannick Pferr
 * @author Ludwig Koch
 * 
 * @version 5.0
 *
 */
public class Solr {

	// Tempor�r, dann in Config
	private final String urlString = PropertyLoader.getPropertyValue(PropertyFile.solr, "core.url");
	private SolrClient solr;
	private final String id = "0";
	private String dbname = "dbtest";
	private String collection = "testcollection";
	private MongoClient mongo;
	private String keywordList = "../properties/keywords";
	private final int PROXIMITY = Integer.parseInt(PropertyLoader.getPropertyValue(PropertyFile.solr, "proximity"));
	
	public Solr(){
	
		solr = new HttpSolrClient.Builder(urlString).build();
		mongo = new MongoClient(PropertyLoader.getPropertyValue(PropertyFile.database, "ADRESS"), 
				Integer.parseInt(PropertyLoader.getPropertyValue(PropertyFile.database, "PORT")));
	}
	
	/**
	 * Searches a tweet for a keyword, by adding a document to solr and deleting
	 * it afterwards
	 * @param tweet - the tweet to be searched
	 * @param keywords - the keywords to search for
	 * @return - true if tweet contains at least one of the keywords, otherwise false
	 */
	public boolean checkForKeyword(String company, String tweet, String[] keywords){
		
		boolean containsKeywords = false;
		
		System.out.println(tweet);
		JSONObject json = new JSONObject(tweet);
		String extractedText = (String) json.get("text");
		boolean isAdded = add(extractedText);
		System.out.println(extractedText);
		
		if(isAdded)
			containsKeywords = search(company, keywords);
		else
			System.err.println("Document couldnt be added, contact an admin");
		
		delete();
		return containsKeywords;
	}
	
	/**
	 * Searches the solr documents for specified keyword
	 * @param keyword
	 * @return true if doc contains keyword else false
	 */
	private boolean search(String company, String[] keywords){
		SolrQuery query = new SolrQuery();
	    query.setQuery("id:" + id + " AND \"" + company + " " + keywords[0] + "\"" + "~" + PROXIMITY);
	    QueryResponse response = null;
		try {
			response = solr.query(query);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    SolrDocumentList results = response.getResults();

	    if(!results.isEmpty())
	    	return true;
	    
	    return false;
	}
	
	/**
	 * Adds a tweet to solr documents
	 * @param tweet - the tweet to be added
	 * @return - returns true if successful else false
	 */
	private boolean add(String tweet){
		
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", id);
		doc.addField("_text_", tweet);
		try {
			solr.add(doc);
			solr.commit();
			return true;
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * deletes the created document after searching it
	 */
	private void delete(){
		try {
			solr.deleteById(id);
			solr.commit();
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes the text to MongoDB
	 * @param text the JsonString of the tweet element
	 */
	
	public void writeToDb(String text) {
		JSONObject json = new JSONObject(text);
		String extractedText = (String) json.get("text");
		
		Document obj = new Document("text", extractedText);
		MongoDatabase db = mongo.getDatabase(dbname);
		MongoCollection<Document> table = db.getCollection(collection);
		table.insertOne(obj);
	}
	
	/**
	 * Reads keywords from keyword textfile
	 * @return - returns all keywords seperated by a space
	 */
	
	public String readKeywords(){
		
		File file = new File(Paths.get(keywordList).toString());
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return null;
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return null;
		}

	    String line = null;
	    StringBuilder stringBuilder = new StringBuilder();

	    try {
	        while((line = reader.readLine()) != null) {
	            stringBuilder.append(line + " ");
	        }
	        reader.close();
	    } catch (IOException e) {
			e.printStackTrace();
			return null;
		} 
	    return stringBuilder.toString();
	}
	
	/**
	 * Reads companies from companies textfile
	 * @return - returns all companies in a list
	 */
	
	public LinkedList<String> loadCompanies() {
		LinkedList<String> l = new LinkedList<String>();
		try {
			FileInputStream in = new FileInputStream(new File(
					"properties/companies"));
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
	
	/**
	 * Closes the server connection
	 */
	public void close(){
		try {
			solr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException {
		new PropertyLoader();
		Solr solr = new Solr();
		solr.add("Projekt Quantum: GPU-Prozess kann Firefox schneller und sicherer machen");
	}
}