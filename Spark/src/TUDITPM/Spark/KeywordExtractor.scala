package TUDITPM.Spark

import org.clulab.processors.Processor
import org.clulab.processors.fastnlp.FastNLPProcessor
import play.api.libs.json._
import scala.util.parsing.json.JSONObject
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.SparkConf
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.commons.MongoDBObject
import org.clulab.struct.DirectedGraphEdgeIterator

/**
 * Extracts the text of a JSON formatted tweet, breaks it down and
 * writes all nouns to mongo db
 * 
 * @author Ludwig Koch
 * @author Yannick Pferr
 * 
 * @version 3.1
 */
object KeywordExtractor {
  
   val mongoConn = MongoClient("localhost", 27017)
   val mongoDB = mongoConn("keywords_dev")
   val mongoColl = mongoDB("company_trump")
  
  def extractKey(tweet: String) = {
   
    val jsonObject = Json.parse(tweet)
    var text = ""
    try {
      text = (jsonObject \ "text").as[String]
    } catch {
      case e: JsResultException => println("Limit reached")
    }

    val proc: Processor = new FastNLPProcessor
    val doc = proc.annotate(text)
    var keywords: String = ""
    for (sentence <- doc.sentences) {
      for ((i, x) <- sentence.tags.get.view.zipWithIndex) {
        if (i.toString().startsWith("N"))
          keywords = keywords + " " + sentence.words.array(x)
      }
    }
    val builder = MongoDBObject.newBuilder
    builder += "text" -> keywords
    val newObj = builder.result
    mongoColl.insert(newObj)
    doc.clear();
  }
}