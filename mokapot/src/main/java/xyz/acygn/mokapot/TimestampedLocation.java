package xyz.acygn.mokapot;

import java.util.Objects;
import xyz.acygn.mokapot.markers.Copiable;
import xyz.acygn.mokapot.util.ObjectUtils;

/**
 * Where an object is, and how many times it's been migrated. This is used to
 * detect and handle chain-migration; if an object is migrated multiple times
 * there may be various claims for its current location in the various network
 * messages. In this case, it's important to know which claim is the newest, in
 * order to prevent forwarding loops (the idea is to only forward a request for
 * an object to a newer location for that object, never an older location for
 * that object).
 * <p>
 * Timestamped locations are immutable, constant objects, and act as though they
 * were value types.
 *
 * @author Alex Smith
 */
class TimestampedLocation implements Copiable, Comparable<TimestampedLocation> {

    /**
     * The location at which the object is believed to be. Cannot be
     * <code>null</code>.
     */
    private final CommunicationAddress location;

    /**
     * The number of times the object had migrated before it reached
     * <code>location</code>. Cannot be negative.
     */
    private final int migrateCount;

    /**
     * Creates a new timestamped location object.
     *
     * @param location The location of the object, after the given number of
     * migrations.
     * @param migrateCount The number of times the object has been migrated.
     * @throws NullPointerException If <code>location</code> is
     * <code>null</code>
     * @throws IllegalArgumentException If <code>migrateCount</code> is negative
     */
    TimestampedLocation(CommunicationAddress location, int migrateCount) throws
            NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(location);
        if (migrateCount < 0) {
            throw new IllegalArgumentException("negative migrateCount");
        }
        this.location = location;
        this.migrateCount = migrateCount;
    }

    /**
     * Returns the object's location at the time recorded in this
     * <code>TimestampedLocation</code> object.
     *
     * @return The object's location.
     */
    public CommunicationAddress getLocation() {
        return location;
    }

    /**
     * Return's the object's location, or <code>null</code> if the object is at
     * the given location. In other words this is <code>getLocation()</code>
     * except that local locations will be described as <code>null</code>.
     *
     * @param localAddress The address which is considered "local", and thus
     * will cause a <code>null</code> return if the object is located there.
     * @return The object's location, or <code>null</code>.
     */
    public CommunicationAddress getRemoteLocation(
            CommunicationAddress localAddress) {
        if (localAddress.equals(location)) {
            return null;
        } else {
            return location;
        }
    }

    /**
     * Returns the number of migrations by which the object reached the recorded
     * location. This makes it possible to determine which location is newer.
     *
     * @return The number of migrations.
     */
    public int getMigrateCount() {
        return migrateCount;
    }

    /**
     * Returns the timestamped location an object would be in after migrating
     * from this timestamped location.
     *
     * @param newAddress The address the object would migrate to.
     * @return A timestamped location with a higher migration account and
     * <code>newAddress</code> as the location.
     */
    public TimestampedLocation migrated(CommunicationAddress newAddress) {
        return new TimestampedLocation(newAddress, migrateCount + 1);
    }

    /**
     * Calculates a number such that equal timestamped locations have equal hash
     * codes. It's possible that unequal timestamped locations can also have
     * equal hash codes, but this event should have low probability.
     *
     * @return The hash code of this timestamped location.
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.location);
        hash = 59 * hash + this.migrateCount;
        return hash;
    }

    /**
     * Checks whether a given object is a timestamped location with the same
     * location and migrate count.
     *
     * @param other The object to compare to.
     * @return <code>true</code> if <code>other</code> has the same class,
     * location and migrate count as this object.
     */
    @Override
    public boolean equals(Object other) {
        return ObjectUtils.equals(this, other,
                TimestampedLocation::getLocation,
                TimestampedLocation::getMigrateCount);
    }

    /**
     * Compares the migration count with that of the given timestamped location.
     *
     * @param other The timestamped location to compare to.
     * @return A positive number if this location is newer; a negative number if
     * this location is older; zero if the locations have the same age.
     */
    @Override
    public int compareTo(TimestampedLocation other) {
        return migrateCount - other.migrateCount;
    }
}
