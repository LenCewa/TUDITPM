package TUDITPM.Kafka;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CountDownLatch;

/**
 * 
 * 
 * @author Tobias Mahncke
 * 
 * @version 3.1
 */
public class ConsumerObserver implements Observer {
	private CountDownLatch latch = new CountDownLatch(1);
	private Object object;

	@Override
	public void update(Observable o, Object arg) {
		object = arg;
		latch.countDown();
	}

	public void waitUntilUpdateIsCalled() throws InterruptedException {
		latch.await();
	}
	
	public Object getResult(){
		return object;
	}
}
