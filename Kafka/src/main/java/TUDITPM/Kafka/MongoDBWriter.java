package TUDITPM.Kafka;

import org.bson.Document;

import TUDITPM.Kafka.Loading.PropertyFile;
import TUDITPM.Kafka.Loading.PropertyLoader;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * Wrapper for the MongoDB database connection.
 * 
 * @author Ludwig Koch
 * @author Tobias Mahncke
 * @version 1.2
 */
public class MongoDBWriter {
	private String dbname;
	private String collection;
	private MongoClient mongo;

	/**
	 * Creates a new writer for the given database and collection.
	 * 
	 * @param dbname
	 *            - database name in which the data will be stored
	 * @param collection
	 *            - collection in which the data will be stored
	 */
	public MongoDBWriter(String dbname, String collection) {
		this.dbname = dbname;
		this.collection = collection;
		mongo = new MongoClient(PropertyLoader.getPropertyValue(
				PropertyFile.database, "ADRESS"),
				Integer.parseInt(PropertyLoader.getPropertyValue(
						PropertyFile.database, "PORT")));
	}

	/**
	 * Writes the given document to the defined database and collection
	 * 
	 * @param obj
	 *            - document to save
	 */
	public void writeToDb(Document obj) {
		MongoDatabase db = mongo.getDatabase(dbname);
		MongoCollection<Document> table = db.getCollection(collection);
		table.insertOne(obj);
	}
}