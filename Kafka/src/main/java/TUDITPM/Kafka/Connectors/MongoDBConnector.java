package TUDITPM.Kafka.Connectors;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

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
	public void writeToDb(Document obj, String collection) {
		MongoCollection<Document> table = database.getCollection(collection);
		table.insertOne(obj);
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
	 * Drops the connected database.
	 */
	public void dropDatabase() {
		mongo.dropDatabase(dbname);
	}
}