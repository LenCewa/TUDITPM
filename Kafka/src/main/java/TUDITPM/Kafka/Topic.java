package TUDITPM.Kafka;

import java.util.ArrayList;
import java.util.Collection;

/**
 * List of all topics available in kafka. Corresponds with the list of sources. 
 * @author Tobias Mahncke
 * @author Yannick Pferr
 *
 * @version 6.0
 */
public enum Topic {
	twitter, rss, rawdata;
	
	/**
	 * Converts the enum in a list of strings.
	 * @return the list of strings
	 */
	public static Collection<String> toList(){
		ArrayList<String> topics = new ArrayList<>();
		for(Topic topic : Topic.values()){
			topics.add(topic.name());
		}
		return topics;
	}
}