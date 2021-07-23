package xyz.acygn.mokapot;

import java.util.concurrent.atomic.AtomicLong;
import xyz.acygn.mokapot.util.Expirable;
import xyz.acygn.mokapot.util.ExpirableMap;

/**
 * Keeps objects referenced from other systems alive. A lifetime manager
 * basically holds a short reference on an object's location manager, in order
 * to keep the object itself alive via keeping a location manager for that
 * object alive. Lifetime managers thus advantage of the invariant that as long
 * as a location manager is alive, the referenced object will be alive. They
 * also help to implement this invariant; if a location manager references an
 * object on a remote system, it will hold a lifetime manager on that system
 * alive.
 * <p>
 * Lifetime managers, more specifically, track the amount of GC weight held by
 * other systems. The total GC weight of an object across the entire system is
 * 0; a lifetime manager represents a negative amount of weight, and is the only
 * source of negative weight. (The system hosting an object can thus generate
 * arbitrarily large amounts of GC weight for it by creating lifetime managers
 * for it, or requesting extra weight.) If a remote system is being given a
 * reference to an object, it'll be given a large amount of weight (typically
 * 2<sup>32</sup>) created this way, so that it can send messages mentioning the
 * object. Any arriving messages mentioning the object will have their weight
 * returned to the lifetime manager.
 * <p>
 * There is a 1-to-1 correspondence between lifetime managers, and (location
 * managers that reference remote systems + messages in transit that refer to a
 * location manager); the lifetime manager is on the referenced system, the
 * location manager on the referencing system (and a message in transit could be
 * anywhere). The lifetime manager is created when a message is sent referring
 * to the location manager, and deleted when the remote location manager is
 * deleted. It's kept alive by being placed in the distributed communicator's
 * keepalive pool; the actual work of creating lifetime managers, and removing
 * them from the map to let them go unreferenced, takes place in
 * <code>LocationManagerStatusMessage</code>.
 *
 * @author Alex Smith
 * @see LocationManager
 * @see LocationManagerStatusMessage
 * @see DistributedCommunicator#getAllLifetimeManagers()
 */
class LifetimeManager implements Expirable {

    /**
     * The location manager that this lifetime manager keeps alive.
     */
    private final LocationManager<?> keepAlive;

    /**
     * The negative weight stored in this lifetime manager. This is the amount
     * of weight that the lifetime manager has generated from thin air, and thus
     * that needs to be returned to it for it to stop holding the object alive.
     * <p>
     * The contract of this value is that if it ever becomes 0, the lifetime
     * manager is set to "dying" state; its weight can never be adjusted away
     * from 0, and any methods called on it (that aren't intended to kill it)
     * report the fact that it's currently dying (allowing a new lifetime
     * manager to be created in its place). In other words, a lifetime manager
     * exists only while its weight is negative.
     */
    private final AtomicLong negativeWeight;

    /**
     * Requests that this lifetime manager generates additional weight. An
     * adjustment can be negative (in which case this is effectively returning
     * weight, rather than requesting weight). The given amount of weight will
     * be supplied to the caller.
     * <p>
     * When returning weight, returning more weight than is stored in the
     * lifetime manager will cause no change to be made. This situation can
     * occur if it was assumed that some GC weight had been lost (e.g. due to a
     * keepalive timeout), and then the weight was discovered to still exist
     * after all. Note that this is nearly always an error condition, and the
     * code here is thus designed to maximise the chance of things working
     * rather than to be perfectly correct (as correctness is impossible
     * following a malfunction in other parts of the system).
     * <p>
     * If all the weight is ever returned to a lifetime manager, it will enter
     * an "expired" state, causing it to act as though it didn't exist; in this
     * state, its weight cannot subsequently be adjusted and all methods
     * (including this one) will throw an <code>ExpiredException</code>.
     *
     * @param myLocation The location of this lifetime manager (given by the
     * caller because lifetime managers don't track locations themselves).
     * @param requestedAmount The amount of weight to request. This can be
     * negative to return weight rather than requesting it.
     * @throws ExpiredException If the weight was previously 0
     */
    void requestWeight(ObjectLocation myLocation,
            long requestedAmount) throws ExpiredException {
        long oldValue = negativeWeight.getAndUpdate((v)
                -> v == 0 ? 0
                        : requestedAmount + v < 0 ? v
                                : requestedAmount + v);

        if (oldValue == 0) {
            throw ExpiredException.SINGLETON;
        }
        if (oldValue + requestedAmount == 0) {
            /* We know that the value is necessarily now 0. So we can force a
               drop safely. */
            keepAlive.getCommunicator().
                    getAllLifetimeManagers().informOfExpiry(myLocation, this);
        }
        if (oldValue + requestedAmount < 0) {
            keepAlive.getCommunicator().sendWarning("object with ID "
                    + keepAlive.getObjectID() + ": " + -requestedAmount
                    + " of GC weight returned, but only " + oldValue
                    + " was believed to still be outstanding");
            keepAlive.adjustTotalGeneratedWeight(-oldValue);
        } else {
            keepAlive.adjustTotalGeneratedWeight(requestedAmount);
        }
    }

    /**
     * Marks the weight of this lifetime manager as 0, setting it into expired
     * state. This is a low-level method that's only public in order to allow it
     * to be called via the <code>Expirable</code> interface when the lifetime
     * manager expires due to timeout.
     * <p>
     * Performing this action is equivalent to "giving up" on any references
     * from the remote system in question, allowing the object to be deallocated
     * even if the system in question still has references to it.
     *
     * @throws ExpiredException If the lifetime manager has expired already
     */
    @Override
    public void expire() throws ExpiredException {
        long oldWeight = negativeWeight.getAndSet(0);
        if (oldWeight == 0) {
            throw ExpiredException.SINGLETON;
        }
        keepAlive.adjustTotalGeneratedWeight(-oldWeight);
        keepAlive.getCommunicator().sendWarning(
                "Timeout handling object with " + keepAlive + ": " + oldWeight
                + " units of GC weight lost, object may have been deallocated early");
    }

    /**
     * Returns the location manager associated with this lifetime manager.
     * That's the local location manager that's being kept alive by the inbound
     * reference.
     *
     * @return The location manager that's being referenced via this lifetime
     * manager.
     */
    public LocationManager<?> getLocationManager() {
        return keepAlive;
    }

    /**
     * Creates a lifetime manager to keep the given location manager alive. This
     * constructor will not in its own right update the map of all lifetime
     * managers correctly; as such, the caller must do that (the lifetime
     * manager itself doesn't have enough information).
     *
     * @param keepAlive The location manager to keep alive. Should not be null.
     * @param weight The initial weight of this lifetime manager. Must be
     * positive.
     */
    LifetimeManager(LocationManager<?> keepAlive, long weight) {
        if (keepAlive == null) {
            throw new NullPointerException(
                    "lifetime manager managing null location manager");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException(
                    "lifetime manager constructed with nonpositive weight");
        }
        this.keepAlive = keepAlive;
        this.negativeWeight = new AtomicLong(weight);
        keepAlive.adjustTotalGeneratedWeight(weight);
    }

    /**
     * Requests the supply of GC weight for an object, or returns previously
     * supplied GC weight. The object in question must be local for a request to
     * be meaningful (and must have, currently or previously, been local for a
     * return to be meaningful, as weight can only be returned to the lifetime
     * manager from which it was requested).
     * <p>
     * If no lifetime manager currently exists, a new one will be created (thus
     * holding the object alive until the GC weight in question is returned). A
     * lifetime manager holding 0 weight is considered nonexistent for this
     * purpose, as it may well be in a process of being removed from indexes or
     * the like by another thread. The object's local location manager needs to
     * be provided when requesting weight (unless it's known for certain that
     * the lifetime manager already exists), as this is needed to construct the
     * lifetime manager.
     * <p>
     * A side effect of this method is that the lifetime manager's timeout will
     * be reset (the timeout measures how long since it was last used, after
     * all). It can be called with an argument of 0 purely to reset the timeout,
     * if necessary.
     *
     * @param loc The object ID and system that uniquely identify the remote
     * location manager (i.e. which object we're generating GC weight for, and
     * which system asked for it).
     * @param requestedAmount The amount of weight to request (if positive); or
     * minus the amount of weight to return (if negative). Can be 0 to keep the
     * lifetime manager from timing out without changing its state.
     * @param localManager If the lifetime manager needs to be created, this is
     * the corresponding local location manager. This can be <code>null</code>
     * if it's known that the lifetime manager already exists (e.g. when
     * returning weight).
     * @param communicator The distributed communicator on which to search for
     * pre-existing lifetime managers, and to which to attach new lifetime
     * managers. If <code>localManager</code> is given, this must be its
     * communicator.
     * @return The lifetime manager whose weight was adjusted. (This might or
     * might not have been newly created.)
     */
    static LifetimeManager requestOrReturnWeight(ObjectLocation loc,
            long requestedAmount, LocationManager<?> localManager,
            DistributedCommunicator communicator) {
        ExpirableMap<ObjectLocation, LifetimeManager> allLifetimeManagers
                = communicator.getAllLifetimeManagers();

        return allLifetimeManagers.runMethodOn(loc,
                (l) -> l.requestWeight(loc, requestedAmount),
                () -> {
                    if (localManager == null) {
                        throw new NullPointerException(
                                "cannot request/return " + requestedAmount
                                + " weight at " + loc
                                + ", location manager not specified");
                    } else if (localManager.getCommunicator() != communicator) {
                        throw new IllegalArgumentException(
                                "communicator inconsistency: " + communicator
                                + " versus " + localManager);
                    }
                    return new LifetimeManager(localManager, requestedAmount);
                });
    }
}
