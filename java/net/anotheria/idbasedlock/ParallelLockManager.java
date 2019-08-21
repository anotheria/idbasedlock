package net.anotheria.idbasedlock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

public class ParallelLockManager<T> {

    private Map<T, IdReentrantLock<T>> locks = new ConcurrentHashMap<>();

    //    private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    //    private final Lock readLock = rwl.readLock();
    //    private final Lock writeLock = rwl.writeLock();
    private StampedLock stampedLock = new StampedLock();

    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ParallelLockManager() {
        initLockCleaner();
    }

    private void initLockCleaner() {
        long initDelay = TimeUnit.SECONDS.toMillis(1);
        long period = TimeUnit.SECONDS.toMillis(1);
        scheduler.scheduleAtFixedRate(new RunnableJob(), initDelay, period, TimeUnit.MILLISECONDS);
    }

    private class RunnableJob implements Runnable {
        @Override
        public void run() {
            if (locks == null || locks.size() == 0) {
                return;
            }

            long stamp = stampedLock.writeLock();
            try {
                locks.entrySet().removeIf(e -> e.getValue().getRefs() <= 0);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stampedLock.unlockWrite(stamp);
                //writeLock.unlock();
            }
        }
    }

    public IdReentrantLock<T> lock(T id) {
        if (id == null) {
            throw new IllegalArgumentException("getLock() failed: id can't be null");
        }

        long stamp = stampedLock.readLock();
        //                    readLock.lock();
        try {
            IdReentrantLock<T> lock = locks.computeIfAbsent(id, v -> new IdReentrantLock<>(id));

            lock.lock();

            return lock;
        } finally {
            stampedLock.unlockRead(stamp);
            ////            readLock.unlock();
        }

    }

    int getLockSize() {
        return locks.size();
    }
}