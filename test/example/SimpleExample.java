package example;

import net.anotheria.idbasedlock.IdBasedLock;
import net.anotheria.idbasedlock.IdBasedLockManager;
import net.anotheria.idbasedlock.SafeIdBasedLockManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * This simple example demonstrates the usage.
 *
 * @author lrosenberg
 * @since 24.01.14 23:47
 */
public class SimpleExample {

	/**
	 * Number of threads used to test.
	 */
	private static final int THREADS = 10;
	/**
	 * Iterations in each thread.
	 */
	private static final int ITERATIONS = 100000;

	static class Counter{
		private int count = 0;

		public void increase(){
			count++;
		}

		public int getCount(){
			return count;
		}
	}

	/**
	 * Cache with counters.
	 */
	private Map<String, Counter> counterCache = new HashMap<String, Counter>();

	IdBasedLockManager<String> lockManager = new SafeIdBasedLockManager<String>();

	public void increaseCounterSafely(String id){
		IdBasedLock<String> lock = lockManager.obtainLock(id);
		lock.lock();
		try{
			Counter c = counterCache.get(id);
			if (c==null){
				c = new Counter();
				counterCache.put(id, c);
			}
			c.increase();

		}finally{
			lock.unlock();
		}
	}

	public void increaseCounterUnsafe(String id){
		Counter c = counterCache.get(id);
		if (c==null){
			c = new Counter();
			counterCache.put(id, c);
		}
		c.increase();
	}

	public void demonstrateModeWithIdLock() throws InterruptedException{
		final Random rnd = new Random(System.nanoTime());
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch finish = new CountDownLatch(THREADS);
		for (int i=0; i<THREADS; i++){
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					try{
						start.await();
					}catch(InterruptedException e){
						throw new AssertionError("Didn't expect that");
					}
					for (int i=0; i<ITERATIONS; i++)
						increaseCounterSafely(""+rnd.nextInt(5));

					finish.countDown();
				}
			});
			t.start();
		}

		System.out.println("Starting threads");
		start.countDown();
		finish.await();
		System.out.println("Finished");
		//calculating sum
		int sum = 0;
		for (Map.Entry<String,Counter> entry : counterCache.entrySet()){
			sum += entry.getValue().getCount();
		}
		int expected = THREADS * ITERATIONS;
		System.out.println("Sum is "+sum+", expected: "+expected+", diff: "+(sum - expected));

	}

	public void demonstrateModeWithoutIdLock() throws InterruptedException{
		final Random rnd = new Random(System.nanoTime());
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch finish = new CountDownLatch(THREADS);
		for (int i=0; i<THREADS; i++){
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					try{
						start.await();
					}catch(InterruptedException e){
						throw new AssertionError("Didn't expect that");
					}
					for (int i=0; i<ITERATIONS; i++)
						increaseCounterUnsafe(""+rnd.nextInt(5));

					finish.countDown();
				}
			});
			t.start();
		}

		System.out.println("Starting threads");
		start.countDown();
		finish.await();
		System.out.println("Finished");
		//calculating sum
		int sum = 0;
		for (Map.Entry<String,Counter> entry : counterCache.entrySet()){
			sum += entry.getValue().getCount();
		}
		int expected = THREADS * ITERATIONS;
		System.out.println("Sum is "+sum+", expected: "+expected+", diff: "+(expected - sum));

	}

	public static void main(String a[]) throws Exception{
		System.out.println("without lock");
		new SimpleExample().demonstrateModeWithIdLock();
		System.out.println("with id lock");
		new SimpleExample().demonstrateModeWithoutIdLock();
	}
}
