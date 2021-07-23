package xyz.acygn.mokapot;

import java.io.DataOutput;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import static xyz.acygn.mokapot.Authorisations.UNRESTRICTED;
import static xyz.acygn.mokapot.StandinTechnique.Functionality.*;
import static xyz.acygn.mokapot.StandinTechnique.findBestStandinFactory;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.util.DeterministicAutocloseable;
import xyz.acygn.mokapot.util.Lazy;
import xyz.acygn.mokapot.wireformat.DescriptionOutput;
import xyz.acygn.mokapot.wireformat.ObjectDescription;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * Knowledge about how to make a copy of objects of a given class. The resulting
 * copy is, in marshalled form, an <code>ObjectCopy</code> object. It becomes a
 * copy of the original object upon being unmarshalled.
 * <p>
 * If an object of the class <code>CopiableKnowledge</code> itself is created,
 * that represents that the class that object is about can be copied
 * <i>reliably</i>. This does not apply to subclasses (that override
 * <code>unmarshalsReliably</code>); see
 * <code>UnreliablyCopiableKnowledge</code> in particular.
 *
 * @author Alex Smith
 * @param <T> The class that this knowledge is about.
 * @see UnreliablyCopiableKnowledge
 */
class CopiableKnowledge<T> extends ClassKnowledge<T> {

    /**
     * A standin factory used to manufacture standins used to provide standin
     * functionality on existing objects.
     * <p>
     * Lazily initialised, in order to avoid an infinite mutual recursion
     * between this class and <code>RuntimeStandinGeneration</code>.
     */
    private final AtomicReference<StandinFactory<T>> defaultIndirectStandinFactory
            = new AtomicReference<>();

    /**
     * Creates new knowledge about how to copy a class.
     *
     * @param about The class that this knowledge is about.
     */
    CopiableKnowledge(Class<T> about) {
        super(about);
    }

    /**
     * Always returns true. This method should be overriden in subclasses where
     * this is not the case, but unmarshalling reliably is the default for
     * <code>Copiable</code> and effectively copiable objects.
     *
     * @return <code>true</code>
     */
    @Override
    public boolean unmarshalsReliably(boolean asField) {
        return true;
    }

    /**
     * Always returns true. The whole point of this class is to use copy-based
     * unmarshalling.
     *
     * @return <code>true</code>.
     */
    @Override
    public boolean unmarshalsAsCopy() {
        return true;
    }

    /**
     * Calculates the size of the object description of an object whose actual
     * type is the class this knowledge is about. This is implemented indirectly
     * using a temporary composition-based standin, but can be overridden for
     * higher performance.
     *
     * @param value The object whose size should be calculated.
     * @param nullable If true, adds an extra byte to the return size to account
     * for the "is null" flag byte.
     * @return The size of a description of an object of the class this
     * knowledge is about.
     */
    @Override
    ObjectDescription.Size descriptionSizeOfObject(
            T value, boolean nullable) {
        if (value == null) {
            return new ObjectDescription.Size(1, 0);
        }
        Standin<?> standin = (value instanceof Standin) ? (Standin) value
                : getIndirectStandinFactory().wrapObject(value);

        return standin.descriptionSize(UNRESTRICTED).
                addBytes(nullable ? 1 : 0);
    }

    /**
     * Adds a description of an object to an existing description. As with
     * <code>descriptionSize</code>, this is implemented indirectly via use of a
     * temporary composition-based standin.
     *
     * @param description The description to append to.
     * @param fieldValue The object to describe.
     * @param nullable If <code>true</code>, uses an extra byte to record
     * whether the value of the object is <code>null</code>
     * @throws IOException If something goes wrong writing the description
     */
    @Override
    void describeFieldInto(DescriptionOutput description, Object fieldValue,
            boolean nullable) throws IOException {
        if (nullable) {
            description.writeBoolean(fieldValue != null);
            if (fieldValue == null) {
                return;
            }
        }
        Standin<?> standin = (fieldValue instanceof Standin)
                ? (Standin) fieldValue : getIndirectStandinFactory()
                        .wrapObject(getAbout().cast(fieldValue));

        if (Marshalling.slowDebugOperationsEnabled()) {
            standin.verifiedDescribeInto(description, UNRESTRICTED);
        } else {
            standin.describeInto(description, UNRESTRICTED);
        }
    }

    /**
     * Writes a description of an object to a data output sink. As with
     * <code>describeFieldInto</code>, this is implemented indirectly via use of
     * a temporary composition-based standin.
     *
     * @param sink The description to append to.
     * @param fieldValue The object to describe.
     * @param nullable If <code>true</code>, uses an extra byte to record
     * whether the value of the object is <code>null</code>
     * @throws IOException If something goes wrong writing the description
     */
    @Override
    void writeFieldDescriptionTo(DataOutput sink, Object fieldValue,
            boolean nullable) throws IOException {
        if (nullable) {
            sink.writeBoolean(fieldValue != null);
            if (fieldValue == null) {
                return;
            }
        }
        Standin<?> standin = (fieldValue instanceof Standin)
                ? (Standin) fieldValue : getIndirectStandinFactory()
                        .wrapObject(getAbout().cast(fieldValue));

        standin.writeTo(sink, UNRESTRICTED);
    }

    @Override
    T reproduce(ReadableDescription description, boolean nullable)
            throws IOException {
        if (nullable) {
            if (!description.readBoolean()) {
                return null;
            }
        }

        Standin<T> standin
                = getIndirectStandinFactory().newFromDescription(description);
        return standin.getReferent(UNRESTRICTED);
    }

    /**
     * Determines which factory to use for creating standins that wrap an
     * existing object of the class this knowledge is about.
     *
     * @return The standin factory; either the default, or the override.
     */
    private StandinFactory<T> getIndirectStandinFactory() {
        StandinFactory<T> override = getStandinFactoryOverride();
        if (override != null) {
            return override;
        }

        /* Functionalities required: DESCRIBE and WRAP_EXISTING are the only
           important ones here, as we're just creating temporary standins to
           send/receive objects on the network. */
        StandinFactory<T> sf = defaultIndirectStandinFactory.get();
        if (sf != null) {
            return sf;
        }

        try (DeterministicAutocloseable ac = Lazy.TIME_BASE.get().pause()) {
            sf = findBestStandinFactory(
                    getWireFormat(), DESCRIBE, WRAP_EXISTING, VITAL);
        }
        if (defaultIndirectStandinFactory.compareAndSet(null, sf)) {
            return sf;
        } else {
            return defaultIndirectStandinFactory.get();
        }
    }
}
