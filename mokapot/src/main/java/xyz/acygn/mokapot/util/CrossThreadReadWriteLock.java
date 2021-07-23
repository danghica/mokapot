package xyz.acygn.mokapot.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import static xyz.acygn.mokapot.util.ThreadUtils.delayInterruptions;

/**
 * A read/write lock in which locking isn't tied to a thread. The read lock is
 * recursive; the write lock is non-recursive.
 * <p>
 * Unlike most locks, this can be locked on one thread and unlocked on another,
 * meaning that the locks can safely be stored on objects that are concurrently
 * accessed from multiple threads. Additionally, as long as at least one read
 * lock is held, further attempts to read lock are guaranteed to succeed; they
 * will not block as a consequence of an attempt by another thread to create a
 * write lock. This avoids a potential deadlock (when a thread needs to take the
 * read lock in order to allow another thread to release it).
 *
 * @author Alex Smith
 */
public class CrossThreadReadWriteLock implements ReadWriteLock {

    /**
     * The number of read locks currently being held.
     */
    private int readCount = 0;

    /**
     * A set of reasons for which this lock was locked. Can be <code>null</code>
     * if reason tracking is not in use for this lock. (By default, reasons are
     * tracked only if assertions are enabled.)
     */
    private final Set<Object> lockReasons;

    /**
     * Whether there's currently a write lock being held.
     */
    private boolean isWriteLocked = false;

    /**
     * Creates a new cross-thread read/write lock. Both the read and write half
     * of the lock are initially unlocked.
     * <p>
     * This lock implementation supports tracking of the reasons why the lock is
     * locked, for debugging purposes. By default, this will be enabled only if
     * assertions are enabled.
     */
    @SuppressWarnings({"NestedAssignment", "AssertWithSideEffects"})
    public CrossThreadReadWriteLock() {
        Set<Object> tentativeLockReasons = null;
        /* Using an assert with side effects is actually the "official" way to
           see if assertions are enabled, for some reason. So here's a comment
           (in addition to the annotation above) to show it's intentional. */
        assert (tentativeLockReasons = new HashSet<>()) != null;
        lockReasons = tentativeLockReasons;
    }

    /**
     * Returns a Lock object that can be used to lock and unlock the read half
     * of this lock. Locking it will block until the write half is no longer
     * held, and then increment a counter. Unlocking it will decrement the
     * counter.
     *
     * @return A Lock object.
     */
    @Override
    public Lock readLock() {
        return new LockAdaptor(this, lockReasons) {
            @Override
            protected LockAdaptor.Wakable markUnlocked() {
                synchronized (CrossThreadReadWriteLock.this) {
                    if (readCount <= 0) {
                        throw new IllegalStateException(
                                "read lock unlocked too many times");
                    }
                    readCount--;
                    return readCount == 0 ? Wakable.ONE : Wakable.NONE;
                }
            }

            @Override
            public boolean tryLock() {
                synchronized (CrossThreadReadWriteLock.this) {
                    if (isWriteLocked) {
                        return false;
                    }
                    readCount++;
                    return true;
                }
            }
        };
    }

    /**
     * Returns a Lock object that can be used to lock and unlock the write half
     * of this lock. Locking it will block until there are no unlocked read
     * locks, and no unlocked write locks.
     *
     * @return A Lock object.
     */
    @Override
    public Lock writeLock() {
        return new LockAdaptor(this, lockReasons) {
            @Override
            protected LockAdaptor.Wakable markUnlocked() {
                synchronized (CrossThreadReadWriteLock.this) {
                    if (!isWriteLocked) {
                        throw new IllegalStateException(
                                "write lock unlocked when not held");
                    }
                    isWriteLocked = false;
                    return Wakable.ALL;
                }
            }

            @Override
            public boolean tryLock() {
                synchronized (CrossThreadReadWriteLock.this) {
                    if (readCount > 0 || isWriteLocked) {
                        return false;
                    }
                    isWriteLocked = true;
                    return true;
                }
            }
        };
    }

    /**
     * Gets an estimate of the number of times this lock is locked. This value
     * is accurate instantaneously, but may immediately become inaccurate due to
     * lock or unlock operations in other threads.
     *
     * @return The estimated number of current read locks using this lock.
     */
    public synchronized int countReadLocks() {
        return readCount;
    }

    /**
     * Produces a string summarising the current state of the lock.
     *
     * @return A string representation of the lock state.
     */
    @Override
    public synchronized String toString() {
        StringBuilder rv = new StringBuilder("lock: ");
        if (readCount > 0) {
            rv.append("read locked ");
            rv.append(readCount);
            rv.append(" times");
        } else if (isWriteLocked) {
            rv.append("write locked");
        } else {
            rv.append("unlocked");
        }
        if (lockReasons != null) {
            rv.append(" [reasons:");
            lockReasons.stream().unordered().distinct().forEach(
                    (reason) -> rv.append(" <").append(reason).append(">"));
            rv.append("]");
        }
        return rv.toString();
    }

    /**
     * An adaptor class that allows most of the Lock interface to be implemented
     * using only two methods. These are no-argument <code>tryLock</code> (which
     * does the same thing as in Lock), and <code>markUnlocked</code> (which
     * handles the marking of the lock as unlocked, but does not handle the
     * waking of blocked threads; that's implemented in this class).
     * <p>
     * The methods in this class are not optimised for high-contention
     * workloads; rather, they're designed to be as simple as possible whilst
     * complying with the specification. To help guarantee this, lock and unlock
     * operations will be serialised, i.e. there will never be two concurrent
     * calls to <code>tryLock</code> and/or <code>markUnlocked</code>.
     * <p>
     * <code>newCondition</code> is not implemented by this class, and will
     * throw an <code>UnsupportedOperationException</code> unless overriden in
     * an implementation.
     */
    private static abstract class LockAdaptor implements InstrumentedLock {

        /**
         * An enumeration listing the number of threads that will be able to be
         * woken as a result of unlocking the lock. In cases where this cannot
         * be accurately predicted, overestimating is preferable (this will
         * allow <code>lock</code> and <code>lockInterruptibly</code> to work
         * correctly, but <code>tryLock</code>-with-timeout may report a timeout
         * early).
         */
        enum Wakable {
            /**
             * Threads waiting for this lock to be unlocked will have to keep
             * waiting; it still can't be taken. (For example, a recursive lock
             * had one of its recursive layers unlocked but is still locked.)
             */
            NONE,
            /**
             * One thread waiting for this lock to be unlocked will be able to
             * take the lock, but any other threads will have to continue
             * waiting. (For example, an exclusive lock being unlocked will
             * allow one other thread to take the lock, but it will then block
             * other threads.)
             */
            ONE,
            /**
             * All threads waiting for this lock to be unlocked can now be
             * unblocked. (For example, a write lock being unlocked when it's
             * blocking read locks.)
             */
            ALL
        }

        /**
         * Object whose monitor is used to block threads that are waiting on a
         * lock. <code>wait()</code> on this object is used by threads to block
         * themselves, and <code>notify()</code> by threads that potentially
         * unlock the lock.
         *
         * @see Object#wait()
         */
        private final Object monitor;

        /**
         * A set of reasons for which this lock was locked. Can be
         * <code>null</code>, in which case the reasons will not be tracked.
         */
        private final Set<Object> lockReasons;

        /**
         * Default constructor. Initialises the monitor used for notifying
         * blocked threads.
         *
         * @param monitor The object whose monitor should be used to prevent
         * concurrent lock and/or unlock operations.
         * @param lockReasons A set of reasons that should be updated when the
         * lock is given a reason for locking or unlocking. Can be
         * <code>null</code> if reason tracking is not necessary.
         */
        protected LockAdaptor(Object monitor, Set<Object> lockReasons) {
            this.monitor = monitor;
            this.lockReasons = lockReasons;
        }

        /**
         * Locks this lock. Implemented in terms of
         * <code>lockInterruptibly</code>, but delays any interruptions until
         * after the locking process is complete.
         */
        @Override
        public void lock() {
            delayInterruptions(this::lockInterruptibly);
        }

        @Override
        public void lockWithReason(Object reason) {
            if (lockReasons != null) {
                synchronized (monitor) {
                    lockReasons.add(reason);
                }
            }
            try {
                lock();
            } catch (Exception ex) {
                if (lockReasons != null) {
                    synchronized (monitor) {
                        lockReasons.remove(reason);
                    }
                }
                throw ex;
            }
        }

        @Override
        public boolean tryLockWithReason(Object reason) {
            boolean lockOK = false;
            if (lockReasons != null) {
                synchronized (monitor) {
                    lockReasons.add(reason);
                }
            }
            try {
                lockOK = tryLock();
            } catch (Exception ex) {
                if (lockReasons != null) {
                    synchronized (monitor) {
                        lockReasons.remove(reason);
                    }
                }
                throw ex;
            }

            if (lockReasons != null && !lockOK) {
                synchronized (monitor) {
                    lockReasons.remove(reason);
                }
            }

            return lockOK;
        }

        /**
         * Locks this lock, without disabling interruptions. Implemented,
         * effectively, as repeatedly retrying a
         * <code>tryLock()</code>-with-infinite-timeout until it works.
         *
         * @throws InterruptedException If the thread was interrupted before the
         * lock could be taken.
         */
        @Override
        public void lockInterruptibly() throws InterruptedException {
            while (!tryLock(0, null)) {
                // loop has no body, we don't do anything between attempts
            }
        }

        /**
         * Generalised function for locking this lock in a blocking manner.
         * Allows an optional timeout (as opposed to the mandatory timeout of
         * the Lock class itself).
         *
         * @param time The length of time to wait before giving up the attempt
         * to lock the lock, measured in units of <code>unit</code>.
         * @param unit The units for <code>time</code>. Can be
         * <code>null</code>, in which case the locking attempt will block
         * indefinitely.
         * @return <code>true</code> if the lock was taken; <code>false</code>
         * if the attempt to take the lock failed due to timeout.
         * @throws InterruptedException If the thread was interrupted before the
         * lock could be taken.
         */
        @Override
        public boolean tryLock(long time, TimeUnit unit)
                throws InterruptedException {
            synchronized (monitor) {
                if (tryLock()) {
                    return true;
                }
                if (unit != null) {
                    unit.timedWait(monitor, time);
                } else {
                    monitor.wait();
                }
                return tryLock();
            }
        }

        /**
         * Performs the parts of an unlock operation that affect the lock
         * itself. Unlike <code>unlock()</code>, this does not wake threads that
         * are waiting for the lock to be unlocked itself (rather, it returns a
         * value indicating how many threads are to be woken).
         *
         * @return An indication of how many threads should be woken as a result
         * of this unlock operation.
         */
        protected abstract Wakable markUnlocked();

        /**
         * Unlocks this lock, waking any threads waiting on the unlock
         * operation.
         */
        @Override
        public void unlock() {
            /* Note: we have to hold the monitor while marking the lock as
               unlocked, so that we can notify on it. */
            synchronized (monitor) {
                switch (markUnlocked()) {
                    case NONE:
                        break;
                    case ONE:
                        monitor.notify();
                        break;
                    case ALL:
                        monitor.notifyAll();
                        break;
                }
            }
        }

        @Override
        public void unlockWithReason(Object reason) {
            if (lockReasons != null) {
                synchronized (monitor) {
                    lockReasons.remove(reason);
                }
            }
            unlock();
        }

        /**
         * Unimplemented method.
         *
         * @return Never returns.
         * @throws UnsupportedOperationException Always
         */
        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException(
                    "this lock implementation does not implement newCondition");
        }

        /**
         * Returns the set of reasons for which this lock was locked.
         *
         * @return A copy of the set of reasons, or <code>null</code> if the set
         * of reasons is not being tracked.
         */
        public Set<Object> getLockReasons() {
            if (lockReasons == null) {
                return null;
            }
            return new HashSet<>(lockReasons);
        }
    }
}
