package xyz.acygn.mokapot.util;

import java.util.concurrent.locks.Lock;

/**
 * A simple utility class that acquires a lock when constructed, and releases it
 * when autoclosed. (In other words, this is RAII for locks, making it
 * impossible to fail to release them or to release them too many times.)
 *
 * @author Alex Smith
 */
public class AutocloseableLockWrapper implements DeterministicAutocloseable {

    /**
     * The lock that we're wrapping. Set to <code>null</code> when the lock is
     * released, to prevent double-releasing.
     */
    private Lock wrappedLock;

    /**
     * Whether the lock was unlocked manually. In this case, unlocking it
     * automatically upon autoclose shouldn't occur.
     */
    private boolean manuallyUnlocked = false;

    /**
     * The reason provided to the lock when it was locked. Will be
     * <code>null</code> if the lock does not accept reasons, or no reason was
     * provided.
     */
    private final Object reason;

    /**
     * Acquires the given lock, and creates an AutocloseableLockWrapper to
     * remember the fact that it's been acquired. The lock is released by
     * autoclosing the wrapper. Intended to be called from inside a
     * try-with-resources statement (if you were going to call it elsewhere, you
     * might as well just use the lock directly).
     *
     * @param lock The lock to acquire, and later release.
     * @param reason The reason why the lock was locked, for debugging purposes.
     * Can be <code>null</code>.
     */
    public AutocloseableLockWrapper(Lock lock, Object reason) {
        wrappedLock = lock;

        if (reason != null && lock instanceof InstrumentedLock) {
            ((InstrumentedLock) lock).lockWithReason(reason);
            this.reason = reason;
        } else {
            lock.lock();
            this.reason = null;
        }
    }

    /**
     * Acquires the given lock in a possibly non-blocking manner, and creates an
     * AutocloseableLockWrapper to remember the fact that it's been acquired.
     * The lock is released by autoclosing the wrapper. Intended to be called
     * from inside a try-with-resources statement (if you were going to call it
     * elsewhere, you might as well just use the lock directly).
     *
     * @param lock The lock to acquire, and later release.
     * @param blocking If true, waits until the lock is acquired; if false,
     * throws an exception if the lock cannot be acquired immediately.
     * @param reason The reason why the lock was locked, for debugging purposes.
     * Can be <code>null</code>.
     * @throws CannotLockException If the lock cannot be acquired immediately
     * and a non-blocking lock was requested
     */
    public AutocloseableLockWrapper(Lock lock, boolean blocking, Object reason)
            throws CannotLockException {
        wrappedLock = lock;
        if (blocking) {
            if (reason != null && lock instanceof InstrumentedLock) {
                ((InstrumentedLock) lock).lockWithReason(reason);
                this.reason = reason;
            } else {
                lock.lock();
                this.reason = null;
            }
        } else {
            if (reason != null && lock instanceof InstrumentedLock) {
                if (!((InstrumentedLock) lock).tryLockWithReason(reason)) {
                    throw new CannotLockException();
                }
                this.reason = reason;
            } else {
                if (!lock.tryLock()) {
                    throw new CannotLockException();
                }
                this.reason = null;
            }
        }
    }

    /**
     * Unlocks the lock, even though the wrapper has not been closed. This will
     * suppress automatic unlocking when the wrapper is closed, and also unlock
     * the lock. It should only be called once
     */
    public synchronized void unlockEarly() {
        if (manuallyUnlocked) {
            throw new IllegalStateException(
                    "AutocloseableLockWrapper unlocked early twice");
        }

        lowLevelUnlock();
        manuallyUnlocked = true;
    }

    /**
     * Releases the lock that this class is wrapping. This method should be
     * called exactly once for each <code>AutocloseableLockWrapper</code> that's
     * created (typically in a <code>finally</code> or try-with-resources
     * block).
     *
     * @throws IllegalStateException If the lock has already been unlocked
     */
    @Override
    public synchronized void close() throws IllegalStateException {
        if (manuallyUnlocked) {
            return;
        }
        if (wrappedLock == null) {
            throw new IllegalStateException(
                    "AutocloseableLockWrapper autoclosed twice");
        }

        lowLevelUnlock();
    }

    /**
     * Unlocks the lock, without checking to see if doing so is safe. This is
     * common code to the two unlocking methods and not intended to be used from
     * other contexts.
     */
    private void lowLevelUnlock() {
        if (reason != null && wrappedLock instanceof InstrumentedLock) {
            ((InstrumentedLock) wrappedLock).unlockWithReason(reason);
        } else {
            wrappedLock.unlock();
        }
        wrappedLock = null;
    }

    /**
     * An exception thrown if a lock cannot be locked immediately.
     */
    public static class CannotLockException extends Exception {

        /**
         * Explicit serialisation version, as is required for a serialisable
         * class to be compatible between machines. The number was originally
         * generated randomly, and should be changed whenever the class's fields
         * are changed in an incompatible way.
         *
         * @see java.io.Serializable
         */
        private static final long serialVersionUID = 0xfb6b4eb4f6e50e79L;
    }
}
