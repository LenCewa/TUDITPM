package TUDITPM.Spark

import com.mongodb.casbah.MongoClient
import scala.util.parsing.json.JSONArray
import scala.util.parsing.json.JSONObject
import com.mongodb.casbah.commons.MongoDBObject
import redis.clients.jedis.Jedis




object RedisWriter {
  val mongoConn = MongoClient("localhost", 27017)
  val mongoDB = mongoConn("dbtest")
  val mongoColl = mongoDB("testcollection")
  
  def writeToRedis(){
    var it = mongoColl.find()
    var news = new JSONArray(it.toList)
    
    val builder = MongoDBObject.newBuilder
    builder += "Meldungen" -> news
    val company = builder.result
    val result = company.toString()
    
    val jedis = new Jedis("localhost")
    jedis.set("TestKey", result)
    println("Stored string : " + jedis.get("TestKey"));
  }
}