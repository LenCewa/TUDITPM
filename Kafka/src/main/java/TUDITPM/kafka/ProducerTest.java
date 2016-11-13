package TUDITPM.Kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class ProducerTest {

  public static void main(String[] args) {
    Properties props = new Properties();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("acks", "all");
    props.put("retries", 0);
    props.put("batch.size", 16384);
    props.put("linger.ms", 1);
    props.put("buffer.memory", 33554432);
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    Producer<String, String> producer = null;
    try {
      producer = new KafkaProducer<>(props);
      String message = ReadFile.getMessage(null);
      boolean first = true;
      while(true){
    	  String m = ReadFile.getMessage(null);
    	  if(!m.equals(message) || first){
    		  String newMessage = m.substring(message.length(), m.length());
    		  System.out.println(m + " new: " + newMessage);
    		  if(!first)
    			  producer.send(new ProducerRecord<String, String>("TestKafkaTopic", newMessage));
    		  else
    			  producer.send(new ProducerRecord<String, String>("TestKafkaTopic", m));
    		  message = m;
    		  first = false;
    	  }
      }
//      for (int i = 0; i < 100; i++) {
//        String msg = "Message " + i;
//        producer.send(new ProducerRecord<String, String>("TestKafkaTopic", msg));
//        System.out.println("Sent:" + msg);
//      }
    } catch (Exception e) {
      e.printStackTrace();

    } finally {
      producer.close();
    }

  }

}
