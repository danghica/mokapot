package xyz.acygn.mokapot;

import java.io.IOException;
import java.util.List;
import static xyz.acygn.mokapot.ReferenceValue.newReferenceValueOf;
import xyz.acygn.mokapot.markers.Copiable;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper;
import xyz.acygn.mokapot.util.DataByteBuffer;
import static xyz.acygn.mokapot.util.StringifyUtils.stringify;
import xyz.acygn.mokapot.wireformat.ObjectDescription;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * The marshalled form of an object description. Unlike an object description
 * itself, this is equivalent to an inert sequence of bytes and thus can be
 * safely sent over a network.
 * <p>
 * This unmarshals "as you read it", i.e. fully reading it from the start to end
 * will cause it to be unmarshalled in the process; because an object and all
 * copies of it must collectively always be unmarshalled exactly once, this
 * implies in turn that the marshalled description and its copies must be
 * collectively read exactly once.
 *
 * @author Alex Smith
 */
class MarshalledDescription extends DataByteBuffer
        implements ReadableDescription, Copiable {

    /**
     * The marshalled forms of the noncopiable objects referred to by the
     * description.
     */
    private final ReferenceValue<?>[] marshalledNoncopiableObjects;

    /**
     * The next element of <code>marshalledNoncopiableObjects</code> to be read.
     */
    private int objectsCursor;

    /**
     * The distributed communicator on which the unmarshalling takes place.
     */
    private transient DistributedCommunicator unmarshalCommunicator;

    /**
     * Whether the noncopiable references within this description are known to
     * be likely to quickly die on the recipient, rather than being retained. If
     * this is set to <code>true</code>, the unmarshalling process will not
     * discourage automatic migration away from the recipient machine.
     */
    private transient boolean temporaryOnRecipient;

    /**
     * Gets at the array of marshalled noncopiable objects. Used for serialising
     * this class without needing to use reflection.
     *
     * @return The array of marshalled noncopiable objects.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    protected ReferenceValue<?>[] getMarshalledNoncopiableObjects() {
        return marshalledNoncopiableObjects;
    }

    /**
     * Sets the distributed communicator that will be used to track long
     * references created when unmarshalling this description.
     *
     * @param unmarshalCommunicator The distributed communicator to use.
     */
    void setUnmarshalCommunicator(
            DistributedCommunicator unmarshalCommunicator) {
        this.unmarshalCommunicator = unmarshalCommunicator;
    }

    /**
     * Specifies whether migratable objects contained within this description
     * are likely to quickly die on the recipient system. This information is
     * used to determine whether to discourage the objects mentioned from
     * migrating away from the recipient system; for example, a
     * <code>MigrationActionsMessage</code> should probably not interfere with
     * automatic migrations. Note that if there's any realistic chance that the
     * recipient system might hold onto a pointer available via this marshalled
     * description, this should be <code>false</code>, as otherwise an object
     * might look like a good candidate for automatic migration while in the
     * middle of a method call.
     * <p>
     * This method can only be usefully called on the recipient, because the
     * value of the temporary-on-recipient field is not retained on
     * serialisation.
     *
     * @param temporaryOnRecipient <code>true</code> if the noncopiable
     * references within this description should not be used to discourage
     * automatic migrations of the objects they describe away from the recipient
     * system
     */
    public void setTemporaryOnRecipient(boolean temporaryOnRecipient) {
        this.temporaryOnRecipient = temporaryOnRecipient;
    }

    /**
     * Creates a marshalled description by marshalling the given object
     * description.
     *
     * @param description The description to marshal. This should be in a state
     * that's reset and ready to read, and will be reset after reading.
     * @param sourceCommunicator The distributed communicator for the system on
     * which this description is originally being marshalled. This must be given
     * so that the communicator in question can be told to track the marshalled
     * objects, in case the target system later needs to refer to them. Can be
     * <code>null</code> in the case that the description in question is of a
     * deeply copiable object (and thus has no noncopiable objects to track).
     * @param targetSystem The system on which this description (or a copy of
     * it) will eventually be unmarshalled. (As usual for marshalling, anything
     * that is marshalled must be unmarshalled exactly once, on a system known
     * when the marshalling took place.) Can be <code>null</code> in the case
     * that the description in question is of a deeply copiable object (and thus
     * does not need special handling from the garbage collector).
     * @throws IllegalArgumentException If <code>targetSystem</code> is null,
     * but the description contains embedded noncopiable objects
     * @throws IllegalStateException If the marshalling operation would create a
     * new long reference at a point in the shutdown sequence where those are
     * not allowed
     */
    @SuppressWarnings("null")
    MarshalledDescription(ObjectDescription description,
            DistributedCommunicator sourceCommunicator,
            CommunicationAddress targetSystem) throws IllegalArgumentException {
        /* Note: this a clone constructor that will copy the noncopiable portion
           of the given description into this description; the cast is
           technically unnecessary (unless DataByteBuffer gets more
           constructors) but makes it clear that only a portion of the object is
           copied. */
        super((DataByteBuffer) description);
        List<?> noncopiableObjects = description.listAllNoncopiableObjects();
        if ((targetSystem == null || sourceCommunicator == null)
                && !noncopiableObjects.isEmpty()) {
            throw new IllegalArgumentException("Attempted to marshal an object "
                    + "as deeply copiable, but it contains an embedded object: "
                    + stringify(noncopiableObjects.get(0)));
        }
        marshalledNoncopiableObjects = noncopiableObjects.stream().
                map((o) -> {
                    ReferenceValue<?> rv;
                    try {
                        rv = newReferenceValueOf(sourceCommunicator
                                .findLocationManagerForObject(o), targetSystem);
                    } catch (AutocloseableLockWrapper.CannotLockException ex) {
                        throw new IllegalStateException(
                                "Trying to create a cross-system long "
                                + "reference in a shutdown stage that does "
                                + "not allow them");
                    }
                    return rv;
                }
                ).toArray(ReferenceValue[]::new);
        objectsCursor = 0;
    }

    /**
     * Creates a marshalled description from its individual fields. This is used
     * when deserialising a marshalled description.
     *
     * @param marshalledNoncopiableObjects The marshalled form of the
     * noncopiable objects.
     * @param byteBuffer The marshalled form of the copiable objects.
     * @param objectsCursor The cursor within the noncopiable objects.
     */
    protected MarshalledDescription(
            ReferenceValue<?>[] marshalledNoncopiableObjects,
            DataByteBuffer byteBuffer, int objectsCursor) {
        super(byteBuffer);
        this.marshalledNoncopiableObjects = marshalledNoncopiableObjects;
        this.objectsCursor = objectsCursor;
    }

    @Override
    public <T> T readNoncopiableObject(Class<T> asClass) throws IOException {
        if (unmarshalCommunicator == null) {
            throw new IllegalStateException(
                    "When this MarshalledDescription was deserialised, it "
                    + "should have had unmarshalCommunicator set");
        }
        try {
            @SuppressWarnings("unchecked")
            ReferenceValue<T> referenceValue
                    = (ReferenceValue<T>) marshalledNoncopiableObjects[objectsCursor];
            objectsCursor++;
            return referenceValue.unmarshal(false, !temporaryOnRecipient,
                    asClass, unmarshalCommunicator);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IOException(e);
        }
    }

    /**
     * Throws an exception. Reading a marshalled description via the regular
     * read/write methods unmarshals it, and an object (and its copies) can only
     * be unmarshalled once. This therefore serves as a safety valve to catch
     * situations where a single description is unmarshalled twice.
     * (Unfortunately, it can't detect situations in which both a marshalled
     * description, and a copy of that marshalled description, are
     * unmarshalled.)
     *
     * @throws IllegalStateException Always
     */
    @Override
    public void resetForRead() throws IllegalStateException {
        throw new IllegalStateException(
                "A description cannot be unmarshalled twice");
    }

    /**
     * Checks to see whether no data has been read from this buffer.
     *
     * @return <code>false</code> if any reads have been made from this buffer;
     * <code>true</code> if it's untouched
     */
    @Override
    public boolean isRewound() {
        return super.isRewound() && objectsCursor == 0;
    }
}
