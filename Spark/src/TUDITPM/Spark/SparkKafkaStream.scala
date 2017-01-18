package TUDITPM.Spark

import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import java.io.PrintWriter
import java.io.File
import org.apache.spark.storage.StorageLevel
import java.util.LinkedList
import scala.collection.JavaConversions._
import TUDITPM.Spark.Loading.PropertyLoader
import TUDITPM.Spark.Loading.PropertyFile

/**
 * Starts a Kafka Stream which reads from twitter topic and passes
 * tweets to KeywordExtractor
 *
 * @author Yannick Pferr
 * @version 4.0
 */
object SparkKafkaStream {

  val tweets = new LinkedList[String]()

  def main(args: Array[String]) {
    
    //EXCEPTION NOCH ABFANGEN
    new PropertyLoader()

    val conf = new SparkConf().setAppName("Spark Twitter")
    conf.setMaster(PropertyLoader.getPropertyValue(PropertyFile.spark, "master"))
    val ssc = new StreamingContext(conf, Seconds(Integer.parseInt(PropertyLoader.getPropertyValue(PropertyFile.spark, "batch.size"))))

    //set topic(s) to listen for
    val topics = Map("twitter" -> 1)
    //create kafka stream with specified topic
    val lines = KafkaUtils.createStream(ssc, 
        PropertyLoader.getPropertyValue(PropertyFile.spark, "server"), 
        PropertyLoader.getPropertyValue(PropertyFile.spark, "group.id"), 
        topics, 
        StorageLevel.DISK_ONLY).map(_._2)

    //adds tweets to list
    lines.foreachRDD(_.foreach(tweets.add(_)))

    //thread that checks if max tweets level is reached, if so it stops the stream,
    //calls the function with annotates the text and writes all nouns to mongo db
    val thread = new Thread {
      override def run {
        while (true) {
          println(tweets.size())
          if (tweets.size() >= 98) {
            ssc.stop(false)
            val solr = new Solr
            for (tweet <- tweets){
               if(solr.checkForKeyword(tweet, "trump"))
                 solr.writeToDb(tweet);
            }
            
            RedisWriter.writeToRedis()
            System.exit(1)

          }
        }
      }
    }
    thread.start()

    //start the kafka stream and wait until manually finished or
    ssc.start()
    ssc.awaitTermination()
  }
}