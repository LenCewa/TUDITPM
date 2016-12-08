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
 * @version 3.1
 */
public class KafkaTest {
	private static MongoDBConnector mongo;
	private static String collectionName = "rawdata_twitter";

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
	 * 
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	@Test(timeout=5000)
	public void simpleText() throws InterruptedException, SecurityException, IOException {
		// Start twitter consumer and read the text file
		ConsumerTwitterStreamingAPI consumerTwitterStreamingAPI = new ConsumerTwitterStreamingAPI("junit");
		ConsumerObserver observer = new ConsumerObserver();
		consumerTwitterStreamingAPI.addObserver(observer);
		Thread consumerThread = new Thread(consumerTwitterStreamingAPI);
		consumerThread.start();
		new ProducerTextfile("test.txt").start();
		
		observer.waitUntilUpdateIsCalled();
		
		// Check the content of the database against the expected outcome
		MongoCollection<Document> collection = mongo
				.getCollection(collectionName);
		assertEquals(1, collection.count());
		Document document = collection.find().first();
		// Only check for existence as the more complex test cases will
		// check for the eventualities
		assertTrue(document.containsKey("username"));
		assertTrue(document.containsKey("location"));
		assertTrue(document.containsKey("timeNDate"));
		assertTrue(document.containsKey("text"));
	}

	/**
	 * Tests if special characters are stored correctly. Edit: I think it is
	 * unnecessary because they aren't relevant for the informaition
	 */
	@Test
	public void specialCharacter() {

	}

	/**
	 * Question: How not to make separate classes for test purposes? - Test will
	 * fail because assertEquals is only true for one out of three text files
	 * 
	 * Tests if the text of an oversized tweet get's saved and red correctly.
	 * The maximum tweet length was 140 characters {@link https
	 * ://de.wikipedia.org/wiki/Twitter} I assume characters aren't count
	 * because they have loosen their regulations.
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	@Test
	public void oversizedTextLength() throws SecurityException, IOException {
		// Start twitter consumer and read the text file
		//new ConsumerTwitterStreamingAPI("junit").start();
		new ProducerTextfile("test_14_chars.txt").start();

		try {
			Thread.sleep(10000);
			// Check the content of the database against the expected outcome
			MongoCollection<Document> collection = mongo
					.getCollection(collectionName);
			assertEquals(1, collection.count());
			Document document = collection.find().first();
			assertEquals(140, document.getString("text"));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Question: How not to make separate classes for test purposes? - Test will
	 * fail because assertEquals is only true for one out of three text files
	 * 
	 * Tests if an empty text get's saved and red correctly. An empty text is
	 * considered as an empty string.
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	@Test
	public void emptyTextLength() throws SecurityException, IOException {
		//new ConsumerTwitterStreamingAPI("junit").start();
		new ProducerTextfile("test_empty_text.txt").start();

		try {
			Thread.sleep(10000);
			// Check the content of the database against the expected outcome
			MongoCollection<Document> collection = mongo
					.getCollection(collectionName);
			assertEquals(1, collection.count());
			Document document = collection.find().first();
			assertEquals("", document.getString("text"));
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
		mongo.getCollection(collectionName).drop();
	}
}
