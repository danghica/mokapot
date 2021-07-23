package xyz.acygn.mokapot;

import xyz.acygn.mokapot.markers.Copiable;
import xyz.acygn.mokapot.util.ObjectUtils;

/**
 * A reference to an object, together with a location from which that object is
 * or can be referenced. (In other words, we're not talking about "where the
 * object is", but "who to ask to find the object".) This sort of value can
 * uniquely describe a location manager globally, or a lifetime manager on an
 * individual system, and is used to specify location managers and lifetime
 * managers by reference without needing a copy of the manager itself.
 *
 * @author Alex Smith
 */
class ObjectLocation implements Copiable {

    /**
     * The identifier of the object that this location locates.
     */
    private final GlobalID objectID;

    /**
     * The system via which the object is being located.
     */
    private final CommunicationAddress locatedVia;

    /**
     * Creates an object location, specifying the fields individually.
     *
     * @param objectID The identifier of the object being located.
     * @param locatedVia The communication address of the system via which to
     * locate the object.
     */
    ObjectLocation(GlobalID objectID, CommunicationAddress locatedVia) {
        this.objectID = objectID;
        this.locatedVia = locatedVia;
    }

    /**
     * Creates an object location, describing the given location manager. The
     * object to locate will be the one referenced by the location manager, and
     * the system via which to locate it will be the system on which the
     * location manager resides (i.e. the current system, because the location
     * manager must be given via a short reference).
     *
     * @param manager The location manager which should be described by the
     * constructed object location.
     */
    ObjectLocation(LocationManager<?> manager) {
        this(manager.getObjectID(),
                manager.getCommunicator().getMyAddress());
    }

    /**
     * Gets the ID of the object being located.
     *
     * @return The ID of the object being located.
     */
    public GlobalID getObjectID() {
        return objectID;
    }

    /**
     * Gets the communication address via which to locate the object.
     *
     * @return The communication address at which to find a location manager for
     * the object.
     */
    public CommunicationAddress getLocatedVia() {
        return locatedVia;
    }

    /**
     * Returns a value that's consistent between two
     * <code>ObjectLocation</code>s that refer to the same object via the same
     * address. Two <code>ObjectLocation</code>s that refer to different
     * objects, or different locations at which to find them, will usually
     * return different hash codes (although this is not guaranteed).
     *
     * @return The hash code of this object location.
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + this.objectID.hashCode();
        hash = 67 * hash + this.locatedVia.hashCode();
        return hash;
    }

    /**
     * Value equality of this object and another object. If the other object is
     * not an <code>ObjectLocation</code>, always returns false; otherwise
     * compares the objects field by field.
     *
     * @param other The object to compare to.
     * @return <code>true</code> if this object is value-equal to
     * <code>obj</code> <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object other) {
        return ObjectUtils.equals(this, other,
                ObjectLocation::getObjectID, ObjectLocation::getLocatedVia);
    }

    /**
     * Returns a string representation of this object location.
     *
     * @return A string representation of the object location.
     */
    @Override
    public String toString() {
        return "(object with ID " + objectID
                + ", referenced via " + locatedVia + ")";
    }
}
