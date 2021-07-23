package xyz.acygn.mokapot;

import java.io.DataOutput;
import java.io.IOException;
import static java.lang.reflect.Modifier.isFinal;
import static java.util.Collections.synchronizedMap;
import java.util.EnumMap;
import java.util.Map;
import static xyz.acygn.mokapot.StandinTechnique.Functionality.*;
import static xyz.acygn.mokapot.StandinTechnique.findBestStandinFactory;
import xyz.acygn.mokapot.util.DeterministicAutocloseable;
import xyz.acygn.mokapot.util.Lazy;
import static xyz.acygn.mokapot.util.StringifyUtils.stringify;
import xyz.acygn.mokapot.wireformat.DescriptionOutput;
import xyz.acygn.mokapot.wireformat.ObjectDescription.Size;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * Knowledge about a class that allows us to create long references to it. This
 * is mostly concerned with tracking the appropriate proxy classes, and is used
 * in cases where objects of the class cannot safely be copied.
 * <p>
 * If the class is <code>final</code>, we can't do this reliably; instead we
 * have to create a long reference to the superclass and hope that the exact
 * identity of the class never becomes relevant.
 *
 * @author Alex Smith
 * @param <T> The class that this class tracks knowledge about.
 */
class NonCopiableKnowledge<T> extends ClassKnowledge<T> {

    /**
     * The various purposes for which a standin factory can be created.
     */
    enum StandinFactoryPurpose {
        /**
         * As a constructor of long references for location managers to use. The
         * resulting standins will be given to user code and used to make remote
         * calls.
         */
        LONG_REFERENCE,
        /**
         * As a constructor of wrappers around existing objects. The resulting
         * standins will typically only be used by the distributed communication
         * library itself, to gain standin functionality on the object (such as
         * description).
         */
        STANDIN_WRAPPER,
        /**
         * As a constructor of replacements for existing objects. The resulting
         * standins will be given to user code and, initially, used only for
         * local calls (to the object itself or, if necessary, to a shallow copy
         * of it). Unlike with standin wrappers, it isn't vital to preserve the
         * initially constructed object (although doing so may be easier).
         */
        MIGRATION_WRAPPER,
        /**
         * As a constructor of replacements for existing objects specified via
         * an interface. This is like MIGRATION_WRAPPER except that it has few
         * requirements on the object's actual base class (because the
         * requirement to call only interface methods makes handling things like
         * private methods much easier).
         */
        INTERFACE_MIGRATION_WRAPPER,
    }

    /**
     * A cache of standin factories that have already been created. Creating
     * standin factories can be slow, so we create the factory the first time
     * it's used and store it here for the lifetime of the program.
     */
    private final Map<StandinFactoryPurpose, StandinFactory<T>> factoryCache
            = synchronizedMap(new EnumMap<>(StandinFactoryPurpose.class));

    /**
     * Creates new knowledge about how to marshal and unmarshal the given class
     * without making copies.
     *
     * @param about The class to create knowledge about.
     */
    NonCopiableKnowledge(Class<T> about) {
        super(about);
    }

    /**
     * Returns whether we know the class can be unmarshalled reliably. This is
     * based on whether the "signpost class" (i.e. class that forwards methods
     * to an actual instance of the class) could be made to extend the class
     * itself. (In some instances we have to rely on signposts that merely
     * implement as many of the same methods as possible, without extending the
     * class in question; doing so is unreliable.)
     *
     * @return Whether a fully general signpost class could be created for
     * <code>T</code>.
     */
    @Override
    public boolean unmarshalsReliably(boolean asField) {
        return !isFinal(getAbout().getModifiers());
    }

    /**
     * Always returns false. Part of our knowledge is that the class cannot be
     * safely copied; as such, we don't use copies to unmarshal.
     *
     * @return <code>false</code>.
     */
    @Override
    public boolean unmarshalsAsCopy() {
        return false;
    }

    /**
     * Always throws an exception. Marshalling noncopiable fields is always done
     * by reference, so the size is a constant "1 reference" and does not depend
     * on the object itself.
     *
     * @param value Ignored.
     * @param nullable Ignored.
     * @return Never returns.
     * @throws IllegalStateException Always
     */
    @Override
    Size descriptionSizeOfObject(T value, boolean nullable) {
        throw new IllegalStateException(
                "Inspecting a noncopiable object while describing a "
                + "reference to it");
    }

    /**
     * Appends a reference to an object of the class this knowledge is about to
     * the given description. Because the class is noncopiable, we don't make
     * any attempt to recursively describe the object, as that would copy it.
     *
     * @param description The description to append to.
     * @param fieldValue The object to append.
     * @param nullable Ignored.
     * @throws IOException If the description is out of capacity for objects
     */
    @Override
    void describeFieldInto(DescriptionOutput description, Object fieldValue,
            boolean nullable) throws IOException {
        description.writeNoncopiableObject(fieldValue);
    }

    /**
     * Reproduces this object via reading it from the description. Noncopiable
     * objects are stored as references, so we simply have to read the reference
     * back.
     *
     * @param description The description to read the object from.
     * @param nullable Ignored.
     * @return The object read from the description.
     * @throws IOException If the description is detected as being corrupted
     */
    @Override
    T reproduce(ReadableDescription description, boolean nullable)
            throws IOException {
        try {
            return description.readNoncopiableObject(getAbout());
        } catch (ClassCastException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Throws an exception. <code>writeFieldDescriptionTo</code> is only
     * intended to be used with deeply copiable objects, and this object is
     * noncopiable by definition.
     *
     * @param sink Ignored.
     * @param fieldValue Ignored.
     * @param nullable Ignored.
     * @throws UnsupportedOperationException Always
     */
    @Override
    void writeFieldDescriptionTo(DataOutput sink, Object fieldValue,
            boolean nullable) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "Cannot deeply serialise a noncopiable object: "
                + stringify(fieldValue));
    }

    /**
     * Returns a standin factory suitable for creating standins for the class
     * this knowledge is about, that have a particular purpose. This will either
     * be the class's standin factory override (if it has one), or else the
     * factory recommended by <code>StandinTechnique</code> for the purpose in
     * question.
     *
     * @param purpose The purpose for which the standins are being created. (In
     * some cases, different factories will be recommended for different
     * purposes.)
     * @return The recommended factory to use.
     */
    @Override
    StandinFactory<T> getStandinFactory(StandinFactoryPurpose purpose) {
        if (getStandinFactoryOverride() != null) {
            return getStandinFactoryOverride();
        }

        try (DeterministicAutocloseable ac = Lazy.TIME_BASE.get().pause()) {
            return factoryCache.computeIfAbsent(purpose, (p) -> {
                switch (p) {
                    case LONG_REFERENCE:
                        /* Most important for long references are the ability to
                       wrap a location manager and forward to remote objects
                       (that's what a long reference does), and to imitate the
                       original object as closely as possible. Description and
                       migration are nice but less necessary. */
                        return findBestStandinFactory(
                                getWireFormat(), WRAP_MANAGED, REMOTE_STORAGE,
                                CALL_INTERFACE, VITAL, CALL_SUPERCLASS, CALL_DIRECT,
                                STORE_IN_OWN_CLASS, DESCRIBE, MIGRATION);
                    case STANDIN_WRAPPER:
                        /* Most important for standin wrappers are the ability to
                       wrap an existing object (which serves as a local
                       referent), and to describe (a common use for them). The
                       other common use is inside a location manager for a local
                       object, so the relevant functionalities for that come
                       next. */
                        return findBestStandinFactory(
                                getWireFormat(), WRAP_EXISTING, LOCAL_STORAGE,
                                DESCRIBE, VITAL, WRAP_MANAGED, REMOTE_STORAGE,
                                CALL_INTERFACE, CALL_SUPERCLASS, CALL_DIRECT,
                                STORE_IN_OWN_CLASS, MIGRATION);
                    case MIGRATION_WRAPPER:
                        /* Most important for migration wrappers are the abilties to
                       migrate (obviously) and to have the correct class
                       (because they're being used by user code in place of a
                       local object). Description is also required for migration
                       (thus listed early). Both local and remote storage are
                       required to migrate (it changes back and forth), and
                       migration requires a location manager. The ability to
                       wrap existing objects is nice but not necessary, and thus
                       is listed last (we can work around this by copying the
                       object instead). */
                        return findBestStandinFactory(
                                getWireFormat(), MIGRATION, STORE_IN_OWN_CLASS,
                                CALL_DIRECT, DESCRIBE, CALL_INTERFACE, VITAL,
                                CALL_SUPERCLASS, LOCAL_STORAGE, REMOTE_STORAGE,
                                WRAP_MANAGED, WRAP_EXISTING);
                    case INTERFACE_MIGRATION_WRAPPER:
                        /* This is an identical functionality list to a regular
                       migration wrapper, except that calls other than interface
                       calls are not required. (This substantially increases the
                       list of standin factories that could work.) */
                        return findBestStandinFactory(
                                getWireFormat(), MIGRATION, DESCRIBE,
                                CALL_INTERFACE, LOCAL_STORAGE, REMOTE_STORAGE,
                                WRAP_MANAGED, WRAP_EXISTING);
                    default:
                        throw new IllegalArgumentException(
                                "Unknown standin factory purpose " + p);
                }
            });
        }
    }
}
