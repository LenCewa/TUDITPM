package TUDITPM.Spark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;


import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.bson.Document;
import org.clulab.serialization.json.JSONSerializer;
import org.json.JSONObject;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;


/**
 * 
 * @author Yannick Pferr
 * @author Ludwig Koch
 * 
 * @version 4.1
 *
 */
public class Solr {

	// Temporär, dann in Config
	private final String urlString = "http://localhost:8983/solr/new_core";
	private SolrClient solr;
	private final String id = "0";
	private String dbname = "dbtest";
	private String collection = "testcollection";
	private MongoClient mongo;
	private String keywordList = "C:/Users/Ludwig/Documents/TUDITPM/Kafka/properties/keywords";
	
	public Solr(){
	
		solr = new HttpSolrClient.Builder(urlString).build();
		mongo = new MongoClient("localhost", 27017);
		
		
	}
	
	/**
	 * Searches a tweet for a keyword, by adding a document to solr and deleting
	 * it afterwards
	 * @param tweet - the tweet to be searched
	 * @param keywords - the keywords to search for
	 * @return - true if tweet contains at least one of the keywords, otherwise false
	 */
	public boolean checkForKeyword(String tweet, String keywords){
		
		boolean containsKeywords = false;
		
		System.out.println(tweet);
		JSONObject json = new JSONObject(tweet);
		String extractedText = (String) json.get("text");
		boolean isAdded = add(extractedText);
		System.out.println(extractedText);
		
		if(isAdded)
			containsKeywords = search(keywords);
		else
			System.err.println("Document couldnt be added, contact an admin");
		
		delete();
		return containsKeywords;
	}
	
	/**
	 * Searches the solr documents for specified keyword
	 * @param keyword
	 * @return
	 */
	private boolean search(String keywords){
		SolrQuery query = new SolrQuery();
	    query.setQuery(keywords);
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

	    String         line = null;
	    StringBuilder  stringBuilder = new StringBuilder();

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
}
