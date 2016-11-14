package TUDITPM.Kafka;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoDBConsumer {
	public static void writetoDb(Document obj) {
		MongoClient mongo = new MongoClient("localhost", 27017);
		MongoDatabase db = mongo.getDatabase("testDB");
		MongoCollection<Document> table = db.getCollection("startup_log");

		table.insertOne(obj);
	}

}