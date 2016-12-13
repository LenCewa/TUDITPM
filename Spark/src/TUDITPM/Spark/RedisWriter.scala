package TUDITPM.Spark

import com.mongodb.casbah.MongoClient
import scala.util.parsing.json.JSONArray
import scala.util.parsing.json.JSONObject
import com.mongodb.casbah.commons.MongoDBObject
import redis.clients.jedis.Jedis
import java.util.ArrayList
import com.mongodb.DBObject

/**
 * Stores a MongoDB collection in Redis
 *
 * @author Ludwig Koch
 * @version 3.0
 */

object RedisWriter {
  //Open MongoDB connection
  val mongoConn = MongoClient("localhost", 27017)
  val mongoDB = mongoConn("dbtest")
  val mongoColl = mongoDB("testcollection")
  
  
  def writeToRedis(){
    //Read all elements in the MongoDB collection
    var it = mongoColl.find()
    
    //Build JSONString from Array
    val builder = MongoDBObject.newBuilder
    builder += "Meldungen" -> it.toArray
    val company = builder.result
    val result = company.toString()
    
    //Open Redis connection and store the collection
    val jedis = new Jedis("localhost")
    jedis.set("TestKey", result)
    println("Stored string : " + jedis.get("TestKey"));
  }
}