idbasedlock
===========

## Why do we need another lock?

Java has great support for concurrency and locking, probably the best support a modern language offers. Not only the language has builtin synchronization, but there are also utilities like CountDownLatches, Semaphores, Barriers, and other locks based on the wonderful AQS framework. However, there is one case that is not supported directly, which is when you actually need to lock not an _object itself_, but the _idea of the object_.

Imagine following example, you have a service that is maintaining mailboxes. It has a method to add a new message to a mailbox, and a method to retrieve a message from it. Of course you are multithreaded, since you want to serve tons of users sending gazillions of messages to each other. To protect the mailbox from corruption you want to restrict the number of the threads that access that mailbox to one:

```java
	public void sendMessage(UserId from, UserId to, Message message){
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

assuming that we have a min/max function on mailboxes, which maybe just compares hashCode or the owners id, and we use it to keep an order of synchronization (smaller first) to prevent deadlock, the implementation looks pretty safe, isn’t it?

Well it depends on what the _getMailbox()_ method actually does. If it guarantees us to always return the same object, than yes, we are save. But this a guarantee almost impossible to fulfill. Ok we could lock the whole _sendMessage()_ method by adding synchronize to it, but that would kill our performance, since we would only be able to deliver one message at a time. 

Just to illustrate what could possible get wrong in getMailbox, here is an implementation: 
```java
	private Mailbox getMailbox(UserId id){
		Mailbox mailbox = cache.get(id);
		if (mailbox==null){
			mailbox = new Mailbox();
			cache.put(id, mailbox);
		}
		return mailbox;
	}
```

Its obvious, that its unsafe, because it allows to parallel thread to create a new mailbox. Two mailboxes would exist in the system, and no one knows which one will survive. 
Of course you can solve this by locking the new mailbox creation, which brings back our problem with performance or do some funky ConcurrentMap style operations on your cache: putIfAbsent and stuff. But imagine that you don’t create the mailbox from scratch, but also load from database. This will be much harder to synchronize properly (and to prevent from parallel requests to the database.

Luckily id-based-locking solves exactly this problem by locking the concept of the mailbox instead of the mailbox object. 
```java

	IdBasedLockManager<UserId> manager = new SafeIdBasedLockManager<UserId>();
	
	public void sendMessageSafe(UserId from, UserId to, Message message){

		IdBasedLock<UserId> minLock = manager.obtainLock (min(from,to));
		IdBasedLock<UserId> maxLock = manager.obtainLock (max(from,to));
		try{
			minLock.lock();
			maxLock.lock();
			
			Mailbox sendersBox = getMailbox(from);
			Mailbox recipientsBox = getMailbox(to);
			sendersBox.addIncomingMessage(message);
			recipientsBox.addSentMessage(message);
		}finally{
			maxLock.lock();
			minLock.lock();
			
		}
	}
```

One might say, this example is to complicated, because it modifies two objects at a time. IdBasedLocking makes sense with one object too, consider this counter example:

```java
	public void increaseCounterUnsafe(String id){
		Counter c = counterCache.get(id);
		if (c==null){
			c = new Counter();
			counterCache.put(id, c);
		}
		c.increase();
	}
```
and again a safe version:

```java
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
```


