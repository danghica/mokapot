package xyz.acygn.mokapot;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.function.Supplier;
import static xyz.acygn.mokapot.Authorisations.UNRESTRICTED;
import static xyz.acygn.mokapot.ClassKnowledge.knowledgeForActualClass;
import static xyz.acygn.mokapot.NonCopiableKnowledge.StandinFactoryPurpose.INTERFACE_MIGRATION_WRAPPER;
import static xyz.acygn.mokapot.NonCopiableKnowledge.StandinFactoryPurpose.MIGRATION_WRAPPER;
import xyz.acygn.mokapot.markers.Copiable;
import xyz.acygn.mokapot.markers.NonCopiable;
import xyz.acygn.mokapot.markers.NonMigratable;
import xyz.acygn.mokapot.skeletons.ForwardingStandinStorage;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.skeletons.StandinStorage;
import xyz.acygn.mokapot.util.BackgroundGarbageCollection;
import xyz.acygn.mokapot.wireformat.ObjectDescription;

/**
 * The migration-related actions that can be performed on an object. Each
 * <code>MigrationActions</code> object refers to one specific object, and
 * operates on that object. This means that the methods can act like instance
 * methods &mdash; which they conceptually are &mdash; without having to be
 * actual methods of the object.
 * <p>
 * It should be noted that the mere existence of a <code>MigrationActions</code>
 * object does not keep the object to which it pertains alive; if the object
 * ever becomes entirely unreferenced, it may be deallocated. (This is safe
 * because there's no way to get at the original object via methods of
 * <code>MigrationActions</code>.)
 * <p>
 * This class also contains some static methods that are useful when dealing
 * with migration.
 * <p>
 * To construct instances of this class, use
 * <code>DistributedCommunicator#getMigrationActionsFor</code>.
 *
 * @author Alex Smith
 * @param <T> The actual class of the object to which these actions relate.
 * @see DistributedCommunicator#getMigrationActionsFor(java.lang.Object)
 */
public class MigrationActions<T> implements NonCopiable, NonMigratable {

    /**
     * Returns whether the data for the given reference is currently stored
     * remotely. In other words, whether <code>ref</code> is currently acting as
     * a long reference.
     * <p>
     * The difference between this and <code>instanceof Standin</code> is as
     * follows: a <code>Standin</code> <i>could potentially</i> be stored
     * remotely, and whether an object is a standin will not change over the
     * lifetime of the reference even if the place where the data stores is
     * actually changed; <code>isStoredRemotely</code> means that the data is
     * definitely stored remotely right now (or with a return of
     * <code>false</code>, isn't), but doesn't make guarantees about the
     * location of the data in the future (it can change over time, including in
     * a racy way before your code has a chance to read the return value of this
     * method).
     * <p>
     * An object that is in the process of migration (i.e. between prepare and
     * conclude) will always be shown as local by this method, regardless of
     * which system the data is currently on (in fact, it might even currently
     * be in transit). Use <code>getCurrentLocation</code> (which is much slower
     * than this method, and which will block until it has a definitive value to
     * return) if accurate information is needed.
     *
     * @param ref The reference to check.
     * @return <code>true</code> if the data is stored only on another virtual
     * machine; <code>false</code> if the data is stored on this virtual machine
     * (potentially in addition to others) or if <code>ref</code> is
     * <code>null</code>
     */
    public static boolean isStoredRemotely(Object ref) {
        if (ref == null) {
            return false;
        }
        if (!(ref instanceof Standin)) {
            return false;
        }
        return !((Standin) ref).getStorage(UNRESTRICTED).certainlyLocal();
    }

    /**
     * The location manager of the object to which this action relates. (Note
     * that storing the location manager here, not the object itself, means that
     * we won't keep the object alive locally in cases where we don't want that.
     * Note also that we can create a long reference to a
     * <code>MigrationActions</code> object but that doesn't cause the creation
     * of a long reference to <code>lm</code>; that'll be an ordinary short
     * reference.)
     */
    private final LocationManager<T> lm;

    /**
     * Creates a set of migration actions relating to the object with the
     * specified location manager.
     *
     * @param lm The location manager of the object for which to create the
     * actions.
     */
    MigrationActions(LocationManager<T> lm) {
        this.lm = lm;
    }

    /**
     * Creates an object in such a way that it can be migrated in the future.
     * <p>
     * Most objects cannot be safely migrated because there's no way for the
     * distributed communication framework to be aware of all references to the
     * object. This method takes a supplier as its argument, and assumes that
     * there are no references to the supplier's return value other than that
     * return value itself. The return value will be an object that's as similar
     * as possible but that supports migration. (Alternatively, you can request
     * an object that supports the same interfaces, which is substantially more
     * efficient than trying to match the type exactly, and also removes the
     * risk of attempting some unsupported operations such as direct private
     * method calls and direct accesses to fields; as such, you should set
     * <code>interfaceOnly</code> whenever your code has no need for more
     * stringent assumptions than that.)
     * <p>
     * As a special case, if the created object is <code>Copiable</code>, or
     * implicitly copiable, it will be returned untouched (because if a copiable
     * object is migrated, there is by definition no harm if stray references
     * continue to use the original).
     *
     * @param <T> The type of the created object.
     * @param constructor A method that creates objects of type <code>T</code>.
     * This could be either an actual constructor, or a factory method. However,
     * its return value <code>must</code> be entirely unreferenced from anywhere
     * else (i.e. the returned reference must be the only reference to the
     * created object). Most (but not all) constructors will naturally fulfil
     * this requirement.
     * @param interfaceOnly Whether it's OK to return an object that only
     * implements the same interfaces as the returned object, as opposed to
     * having the same (or a maximally similar) type. (If you set this to
     * <code>true</code>, you probably want <code>T</code> to be an interface,
     * to avoid the situation where the method returns a type other than that
     * shown as its return type.)
     * @return An object that supports migration, and is as similar as possible
     * to the object returned from <code>constructor</code> (or, if
     * <code>interfaceOnly</code> is set, implements the same interfaces).
     * @throws IllegalArgumentException If the created object implements
     * <code>NonMigratable</code>
     * @throws NullPointerException If <code>constructor</code> returns
     * <code>null</code>
     */
    public static <T> T createMigratably(Supplier<T> constructor,
            boolean interfaceOnly)
            throws IllegalArgumentException, NullPointerException {
        T constructed = constructor.get();
        if (constructed instanceof Copiable
                || constructed instanceof Standin) {
            return constructed;
        }

        ClassKnowledge<? extends T> knowledge
                = knowledgeForActualClass(constructed);
        if (!(knowledge instanceof NonCopiableKnowledge)) {
            return constructed;
        }

        StandinFactory<? extends T> standinFactory
                = knowledge.getStandinFactory(interfaceOnly
                        ? INTERFACE_MIGRATION_WRAPPER : MIGRATION_WRAPPER);

        /* We put CALL_DIRECT very early in the list of required functionality,
           so it's quite likely we get an actual T here (assuming we need one).
           CALL_INTERFACE is also very early (and impossible to not satisfy...),
           so if T is an interface, it'll have the right type. This unchecked
           cast is thus an "educated hopeful" sort of cast. */
        try {
            @SuppressWarnings("unchecked")
            T rv = (T) standinFactory.castAndWrapObject(constructed);
            return rv;
        } catch (UnsupportedOperationException ex) {
            /* The ability to wrap existing objects is last on the list because
               we don't need it; we can instead describe the existing object and
               undescribe it into a standin. */
            try {
                ObjectDescription description
                        = Marshalling.describeStatic(constructed);
                @SuppressWarnings("unchecked")
                T rv = (T) standinFactory.newFromDescription(description);
                return rv;
            } catch (IOException ex1) {
                /* We shouldn't have a case where describing the object produces
                   an invalid/corrupt description. */
                throw new RuntimeException(ex1);
            }
        }
    }

    /**
     * Returns whether the object is currently looking for a place to migrate
     * to. An object is placed into this state if <code>migrateCommit</code> is
     * called with <code>null</code> as its <code>newLocation</code>, or if all
     * references to the object from its current location have died. While in
     * this state, the object will automatically be migrated to any communicator
     * that attempts to call a method on it (taking it out of the state); it can
     * also be taken out of the state by migrating the object manually.
     * <p>
     * Note that although the return value is instantaneously valid, it may
     * quickly become invalid; if the garbage collector (which runs
     * asynchronously) notices that the last local reference to the object has
     * died, the object may automatically be placed into migrate-anywhere state,
     * and if a method is called on an object in migrate-anywhere state, it will
     * automatically be migrated to the place that the method was called from
     * (and taken out of the state in question). As such, this method is mostly
     * only useful for testing.
     *
     * @return Whether the object described by this set of migration actions is
     * currently in migrate-anywhere state,
     */
    public boolean isMigratingAnywhere() {
        /* Internal definition of isMigratingAnywhere: this is defined only when
           the migration lock is read-lockable, and only on the same
           communicator as the object, in which case we just check whether the
           location manager is offline. */
        try (MigrationMonitor<T> mm = new MigrationMonitor<>(lm, true)) {
            if (mm.getRemoteActions() != null) {
                /* The migration monitor couldn't get a lock on the manager. */
                return mm.getRemoteActions().isMigratingAnywhere();
            }
            return lm.isMaybeLocallyUnreferenced();
        }
    }

    /**
     * Prepares to migrate the object to which this action relates. This will
     * have no immediate impact on the behaviour of the program. However, method
     * calls on the object between <code>migratePrepare</code> and
     * <code>migrateConclude</code> may become less efficient.
     * <p>
     * This method may be called while the object is in use (i.e. while method
     * calls on the object are still being made), as can
     * <code>migrateCommit</code>. However, no single method call on the object
     * may be active during <i>both</i> the <code>migratePrepare</code> and
     * <code>migrateCommit</code> call, i.e. any call that's still active at the
     * time of <code>migrateCommit</code> must have started since the
     * corresponding <code>migratePrepare</code>. It <b>is not possible</b> to
     * automatically check for this condition; violating this requirement could
     * cause unpredictable behaviour.
     * <p>
     * This method is idempotent, i.e. if the object is already prepared for
     * migration, it will have no effect.
     * <p>
     * Not all objects can be migrated. Copiable objects can be migrated
     * trivially (via copying them to the target system). Otherwise, an object
     * can only safely be migrated if it was created migratably. (If an object
     * was not created migratably, it's still possible to attempt to migrate it,
     * even if it implements <code>NonMigratable</code>!, but doing so will
     * cause any references to the object from its original location to start
     * behaving in an undefined manner, as there's no way to update them to
     * point at its new location. The migrated copy will be created migratably
     * in order to avoid issues if a second migration is needed.)
     *
     * @param force Whether to prepare the object for migration even if it's of
     * a type that cannot safely be migrated (and thus break the semantics of
     * the program if any references to the object on the virtual machine where
     * it was created are subsequently used).
     * @throws CannotMigrateException If the object is not of a migratable or
     * copiable type, and <code>force</code> is <code>false</code>
     */
    public void migratePrepare(boolean force) throws CannotMigrateException {
        try (MigrationMonitor<T> mm = new MigrationMonitor<>(lm, true)) {
            if (mm.getRemoteActions() != null) {
                /* The migration monitor couldn't get a lock on the manager. */
                mm.getRemoteActions().migratePrepare(force);
            } else {
                if (!force && !lm.standinOwnsReferent()) {
                    throw new CannotMigrateException(
                            "attempting to prepare the migration of an object "
                            + "of " + lm.getObjectClass() + " which might have "
                            + "unknown external references");
                }

                /* The manager is locked. Take the object out of stored-in-self
                   mode. */
                Standin<T> standin = lm.getLocalStandin();
                // TODO: Prevent the standin getting deallocated mid-migration,
                // or else work out an algorithm that makes that unnecessary.
                setStorageToForwarding(lm, standin);
                BackgroundGarbageCollection.volatileAccess(standin);
            }
        }
    }

    /**
     * Changes the canonical location on which the data of the object to which
     * this action relates is stored. In other words, the object's data will be
     * copied from its current location to a new location (typically on a
     * different virtual machine), and all future method calls on the object
     * will then run on the new system. This is known as "migrating" the object.
     * <p>
     * This method may not be called unless <code>migratePrepare()</code> was
     * called on the object earlier, without an intervening call to
     * <code>migrateConclude()</code>. It is possible to migrate an object
     * multiple times between a prepare and conclude.
     * <p>
     * This method may be called while there are currently method calls being
     * made on the object (so long as they started some time after
     * <code>migratePrepare()</code> was called). However, it will block until
     * all such method calls have finished executing. A side effect of this is
     * that an object should not attempt to migrate itself, as doing so would
     * trivially lead to a deadlock (and more generally, that no attempt should
     * be made to migrate an object from a method called from a method of that
     * object, even indirectly).
     *
     * @param newLocation The virtual machine on which the object's data should
     * be stored. This can be <code>null</code>, in which case the data will be
     * sent to the next virtual machine that attempts to call a method on that
     * object. (This may cause the migration to be cancelled, if the next
     * attempt to access the object is from its current location.)
     * @param remigrate Determines what to do if the object is not currently on
     * the machine it was expected to migrate from (i.e. it has migrated since
     * this <code>MigrationActions</code> object was created, perhaps due to an
     * automatic migration of the object). <code>true</code> will migrate the
     * object again. <code>false</code> will cancel the migration in this case,
     * causing the method to fail with an exception.
     * @throws CannotMigrateException If the object has been migrated since the
     * <code>MigrationActions</code> object was created and
     * <code>remigrate</code> is <code>false</code>
     * @throws IllegalStateException If <code>migratePrepare()</code> has not
     * been called since the most recent <code>migrateConclude()</code> on this
     * object
     */
    public void migrateCommit(CommunicationAddress newLocation,
            boolean remigrate)
            throws CannotMigrateException, IllegalStateException {
        try (MigrationMonitor<T> mm = new MigrationMonitor<>(lm, false)) {
            if (mm.getRemoteActions() != null) {
                /* Huh, what happened to the object? */
                if (!remigrate) {
                    throw new CannotMigrateException(
                            "attempting to migrate an object that already migrated "
                            + "with 'remigrate' set to false");
                }
                mm.getRemoteActions().migrateCommit(newLocation, remigrate);
            } else {
                /* Everything should be in position: the object is pinned to the
                   local system, all method calls via it have halted (due to the
                   "false" in MigrationManager's constructor), so we simply have
                   to send the data to a remote system. However, that assumes
                   that this method was called in accordance with its
                   specification, and (especially as it's a public method) we
                   can't know that. So step 1 is to check that the object's
                   state is what is expected. The only legal possibility is a
                   location manager whose standin uses ForwardingStandinStorage
                   (with an undropped referent, but MigrationManager checked
                   that). */
                Standin<T> localStandin = lm.getLocalStandin();
                StandinStorage<T> oldStorage
                        = localStandin.getStorage(UNRESTRICTED);
                synchronized (oldStorage) {
                    if (!lm.isMigrating()) {
                        throw new IllegalStateException(
                                "migrateCommit() called without migratePrepare()");
                    }

                    /* TODO: Step 2: Mark the object as migrate-anywhere, if
                       necessary.

                       Step 3: if we have a specific destination, migrate
                       there. */
                    if (newLocation != null) {
                        lm.migrateTo(true, newLocation);
                    }
                }
            }
        }
    }

    /**
     * Undoes the effect of <code>migratePrepare()</code>. In other words, any
     * performance penalty from <code>migratePrepare()</code> will be removed,
     * but <code>migratePrepare()</code> will once again need to be called
     * before <code>migrateCommit()</code> can run successfully.
     * <p>
     * If <code>migrateCommit()</code> was called to "just migrate the object
     * anywhere", but it has not yet moved, this method will additionally cancel
     * the migration and the object will remain on its existing system.
     * <p>
     * Note that there's a benefit from calling this method even after a
     * migration has occurred, in order to remove the performance penalty from
     * migration preparation. (This is to prevent any undefined behaviour caused
     * by race conditions between automatic and manual migration.)
     * <p>
     * This method will succeed with no effect if no migration is in progress
     * for the object to which this action relates.
     */
    public void migrateConclude() {
        try (MigrationMonitor<T> mm = new MigrationMonitor<>(lm, true)) {
            if (mm.getRemoteActions() != null) {
                /* The migration monitor couldn't get a lock on the manager. */
                mm.getRemoteActions().migrateConclude();
            } else {
                /* The manager is locked. Put the object into stored-in-self
                   mode. (The migration monitor has verified that it's
                   local.) */
                Standin<T> standin = lm.getLocalStandin();
                if (standin == null || lm.isMaybeLocallyUnreferenced()) {
                    /* Force the object online. The "best reference" will
                       actually be the (possibly newly constructed) standin. */
                    T bestReference = lm.getBestReference();
                    standin = lm.getLocalStandin();
                    if (bestReference != standin) {
                        throw new RuntimeException(
                                "An object was offline but has a direct referent");
                    }
                }
                setStoragetoTight(lm, standin);
                lm.maybeLoosen();
            }
        }
    }

    /**
     * Safely sets the storage of the given standin, which references the given
     * location manager, to <code>ForwardingStandinStorage</code>. Typically
     * used when a standin that was previously used as a live object needs to
     * become a migration wrapper.
     *
     * @param <T> The actual type of the standin's referent.
     * @param lm The location manager with which the standin is associated.
     * @param standin The standin to set.
     */
    static <T> void setStorageToForwarding(
            LocationManager<T> lm, Standin<T> standin) {
        while (true) {
            StandinStorage<T> oldStorage
                    = standin.getStorage(UNRESTRICTED);
            if (oldStorage instanceof ForwardingStandinStorage) {
                /* nothing to do */
                break;
            } else if (!(oldStorage instanceof LooseTightStandinReference.TightStorage)) {
                /* LooseStorage is a possible and legal value; we can detect it
                   by determining that the storage doesn't change upon
                   tightening (and if it is loose, we need to tighten
                   anyway; LooseStorage should never be changed except via a
                   call to tighten()). */
                lm.tighten();
                if (standin.getStorage(UNRESTRICTED) != oldStorage) {
                    continue;
                }
                throw new IllegalStateException(
                        lm + " is managing a standin with no location manager");
            }
            try {
                standin.safeSetStorage(new ForwardingStandinStorage<>(lm),
                        oldStorage, UNRESTRICTED);
                break;
            } catch (ConcurrentModificationException ex) {
                /* go round the loop again */
            }
        }
    }

    /**
     * Safely sets the storage of the given standin, which references the given
     * location manager, to <code>TightStorage</code>. Typically used when a
     * standin that was previously used as a migration wrapper needs to become a
     * live object, or when migration is concluded.
     * <p>
     * This operation is only sensible / invariant-respecting if the standin
     * already has a usable referent.
     *
     * @param <T> The actual type of the standin's referent.
     * @param lm The location manager with which the standin is associated.
     * @param standin The standin to set.
     */
    static <T> void setStoragetoTight(
            LocationManager<T> lm, Standin<T> standin) {
        while (true) {
            StandinStorage<T> oldStorage
                    = standin.getStorage(UNRESTRICTED);
            if (oldStorage instanceof LooseTightStandinReference.TightStorage) {
                /* nothing to do */
                break;
            } else if (!(oldStorage instanceof ForwardingStandinStorage)) {
                lm.tighten();
                if (standin.getStorage(UNRESTRICTED) != oldStorage) {
                    continue;
                }
                throw new IllegalStateException(
                        lm + " is managing a standin with no location manager");
            }
            try {
                standin.safeSetStorage(new LooseTightStandinReference.TightStorage<>(lm),
                        oldStorage, UNRESTRICTED);
                break;
            } catch (ConcurrentModificationException ex) {
                /* go back round the loop and try again */
            }
        }
    }

    /**
     * Returns the location where the data of the object to which this action
     * relates is currently stored. Note that although this will be a location
     * on which the object is currently or was formerly stored, it may have
     * become invalid by the time the method returns as a consequence of the
     * object being migrated.
     * <p>
     * If necessary, other virtual machines involved in the distributed
     * communication will be contacted to determine the object's up-to-date
     * current location (even if the present virtual machine is unaware of it).
     * As such, this is potentially a slow operation.
     *
     * @return The location of the object's data.
     */
    public CommunicationAddress getCurrentLocation() {
        try (MigrationMonitor<T> mm = new MigrationMonitor<>(lm, true)) {
            MigrationActions<T> remoteActions = mm.getRemoteActions();
            if (remoteActions == null) {
                return lm.getCommunicator().getMyAddress();
            } else {
                return remoteActions.getCurrentLocation();
            }
        }
    }

    /**
     * An exception thrown when a manual migration operation fails. This is a
     * checked exception; most manual-migration-related operations can only work
     * on objects that are correctly instrumented for migration (so that calls
     * to the existing object, if any, can be rerouted to its newly migrated
     * location).
     * <p>
     * This exception is not used if the call sequence for the migration methods
     * is wrong; that throws an <code>IllegalStateException</code> instead.
     */
    public static class CannotMigrateException extends Exception {

        /**
         * Version number of this class for serialisation. This was initially
         * generated randomly, and should be rerandomised whenever the class is
         * changed in an incompatible way.
         */
        private static final long serialVersionUID = 0x5cdc96df8f83b3L;

        /**
         * Constructs a new CannotMigrateException with a given message.
         *
         * @param message The reason why the migration cannot occur.
         */
        public CannotMigrateException(String message) {
            super(message);
        }

        /**
         * Constructs a new CannotMigrateException, that was a consequence of a
         * given exception, with a given message.
         *
         * @param message The reason why the message cannot occur, as text.
         * @param cause The exception that caused the failure to migrate.
         */
        public CannotMigrateException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
