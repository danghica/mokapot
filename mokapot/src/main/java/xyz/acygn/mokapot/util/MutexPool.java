package xyz.acygn.mokapot.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A "keyed" mutex; each thread specifies a key when locking the mutex, and two
 * threads can access the mutex concurrently as long as they specify different
 * keys. As such, this is a very permissive sort of mutex; the only time it will
 * block a thread is if another thread already has it locked with the same key.
 * <p>
 * The intended use is to ensure that if two threads perform the same operation
 * concurrently, they're operating on disjoint data (and thus will not interfere
 * with each other).
 *
 * @author Alex Smith
 * @param <K> The type of the keys used to control access to the mutex.
 */
public class MutexPool<K> {

    /**
     * The locks corresponding to each key. We have to be very careful to avoid
     * memory leaks here. There are three states the values can be in:
     * <ul>
     * <li><code>null</code>, only if the mutex is not locked with the
     * corresponding key;
     * <li><code>WeakReference(null)</code>, again only if the mutex is not
     * locked with the corresponding key;
     * <li><code>WeakReference(Lock)</code>, whether or not the mutex is locked
     * with that key.
     * </ul>
     *
     * To lock a key, we first ensure that the third case above applies, via an
     * atomic computation that will ensure that we have an actual lock in the
     * corresponding value. The lock is then held in <code>lockedMutex</code> in
     * order to prevent the weak reference self-clearing. When the weak
     * reference clears (which guarantees that the lock is not currently held),
     * it removes itself from the map, if it's there; if it is there, there's no
     * replacement lock, thus we know it's safe to clear the entry; if it isn't
     * there (e.g. the entry got re-locked with a different lock), then we don't
     * have to clear the entry.
     */
    private final ConcurrentMap<K, LockReference<Lock>> mutexes
            = new ConcurrentHashMap<>();

    /**
     * The lock that's locked by the current thread.
     */
    private final ThreadLocal<Lock> lockedMutex = new ThreadLocal<>();

    /**
     * A queue of lock references that might need to be removed from
     * <code>mutexes</code> to prevent memory leaks.
     */
    private final ReferenceQueue<Lock> cleanupQueue = new ReferenceQueue<>();

    /**
     * Locks this mutex pool. Blocks until no other threads are locking the
     * mutex pool with the given key.
     *
     * Locking the mutex twice (with the same or different keys) from the same
     * thread without unlocking it in between is not allowed.
     *
     * @param key The key with which to lock this mutex pool.
     * @return A value that can be used to automatically unlock the mutex pool
     * in a try-with-resources statement. (This doesn't have to be used; you can
     * unlock it manually if you prefer.)
     * @throws IllegalStateException If an attempt is made to lock a mutex pool
     * from a thread that already has it locked.
     */
    public DeterministicAutocloseable lock(K key) throws IllegalStateException {
        /* Make sure we don't already have the mutex locked. */
        if (lockedMutex.get() != null) {
            throw new IllegalStateException(
                    "locking a mutex pool recursively");
        }

        /* Make sure that there's something to lock.
           If we have to create a new lock (and most of the time, we will), we
           need to make sure it doesn't disappear from lref while this function
           is running; we do that by maintaining a strong reference to it
           through the course of the function. */
        ReentrantLock newLock = new ReentrantLock();
        LockReference<Lock> lref = mutexes.compute(key, (k, old)
                -> (old == null || old.get() == null)
                        ? new LockReference<>(k, newLock, cleanupQueue) : old);

        /* It's theoretically possible that lref was an existing weak
           reference, and self-cleared just after we ensured it was nonempty.
           (If it's a new weak reference, it can't self-clear because newLock
           is still live.) In this case, just try again. */
        Lock lock = lref.get();
        if (lock == null) {
            return lock(key);
        }

        /* We now know that the value corresponding to key in mutexes is
           definitely lref (because lref is not cleared, and lock references
           can't be uncleared and can't be removed from mutexes unless clear).
           Our strong reference to lock will prevent lref clearing (or being
           evicted from mutexes) until this function ends; and lockedMutex
           prevents a lock being evicted while anyone owns it.
         */
        lockedMutex.set(lock);
        lock.lock();

        /* We need to use the value of newLock in order to prevent it being
           optimized out, violating our safety guarantees. We use it to control
           the rate at which we clean up the cleanup queue. (1 would obviously
           be fast enough, but Java doesn't know that.) */
        for (int i = 0; i < (newLock != lref.get() ? 2 : 3); i++) {
            Reference<? extends Lock> deadReference = cleanupQueue.poll();
            if (deadReference != null
                    && deadReference instanceof LockReference) {
                ((LockReference) deadReference).cleanup();
            }
        }

        return () -> MutexPool.this.unlock();
    }

    /**
     * Unlocks this mutex pool. The current thread must be currently be locking
     * the mutex.
     *
     * @throws IllegalStateException If the mutex isn't currently locked.
     */
    public void unlock() throws IllegalStateException {
        try {
            lockedMutex.get().unlock();
        } catch (NullPointerException ex) {
            /* Presumably it wasn't locked. */
            throw new IllegalStateException(
                    "Unlocking a non-locked mutex pool");
        }
        lockedMutex.remove();
    }

    /**
     * A value of the map holding the mutexes; this is basically just a lock,
     * but it cleans up behind itself once it's no longer needed.
     *
     * @param <V> <code>Lock</code>, but given as a type parameter so that Java
     * can determine whether or not something is a <code>LockReference</code>
     * despite the presence of type erasure.
     */
    private class LockReference<V> extends WeakReference<V> {

        /**
         * The only key within <code>mutexes</code> where this particular lock
         * reference can be stored.
         */
        private final K key;

        /**
         * Create a lock reference appropriate for storing in
         * <code>mutexes</code>.
         *
         * @param key The key within <code>mutexes</code> at which the caller
         * plans to store this lock reference. (It cannot safely be stored at
         * any other key).
         * @param lock The lock to initially place within the lock reference.
         * @param queue <code>cleanupQueue</code>, but passed as a separate
         * parameter so that Java's type inference can handle the fact that its
         * type is <code>ReferenceQueue&lt;Lock&gt;</code> rather than
         * <code>ReferenceQueue&lt;V&gt;</code>.
         */
        LockReference(K key, V lock, ReferenceQueue<V> queue) {
            super(lock, queue);
            this.key = key;
        }

        /**
         * Atomically delete this lock reference from <code>mutexes</code>, if
         * it's there. Should be called only after this reference is cleared.
         * (The reference won't be placed onto a reference queue unless it's
         * cleared; thus it's simplest to call this only on references taken
         * from the cleanup queue.)
         */
        void cleanup() {
            mutexes.remove(key, this);
        }
    }
}
