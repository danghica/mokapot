package xyz.acygn.mokapot;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper;
import xyz.acygn.mokapot.util.BackgroundGarbageCollection;
import xyz.acygn.mokapot.util.Expirable;

/**
 * A stage in the shutdown of a distributed communicator. Everything that
 * requires the communicator to run is associated with a shutdown stage; while
 * shutting down, the shutdown stages are completed one at a time, and nothing
 * that requires further use of a particular shutdown stage can start once the
 * communicator has gone beyond that stage.
 * <p>
 * Ordering of enum constants is relevant; those with lower ordinals are dealt
 * with earlier in the shutdown process.
 *
 * @author Alex Smith
 */
enum ShutdownStage implements Comparable<ShutdownStage> {

    /**
     * The stage at which the communicator can be stopped. Once this stage is
     * complete (i.e. we've started the shutdown process), we can't call
     * <code>stopCommunication()</code> a second time concurrently. This has to
     * be the first stage, for obvious reasons.
     */
    STOP_COMMUNICATION((communicator) -> {
        /* Inability to get the "stop communicator" lock is always fatal to an
           attempt to stop communication. */
        if (communicator.isRunning()) {
            throw new IllegalStateException(
                    "Stopping a communicator that's already stopped");
        } else {
            throw new IllegalStateException(
                    "Stopping a communicator that's already being stopped by another thread");

        }
    }),
    /**
     * The stage at which secondary communicators belonging to this communicator
     * can exist. After this stage, all of those secondary communicators must
     * have shut down.
     */
    SECONDARY_COMMUNICATOR((communicator) -> {
        /* The secondary communicators are supposed to have been shut down
           first. Perhaps they're being shut down by another thread in parallel;
           in that case, our best course of action is to wait on the lock. */
    }),
    /**
     * The stage at which test hooks are still usable. This could be done later
     * in the shutdown process, because they typically perform only local
     * operations; however, test hooks are not required for any of the other
     * shutdown tasks, and it's plausible that a user might accidentally leave a
     * set active before the shutdown, so we lock them out before doing any real
     * shutdown work.
     * <p>
     * This must come before <code>LOCATION_MANAGER</code> as the test hooks
     * allow forcible creation of local-to-local long references, which requires
     * a location manager.
     */
    TEST_HOOKS((communicator) -> {
        BackgroundGarbageCollection.perform(
                BackgroundGarbageCollection.Operation.GC_THEN_FINALIZE);
    }),
    /**
     * The stage at which this communicator cares about thread-local state that
     * is being stored for it by other communicators. Once this stage is
     * complete, we don't ask other communicators to track it, and don't ourself
     * track the information about what time to discard it.
     */
    THREAD_PROJECTION_TRACKING((communicator) -> {
        /* If any threads that communicated with other systems in the past
           happen to be still alive, tell the remote systems in question that
           they don't have to store their thread-local states. */
        communicator.getAllProjectionTrackers().forEach((tpt) -> {
            try {
                tpt.expire();
            } catch (Expirable.ExpiredException ex) {
                /* do nothing; the thread must have exited concurrently with the
                   shutdown, which is not a problem */
            }
        });
    }),
    /**
     * The stage at which other communicators' thread-local state can still be
     * projected onto this communicator. Once this stage is complete, any
     * thread-local state will be discarded at the end of each message.
     */
    THREAD_PROJECTION(DistributedCommunicator::dropResidualThreadLocalState),
    /**
     * The stage at which location managers can still exist. Once this stage is
     * complete, no object can be exist on this communicator and be referenced
     * by another, or vice versa.
     */
    LOCATION_MANAGER((communicator) -> {
        /* Find the set of all remote communicators that are holding objects
           alive here. */
        Set<CommunicationAddress> s = new HashSet<>();
        communicator.getAllLifetimeManagers().forEachKey((ol)
                -> s.add(ol.getLocatedVia()));
        /* Ask them all to do GC. */
        s.forEach((address) -> {
            try {
                communicator.sendMessageAsync(
                        new GarbageCollectionMessage(
                                BackgroundGarbageCollection.Operation.GC_THEN_FINALIZE),
                        null, address);
            } catch (AutocloseableLockWrapper.CannotLockException ex) {
                // We should still be able to send messages at this point!
                throw new RuntimeException(
                        "Running shutdown stages out of order", ex);
            }
        });
        /* Also do GC ourselves. */
        BackgroundGarbageCollection.perform(
                BackgroundGarbageCollection.Operation.GC_THEN_FINALIZE);
    }),
    /**
     * The stage at which messages can still be sent to and received from other
     * communicators. Once this stage is complete, only control metadata can be
     * sent over the connections with other systems.
     */
    MESSAGE((communicator) -> {
        /* Nothing to do; the message should send itself eventually and no
           longer be blocking the communicator. */
    }),
    /**
     * The stage at which new inbound connections can still be formed. After
     * this stage, communication is impossible without an established
     * connection, and broken connections will stay broken permanently.
     */
    ACCEPTOR((communicator) -> communicator.getAcceptor().endListenLoop()),
    /**
     * The stage at which networking is still possible. After this stage, no
     * connections to other communicators exist, and no outbound connections can
     * be formed.
     */
    CONNECTION((communicator) -> communicator.getAcceptor().shutdownAllConnections());
    /**
     * Actions to perform to try to make this stage runnable, if it isn't
     * already.
     */
    private final Consumer<DistributedCommunicator> shutdownActions;

    /**
     * Constructor for enum constant objects.
     *
     * @param shutdownActions The actions that should be performed at this stage
     * of shutting down.
     */
    private ShutdownStage(Consumer<DistributedCommunicator> shutdownActions) {
        this.shutdownActions = shutdownActions;
    }

    /**
     * Attempts to make this stage of the shutdown possible. This might involve,
     * e.g., dropping residual thread state.
     * <p>
     * This will throw exceptions only in extreme circumstances that make it
     * impossible to proceed with the shutdown.
     *
     * @param communicator The communicator that's shutting down.
     */
    void tryToMakePossible(DistributedCommunicator communicator) {
        shutdownActions.accept(communicator);
    }
}
