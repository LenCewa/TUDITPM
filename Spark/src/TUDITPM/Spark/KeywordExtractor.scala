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
 * @version 3.0
 */
object KeywordExtractor {

  // declared and initialized here so there is only one connection for every tweet
  val mongoConn = MongoClient("localhost", 27017)
  val mongoDB = mongoConn("dbtest")
  val mongoColl = mongoDB("testcollection")

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
    // loops through all the sentences 
    for (sentence <- doc.sentences) {
      // loops through every Pos Tag of a sentence
      for ((i, x) <- sentence.tags.get.view.zipWithIndex) {
        // "N" because all the nouns should be extracted and
        // only noun Pos Tags start with "N"
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