package net.anotheria.idbasedlock;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author
 */
public class IdReentrantLock<T> extends ReentrantLock {

    private T id;
    private AtomicInteger refs = new AtomicInteger(0);

    IdReentrantLock(T id) {
        this.id = id;
    }

    public T getId() {
        return id;
    }

    public int getRefs() {
        return refs.get();
    }

    @Override
    public void lock() {
        refs.incrementAndGet();
        super.lock();
    }

    @Override
    public void unlock() {
        refs.decrementAndGet();
        super.unlock();
    }

    @Override
    protected Thread getOwner() {
        return super.getOwner();
    }
}
