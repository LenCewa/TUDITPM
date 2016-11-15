package TUDITPM.Kafka;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

public class SearchTweetsStreamingAPI {

	List<Long> followings = null;
	List<String> terms = null;

	public void setUsers(Long... users){
		followings = Lists.newArrayList(users);
	}
	
	public void setKeywords(String... keywords){
		terms = Lists.newArrayList(keywords);
	}
	
	public List<String> searchTweets(){

		if(followings == null && terms == null){
			System.out.println("You need to set at least one of the search parameters");
			return null;
		}
		
		Authentication auth = new OAuth1(ApplicationCredentials.OAUTHCONSUMERKEY, 
				ApplicationCredentials.OAUTHCONSUMERSECRET, 
				ApplicationCredentials.OAUTHACCESSTOKEN,
				ApplicationCredentials.OAUTHACCESSTOKENSECRET);

		BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);

		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
		
		//Fetches Tweets from specific Users with their User Id
		if(followings != null)
			hosebirdEndpoint.followings(followings);
		
		//Fetches Tweets that contain specified keywords
		if(terms != null)
			hosebirdEndpoint.trackTerms(terms);

		Client builder = new ClientBuilder()
				.hosts(Constants.STREAM_HOST)
				.authentication(auth)
				.endpoint(hosebirdEndpoint).processor(new StringDelimitedProcessor(msgQueue)).build();
																								
		builder.connect();

		List<String> tweets = new LinkedList<String>();
		//read 100 Tweets
		for (int i = 0; i < 100; i++){
			try {
				tweets.add(msgQueue.take());
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.out.println("Couldnt fetch tweets");
			}
		}
		
		builder.stop();
		
		return tweets;
	}
}
