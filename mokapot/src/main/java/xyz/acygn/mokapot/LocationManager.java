package xyz.acygn.mokapot;

import static java.lang.System.arraycopy;
import java.lang.ref.WeakReference;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import static xyz.acygn.mokapot.Authorisations.UNRESTRICTED;
import static xyz.acygn.mokapot.ClassKnowledge.knowledgeForClass;
import static xyz.acygn.mokapot.DistributedCommunicator.LIFETIME_TIMEOUT;
import static xyz.acygn.mokapot.LengthIndependent.getActualClassInternal;
import static xyz.acygn.mokapot.NonCopiableKnowledge.StandinFactoryPurpose.LONG_REFERENCE;
import xyz.acygn.mokapot.markers.DistributedError;
import xyz.acygn.mokapot.skeletons.ForwardingStandinStorage;
import xyz.acygn.mokapot.skeletons.InvokeByCode;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper;
import static xyz.acygn.mokapot.util.BackgroundGarbageCollection.addFinaliser;
import xyz.acygn.mokapot.util.DeterministicAutocloseable;
import xyz.acygn.mokapot.util.DummyScheduledFuture;
import xyz.acygn.mokapot.util.Pair;
import static xyz.acygn.mokapot.util.ThreadUtils.delayInterruptions;
import xyz.acygn.mokapot.util.WeakValuedConcurrentMap;
import xyz.acygn.mokapot.wireformat.MethodCodes;
import xyz.acygn.mokapot.wireformat.ObjectDescription;

/**
 * A location manager records the current location of an object. This could be
 * as a local object, on a remote machine, or as a description (i.e. in the
 * progress of migrating).
 * <p>
 * A long reference is a reference that goes via a location manager. These are
 * implemented as standins using <code>ForwardingStandinStorage</code>; the
 * location manager is what the method calls get forwarded to.
 * <p>
 * For each object, there's at most one location manager per VM that refers to
 * that object. The location manager exists only if at least one of the
 * following holds: there's a long reference to that object on the same system
 * as the location manager; there's a location manager on a remote system that
 * last observed the object on this VM (whether or not the object is actually on
 * this VM at the time); or there's a marshalled reference (i.e. a
 * <code>ReferenceValue</code>) to the object that has not yet been
 * unmarshalled, and which specifies this VM as the expected location for
 * locating the object.
 * <p>
 * In the case that the object is on another VM, this basically just delegates
 * all method calls to the location manager for the object on the last VM on
 * which that object was observed.
 * <p>
 * Each location manager that last observed its object on another VM has a
 * corresponding "lifetime manager" on that VM. Its purpose is to keep the
 * object alive (by keeping the location manager on the remote VM alive) for as
 * long as the location manager is referencing it. When the location manager is
 * finalised, the corresponding lifetime manager deallocates itself (lifetime
 * managers manage their own lifetimes via placing and removing references to
 * them on/from a global chain).
 * <p>
 * In the case that the object is on the local VM, the location manager doesn't
 * try anything special to keep itself alive; it will be kept alive by long
 * references to the object on the local system because it's referenced by their
 * standin storage, and it will be kept alive by location managers on other VMs
 * via the lifetime managers corresponding to those location managers. Standins
 * only keep a location manager alive if they're currently acting as long
 * references; <code>ManagedLocalStandinStorage</code> has only a weak reference
 * to the location manager (because it acts as a short reference), allowing the
 * location manager to be deallocated if no longer needed.
 * <p>
 * If the object is referenced from a serialisation in transit, a counter will
 * be increased on the lifetime manager, and decreased only when the recipient
 * system reports that the serialisation was received. (In order to save on
 * network traffic, the remote location manager won't report this immediately if
 * it knows that this location manager needs to be kept alive anyway; the
 * "serialisation received" reports are batched and sent only when the object
 * becomes dead on the remote system.)
 *
 * @param <T> A class which the referenced object is known to belong to.
 * @see ReferenceValue
 * @see Standin
 *
 * @author Alex Smith
 */
class LocationManager<T> extends LooseTightStandinReference<T>
        implements InvokeByCode<T> {

    /* Invariants: The referenced standin always exists, and has a dropped
       referent if and only if the object this location manager describes is
       remote; the standin's storage is ForwardingStandinStorage if either the
       object is remote or it's being migrated, and LooseStorage/TightStorage if
       it's local and not being migrated; "migrate-anywhere" status is
       represented via this reference being tight in a situation where being
       loose would not violate any other invariants. */
    /**
     * An index of which global IDs correspond to which location managers. The
     * main purpose of this index is to send the same object to a remote JVM
     * twice and have it use the same location manager each time.
     * <p>
     * We also need to include the communicator as part of the key of this map.
     * That's because we can have two location managers managing the object with
     * the same global ID, if those managers are running on different
     * communicators.
     */
    private static final WeakValuedConcurrentMap<
            Pair<DistributedCommunicator, GlobalID>, LocationManager<?>> ID_INDEX
            = new WeakValuedConcurrentMap<>();

    /**
     * State required by this location manager which will still need to persist
     * after its deallocation. This is a separate object so that the location
     * manager itself can be freed without freeing its state.
     */
    private final LocationManagerState<T> state;

    /**
     * The total amount of GC weight that has been generated from scratch by
     * this location manager's lifetime managers. That is, the amount of total
     * negative weight stored in the lifetime managers. This is used during
     * migration to ensure that the GC weight invariants are upheld.
     * <p>
     * Note that this can be updated even while both migration locks are held,
     * but only to reduce it (LocationManagerStatusMessage alters it without
     * locking but gives it a net smaller value, ReferenceValue takes the
     * migration lock when altering it).
     */
    private final AtomicLong generatedWeight = new AtomicLong(0);

    /**
     * Lock used to guard any operations that require the object to stay in the
     * same place. This includes invoking methods on it, migrating it, changing
     * its storage, etc..
     */
    private final ReadWriteLock migrationLock = new ReentrantReadWriteLock();

    /**
     * Whether direct references to the standin's referent are permitted. If
     * this is <code>true</code>, such references might exist already, and new
     * ones may be created. If this is <code>false</code>, no such references
     * exist, and new ones must not be created.
     * <p>
     * If this is <code>true</code>, the location manager must not be placed
     * into loose mode (because the deallocation of the standin would not
     * necessarily imply the deallocation of the referent).
     * <p>
     * Access to this field is guarded by the migration lock. (Normally it won't
     * change; however, it changes from <code>true</code> to <code>false</code>
     * when a local object is force-migrated away.)
     */
    private boolean directReferencesToStandinReferent;

    /**
     * The class of the referenced object. This is basically just a local cache,
     * used to avoid network traffic when requesting an object's class. (Object
     * classes are immutable, so caching it is safe.)
     * <p>
     * If the referenced object is actually a standin, this contains the class
     * that the standin is standing in for, not that of the object itself.
     */
    private final Class<T> objectClass;

    /**
     * Returns the global ID of the object that this location manager is
     * managing.
     *
     * @return The object's global ID.
     */
    public GlobalID getObjectID() {
        return state.objectID;
    }

    /**
     * Returns the class of the object that this location manager is managing.
     * This information is cached locally, and thus does not need a trip over
     * the network to determine, even if the object is on a remote system.
     *
     * @return The object's class.
     */
    @Override
    public Class<T> getObjectClass() {
        return objectClass;
    }

    /**
     * Returns the distributed communicator with which this location manager is
     * associated.
     *
     * @return The distributed communicator.
     */
    public DistributedCommunicator getCommunicator() {
        return state.communicator;
    }

    /**
     * Returns the standin that this location manager is using to track the
     * object. This might be the object itself (or a standin wrapper for it), or
     * it might be a long reference to the object. (In the latter case, the
     * reference will be back via this location manager).
     * <p>
     * This is a fairly low-level operation and not intended for general use.
     * Its main purpose is to allow migration code to manipulate the standin's
     * migration-related state (such as its storage) directly.
     * <p>
     * This operation must only be called while the migration lock on this
     * location manager is at least read-locked. The method does not check to
     * ensure that this is the case.
     *
     * @return The local standin, or <code>null</code>.
     */
    Standin<T> getLocalStandin() {
        return get();
    }

    /**
     * Performs an operation with the migration lock read-locked. This means
     * that these operations can coexist with each other, but not with an active
     * migration attempt.
     *
     * @param <T> The return value of the operation.
     * @param operation The operation itself.
     * @param reason The reason for which the lock is being locked.
     * @return The operation's return value.
     */
    private <T> T readLocked(Supplier<T> operation, String reason) {
        try (AutocloseableLockWrapper w
                = new AutocloseableLockWrapper(migrationLock.readLock(), reason)) {
            return operation.get();
        }
    }

    /**
     * Returns half of the migration lock, without locking it. This is mostly
     * intended to allow <code>MigrationMonitor</code> to lock and unlock the
     * migration lock at appropriate times. Note that this is not an RAII method
     * and thus must not be used without care to ensure that locks and unlocks
     * match exactly.
     *
     * @param readOnly <code>true</code> to return the read half of the lock;
     * <code>false</code> to return the write half of the lock.
     * @return The read or write half of the migration lock, in its current
     * state.
     */
    Lock getMigrationLock(boolean readOnly) {
        return readOnly ? migrationLock.readLock() : migrationLock.writeLock();
    }

    /**
     * Performs an operation with the migration lock write-locked. The operation
     * will not run until all other operations have stopped, and will block
     * other operations until it completes. Used for things like migrating the
     * object to a different system.
     *
     * @param <T> The return value of the operation.
     * @param operation The operation itself.
     * @param reason The reason for which the lock is being locked.
     * @return The operation's return value.
     */
    private <T> T writeLocked(Supplier<T> operation, String reason) {
        try (AutocloseableLockWrapper w
                = new AutocloseableLockWrapper(
                        migrationLock.writeLock(), reason)) {
            return operation.get();
        }
    }

    /**
     * Returns the best-quality reference available for the object this location
     * manager is managing. This could be:
     * <ul>
     * <li>If the managed object is not stored locally, a long reference, or a
     * variable-length reference that's currently long (because no short or
     * medium reference types are available for non-local objects);
     * <li>If the managed object is intended to be accessed only via a standin,
     * a standin for the object (typically variable-length, but likely set to
     * managed-local mode unless there's a suspicion that the object may be
     * migrated soon);
     * <li>If the managed object can be accessed directly, a short reference to
     * the object.
     * </ul>
     * <p>
     * A returned reference to a local object is guaranteed to have a viable
     * referent (i.e. either it <i>is</i> the local object in question, or else
     * it's a standin with a valid referent that is the local object in
     * question). This may bring the object online if necessary to produce the
     * referent.
     *
     * @return An object, or standin for the object. (The return type may be a
     * minor lie in cases where the object's type cannot be matched exactly.)
     */
    @SuppressWarnings("unchecked")
    T getBestReference() {
        try (AutocloseableLockWrapper w
                = new AutocloseableLockWrapper(migrationLock.readLock(),
                        "getBestReference")) {
            if (directReferencesToStandinReferent) {
                return getLocalStandin().getReferent(UNRESTRICTED);
            } else {
                Standin<T> standin = getLocalStandin();
                return (T) standin;
            }
        }
    }

    /**
     * Runs <code>getRemoteLocation()</code> on a given timestamped location.
     * This makes use of the fact that we know which distributed communicator
     * we're associated with to determine the appropriate local address.
     *
     * @param loc The timestamped location to extract an address from.
     * @return <code>null</code> if the timestamped location refers to the same
     * communicator that's managing this location manager; otherwise, the
     * communication address within the timestamped location.
     */
    private CommunicationAddress getRemoteLocation(TimestampedLocation loc) {
        return loc.getRemoteLocation(getCommunicator().getMyAddress());
    }

    /**
     * Returns the timestamp with which the object became locally stored on this
     * system. This method will only return stable values while holding the
     * migration read lock.
     *
     * @return <code>null</code> if the object is currently remote; otherwise, a
     * timestamped location whose location is the local communication address
     * and with an appropriate timestamp.
     */
    TimestampedLocation getLocalObjectTimestamp() {
        TimestampedLocation rv = state.locationAndWeight.get().location;
        if (getRemoteLocation(rv) == null) {
            return rv;
        } else {
            return null;
        }
    }

    /**
     * Cause this location manager to record the object as being locally stored
     * here, with the given timestamp. This should only be called with the
     * migration write lock held, and only during migration when the object
     * migrates here (thus, <code>MigrationMessage</code> is probably the only
     * viable caller of this method). This also sets the standin to have an
     * undropped referent, but does not undescribe into it.
     *
     * @param timestamp The timestamp to use.
     */
    void setLocalObjectTimestamp(int timestamp) {
        Standin<T> standin = getLocalStandin();
        if (!(standin.getStorage(null) instanceof ForwardingStandinStorage)) {
            throw new IllegalStateException(
                    "remote objects should be using ForwardingStandinStorage");
        }
        standin.undropResources(UNRESTRICTED);

        BorrowedWeight newLaW
                = new BorrowedWeight(new TimestampedLocation(
                        getCommunicator().getMyAddress(), timestamp), 0L);

        BorrowedWeight oldLaW
                = state.locationAndWeight.getAndSet(newLaW);
        returnMyGCWeight(oldLaW);

        if (oldLaW.weight != 0) {
            /* Stop the old keepalive thread. Put a placeholder in it to stop
               a new keepalive thread being started there in case of race
               condition. */
            ScheduledFuture<?> oldKeepalive
                    = oldLaW.keepAlive.getAndSet(new DummyScheduledFuture());
            oldKeepalive.cancel(false);
        }
    }

    /**
     * Returns the address of a system that should have a better idea of where
     * the object is stored than this system does. Repeatedly calling
     * <code>followLocationChain</code> to locate a system, then remotely
     * calling <code>followLocationChain</code> on that system, and so on, will
     * eventually end up reaching the system on which the object is actually
     * stored (at which point it will return <code>null</code>).
     * <p>
     * Note that the object may move, thus changing the location chain. This
     * method is guaranteed to eventually end up in the right place regardless,
     * but a <code>null</code> return value may immediately become outdated
     * unless the migration read lock is held.
     *
     * @return <code>null</code> if the object is currently stored locally;
     * otherwise, a communication address whose location manager has more
     * information about the object than this system's.
     */
    CommunicationAddress followLocationChain() {
        return getRemoteLocation(state.locationAndWeight.get().location);
    }

    /**
     * Returns whether all references to the standin's referent go via the
     * standin. For location managers created from standins that encapsulate a
     * reference, this is <code>true</code>; this is also true for location
     * managers which created their referents themselves, and for objects that
     * were created migratably. However, for a location manager created from an
     * existing object, this will be <code>false</code>, as it's possible that
     * other, external references to the object exist.
     * <p>
     * This method can be used to determine whether it's safe to prepare the
     * migration of an object. (Preparing a migration forces all method calls to
     * go via the location manager, thus allowing it to divert them elsewhere,
     * but if direct references exist, those won't be able to be intercepted.)
     *
     * @return Whether the location manager is capable of intercepting all
     * method calls to the object being managed from this virtual machine.
     */
    public boolean standinOwnsReferent() {
        return readLocked(() -> !directReferencesToStandinReferent,
                "standinOwnsReferent");
    }

    /**
     * Supplies the given amount of weight to this location manager. GC weight
     * is a mechanism used to ensure that the object is not collected while
     * still in use; an object can have GC weight with respect to JVMs and with
     * respect to objects in transit, must have positive GC weight everywhere
     * it's referenced other than its actual location, and the weights must sum
     * to zero. As such, giving weight to this location manager allows it to
     * hold the object it's managing alive, even if the object is hosted
     * elsewhere.
     *
     * @param suppliedWeight The amount of weight to supply. Must be positive.
     * @param expectedLKL The timestamped location which the supplier of the
     * weight believes the recipient (i.e. this system) believes the object is
     * stored at. If this is incorrect, the older weight will need to be
     * returned to the system it came from.
     * @throws IllegalArgumentException If <code>suppliedWeight</code> is not
     * positive
     */
    void supplyGCWeight(long suppliedWeight, TimestampedLocation expectedLKL)
            throws IllegalArgumentException {
        if (suppliedWeight <= 0) {
            throw new IllegalArgumentException(
                    "attempt to supply negative or zero GC weight");
        }

        /* First, work out what we /would/ do, if no concurrent updates
           intervened. */
        BorrowedWeight oldLaW = state.locationAndWeight.get();

        BorrowedWeight newLaW;
        boolean returnOldWeight;
        if (oldLaW.location.equals(expectedLKL)) {
            /* The simple case: the message came from the expected location. */
            newLaW = new BorrowedWeight(oldLaW, suppliedWeight);
            returnOldWeight = false;
        } else if (oldLaW.location.compareTo(expectedLKL) > 0) {
            /* The location we already have is newer. Return the supplied weight
               (from an older location) to the sender. Note that this means that
               we have no need to update anything /here/, and can just return
               once we've sent the message. */
            LocationManagerStatusMessage lmsm
                    = new LocationManagerStatusMessage(this,
                            -suppliedWeight);
            try {
                getCommunicator().sendMessageAsync(
                        lmsm, null, expectedLKL.getLocation());
            } catch (AutocloseableLockWrapper.CannotLockException ex) {
                /* This should be impossible because we're holding the
                   communicator's keepalive lock, thus this is effectively a
                   recursive lock (which always succeeds). */
                throw new RuntimeException(ex);
            }
            return;
        } else {
            /* The location we already have is older. We'll use the new supply
               of weight as the entire weight for the location manager, and
               return the old weight. However, we can't return it yet; we first
               have to do succeed at the atomic update of the LaW field (to
               avoid two racing threads both trying to return it at once).
               Exception: if the old weight was 0, there's nothing to return. */
            returnOldWeight = oldLaW.weight != 0;
            newLaW = new BorrowedWeight(oldLaW, expectedLKL, suppliedWeight);
        }

        if (!state.locationAndWeight.compareAndSet(oldLaW, newLaW)) {
            /* Someone made a concurrent change. We haven't actually done
               anything with side effects yet, so ignore all our calculations
               so far and try again with the newly changed data. */
            supplyGCWeight(suppliedWeight, expectedLKL);
            return;
        }

        if (returnOldWeight) {
            returnMyGCWeight(oldLaW);
        }

        if (oldLaW.weight == 0) {
            /* Schedule keepalives so that the remote system knows that the GC
               weight we're holding hasn't just disappeared. */
            DistributedCommunicator communicator = getCommunicator();
            /* Note: it's important that this lambda doesn't hold the location
               manager alive, thus the use of a local variable for the state. */
            final LocationManagerState<T> finalState = state;
            ScheduledFuture<?> future = communicator.getExecutorService()
                    .scheduleWithFixedDelay(() -> {
                        try {
                            /* Note: remoteLocation can't be null when we set
                               the timer up, but could later become null as a
                               result of migrations; we want to avoid problems
                               if the keepalive timer races against a migration
                               process. It's also an unstable value, thus we
                               need to cache it in a variable.

                               Bear in mind that we can't use instance methods
                               here, as doing so would capture <code>this</code>,
                               so we call getRemoteLocation the convoluted way. */
                            CommunicationAddress myAddress
                                    = finalState.communicator.getMyAddress();
                            CommunicationAddress remoteLocation
                                    = finalState.locationAndWeight.get().location
                                            .getRemoteLocation(myAddress);

                            if (remoteLocation != null) {
                                communicator.sendMessageAsync(
                                        new LocationManagerStatusMessage(
                                                finalState.objectID,
                                                myAddress, 0),
                                        new GlobalID(myAddress), remoteLocation);
                            }
                        } catch (DistributedError | IllegalArgumentException
                                | AutocloseableLockWrapper.CannotLockException ex) {
                            communicator.asyncExceptionHandler(ex);
                        }
                    }, LIFETIME_TIMEOUT / 2,
                            LIFETIME_TIMEOUT / 2, SECONDS);
            if (!newLaW.keepAlive.compareAndSet(null, future)) {
                /* This happens if the object migrated here after we updated
                   the weight but before we could start the timer. We have no
                   need for the timer in this case, so get rid of it. */
                future.cancel(false);
            }
        }
    }

    /**
     * Returns the entire supply of GC weight that this location manager had
     * previously borrowed from a given location. This is used when switching
     * over to a new source of GC weight, which might be done for more than one
     * reason.
     *
     * @param oldLaW The previous place from which the GC weight was borrowed,
     * and the amount of GC weight borrowed.
     */
    private void returnMyGCWeight(BorrowedWeight oldLaW) {
        /* Use a location manager status message to return the weight we
           were previously holding. */
        LocationManagerStatusMessage lmsm
                = new LocationManagerStatusMessage(
                        this, -oldLaW.weight);
        try {
            getCommunicator().sendMessageAsync(lmsm, null,
                    oldLaW.location.getLocation());
        } catch (AutocloseableLockWrapper.CannotLockException ex) {
            /* This should be impossible because we're holding the
               communicator's keepalive lock, thus this is effectively a
               recursive lock (which always succeeds). */
            throw new RuntimeException(ex);
        }
    }

    /**
     * Adjusts the record of the total amount of negative weight generated by
     * this location manager's lifetime managers. Every time a lifetime
     * manager's negative weight increases, it calls this method with the amount
     * it increased by. Every time it decreases, it calls this method with the
     * amount it decreased by.
     * <p>
     * This method is only intended to be called from lifetime managers.
     *
     * @param negativeWeight The amount of negative weight to add to or subtract
     * from the total amount of generated weight.
     */
    void adjustTotalGeneratedWeight(long negativeWeight) {
        generatedWeight.addAndGet(negativeWeight);
    }

    /**
     * Requests the given amount of weight from this location manager.
     * <p>
     * Small amounts of weight may be requested from the location manager (by
     * using a negative argument to this method) in order to be able to
     * construct a message referencing the managed object (the message needs
     * some weight of its own to prevent the object being deallocated while the
     * message is in transit). Although the supply is finite, the original host
     * of the object should have provided this location manager with a
     * sufficient supply that all such requests can be honoured.
     * <p>
     * However, GC weight must always be returned to its original source; as
     * such, this method may fail because the GC weight that this system
     * actually has came from somewhere else. In such a case, the GC weight will
     * have to be obtained over the network. (This method does no network
     * connection itself, and thus the caller will need to obtain it. It's an
     * API guarantee, relied on by <code>invoke</code>, that this method cannot
     * do IO or block in other ways.)
     *
     * @param requestedWeight The amount of GC weight to request.
     * @param forSystem The system to which the requested GC weight will be
     * returned.
     * @return An appropriately timestamped version of <code>forSystem</code>,
     * if the weight was successfully obtained; or <code>null</code> if this
     * location manager's GC weight comes from a different system, and thus no
     * weight could be returned to the system in question
     */
    TimestampedLocation requestGCWeight(
            long requestedWeight, CommunicationAddress forSystem) {
        BorrowedWeight oldLaW = state.locationAndWeight.get();
        if (!oldLaW.location.getLocation().equals(forSystem)) {
            return null;
        }

        if (oldLaW.weight < requestedWeight) {
            throw new DistributedError(new OutOfMemoryError(),
                    this + ": available GC weight exhausted, "
                    + oldLaW.weight + " available, "
                    + requestedWeight + " requested");
        }

        BorrowedWeight newLaW
                = new BorrowedWeight(oldLaW, -requestedWeight);

        if (state.locationAndWeight.compareAndSet(oldLaW, newLaW)) {
            return newLaW.location;
        }

        /* Looks like there was a conflicting intermediate change. Try again. */
        return requestGCWeight(requestedWeight, forSystem);
    }

    /**
     * Ensures that the various indexes of location manager state are kept up to
     * date. This adds index entries immediately, and also creates finalisers so
     * that the index entries will be removed again when the location manager is
     * deallocated.
     */
    private void initIndexesAndFinalisers() {
        final LocationManagerState<T> thisState = this.state;
        addFinaliser(this, () -> thisState.start(thisState.communicator));
        ID_INDEX.putIfAbsent(new Pair<>(
                thisState.communicator, thisState.objectID), this);
    }

    /**
     * Creates a location manager for an object on the local system.
     * <p>
     * This constructor is intended to be called only from
     * <code>DistributedCommunicator#findLocationManagerForObject</code>. As
     * such, it makes a few assumptions: that the "local object" is already a
     * standin (possibly a wrapper standin that was just created around the
     * object), and that <b>the caller holds the standin's storage's monitor</b>
     * (this requirement is bolded as it cannot be checked automatically, and
     * avoids a race condition between multiple attempts to create the same
     * location manager at once).
     * <p>
     * This assumes that the object doesn't already have a location manager (and
     * thus doesn't have a global ID assigned yet; the object's ID exists only
     * if its location manager does).
     * <p>
     * This constructor does not enforce the invariant that only one location
     * manager to a given object exists on any given system; that's the caller's
     * responsibility.
     * <p>
     * Can only be called while there's an active distributed communicator. The
     * caller is responsible for setting the referent's standin storage
     * appropriately, and must loosen the location manager when it's done.
     *
     * @param localStandin The local object for which to create a location
     * manager. This must be given via standin (the location manager will be
     * created for the standin's referent). The referents's actual class must be
     * <code>T</code> (not a subclass of it); currently this is assumed without
     * verification. Note that this also implies that the standin must have a
     * live referent.
     * @param standinDoesntOwnReferent Whether the standin doesn't owns its
     * referent, i.e. some references to the standin's referent don't go via the
     * standin itself. (This is trivially false in cases where the standin is
     * its own referent). If this is <code>true</code>, various
     * garbage-collection operations must be disabled, and in particular
     * automatic migration will not be supported.
     * @param communicator The distributed communicator with which the location
     * manager is associated.
     * DistributedCommunicator#findLocationManagerForObject(java.lang.Object)
     * @throws AutocloseableLockWrapper.CannotLockException If the
     * communicator's location tracking has shut down
     */
    @SuppressWarnings("unchecked")
    LocationManager(Standin<T> localStandin, boolean standinDoesntOwnReferent,
            DistributedCommunicator communicator)
            throws IllegalArgumentException,
            AutocloseableLockWrapper.CannotLockException {
        super(localStandin);
        directReferencesToStandinReferent = standinDoesntOwnReferent;

        WeakReference<Standin<T>> standinRef
                = new WeakReference<>(localStandin);
        GlobalID newID = new GlobalID(communicator.getMyAddress());
        this.state = new LocationManagerState<>(
                new TimestampedLocation(communicator.getMyAddress(), 0),
                newID, communicator);
        this.objectClass = (Class) getActualClassInternal(
                localStandin.getReferent(UNRESTRICTED));

        initIndexesAndFinalisers();
    }

    /**
     * Creates a location manager for an object on a remote system.
     * <p>
     * This constructor does not itself update location manager metadata on the
     * distributed communicator, and does not update lifetime manager metadata
     * on the remote system. Additionally, it does not enforce the invariant
     * that only one location manager to a given object exists on any given
     * system. As such, it should probably only be called from
     * <code>locationManagerForID</code>, which handles these cases. The initial
     * weight of the location manager will be 0; the correct value should be set
     * by the caller of <code>locationManagerForID</code>.
     * <p>
     * Note that the object in question won't be prevented from being garbage
     * collected until a lifetime manager has been set up on the remote system;
     * this is something that can only be done <i>after</i> the location manager
     * is created (to avoid a potential race condition). As such, you'll need to
     * keep the object alive some other way in the meantime.
     * <p>
     * Should only be called while there's an active distributed communicator.
     *
     * @param lastKnownLocation The virtual machine on which the object was last
     * observed.
     * @param objectID The object's global identifier.
     * @param objectClass The object's class.
     * @param communicator The distributed communicator with which the location
     * manager is associated.
     * @throws AutocloseableLockWrapper.CannotLockException If the
     * communicator's location tracking has shut down
     *
     * @see #locationManagerForID
     */
    private LocationManager(TimestampedLocation lastKnownLocation,
            GlobalID objectID, Class<T> objectClass,
            DistributedCommunicator communicator)
            throws AutocloseableLockWrapper.CannotLockException {
        super();

        this.state = new LocationManagerState<>(
                lastKnownLocation, objectID, communicator);
        this.objectClass = objectClass;
        this.directReferencesToStandinReferent = false;

        finishInitialization(newLongReference());
        initIndexesAndFinalisers();
    }

    /**
     * Returns the location manager for the object with the given ID. This
     * requires knowledge of a machine where such a location manager already
     * exists.
     * <p>
     * Calling this method will not adjust the location manager's weight; most
     * operations that would require looking up a location manager by ID would
     * also require adjusting its weight, but that is the caller's
     * responsibility. In particular, if this method creates a new location
     * manager, its initial weight of 0 is unlikely to be correct.
     *
     * @param <T> The declared class of the location manager.
     * @param id The ID of the object in question.
     * @param expectedLocation The communication address of a virtual machine
     * which already has a location manager for that object. In the case that an
     * appropriate location manager already exists on this system, this is
     * ignored. This can be <code>null</code> to assert that the location
     * manager should already exist.
     * @param actualClass The actual class of the object.
     * @param communicator The communicator whose location managers should be
     * looked up. (For testing/benchmarking purposes, it's possible to run two
     * communicators on the same JVM at the same time, in which case each will
     * have its own set of location managers.)
     * @return The location manager for that object.
     * @throws NoSuchElementException If the location manager does not already
     * exist in the expected location
     */
    static <T> LocationManager<T> locationManagerForID(GlobalID id,
            TimestampedLocation expectedLocation, Class<T> actualClass,
            DistributedCommunicator communicator)
            throws NoSuchElementException {
        LocationManager<T> manager;
        synchronized (LocationManager.class) {
            LocationManager<?> foundManager = ID_INDEX.get(
                    new Pair<>(communicator, id));
            if (foundManager != null) {
                return foundManager.assertClass(actualClass);
            }

            if (expectedLocation == null
                    || expectedLocation.getRemoteLocation(
                            communicator.getMyAddress()) == null) {
                throw new NoSuchElementException(
                        "cannot find a location manager for ID " + id
                        + " on machine " + communicator.getMyAddress());
            }
            try {
                manager = new LocationManager<>(
                        expectedLocation, id, actualClass, communicator);
            } catch (AutocloseableLockWrapper.CannotLockException ex) {
                throw new NoSuchElementException(
                        "location tracking has shut down, no location manager "
                        + "for ID " + id + " on machine "
                        + communicator.getMyAddress() + " can exist");
            }
        }
        /* end of synchronized(LocationManager.class) {} */

        return manager;
    }

    /**
     * Runs the given method on the object managed by this location manager.
     * With this call pattern, the method is specified via a method handle
     * relative to the object's actual class (which can be obtained using the
     * <code>getObjectClass</code> on this location manager).
     * <p>
     * If the object exists on the local VM, the method will be called on the
     * object with no further indirection. Otherwise, the location manager will
     * attempt to discover the VM on which the object is currently hosted, and
     * forward the request to run the method to that VM.
     * <p>
     * <b>TODO</b>: Make sure that the caller is actually allowed to call this
     * method! This is an interface method (thus effectively public via the
     * <code>InvokeByCode</code> method), and could be called by anyone in any
     * context, whereas we only actually want to invoke the method if it's being
     * called as a consequence of a "valid" attempt to call the method on this
     * caller.
     *
     * @param methodCode The method code of the method to run.
     * @param methodParams The parameters of the method to run.
     * @return The return value of the invoked method.
     * @throws Throwable If invoking the method on the object throws an
     * exception, this method throws the same exception.
     */
    @Override
    public Object invoke(long methodCode, Object[] methodParams)
            throws Throwable {
        /* A special case: never forward finalize(), because that gets called
           automatically, and the long reference mechanism makes it happen too
           early, then again (directly on the object) at the right time. A
           no-argument finalize is always the special-cased finalizer. A
           "finalize" with more arguments is just a regular method with a
           misleading name, so we can forward that safely.

           TODO: This technically changes semantics if someone decides to call
           finalize() manually (which is in general a terrible idea, so it's
           unlikely to come up). We could possibly fix this using
           implementation-specific knowledge of where finalizers are supposed to
           be called from. */
        if (methodCode == MethodCodes.FINALIZE) {
            return null;
        }

        /* We're going to need to take the migration lock to do this safely.
           However, it's important that we don't block here if the object isn't
           local; migration has a lock order (referent system migration lock,
           referencing system migration lock), whereas this is potentially
           taking a migration lock for a referencing system, and any contact we
           make with the referenced system is likely to lock the lock there.
           Conclusion: if we don't unlock the lock before doing anything that
           might block on an action of another system, we're facing potential
           deadlocks. The lock is thus unlocked early (inside the try{} block
           because the control flow would be mind-bending otherwise) in order to
           prevent the issue from occuring. */
        try (AutocloseableLockWrapper locked = new AutocloseableLockWrapper(
                migrationLock.readLock(), "invoke")) {
            if (!state.isObjectLocal()) {
                /* We'll send a MethodMessage to the system where we think the
                   object is stored, asking it to call the method for us. The
                   object itself will be referred to by a RemoteOnlyStandin
                   (which should end up being unmarshalled, at the other end,
                   into the object itself, assuming it's stored there).

                   We have a potential problem, though: what if the object gets
                   moved from the system where we think it is between when we
                   create the method message, and when the method message gets
                   marshalled? Although the object itself must remain allocated,
                   it's possible that the system in question will end up not
                   knowing where it is, due to having no inbound or outbound
                   references (the outbound reference from this location manager
                   might be updated to an outbound reference to a different
                   system).

                   The solution is to grab enough GC weight to cover the message
                   first, and then return it again when we're done. The GC
                   weight being held here is sufficient to stop the reference on
                   the system in question from being entirely removed. */
                DistributedCommunicator communicator = getCommunicator();
                while (true) {
                    TimestampedLocation believedLocation
                            = state.locationAndWeight.get().location;
                    if (getRemoteLocation(believedLocation) == null) {
                        /* Something's gone badly wrong; the migration lock is
                           intended to stop the object migrating /here/! */
                        throw new RuntimeException("object migrated here"
                                + " while we were holding the migration lock");
                    }
                    final long storedWeight = 1000000L;
                    /* This is only safe because requestGCWeight never does I/O
                       on its own. */
                    if (requestGCWeight(storedWeight,
                            believedLocation.getLocation()) == null) {
                        continue;
                    }

                    try {
                        /* At this point, nothing more we do requires holding
                           the migration lock (e.g. MethodMessage works fine
                           even if the object migrates here). */
                        locked.unlockEarly();

                        Object[] newParams = new Object[methodParams.length + 1];
                        arraycopy(methodParams, 0, newParams, 1,
                                methodParams.length);
                        newParams[0] = new RemoteOnlyStandin<>(this);
                        MethodMessage methodMessage
                                = new MethodMessage(methodCode, newParams);
                        return communicator.sendMessageSync(
                                methodMessage, believedLocation.getLocation());
                    } finally {
                        supplyGCWeight(storedWeight, believedLocation);
                    }
                }
            }

            /* If this reference was tight, it shouldn't be any more; we just
               used the object locally, so we don't want to migrate it away. */
            maybeLoosen();
            return getLocalStandin().invoke(
                    methodCode, methodParams, UNRESTRICTED);
        }
    }

    /**
     * Produces a string representation of this location manager. It includes a
     * summary of the object's location, and the object's class. There is no
     * attempt to stringify the object itself, even if it happens to be stored
     * locally.
     *
     * @return A string describing the location manager.
     */
    @Override
    public String toString() {
        return "location manager, " + objectClass
                + ", location = " + state.locationAndWeight
                        .get().location.getLocation().toString()
                + ", ID = " + state.objectID;
    }

    /**
     * Verifies that this location manager is managing an object that can be
     * stored in the given class. Used to ensure that accessing an object
     * indirectly via a location manager will not break type safety.
     *
     * @param <U> The class that we're asserting the managed object belongs to.
     * @param assertOfClass <code>U.class</code>, given explicitly due to Java's
     * type erasure rules.
     * @return <code>this</code>, in a type-safe way.
     * @throws ClassCastException If the object being managed cannot be stored
     * in a variable of type U.
     */
    @SuppressWarnings("unchecked")
    public <U> LocationManager<U> assertClass(Class<U> assertOfClass)
            throws ClassCastException {
        if (assertOfClass.isAssignableFrom(objectClass)) {
            return (LocationManager<U>) this;
        } else {
            throw new ClassCastException(
                    "Location manager is managing an object of type "
                    + objectClass + ", expected " + assertOfClass);
        }
    }

    /**
     * Returns a guess at whether the object is locally unreferenced. For local
     * objects for which the standin owns its referent, this class aims to
     * reference them loosely; so if the reference has become tight, that's a
     * clue that it became unreferenced at some point. However, there are both
     * false negatives (objects where the standin doesn't own its referent but
     * happens to be the only reference anyway) and false positives (reference
     * cycles that became locally live due to a remote system giving us a
     * reference to a different object in the cycle as a method parameter), so
     * the return value of this method should be treated mostly as an
     * optimisation hint. (That said, it is likely to be accurate most of the
     * time.)
     * <p>
     * This method should only be called with the migration read lock held, and
     * the return value is only meaningful while the object is local. You should
     * probably hold the local standin alive before calling this method, as
     * otherwise the object may spontaneously become offline due to the last
     * reference to the standin dying, making a <code>false</code> return value
     * out-of-date.
     *
     * @return <code>true</code> if the object is probably locally unreferenced.
     */
    boolean isMaybeLocallyUnreferenced() {
        return !directReferencesToStandinReferent && isTight();
    }

    /**
     * Loosens the location manager's reference if possible. The reference will
     * be loosened only if it's currently managing a local object, messages
     * aren't being directed via the location manager (i.e. we don't have
     * migration prepared), and it isn't loose already.
     * <p>
     * Local objects are loosened whenever they can be, in order to determine
     * when an object should be automatically migrated.
     *
     * @return <code>true</code> if the reference was loosened;
     * <code>false</code> if it couldn't be loosened or was already loose.
     */
    @Override
    boolean maybeLoosen() {
        if (directReferencesToStandinReferent || !state.isObjectLocal()) {
            return false;
        } else {
            return super.maybeLoosen();
        }
    }

    /**
     * Creates a new long reference that forwards calls via this location
     * manager.
     * <p>
     * In general, you should be using <code>localStandin</code> rather than
     * explicitly creating a long reference; this will automatically handle
     * memory management, the standin's length changing due to migration, etc..
     * (So in other words, you get a variable-length reference, which is
     * probably what you wanted, rather than a fixed-long reference). This
     * method is thus mostly only useful for initialising
     * <code>localStandin</code> itself, or for testing purposes.
     * <p>
     * @return A freshly constructed long reference that forwards all method
     * calls to this location manager.
     */
    final Standin<T> newLongReference() {
        ClassKnowledge<T> objectClassKnowledge = knowledgeForClass(objectClass);

        return objectClassKnowledge.getStandinFactory(LONG_REFERENCE)
                .standinFromLocationManager(assertClass(objectClass));
    }

    /**
     * Determines whether the location manager is currently in a migrating
     * state. This is defined as "the location manager is using
     * <code>ForwardingStandinStorage</code> even though other storages would be
     * appropriate".
     * <p>
     * To get a stable return value from this method (i.e. one that won't change
     * to <code>false</code> as a consequence of actions on another thread), you
     * need to hold the location manager's local standin's storage's monitor.
     *
     * @return Whether the location manager is migrating.
     */
    boolean isMigrating() {
        Standin<T> localStandin = getLocalStandin();
        return localStandin.getStorage(UNRESTRICTED) instanceof ForwardingStandinStorage;
    }

    /**
     * Migrates the information in this offline location manager to the given
     * system. This method should only be called with the migration write lock
     * held, and only while the object is local and the standin is using
     * forwarding standin storage. The object will become remote as a
     * consequence of the method being called.
     *
     * @param duringManualMigration <code>true</code> if the object is in the
     * middle of manual migration (typically because <code>migrateTo</code> was
     * called to implement the manual migration, but this should also be
     * <code>true</code> for automatic migrations that happen to occur on an
     * object while it's prepared for manual migration).
     * @param newLocation The communication address of the system to migrate to.
     */
    void migrateTo(boolean duringManualMigration,
            CommunicationAddress newLocation) {
        if (!state.isObjectLocal()) {
            throw new IllegalStateException(
                    "attempting to migrate away an object, but it isn't here");
        }

        try (AutocloseableLockWrapper lw = getCommunicator().sendMessageSync(
                new MigrationSynchronisationMessage(
                        new RemoteOnlyStandin<>(this)), newLocation)) {
            Standin<T> standin = getLocalStandin();
            ObjectDescription description = Marshalling.describeStandin(standin);

            /* We need to request more GC weight from the recipient than we're
               currently lending out (by enough to let us send messages
               mentioning the object in the future). Calculate a reasonable
               amount first. (The "request" is more of a demand, as the object
               is created on the recipient with the specified amount of GC
               weight already borrowed.) */
            long sentWeight = generatedWeight.get() + 4294967296L;
            BorrowedWeight oldLaW = state.locationAndWeight.get();
            /* note: use supplyWeight rather than just setting the weight
               directly in newLaW, so that the keepalive timer is updated; copy
               the keepalive timer even though the weight is being dropped to 0,
               so that we can keep the same timer alive through into the
               future (that said, we're unlikely to have a timer right now
               anyway) */
            BorrowedWeight newLaW = new BorrowedWeight(oldLaW,
                    oldLaW.location.migrated(newLocation), 0L);

            /* Update the fields for the other end of the connection. */
            getCommunicator().sendMessageSync(new MigrationMessage<>(
                    new ObjectLocation(this), objectClass, description,
                    sentWeight, duringManualMigration,
                    newLaW.location.getMigrateCount()), newLocation);

            /* At our end, we'll need to change our state to reflect the new
               location of the object. (Right now, the location manager is in a
               "no standin referent, waiting for description" state, which we
               don't want as the description won't arrive and thus things will
               block indefinitely if we don't change it.) */
            state.locationAndWeight.set(newLaW);
            supplyGCWeight(sentWeight, newLaW.location);

            /* The standin needs its referent dropped, as it's going from local
               to remote. The storage is already correct. */
            standin.dropResources(UNRESTRICTED);
        } catch (Throwable ex) {
            /* Sadly, we have to multicatch everything like this because
               sendMessageSync could forward absolutely any sort of exception
               from the far side of the connection. */
            throw new DistributedError(ex, "migrating to " + newLocation
                    + ": " + this);
        }
    }

    /**
     * State used by a location manager and that's required after the location
     * manager is deallocated. The state is stored in a separate object so that
     * it's still accessible post-deallocation. The object itself is a thread,
     * which will run when the location manager is deallocated, and handle the
     * cleanup for it.
     *
     * @param <T> The referent class of the location manager.
     */
    private static class LocationManagerState<T> extends PooledThread {

        /**
         * Creates a new object for holding location manager state, initialising
         * its fields to the specified values.
         *
         * @param lastKnownLocation The most recently seen location on which the
         * appropriate referent is stored.
         * @param objectID The global ID of the object being managed.
         * @param communicator The distributed communicator with which the
         * location manager is associated.
         * @throws AutocloseableLockWrapper.CannotLockException If the
         * communicator's location tracking has shut down
         */
        private LocationManagerState(
                TimestampedLocation lastKnownLocation, GlobalID objectID,
                DistributedCommunicator communicator)
                throws AutocloseableLockWrapper.CannotLockException {
            this.locationAndWeight = new AtomicReference<>(
                    new BorrowedWeight(lastKnownLocation, 0L));
            this.objectID = objectID;
            this.communicatorKeepalive = communicator.maybeGetKeepaliveLock(
                    ShutdownStage.LOCATION_MANAGER, objectID);
            this.communicator = communicator;
        }

        /**
         * The distributed communicator associated with the location manager.
         * This is used during test situations to handle circumstances in which
         * multiple communicators exist on the same Java virtual machine.
         */
        private final DistributedCommunicator communicator;

        /**
         * Where and when the object was last seen, and the GC weight of the
         * reference from this system to that system. Also contains the
         * keepalive thread that prevents borrowed weight disappearing. This is
         * a single value because changes may need to be made that are atomic
         * across all three values at once.
         * <p>
         * There are several invariants on this field:
         * <ul><li>If the object is currently located here, the location must be
         * <code>CommunicationAddress.getCommunicator().getMyAddress()</code>,
         * the timestamped location must be the highest among any location
         * manager for this object (although other location managers can use the
         * same timestamp if they use the same location, i.e. here), and the
         * weight must be 0.</li>
         * <li>If the object is not currently located here, the location must
         * either be the object's current location (with the correct timestamp),
         * or a former location (with the timestamp it had at the time); the
         * former location will have a higher timestamp for its timestamped
         * location. The GC weight must equal the weight in the lifetime manager
         * on the referenced system, minus any weight for messages that are
         * currently in transit. It must also be positive (unless the location
         * manager corresponding to this location manager state has been
         * deallocated, in which case the entire weight is returned, effectively
         * setting this field to zero).</li>
         * <li>The keepalive thread must be running if and only if the weight is
         * nonzero.</li>
         * <li>Updates to this field that change whether or not the object is
         * located here must be guarded by <code>migrationLock</code>.</li>
         * </ul>
         * <p>
         * The weight component refers to the amount of outbound weight that the
         * location manager is holding in order to prevent other JVMs releasing
         * the object this location manager is managing. (Inbound weight is
         * stored on lifetime managers, not here.)
         * <p>
         * A large amount of weight (around 2<sup>32</sup>) will have been
         * supplied to this JVM when it was given a reference to the object; as
         * all references to the object require weight, this allows the location
         * manager to give up a small amount of GC weight in order to allow the
         * object to be mentioned in a message. (Typically, 1 point of weight
         * will be spent whenever the message is mentioned in an outbound
         * communication.)
         */
        private final AtomicReference<BorrowedWeight> locationAndWeight;

        /**
         * A global ID used to represent the object. Used to find the location
         * manager for a specific object, in cases where no short (i.e. Java)
         * reference to that object is available.
         */
        private final GlobalID objectID;

        /**
         * A lock preventing the local communicator shutting down. Used to
         * ensure that the communicator won't shut down while it's still
         * responsible for managing remote references.
         */
        private final DeterministicAutocloseable communicatorKeepalive;

        /**
         * Returns whether the object is currently stored on this system. In
         * order to prevent the object being moved to or from this system while
         * you are calling this method, you need to hold the migration read
         * lock; otherwise, the results could be outdated.
         *
         * @return <code>true</code> if the object is local.
         */
        boolean isObjectLocal() {
            return locationAndWeight.get().location.getRemoteLocation(
                    communicator.getMyAddress()) == null;
        }

        /**
         * Cleans up the location manager. This removes the keepalive timer, the
         * keepalive on the distributed communicator, and the standin's
         * reference to the location manager.
         * <p>
         * Note that this operation effectively holds a migration write lock on
         * the location manager; the location manager's been deallocated at this
         * point (meaning that the location manager state is orphaned and has no
         * inbound references except from the garbage collector), so nothing
         * else can interfere with our read or write operations on fields that
         * normally need synchronisation. Likewise, nothing else has a reference
         * to the GC weight field, so we know that it won't change in unexpected
         * ways.
         */
        @Override
        protected void run() {
            ScheduledFuture<?> future = locationAndWeight.get().keepAlive.get();

            if (future != null) {
                future.cancel(false);
                delayInterruptions(() -> {
                    try {
                        future.get();
                    } catch (CancellationException ex) {
                        /* We're actually expecting an exception here; the
                           purpose is to block until the keepalive future is
                           actually cancelled. */
                    } catch (ExecutionException ex) {
                        communicator.asyncExceptionHandler(ex);
                    }
                });
            }

            /* If we were keeping an object on a remote system alive, let it
               know that we were finalized. */
            if (!isObjectLocal()) {
                /* note: we can't get a CannotLockException here because our
                   keepalive is holding the lock for us */
                try {
                    communicator.sendMessageAsync(
                            new LocationManagerStatusMessage(
                                    objectID, communicator.getMyAddress(),
                                    -locationAndWeight.get().weight),
                            new GlobalID(communicator.getMyAddress()),
                            locationAndWeight.get().location.getLocation());
                } catch (DistributedError | IllegalArgumentException
                        | AutocloseableLockWrapper.CannotLockException ex) {
                    communicator.asyncExceptionHandler(ex);
                }
            }
            communicatorKeepalive.close();
        }
    }

    /**
     * Information about the amount of GC weight we've borrowed, and where from.
     * This can also mark the object as local if we're listing ourself as the
     * source of GC weight.
     */
    private static class BorrowedWeight {

        /**
         * Where the weight was borrowed from. (That could be here, to mark the
         * object as local.)
         */
        private final TimestampedLocation location;

        /**
         * The amount of weight that was borrowed.
         */
        private final long weight;

        /**
         * The keepalive timer that prevents the corresponding lifetime manager
         * expiring. (This can be changed "after the fact" because the keepalive
         * timer may be created lazily, but should be treated as though it were
         * valid whenever <code>weight</code> is nonzero.)
         */
        private AtomicReference<ScheduledFuture<?>> keepAlive;

        /**
         * Creates a structure to describe borrowed weight, with a fresh
         * keepalive field.
         *
         * @param location The location the weight was borrowed from.
         * @param weight The amount of weight borrowed.
         */
        BorrowedWeight(TimestampedLocation location, long weight) {
            this.location = location;
            this.weight = weight;
            this.keepAlive = new AtomicReference<>(null);
        }

        /**
         * Creates a structure to describe borrowed weight, adjusting the weight
         * field of another borrowed weight structure.
         *
         * @param bw The old borrowed weight structure to copy.
         * @param extraWeight The amount of additional weight borrowed.
         */
        BorrowedWeight(BorrowedWeight bw, long extraWeight) {
            this.location = bw.location;
            this.weight = bw.weight + extraWeight;
            this.keepAlive = bw.keepAlive;
        }

        /**
         * Creates a structure to describe borrowed weight, copying the
         * keepalive field of another borrowed weight structure.
         *
         * @param bw The old borrowed weight structure to copy.
         * @param location The new location to borrow the weight from.
         * @param weight The amount of weight to borrow from the new location.
         */
        BorrowedWeight(BorrowedWeight bw, TimestampedLocation location, long weight) {
            this.location = location;
            this.weight = weight;
            this.keepAlive = bw.keepAlive;
        }
    }
}
