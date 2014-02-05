package net.anotheria.idbasedlock;
import com.romix.scala.collection.concurrent.TrieMap;

import java.util.Map;

public class CtrieIdBasedLockManager<K> extends AbstractIdBasedLockManager<K> implements IdBasedLockManager<K> {
	/**
	 *
	 */
    private static final long serialVersionUID = -7086955847344168762L;
    private Map<K, IdBasedLock<K>> locks = new TrieMap<K, IdBasedLock<K>>();

    public IdBasedLock<K> obtainLock(K id) {
        IdBasedLock<K> lock = locks.get(id);
        if (lock != null) {
            lock.increaseRefCount();
            return lock;
        }

        lock = new IdBasedLock<K>(id, this);
        locks.put(id, lock);
        return lock;
    }

    public void releaseLock(IdBasedLock<K> lock) {
        K id = lock.getId();
        if (lock.getRefCount().get() == 1) {
            locks.remove(id);
        }
        lock.decreaseRefCount();
    }

    @Override
    protected Map<K, IdBasedLock<K>> getLockMap() {
        return locks;
    }
}
