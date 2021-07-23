package xyz.acygn.mokapot;

import java.time.Duration;

/**
 * A message that informs the recipient of changes in status of a location
 * manager that references the recipient. The normal behaviour is that
 * construction of a location manager does not need to be communicated over the
 * network (because a lifetime manager is created for messages "in transit" and
 * the location manager has the same effect on the lifetime manager as the
 * message itself would), but the destruction of a location manager does.
 * <p>
 * This message is designed to work asynchronously, and informs a lifetime
 * manager of the return of borrowed garbage collection weight (via specifying a
 * negative adjustment). Typically this would happen when a location manager is
 * destroyed, but could also happen if (due to migrations) a location manager
 * switches to a different source of GC weight.
 * <p>
 * This can also be used to perform a no-op operation on a location manager,
 * using a weight adjustment of 0, to prevent it from timing out and expiring.
 *
 * @author Alex Smith
 */
class LocationManagerStatusMessage extends AsynchronousMessage {

    /**
     * Information that uniquely references the location manager whose status
     * changed. This is a combination of the object's ID, and the system on
     * which the referencing location manager resides. The status message is
     * sent to the referenced location manager, i.e. the location manager on the
     * system containing the believed location of the object, with the same ID.
     */
    private final ObjectLocation objectLocation;

    /**
     * The amount of change to the corresponding lifetime manager's weight.
     */
    private final long adjustment;

    /**
     * Constructor for the inbound long reference status update message. Simply
     * sets the information within the message, based on a specified location
     * manager and the way in which its status was changed; the message will not
     * do anything until <code>run()</code> is called (presumably on the system
     * that the location manager references).
     *
     * @param manager The location manager whose status changed.
     * @param adjustment The amount of weight to adjust the lifetime manager's
     * weight by. In most cases, this will be a negative number; it cannot be
     * positive unless it's known for certain that the lifetime manager already
     * exists.
     * @see #process()
     */
    LocationManagerStatusMessage(LocationManager<?> manager, long adjustment) {
        this.objectLocation = new ObjectLocation(manager);
        this.adjustment = adjustment;
    }

    /**
     * Constructor for the inbound long reference status update message. Simply
     * sets the information within the message, based on a deallocated location
     * manager and the way in which its status was changed; the message will not
     * do anything until <code>run()</code> is called (presumably on the system
     * that the location manager references).
     *
     * @param managedObjectID The object ID of the object managed by the
     * location manager that just got deallocated.
     * @param myAddress The communication address of the message's sender (i.e.
     * the location where the deallocated/kept-alive location manager resides).
     * @param adjustment The amount of weight to adjust the lifetime manager's
     * weight by. This must not be a positive number, as this message is
     * designed to run without locking; a request for GC weight (i.e. a positive
     * adjustment) might have to be postponed until a migration is complete,
     * whereas a negative adjustment can be done immediately. This could be 0,
     * for a keep-alive message.
     * @see #process()
     */
    LocationManagerStatusMessage(GlobalID managedObjectID,
            CommunicationAddress myAddress, long adjustment) {
        this.objectLocation = new ObjectLocation(managedObjectID, myAddress);
        this.adjustment = adjustment;
        if (adjustment > 0) {
            throw new IllegalArgumentException(
                    "attempt to borrow GC weight without locking");
        }
    }

    /**
     * Adjust the lifetime manager's weight appropriately. If this reduces its
     * weight to 0, it will be destroyed.
     *
     * @param communicator The communicator on which to look for lifetime
     * managers.
     */
    @Override
    public void process(DistributedCommunicator communicator) {
        LifetimeManager.requestOrReturnWeight(objectLocation, adjustment,
                null, communicator);
    }

    /**
     * Produces a human-readable string describing this location manager status
     * message.
     *
     * @return A human-readable version of the message.
     */
    @Override
    public String toString() {
        return "location manager (for " + objectLocation.getObjectID()
                + " on " + objectLocation.getLocatedVia()
                + "): weight changed by " + adjustment;
    }

    /**
     * Always returns true. Although fairly complex, this message is incapable
     * of blocking and should run quite quickly.
     *
     * @return <code>true</code>.
     */
    @Override
    public boolean lightweightSafe() {
        return true;
    }

    @Override
    public Duration periodic() {
        return adjustment == 0
                ? Duration.ofSeconds(DistributedCommunicator.LIFETIME_TIMEOUT / 2)
                : null;
    }
}
