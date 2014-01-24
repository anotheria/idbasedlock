package net.anotheria.idbasedlock;

import java.io.Serializable;

/**
 * Manager/Container for a set of id based locks. The locks are obtained withing the context of the manager.
 *
 * @param <K> the type of the id.
 */
public interface IdBasedLockManager<K> extends Serializable {
    /**
     * Gets the lock for the specified id. Does not locks but just returns the lock object.
     *
     * @param id the id to get the lock for.
     *
     * @return the lock object.
     */
    IdBasedLock<K> obtainLock(K id);

    void releaseLock(IdBasedLock<K> lock);
}
