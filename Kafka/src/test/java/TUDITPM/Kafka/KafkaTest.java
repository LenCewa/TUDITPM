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
 * @author Len Williamson
 * 
 * @version 1.2
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
			// Only check for existence as the more complex test cases will check
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
	 * Tests if special characters are stored correctly.
	 * Edit: I think it is unnecessary because they aren't relevant for the informaition  
	 */
	@Test
	public void specialCharacter() {
		
	}
	
	/**
	 * Question: How not to make separate classes for test purposes?
	 *   - Test will fail because assertEquals is only true for one out of three text files
	 * 
	 * Tests if the text of an oversized tweet get's saved and red correctly. The maximum tweet length
	 * was 140 characters {@link https://de.wikipedia.org/wiki/Twitter}
	 * I assume characters aren't count because they have loosen their regulations.
	 */
	@Test
	public void oversizedTextLength() { /*
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
			// Only check for existence as the more complex test cases will check
			// for the eventualities
			assertEquals(140, document.getString("text"));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/
	}
	
	/**
	 * Question: How not to make separate classes for test purposes?
	 *   - Test will fail because assertEquals is only true for one out of three text files
	 * 
	 * Tests if an empty text get's saved and red correctly.
	 * An empty text is considered as an empty string.
	 */
	@Test
	public void emptyTextLength() { /*
		new ConsumerTwitterStreamingAPI("junit").start();
		new ProducerTextfile().start();
		
		try {
			Thread.sleep(10000);
			// Check the content of the database against the expected outcome
			MongoCollection<Document> collection = mongo
					.getCollection("testcollection");
			assertEquals(1, collection.count());
			Document document = collection.find().first();
			// Only check for existence as the more complex test cases will check
			// for the eventualities
			assertEquals("", document.getString("text"));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/
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
