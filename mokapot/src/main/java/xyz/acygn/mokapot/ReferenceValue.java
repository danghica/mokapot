package xyz.acygn.mokapot;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ConcurrentModificationException;
import static xyz.acygn.mokapot.LocationManager.locationManagerForID;
import xyz.acygn.mokapot.markers.Copiable;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper;
import xyz.acygn.mokapot.util.DeterministicAutocloseable;

/**
 * A class representing information needed to construct a reference to an object
 * that's possibly on another machine. Unlike a <code>Standin</code>, which is
 * not necessarily <code>Serializable</code> but which can have methods invoked
 * on it to invoke those same methods on the original object, a
 * <code>ReferenceValue</code> can be transmitted over a network, but does not
 * do method forwarding; it must be converted to a reference (usually long,
 * unless the object it's referencing is actually on the remote machine already)
 * via <code>#unmarshal</code> (most commonly called indirectly via
 * <code>ObjectCopy#unmarshal</code>) before it becomes usable.
 * <p>
 * This conversion must be done exactly once (among the original
 * <code>ReferenceValue</code> and all copies of it), and on the machine
 * specified when calling its constructor, in order to maintain garbage
 * collection semantics, because the ReferenceValue object itself holds some
 * garbage collection weight. (The amount is fixed at 2<sup>32</sup> when the
 * reference value was created on the original location of the object, and 1
 * when the reference value was created elsewhere. When sending a local object
 * to another system, unlimited weight is available, and thus a large amount of
 * weight is sent along with the object so that the recipient system has weight
 * to construct <code>ReferenceValue</code>s of its own; it's the
 * <code>ReferenceValue</code> itself that's used to transmit that weight. When
 * sending a message about a remote object, GC weight is potentially in short
 * supply so only a minimal amount of it is used.)
 * <p>
 * A <code>ReferenceValue</code> is similar in nature to the simpler
 * <code>ObjectLocation</code>; the differences are, besides the garbage
 * collection rules, that the <code>ReferenceValue</code> encodes some
 * information about the referenced object itself, in addition to information
 * about its location manager.
 *
 * @param <T> A declared type for this reference.
 * @author Alex Smith
 */
class ReferenceValue<T> implements Copiable {

    /**
     * Creates a new reference value, which can be unmarshalled into a reference
     * to the object managed by the given location manager. The reference value
     * will also keep the object alive until (at least) it is unmarshalled, via
     * increasing the weight of the object's lifetime manager on the original
     * system. Doing this requires knowing the system on which the reference
     * value will eventually be unmarshalled.
     * <p>
     * This method can only be run with an active distributed communicator (this
     * constraint is unlikely to be violated in practice, because the existence
     * of a location manager implies an active distributed communicator). In
     * some cases (specifically, when the object's storage is neither local nor
     * on the target system), the reference value cannot be constructed locally,
     * and will instead be constructed on a remote system and sent to this
     * system over the network.
     *
     * @param <T> The type of the referent of the new reference value.
     * @param manager A location manager that manages the object to which the
     * reference value will be constructed.
     * @param targetSystem The system on which this reference value will
     * eventually be unmarshalled. (It must be unmarshalled exactly once, in
     * order to maintained the desired garbage collection semantics.)
     * @return The newly constructed object.
     */
    static <T> ReferenceValue<T> newReferenceValueOf(
            LocationManager<T> manager, CommunicationAddress targetSystem) {
        boolean localCreation
                = targetSystem.equals(manager.getCommunicator().getMyAddress());

        try (DeterministicAutocloseable wrapper
                = new AutocloseableLockWrapper(manager.getMigrationLock(true),
                        "newReferenceValueOf")) {
            TimestampedLocation timestamp = manager.getLocalObjectTimestamp();
            if (timestamp != null) {
                /* The simplest case: the object is here right now, and as
                   we're holding the migration lock, won't move. */
                long weight = localCreation ? 0 : 1L << 32;
                if (weight > 0) {
                    LifetimeManager.requestOrReturnWeight(
                            new ObjectLocation(manager.getObjectID(), targetSystem),
                            weight, manager, manager.getCommunicator());
                }
                return new ReferenceValue<>(
                        manager, targetSystem, timestamp, weight);
            }

            /* Next try: get 1L worth of weight from the local location
               manager. */
            timestamp = manager.requestGCWeight(1L, targetSystem);
            if (timestamp != null) {
                /* That was easy. */
                return new ReferenceValue<>(
                        manager, targetSystem, timestamp, 1L);
            }

            /* Third attempt: try to get the reference value from a different
               system's location manager. */
            CommunicationAddress location = manager.followLocationChain();
            if (location == null) {
                throw new ConcurrentModificationException(
                        "object migrated while we held the migration lock");
            }

            try {
                return manager.getCommunicator().
                        sendMessageSync(new ThirdPartyMessage<>(
                                manager.getBestReference(), targetSystem),
                                location);
            } catch (Throwable ex) {
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                }
                if (ex instanceof Error) {
                    throw (Error) ex;
                }
                throw new UndeclaredThrowableException(ex);
            }
        }
    }

    /**
     * The ID of the object being referenced. (This is stored on the object's
     * location managers.)
     */
    private final GlobalID objectID;

    /**
     * The expected location of the object, if not on the recipient. A
     * <code>ReferenceValue</code> must either be sent from the object's owner
     * to another machine, or the other way round; this value is equal to
     * <code>null</code> if the reference value is being sent to the owner, or
     * an appropriately timestamped sender address if the reference value is
     * being sent to the recipient.
     * <p>
     * As a special case, if the object's owner is sending a reference to
     * itself, this is non-null and its address portion is
     * <code>referencer</code>.
     */
    private final TimestampedLocation expectedLocation;

    /**
     * Returns whether this reference value was created by the owner of the
     * object. The owner must be the sender or recipient; if
     * <code>expectedLocation</code> is <code>null</code>, the owner is the
     * recipient (by definition); otherwise, it's the sender.
     *
     * @return <code>true</code> if the object's owner created this reference
     * value (i.e. is the sender)
     */
    final boolean sentFromOwner() {
        return expectedLocation != null;
    }

    /**
     * The system from which the long reference starts. This might be the sender
     * of the <code>ReferenceValue</code>, if it's telling a system to create a
     * reference to an object it already owns; in other cases, it will be the
     * recipient (who is being told to create a reference back to the sender).
     */
    private final CommunicationAddress referencer;

    /**
     * The amount of GC weight stored in this reference value, assuming it
     * hasn't been unmarshalled. (Unmarshalling a reference value transfers its
     * weight to the system on which it was unmarshalled, leaving it with a
     * conceptual weight of 0; however, because <code>ReferenceValue</code> is
     * <code>Copiable</code>, there's no way in general to determine whether
     * this has happened or not. This is the reason why a
     * <code>ReferenceValue</code> must be unmarshalled exactly once.)
     *
     * @return The amount of GC weight.
     */
    final long weight() {
        return !sentFromOwner() ? 1L
                : referencer.equals(expectedLocation.getLocation()) ? 0L : 1L << 32;
    }

    /**
     * Creates a new reference value, which can be unmarshalled into a reference
     * to the object managed by the given location manager. This constructor is
     * limited to cases where either the location manager's referent is local,
     * or the location manager's referent is on the target system. (Otherwise
     * the reference value object will have to be constructed on some other
     * system. The factory method <code>newReferenceValueOf</code> abstracts
     * away this detail.)
     * <p>
     * This method does not take GC weight itself (that's the caller's job), but
     * it does verify the amount of weight to see if it's correct, to help avoid
     * mistakes in the call pattern.
     *
     * @param manager A location manager that manages the object to which the
     * reference value will be constructed.
     * @param targetSystem The system on which this reference value will
     * eventually be unmarshalled. (It must be unmarshalled exactly once, in
     * order to maintained the desired garbage collection semantics.)
     * @param location The last known location of the object. (This duplicates
     * information in the location manager, but the caller needs to hold the
     * object's migration lock to stop it changing in an unstable manner, and so
     * the caller will likely have the information to hand already.)
     * @param weight The amount of GC weight that was supplied in the creation
     * of the reference value. There is only one correct value for this, and the
     * constructor will check to ensure that it's correct.
     * @throws IllegalArgumentException If <code>weight</code> has the wrong
     * value for this sort of reference value
     */
    private ReferenceValue(LocationManager<T> manager,
            CommunicationAddress targetSystem,
            TimestampedLocation location, long weight)
            throws IllegalArgumentException {
        CommunicationAddress myAddress
                = manager.getCommunicator().getMyAddress();
        objectID = manager.getObjectID();

        if (location.getRemoteLocation(myAddress) == null) {
            /* The canonical place for the object is here. Request weight from
               the appropriate lifetime manager (creating one if necessary);
               we'll send it along with the object. */
            referencer = targetSystem;
            expectedLocation = location;
        } else if (location.getLocation().equals(targetSystem)) {
            referencer = myAddress;
            expectedLocation = null;
        } else {
            /* We can't do this directly; the object's owner needs to know which
               systems have GC weight for it in case some of them go wrong. */
            throw new IllegalArgumentException(
                    "A ReferenceValue cannot be constructed on a system where"
                    + " the object is not stored, unless the message's"
                    + " unmarshaller is storing the object");
        }

        if (weight != weight()) {
            throw new IllegalArgumentException(
                    "GC weight mismatch in ReferenceValue: actual "
                    + weight + " expected " + weight());
        }
    }

    /**
     * Unmarshal a reference value into a reference of the given class. This
     * operation relies on the fact that the original location manager that was
     * given when constructing this <code>ReferenceValue</code> must still be
     * alive (it's kept alive via the weight increase on the lifetime manager
     * that happens in the constructor).
     * <p>
     * Note that this method does not always return a long reference; if the
     * object referred to by the reference value can be located on the current
     * system, it will return a <i>short</i> reference to that object unless
     * <code>forceLong</code> is set.
     * <p>
     * Be aware that this method will increase the weight of the location
     * manager, to match the increase in the lifetime manager's weight. In
     * particular, it is not idempotent, and should be called exactly once on
     * each reference value, and only from its target system.
     *
     * @param forceLong Whether to <i>force</i> the return value to be a long
     * reference; if this is <code>false</code>, a short reference will be
     * returned if possible, but long references will still be used otherwise.
     * Note that in a few cases, a long reference won't have a type compatible
     * with that of the original object; use this with caution!
     * @param tryLoose Whether to attempt to loosen the existing location
     * manager for the object. This has the effect of making it less inclined to
     * automatically migrate from the recipient system, and should be the case
     * whenever the recipient system might potentially hang on to the reference
     * it's given rather than using and discarding it immediately.
     * @param objectClass The actual class of the object referred to by the
     * reference (i.e. <code>T.class</code>). In order to avoid redundant
     * information in the marshalled form of an object, this is not recorded by
     * this class itself, and so must be tracked by the caller.
     * @param communicator The distributed communicator responsible for tracking
     * the newly created reference (this is the communicator for the system on
     * which the new reference is created). If the reference is created as a
     * long reference, its location manager will be associated with this
     * communicator.
     * @return An object of a class that extends or implements <code>T</code>
     * (depending on whether <code>T</code> is a class or an interface), such
     * that methods called on the return value will call the same methods on the
     * object referenced by this <code>ReferenceValue</code>.
     */
    @SuppressWarnings("unchecked")
    T unmarshal(boolean forceLong, boolean tryLoose, Class<T> objectClass,
            DistributedCommunicator communicator) {
        CommunicationAddress myAddress
                = communicator.getMyAddress();

        LocationManager<T> reconstructedManager;

        if (expectedLocation != null && !myAddress.equals(referencer)) {
            throw new IllegalStateException("Unexpected ReferenceValue "
                    + "unmarshal: expected to unmarshal on "
                    + referencer + ", was unmarshalled on " + myAddress);
        }

        if (sentFromOwner()) {
            reconstructedManager = locationManagerForID(
                    objectID, expectedLocation, objectClass, communicator);
            /* Give the location manager this object's weight; a) it'll need
               some to get started (and ReferenceValue is where its weight comes
               from); b) the total amount of GC weight in the system must remain
               constant. */
            reconstructedManager.supplyGCWeight(weight(), expectedLocation);
        } else {
            /* Our GC weight originally came from a lifetime manager on this
               system. Balance the books by returning it to the lifetime
               manager (and while we're at it, ask the lifetime manager where
               the location manager is). */
            reconstructedManager = (LocationManager) LifetimeManager.requestOrReturnWeight(
                    new ObjectLocation(objectID, referencer), -weight(),
                    null, communicator).getLocationManager();
        }

        if (forceLong) {
            return (T) reconstructedManager.newLongReference();
        } else {
            if (tryLoose) {
                reconstructedManager.maybeLoosen();
            }
            return reconstructedManager.getBestReference();
        }
    }

    /**
     * Creates a string representation of this reference value.
     *
     * @return A string containing the object ID, expected location, and (if the
     * information is easily accessible) information on the referenced object.
     */
    @Override
    public String toString() {
        if (sentFromOwner()) {
            return "reference to " + objectID
                    + ", currently stored on " + expectedLocation.getLocation()
                    + ", will be unmarshaled on " + referencer;
        } else {
            return "reference to " + objectID
                    + ", sent back to its owner from " + referencer;
        }
    }
}
