package xyz.acygn.mokapot;

import java.time.Duration;
import java.util.concurrent.locks.Lock;
import xyz.acygn.mokapot.markers.NonCopiable;
import xyz.acygn.mokapot.markers.NonMigratable;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper;

/**
 * A distributed message that requests a migration write lock on a location
 * manager. This is used to stabilise the migration-related state on both
 * systems from changes for other reasons, holding it steady and unread while
 * the migration itself changes the current state of an object.
 * <p>
 * This method must only be sent from the system where the object is currently
 * hosted to a system that's not currently hosting it. (In particular, the
 * recipient of this message may safely assume that the "object" it receives is
 * a long reference.)
 *
 * @author Alex Smith
 */
class MigrationSynchronisationMessage extends SynchronousMessage<AutocloseableLockWrapper> {

    /**
     * Creates a new migration synchronisation message.
     *
     * @param object The object whose location manager contains the migration
     * write lock that needs locking. (This is expressed as a
     * <code>RemoteOnlyStandin</code> to help prevent accidental misuse of this
     * class; any valid caller should have one to hand.)
     */
    MigrationSynchronisationMessage(RemoteOnlyStandin<?> object) {
        this.object = object;
    }

    /**
     * The object to synchronise on. The object's location manager's migration
     * write lock will be the lock that this message is about.
     */
    private final Object object;

    /**
     * Locks the object's location manager's migration write lock. The return
     * value can also be used to unlock the lock (via <code>close()</code> and
     * possibly <code>unlockEarly()</code>).
     *
     * @return An object that can (and should!) be used to unlock the lock once
     * the lock on it is no longer required.
     */
    @Override
    protected AutocloseableLockWrapper calculateReply() {
        try {
            return new LockWrapper(getCommunicator()
                    .findLocationManagerForObject(object).getMigrationLock(false));
        } catch (AutocloseableLockWrapper.CannotLockException ex) {
            /* This shouldn't be possible; the location manager must already
               exist, because otherwise the remote system couldn't have told us
               about the object. */
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Duration periodic() {
        return null;
    }

    /**
     * A class that locks a lock during construction and unlocks it during
     * close. A long reference to this is sent over the network, so that unlocks
     * can be network-abstracted. (This is <code>NonCopiable</code> to protect
     * any Java API classes that might form the lock class from being
     * marshalled, and <code>NonMigratable</code> as it can only sensibly do its
     * job on the system where it was created: otherwise we'd have to marshal
     * the Java API classes in question anyway during the offlining process, and
     * there'd be no reason to expect the resulting method calls to be any more
     * efficient as they'd have to go cross-system anyway.)
     */
    private class LockWrapper extends AutocloseableLockWrapper implements
            NonCopiable, NonMigratable {

        /**
         * Creates a new lock wrapper for the given lock. As a side effect,
         * locks the lock in question.
         *
         * @param lock The lock to unlock when this class is autoclosed.
         */
        LockWrapper(Lock lock) {
            super(lock, "MigrationSynchronisationMessage");
        }
    }
}
