package TUDITPM.Kafka.Connectors;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import org.bson.Document;
import org.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

/**
 * Wrapper for the MongoDB database connection.
 * 
 * @author Ludwig Koch
 * @author Tobias Mahncke
 * @version 5.0
 */
public class MongoDBConnector {
	private String dbname;
	private MongoClient mongo;
	private MongoDatabase database;

	/**
	 * Creates a new writer for the given database and collection.
	 * 
	 * @param dbname
	 *            - database name in which the data will be stored
	 */
	public MongoDBConnector(String dbname) {
		this.dbname = dbname;
		mongo = new MongoClient(PropertyLoader.getPropertyValue(
				PropertyFile.database, "adress"),
				Integer.parseInt(PropertyLoader.getPropertyValue(
						PropertyFile.database, "port")));
		database = mongo.getDatabase(dbname);
	}

	/**
	 * Writes the given document to the defined database and collection
	 * 
	 * @param obj
	 *            - document to save
	 * @param collection
	 *            - collection in which the data will be stored
	 */
	public String writeToDb(Document obj, String collection) {
		MongoCollection<Document> table = database.getCollection(collection);
		table.insertOne(obj);
		
		BasicDBObject query = new BasicDBObject();
		query.put("link", obj.getString("link"));
		query.put("company", obj.getString("company"));
		query.put("category", obj.getString("category"));
		query.put("keyword", obj.getString("keyword"));
		for(Document doc : table.find(query)){
			return doc.get("_id").toString();
		}
		return null;
	}
	
	/**
	 * Gets every collection name contained in the database
	 * @return - names of every collection in the database
	 */
	public MongoIterable<String> getAllCollectionNames(){
		return database.listCollectionNames();
	}

	/**
	 * Gets the collection for the given name.
	 * 
	 * @param name
	 *            - name of the collection to retrieve
	 * @return
	 */
	public MongoCollection<Document> getCollection(String name) {
		return database.getCollection(name);
	}
	
	/**
	 * Executes a query to find specific data
	 * @param collection - the collection to be searched
	 * @param hm - A HashMap which contains all values to be searched for
	 * @return true if found else false
	 */
	public boolean contains(String collection, HashMap<String, String> hm){
		BasicDBObject query = new BasicDBObject();
		query.putAll(hm);
		
		for(Document doc : getCollection(collection).find(query))
			return true;
		
		return false;
	}
	
	/**
	 * Executes a query to find specific data
	 * @param collection - the collection to be searched
	 * @return - LinkedList with all found documents
	 */
	public LinkedList<Document> find(String collection){
		
		LinkedList<Document> data = new LinkedList<>();
		for(Document doc : getCollection(collection).find())
			data.add(doc);
		
		return data;
	}
	
	/**
	 * Executes a query to find specific data
	 * @param collection - the collection to be searched
	 * @param hm - A HashMap which contains all values to be searched for
	 * @return - LinkedList with all found documents
	 */
	public LinkedList<Document> find(String collection, HashMap<String, String> hm){
		BasicDBObject query = new BasicDBObject();
		query.putAll(hm);
		
		LinkedList<Document> data = new LinkedList<>();
		for(Document doc : getCollection(collection).find(query))
			data.add(doc);
		
		return data;
	}
	
	/**
	 * Drops the connected database.
	 */
	public void dropDatabase() {
		mongo.dropDatabase(dbname);
	}
	
	/**
	 * Closes the database connection
	 */
	public void close(){
		mongo.close();
	}
}