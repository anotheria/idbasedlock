Idbasedlock
===

## Why do we need another lock?

Java has great support for concurrency and locking, probably the best support a modern language can offer. Not only the language has built-in synchronization, but there are also utilities like *CountDownLatches*, *Semaphores*, *Barriers*, and other locks based on the wonderful *AQS framework*. However, there is one case that is not supported directly, which is when you actually need to lock not an **object itself**, but the **idea of the object**.

Imagine the following example: you have a service that maintains mailboxes. It has a method to add a new message to a mailbox, and a method to retrieve a message from it. Of course, you are multithreaded, since you want to serve tons of users sending gazillions of messages to each other. To protect the mailbox from corruption, you want to restrict the number of the threads that access that mailbox to one thread:

```java
	public void sendMessage(UserId from, UserId to, Message message) { 
		Mailbox sendersBox = getMailbox(from);
		Mailbox recipientsBox = getMailbox(to);
		min(sendersBox,recipientsBox).lock();
		max(sendersBox, recipientsBox).lock();
		sendersBox.addIncomingMessage(message);
		recipientsBox.addSentMessage(message);
		max(sendersBox, recipientsBox).lock();
		min(sendersBox,recipientsBox).lock();
	}
```	

Let's assuming that we have a min/max function on mailboxes, which maybe just compares *hashCode* or the owners *id*, and we use it to keep an order of synchronization (smaller first) to prevent deadlock. Then the implementation looks pretty safe, isn’t it?

Well, it depends on what the _getMailbox()_ method actually does. If it guarantees us to always return the same object, than yes, we are save. But it is always impossible to reach this guarantee. Ok, we could lock the whole _sendMessage()_ method by adding *synchronize* to it, but that would kill our performance, since we would only be able to deliver one message at a time. 

Just to illustrate what could possibly go wrong in _getMailbox()_, here is an implementation:

```java
	private Mailbox getMailbox(UserId id) {
		Mailbox mailbox = cache.get(id);
		if (mailbox == null) {
			mailbox = new Mailbox();
			cache.put(id, mailbox);
		}
		return mailbox;
	}
```

It's obvious that it is unsafe, since it allows parallel threads to create a new mailbox. Two mailboxes would exist in the system, and nobody knows which one survives.

Surely, you can solve this by locking the new mailbox creation (which brings back our problem with performance) or do some funky *ConcurrentMap* style operations in your cache: _putIfAbsent_ and stuff. But imagine that you don’t create the mailbox from scratch, but load it from database. This will be much harder to synchronize properly (and to prevent from parallel requests to the database.

Luckily, id-based-locking solves this very problem by locking the **concept** of the mailbox instead of the mailbox **object**.

```java

	IdBasedLockManager<UserId> manager = new SafeIdBasedLockManager<UserId>();
	
	public void sendMessageSafe(UserId from, UserId to, Message message) {

		IdBasedLock<UserId> minLock = manager.obtainLock(min(from, to));
		IdBasedLock<UserId> maxLock = manager.obtainLock(max(from, to));
		try {
			minLock.lock();
			maxLock.lock();
			
			Mailbox sendersBox = getMailbox(from);
			Mailbox recipientsBox = getMailbox(to);
			sendersBox.addIncomingMessage(message);
			recipientsBox.addSentMessage(message);
		} finally {
			maxLock.unlock();
			minLock.unlock();
			
		}
	}
```

One might say, this example is too complicated, because it modifies two objects at a time. IdBasedLocking makes sense with one object too. Consider the counter example below:

```java
	}
	public void increaseCounterUnsafe(String id) {
		Counter c = counterCache.get(id);
		if (c == null) {
			c = new Counter();
			counterCache.put(id, c);
		}
		c.increase();
	}
```
and, again, the safe version:

```java
		
	public void increaseCounterSafely(String id) {
		IdBasedLock<String> lock = lockManager.obtainLock(id);
		lock.lock();
		try{
			Counter c = counterCache.get(id);
			if (c == null) {
				c = new Counter();
				counterCache.put(id, c);
			}
			c.increase();

		} finally {
			lock.unlock();
		}
	}
```


## How to get

Convinced? Add to your **pom.xml**:

```xml
<dependency>
  <groupId>net.anotheria</groupId>
  <artifactId>idbasedlock</artifactId>
  <version>1.0.0</version>
</dependency>
```

or fork and play for yourself!

## Which lock?

Ah, you already looked at the code and noticed that there are two implementations available:
__SafeIdBasedLockManager__ and __UnsafeIdBasedLockManager__. 

The difference is the performance and the tradeof. In 99.9% of the use cases you should use **SafeIdBasedLockManager**. 

**UnsafeIdBasedLockManager** doesn't lock the process of obtaining the lock, and is therefore maybe 1 nanosecond faster, but it also has a race condition, so it shouldn't be used in highly concurrent environments. 

In other words: stick to the **SafeIdBasedLockManager**.


ENJOY!