package xyz.acygn.mokapot.util;

import java.util.concurrent.locks.Lock;

/**
 * A Lock that records the reason it was locked. Used for testing purposes.
 *
 * @author Alex Smith
 */
public interface InstrumentedLock extends Lock {

    /**
     * Locks this lock, tracking the reason why it was locked. If a lock is
     * locked via this mechanism, it must be unlocked using
     * <code>unlockWithReason</code>.
     *
     * @param reason The reason why the lock was locked.
     */
    void lockWithReason(Object reason);

    /**
     * Attempts to lock this lock, tracking the reason why it was locked. If a
     * lock is locked via this mechanism, it must be unlocked using
     * <code>unlockWithReason</code>.
     *
     * @param reason The reason why the lock was locked.
     * @return <code>true</code> if the lock was locked.
     */
    boolean tryLockWithReason(Object reason);

    /**
     * Unlocks this lock if it was locked via <code>lockWithReason</code>.
     *
     * @param reason The reason why the lock was locked. This must compare equal
     * (via <code>.equals()</code>) to the <code>reason</code> given when the
     * lock was originally locked.
     */
    void unlockWithReason(Object reason);
}
