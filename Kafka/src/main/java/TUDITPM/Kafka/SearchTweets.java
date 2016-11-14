package TUDITPM.Kafka;

import java.util.List;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class SearchTweets {

	/*
	 * searches Twitter for posts associated with the keyword returns List with
	 * all Tweets found or null if nothing was found
	 */
	public List<Status> searchTweets(String keyword) {

		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
		configurationBuilder.setDebugEnabled(true).setOAuthConsumerKey("59CTodxzVORljR7sSyCEKIvwD")
				.setOAuthConsumerSecret("AvksbDOKhyNLPbxLWlpbgs0oi4nKes2KlAdzr2ysgKCJIYfQW8")
				.setOAuthAccessToken("798176734965276673-3itftrppVUnMKcZsYQIR912LCcvm1rF")
				.setOAuthAccessTokenSecret("jnO0Q5oLUwgNqqtUlRNfwtjzORpcLBWcReqIXSB4LzLdC");

		TwitterFactory twitterFactory = new TwitterFactory(configurationBuilder.build());
		Twitter twitter = twitterFactory.getInstance();

		Query query = new Query(keyword);
		// Maximum 100
		// query.setCount(100);
		QueryResult result = null;
		try {
			result = twitter.search(query);
		} catch (TwitterException e) {
			e.printStackTrace();
			System.err.println("Error, Couldnt fech Twitter posts");
		}

		List<Status> tweets = null;
		if (result != null)
			tweets = result.getTweets();

		return tweets;
	}
}
