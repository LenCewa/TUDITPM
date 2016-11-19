package TUDITPM.Kafka;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.bson.Document;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import TUDITPM.Kafka.Loading.PropertyLoader;

import com.mongodb.client.MongoCollection;

/**
 * Test cases for the general kafka functions. Includes a simple producer
 * reading from text files, a MongoDB consumer and several text/data related
 * special cases.
 * 
 * @author Tobias Mahncke
 * @version 1.1
 */
public class KafkaTest {
	private static MongoDBConnector mongo;

	/**
	 * Before running the test case connect to the junit database.
	 */
	@BeforeClass
	public static void connectToMongoDB() {
		try {
			new PropertyLoader();
		} catch (IOException e) {
			System.err.println("Could not load property files.");
			e.printStackTrace();
			System.exit(1);
		}
		mongo = new MongoDBConnector("junit");
	}

	/**
	 * Reads and stores a simple text.
	 */
	@Test
	public void simpleText() {
		// Start twitter consumer and read the text file
		new ConsumerTwitterStreamingAPI("junit").start();
		new ProducerTextfile().start();
		try {
			Thread.sleep(10000);
			// Check the content of the database against the expected outcome
			MongoCollection<Document> collection = mongo
					.getCollection("testcollection");
			assertEquals(1, collection.count());
			Document document = collection.find().first();
			// Only check for existence as the complexer test cases will check
			// for the eventualities
			assertTrue(document.containsKey("username"));
			assertTrue(document.containsKey("location"));
			assertTrue(document.containsKey("timeNDate"));
			assertTrue(document.containsKey("text"));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * After each test case the collections are dropped to create deterministic
	 * tests.
	 */
	@After
	public void emptyMongoDB() {
		// mongo.getCollection("testcollection").drop();
	}
}
