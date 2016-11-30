package TUDITPM.Spark

import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import java.io.PrintWriter
import java.io.File
import org.apache.spark.storage.StorageLevel

/**
 * Starts a Kafka Stream with reads from twitter topic
 * 
 * @author Yannick Pferr
 * @version 1.0
 */
object SparkKafkaStream {
  
  def main(args: Array[String]) {
    
    val conf = new SparkConf().setAppName("Spark Twitter")
    conf.setMaster("local[2]")
    val ssc = new StreamingContext(conf, Seconds(1))
  
    //set topic(s) to listen for
    val topics = Map("twitter" -> 1)
    //create kafka stream with specified topic
    val lines = KafkaUtils.createStream(ssc, "localhost:2181", "group-1", topics, StorageLevel.DISK_ONLY).map(_._2)
    
    //prints the received tweets (has to be in there because otherwhise
    //ssc.start() would not work)
    lines.foreachRDD(_.foreach(println))
    
    //start the kafka stream and wait until manually finished
    ssc.start()
    ssc.awaitTermination()
  }
}