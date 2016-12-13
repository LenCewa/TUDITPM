package TUDITPM.Spark

import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import java.io.PrintWriter
import java.io.File
import org.apache.spark.storage.StorageLevel
import java.util.LinkedList
import scala.collection.JavaConversions._

/**
 * Starts a Kafka Stream which reads from twitter topic and passes
 * tweets to KeywordExtractor
 *
 * @author Yannick Pferr
 * @version 3.0
 */
object SparkKafkaStream {

  val tweets = new LinkedList[String]()

  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName("Spark Twitter")
    conf.setMaster("local[2]")
    val ssc = new StreamingContext(conf, Seconds(1))

    //set topic(s) to listen for
    val topics = Map("twitter" -> 1)
    //create kafka stream with specified topic
    val lines = KafkaUtils.createStream(ssc, "localhost:2181", "group-1", topics, StorageLevel.DISK_ONLY).map(_._2)

    //adds tweets to list
    lines.foreachRDD(_.foreach(tweets.add(_)))

    //thread that checks if max tweets level is reached, if so it stops the stream,
    //calls the function with annotates the text and writes all nouns to mongo db
    val thread = new Thread {
      override def run {
        while (true) {
          println(tweets.size())
          if (tweets.size() >= 10) {
            ssc.stop(false)
            for (tweet <- tweets)
              KeywordExtractor.extractKey(tweet)

            RedisWriter.writeToRedis()
            System.exit(1)

          }
        }
      }
    }
    RedisWriter.writeToRedis()
    thread.start()

    //start the kafka stream and wait until manually finished or
    ssc.start()
    ssc.awaitTermination()
  }
}