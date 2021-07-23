package xyz.acygn.mokapot;

import java.time.Duration;
import static xyz.acygn.mokapot.Authorisations.UNRESTRICTED;
import static xyz.acygn.mokapot.LocationManager.locationManagerForID;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.wireformat.ObjectDescription;

/**
 * Message that migrates an object from the sender to the recipient. This should
 * only be sent while the migration write lock for the object is held by both
 * systems (via use of <code>MigrationSynchronisationMessage</code>).
 *
 * @author Alex Smith
 * @param <T> The actual class of the object.
 */
class MigrationMessage<T> extends SynchronousMessage<Void> {

    /**
     * Creates a new migration message.
     *
     * @param migratedObject The ID and original location of the object to
     * migrate.
     * @param expectedClass The actual class of the migrating object.
     * @param description The semi-serialised fields of the object to migrate.
     * @param initialWeight The amount of weight that, after the migration, the
     * migrated-from system will be borrowing from the migrated-to system.
     * @param preparedMigration Whether the object was in a migration-prepared
     * state (i.e. <code>MigrationActions#migratePrepare</code>) at the time at
     * which the migration occurred. (For an automatic migration, it might not
     * have been.)
     * @param newTimestamp The timestamp of the object post-migration.
     */
    MigrationMessage(ObjectLocation migratedObject, Class<T> expectedClass,
            ObjectDescription description, long initialWeight,
            boolean preparedMigration, int newTimestamp) {
        this.migratedObject = migratedObject;
        this.expectedClass = expectedClass;
        this.description = description;
        this.initialWeight = initialWeight;
        this.preparedMigration = preparedMigration;
        this.newTimestamp = newTimestamp;
    }

    /**
     * The object being migrated. This is specified as an ID, not as the object
     * itself (which would become <code>ReferenceValue</code> in transit), as we
     * specifically don't want GC behaviour here; the GC fields are being held
     * steady for the duration of the migration, so we don't want to do anything
     * that would change that.
     */
    private final ObjectLocation migratedObject;
    // TODO: What if the object contains an indirect reference to itself?
    // Would that cause the consumption of GC weight?

    /**
     * The timestamp of the object after migration.
     */
    private final int newTimestamp;

    /**
     * The actual class of the migrating object.
     */
    private final Class<T> expectedClass;

    /**
     * The description of the object being migrated.
     */
    // TODO: This needs marshalling in a different way from at the moment; as it
    // is, it's (understandably) being sent as non-copiable, meaning we need a
    // network round-trip for each field of the migrated object.
    private final ObjectDescription description;

    /**
     * The amount of GC weight which, after the migration, the migrated-from
     * system will need to be borrowing from the migrated-to system.
     */
    private final long initialWeight;

    /**
     * Whether the object should be in migrate-prepared state after the
     * migration.
     */
    private final boolean preparedMigration;

    @Override
    protected Void calculateReply() throws Throwable {
        LocationManager<T> manager = locationManagerForID(
                migratedObject.getObjectID(), null, expectedClass,
                getCommunicator());

        manager.setLocalObjectTimestamp(newTimestamp);
        LifetimeManager.requestOrReturnWeight(
                migratedObject, initialWeight, manager,
                manager.getCommunicator());
        Standin<T> standin = manager.getLocalStandin();
        standin.replaceWithReproduction(expectedClass,
                description, UNRESTRICTED);
        if (!preparedMigration) {
            MigrationActions.setStoragetoTight(manager, standin);
            manager.maybeLoosen();
        }

        return null;
    }

    /**
     * Produces a debug representation of this object.
     *
     * @return A string containing the fields of the migration message.
     */
    @Override
    public String toString() {
        return "migrating " + migratedObject.getObjectID() + " of "
                + expectedClass + " from "
                + migratedObject.getLocatedVia() + " with timestamp "
                + newTimestamp + " and GC weight " + initialWeight
                + (preparedMigration ? ", to prepared state, "
                        : ", to normal offline state, ")
                + description;

    }

    @Override
    public Duration periodic() {
        return null;
    }
}
