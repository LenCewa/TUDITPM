package TUDITPM.DateChecker;

import java.util.Date;
import java.util.TimerTask;
import java.util.logging.Level;

import org.bson.Document;
import org.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;

import TUDITPM.Kafka.LoggingWrapper;
import TUDITPM.Kafka.Connectors.MongoDBConnector;
import TUDITPM.Kafka.Connectors.RedisConnector;

public class DateChecker extends TimerTask {

	RedisConnector redis;
	MongoDBConnector mongo;
	MongoDBConnector config;

	public DateChecker(String env) {
		redis = new RedisConnector();
		mongo = new MongoDBConnector("enhanceddata_" + env);
		config = new MongoDBConnector("config");// + env);
		loadLast30Days();
	}

	public static boolean isLastMonth(Date date) {

		Date currentDate = new Date();
		long day30 = 2l * 24 * 60 * 60 * 1000;
		boolean olderThan30 = currentDate.before(new Date((date.getTime() + day30)));

		return olderThan30;
	}

	public void run() {

		loadLast30Days();
	}
	
	private void loadLast30Days() {
		LoggingWrapper.log(getClass().getName(), Level.INFO, "Refreshing Redis key monthList...");
		redis.deleteKey("monthList");
		for (Document doc : config.getCollection("companies").find()) {

			Date end = new Date();
			long day30 = 30l * 24 * 60 * 60 * 1000;
			Date start = new Date(end.getTime() - day30);
			
			BasicDBObject query = new BasicDBObject();
			query.put("date", BasicDBObjectBuilder.start("$gte", start).add("$lte", end).get());
	
			for (Document doc2 : mongo.getCollection(doc.getString("searchName")).find(query).sort(new BasicDBObject("dateAdded", -1))) 
				redis.appendJSONToList("monthList", new JSONObject(doc2.toJson()));
		}
	}
}
