package xyz.acygn.mokapot;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import xyz.acygn.mokapot.markers.Copiable;
import xyz.acygn.mokapot.markers.DistributedError;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper;
import xyz.acygn.mokapot.util.BackgroundGarbageCollection;
import xyz.acygn.mokapot.util.DeterministicAutocloseable;
import xyz.acygn.mokapot.util.Expirable;

/**
 * A class that tracks the remote systems which are still running a projection
 * of a specific thread from this system. This information is used to deallocate
 * thread-local state stored on such systems when the thread ends.
 *
 * @author Alex Smith
 */
class ThreadProjectionTracker implements Expirable {

    /**
     * This thread projection tracker's state.
     */
    private final State state;

    /**
     * Creates a new thread projection tracker.
     *
     * @param threadID The thread whose projections should be tracked.
     * @param communicator The communicator from whose point of view we are
     * tracking connections.
     * @throws AutocloseableLockWrapper.CannotLockException If the communicator
     * is shutting down, and no longer needs thread projection tracking
     */
    @SuppressWarnings("LeakingThisInConstructor")
    ThreadProjectionTracker(GlobalID threadID,
            DistributedCommunicator communicator)
            throws AutocloseableLockWrapper.CannotLockException {
        this.state = new State(threadID, communicator);
        BackgroundGarbageCollection.addFinaliser(this, state);
    }

    @Override
    public void expire() throws ExpiredException {
        state.expire();
    }

    /**
     * Tracks a thread projection for the thread whose projections are being
     * tracked by this tracker.
     * <p>
     * Note: this should only be called from the thread in question, and the
     * thread should be held alive in the meantime to avoid a concurrent update
     * to a field.
     *
     * @param where The remote system on which the thread projection resides.
     * @throws ExpiredException If this state object has already expired
     */
    void addProjection(CommunicationAddress where) throws ExpiredException {
        state.addProjection(where);
    }

    /**
     * A class that holds the state of the thread projection tracker, without
     * holding the tracker itself alive. Used to enable a finalizer to be
     * written using only Java's modern set of garbage collector hooks.
     */
    static private class State implements Expirable, Runnable {

        /**
         * The ID number of the thread that this thread projection tracker
         * relates to.
         */
        private final GlobalID threadID;

        /**
         * Whether this thread projection tracker has expired.
         */
        private final AtomicBoolean expired = new AtomicBoolean(false);

        /**
         * The list of communication addresses with residual state relating to
         * this thread.
         */
        private final Collection<CommunicationAddress> remoteProjections
                = new HashSet<>();

        /**
         * The communicator from whose point of view we are tracking the thread
         * projections. In other words, this is the communicator that learns
         * about the thread dying, and pushes notifications about the thread's
         * death to the virtual machines it projected the thread onto.
         */
        private final DistributedCommunicator communicator;

        /**
         * The keepalive lock that holds the communicator alive while it still
         * has thread projection trackers.
         */
        private final DeterministicAutocloseable keepaliveLock;

        /**
         * Constructs a new thread projection tracker state.
         *
         * @param threadID The thread's thread ID.
         * @param communicator The distributed communicator that the relevant
         * section of the thread belongs to.
         * @throws AutocloseableLockWrapper.CannotLockException If the
         * communicator is shutting down, and thus no longer interested in
         * thread projection tracking
         */
        State(GlobalID threadID, DistributedCommunicator communicator)
                throws AutocloseableLockWrapper.CannotLockException {
            this.threadID = threadID;
            this.communicator = communicator;
            this.keepaliveLock = communicator.maybeGetKeepaliveLock(
                    ShutdownStage.THREAD_PROJECTION_TRACKING,
                    "thread projection tracker for thread " + threadID);
        }

        /**
         * Tracks a thread projection for the thread whose projections are being
         * tracked by this tracker.
         * <p>
         * Note: this should only be called from the thread in question, and the
         * thread should be held alive in the meantime to avoid a concurrent
         * update to a field.
         *
         * @param where The remote system on which the thread projection
         * resides.
         * @throws ExpiredException If this state object has already expired
         */
        void addProjection(CommunicationAddress where)
                throws ExpiredException {
            if (expired.get()) {
                throw ExpiredException.SINGLETON;
            }
            remoteProjections.add(where);
        }

        /**
         * Expires the thread projection tracker, and informs any systems which
         * have residual thread projections for this thread that they're safe to
         * deallocate.
         * <p>
         * Unlike the rest of the thread projection tracker operations, this can
         * (and often will) be thrown from any thread.
         *
         * @throws ExpiredException If this state object has already expired
         */
        @Override
        public void expire() throws ExpiredException {
            if (expired.getAndSet(true)) {
                throw ExpiredException.SINGLETON;
            }
            remoteProjections.forEach((projectionLocation) -> {
                try {
                    communicator.sendMessageAsync(
                            new OperationCompleteMessage(
                                    new ThreadHasEnded(), false, false),
                            threadID, projectionLocation);
                } catch (AutocloseableLockWrapper.CannotLockException ex) {
                    /* This should be impossible, because the communicator is
                       supposed to expire its remaining thread projection
                       trackers /before/ shutting down. */
                    throw new DistributedError(
                            ex, "shut down thread projections");
                }
            });
            keepaliveLock.close();
        }

        /**
         * Expires this tracker, unless it already expired earlier.
         */
        @Override
        public void run() {
            try {
                expire();
            } catch (ExpiredException ex) {
                /* nothing to do */
            }
        }
    }

    /**
     * A marker object that is sent via an <code>OperationCompleteMessage</code>
     * to handle the "fake nested return" that marks a thread as entirely
     * finished.
     */
    @SuppressWarnings("ClassMayBeInterface")
    static class ThreadHasEnded implements Copiable {

        /**
         * Returns the string "thread has ended".
         *
         * @return <code>"thread has ended"</code>.
         */
        @Override
        public String toString() {
            return "thread has ended";
        }
    }
}
