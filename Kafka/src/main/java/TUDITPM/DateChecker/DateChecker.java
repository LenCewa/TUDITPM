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
	MongoDBConnector enhanced;
	MongoDBConnector checked;
	MongoDBConnector config;

	public DateChecker(String env) {
		redis = new RedisConnector();
		enhanced = new MongoDBConnector("enhanceddata_" + env);
		checked = new MongoDBConnector("checkeddata_"+ env);
		config = new MongoDBConnector("config_" + env);
	}

	public static boolean isLastMonth(Date date) {

		Date currentDate = new Date();
		long day30 = 2l * 24 * 60 * 60 * 1000;
		boolean olderThan30 = currentDate.before(new Date((date.getTime() + day30)));

		return olderThan30;
	}

	public void run() {

		loadLast30Days();
		checked.dropDatabase();
	}
	
	private void loadLast30Days() {
		LoggingWrapper.log(getClass().getName(), Level.INFO, "Refreshing Redis key monthList...");
		redis.deleteKey("monthList");
		int count = 0;
		for (Document doc : config.getCollection("companies").find()) {
			Date end = new Date();
			long day30 = 30l * 24 * 60 * 60 * 1000;
			Date start = new Date(end.getTime() - day30);
			
			BasicDBObject query = new BasicDBObject();
			query.put("date", BasicDBObjectBuilder.start("$gte", start).add("$lte", end).get());
	
			for (Document doc2 : enhanced.getCollection(doc.getString("searchName")).find(query).sort(new BasicDBObject("dateAdded", -1))){ 
				count++;
				redis.appendJSONToList("monthList", new JSONObject(doc2.toJson()));
			}
		}
		LoggingWrapper.log(getClass().getName(), Level.INFO, "Refreshing Redis key monthList done, " + count + " documents added");
	}
}
