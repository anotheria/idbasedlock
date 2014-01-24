package net.anotheria.idbasedlock;

import java.util.Map;

abstract class AbstractIdBasedLockManager<T> {
	int getLockSize(){
		return getLockMap().size();
	}
	
	String debugString(){
		return getLockMap().toString();
	}
	
	public static void out(Object message){
		//System.out.println(Thread.currentThread().getName()+" "+message);
	}
	
	protected abstract Map<T, IdBasedLock<T>> getLockMap();

}
