package TUDITPM.Kafka;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoDBWriter {
	private String address;
	private int port;
	private String dbname;
	private String collection;
	private MongoClient mongo;
	
	public MongoDBWriter(String address, int port, String dbname, String collection){
		this.address = address;
		this.port = port;
		this.dbname = dbname;
		this.collection = collection;
		mongo = new MongoClient(address, port); 
		
	}
	
	public void writetoDb(Document obj) {
		MongoDatabase db = mongo.getDatabase("testDB");
		MongoCollection<Document> table = db.getCollection("startup_log");

		table.insertOne(obj);
	}

}