package xyz.acygn.mokapot;

import java.util.concurrent.locks.Lock;
import xyz.acygn.mokapot.markers.DistributedError;
import xyz.acygn.mokapot.markers.NonCopiable;
import xyz.acygn.mokapot.markers.NonMigratable;
import xyz.acygn.mokapot.util.DeterministicAutocloseable;

/**
 * Either a set of migration actions that are remoted over a network, or else a
 * lock on a local location manager. This class exists to handle a race
 * condition: if we want to perform a migration action on a possibly remote
 * object, we want to either send a <code>MigrationActionsMessage</code> to the
 * remote object and act on the returned migration actions, or else to do them
 * on the local location manager. However, it's possible that the object will be
 * migrated in between whether we check whether it's local and when we actually
 * do the migration.
 * <p>
 * This object acts to solve the problem. Creating a migration monitor will do
 * one of two things: either it will place a migration read lock on the location
 * manager (thus preventing the object being migrated while it exists); or else
 * it will request a set of migration actions for the object over the network
 * (this does not require locking because the actions will be re-forwarded by
 * the destination if the object moves in the meantime). So this can be seen as
 * a sort of conditional migration lock.
 *
 * @author Alex Smith
 * @param <T> The actual type of the object which the monitored location manager
 * references.
 */
class MigrationMonitor<T> implements DeterministicAutocloseable,
        NonCopiable, NonMigratable {

    /**
     * The lock (if any) that the migration monitor is currently holding. Can be
     * <code>null</code> if no lock was taken, or if the lock has already been
     * released.
     */
    private Lock lock;

    /**
     * The set of migration actions that was obtained over the network. If this
     * is <code>null</code>, the object was local.
     */
    private final MigrationActions<T> remoteActions;

    /**
     * Creates a migration monitor for the given location manager. This will
     * either lock the location manager's migration state until
     * <code>close()</code> is called, or else use the network to gain a set of
     * migration actions. In the former case, the object will be local, and will
     * be prevented from leaving the current system until a call to
     * <code>close()</code>. In the latter case, no locking is done; the object
     * will have been non-local at some point but there is no guarantee that the
     * object will not be migrated (in general, it's impossible to prevent a
     * remote object migrating).
     *
     * @param lm The location manager to create a migration monitor for.
     * @param readOnly If <code>true</code>, the lock taken will only be a read
     * lock; if <code>false</code>, it will be a read/write lock.
     */
    MigrationMonitor(LocationManager<T> lm, boolean readOnly) {
        /* This is a bit like a double-checked-lock, but we actually use a lock
           both times; this is to allow us to do a read followed by a write lock
           in cases where a write lock is requested but the object is remote
           anyway. That lets us handle that case faster. If we only wanted a
           read lock as it is, we can skip the first check (it isn't helpful)
           and simply do the second. */
        CommunicationAddress lkl = readOnly ? null : lm.followLocationChain();
        lock = null;
        if (lkl == null) {
            lock = lm.getMigrationLock(readOnly);
            lock.lock();
            lkl = lm.followLocationChain();
        }

        if (lkl != null) {
            /* The object is remote. We can't prevent it moving. */
            close();
            try {
                remoteActions = lm.getCommunicator().
                        sendMessageSync(new MigrationActionsMessage<>(lm), lkl);
            } catch (Throwable ex) {
                throw new DistributedError(ex, "fetching migration monitor for "
                        + lm);
            }
        } else {
            /* The object is local, and we can prevent it leaving. */
            remoteActions = null;
            /* leave the lock locked */
        }
    }

    /**
     * Returns a set of migration actions for the remote object. If the object
     * is <i>known</i> to be local, <i>and</i>, at the time the migration
     * monitor was created, it took a lock to prevent the object migrating off
     * the local system, this returns <code>null</code>, specifying that it's
     * safe to run the migration action directly. In all other cases, a long
     * reference to a set of migration actions on a remote system is returned
     * (making it possible to migrate the object via calling those actions and
     * having them forwarded to the migration actions on the remote system).
     * <p>
     * This method is intended for use in the implementation of migration
     * actions, to choose between running a local implementation or else
     * remoting the whole thing to the current location of the object.
     *
     * @return A set of migration actions; or <code>null</code>.
     */
    public MigrationActions<T> getRemoteActions() {
        return remoteActions;
    }

    /**
     * Undoes any changes to the location manager's migration lock state that
     * have been made by this migration monitor. This will be a no-op if the
     * state was never locked (i.e. the migration actions were taken remotely
     * over a network), or if <code>close()</code> has been called already.
     */
    @Override
    synchronized final public void close() {
        if (lock == null) {
            return;
        }
        lock.unlock();
        lock = null;
    }
}
