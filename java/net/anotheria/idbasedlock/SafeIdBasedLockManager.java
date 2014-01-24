package net.anotheria.idbasedlock;


import java.util.HashMap;
import java.util.Map;

public class SafeIdBasedLockManager<K> extends AbstractIdBasedLockManager<K>
        implements IdBasedLockManager<K> {
    /**
     * Serialization version unique identifier.
     */
    private static final long serialVersionUID = -7086955847344168761L;
    private Map<K, IdBasedLock<K>> locks = new HashMap<K, IdBasedLock<K>>();

    public synchronized IdBasedLock<K> obtainLock(K id) {
        IdBasedLock<K> lock = locks.get(id);
        if (lock != null) {
            lock.increaseRefCount();
            return lock;
        }

        lock = new IdBasedLock<K>(id, this);
        locks.put(id, lock);
        return lock;
    }

    public synchronized void releaseLock(IdBasedLock<K> lock) {
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