package TUDITPM.Kafka;

import java.io.IOException;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoDBWriter {
	private String dbname;
	private String collection;
	private MongoClient mongo;

	public MongoDBWriter(String dbname, String collection) {
		this.dbname = dbname;
		this.collection = collection;
		try {
			mongo = new MongoClient(PropertyLoader.getPropertyValue(
					PropertyFile.database, "ADRESS"),
					Integer.parseInt(PropertyLoader.getPropertyValue(
							PropertyFile.database, "PORT")));
		} catch (NumberFormatException e) {
			System.err.print("The given port could not be parsed to a number.");
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			System.err.print("Loading configuration for database failed.");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void writetoDb(Document obj) {
		MongoDatabase db = mongo.getDatabase(dbname);
		MongoCollection<Document> table = db.getCollection(collection);

		table.insertOne(obj);
	}

}