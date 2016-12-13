package TUDITPM.Spark

import com.mongodb.casbah.MongoClient
import scala.util.parsing.json.JSONArray
import scala.util.parsing.json.JSONObject
import com.mongodb.casbah.commons.MongoDBObject
import redis.clients.jedis.Jedis

/**
 * Stores collection items in Redis
 *
 * @author Ludwig Koch
 * @version 3.0
 */


object RedisWriter {
  //Open connection to MongoDB
  val mongoConn = MongoClient("localhost", 27017)
  val mongoDB = mongoConn("dbtest")
  val mongoColl = mongoDB("testcollection")
  
  def writeToRedis(){
    //Loads all elements in the collection
    var it = mongoColl.find()
    //Creates new JSONArray with all elements stored in it
    var news = new JSONArray(it.toList)
   
    //Builds new JSONObject
    val builder = MongoDBObject.newBuilder
    builder += "Meldungen" -> news
    val company = builder.result
    //Converts JSONObject to JSONString
    val result = company.toString()
    
    //Opens Redis connection and stores the JSONString
    val jedis = new Jedis("localhost")
    jedis.set("TestKey", result)
    println("Stored string : " + jedis.get("TestKey"));
  }
}