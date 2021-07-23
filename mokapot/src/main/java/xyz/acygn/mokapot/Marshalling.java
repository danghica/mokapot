package xyz.acygn.mokapot;

import java.io.IOException;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import static xyz.acygn.mokapot.Authorisations.UNRESTRICTED;
import static xyz.acygn.mokapot.ClassKnowledge.knowledgeForActualClass;
import static xyz.acygn.mokapot.ClassKnowledge.knowledgeForClass;
import static xyz.acygn.mokapot.LambdaKnowledge.READ_RESOLVE_METHOD;
import static xyz.acygn.mokapot.LambdaKnowledge.SERIALIZED_LAMBDA_KNOWLEDGE;
import static xyz.acygn.mokapot.LengthIndependent.getActualClassInternal;
import static xyz.acygn.mokapot.NonCopiableKnowledge.StandinFactoryPurpose.STANDIN_WRAPPER;
import xyz.acygn.mokapot.markers.DistributedError;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper;
import static xyz.acygn.mokapot.util.StringifyUtils.stringify;
import xyz.acygn.mokapot.wireformat.ClassNameDescriptions;
import static xyz.acygn.mokapot.wireformat.ClassNameDescriptions.NULL_DESCRIPTION_INT;
import xyz.acygn.mokapot.wireformat.ObjectDescription;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * Utility class to namespace static methods that handle marshalling,
 * unmarshalling, and long references. In most cases, this consists of
 * determining what class is supposed to perform the operation, and delegating
 * to it. This class thus serves as a public entry point to otherwise
 * package-private operations like long reference creation, and to make it
 * possible to perform generic objects like marshalling on objects without
 * needing to know the precise form that that marshalling will take.
 *
 * @author Alex Smith
 */
class Marshalling implements TestHooks {

    /**
     * The distributed communicator with which to associate created long
     * references.
     */
    private final DistributedCommunicator communicator;

    /**
     * Package-private constructor. It would be a security risk to allow classes
     * outside this package access to the methods if the user hasn't chosen to
     * allow doing so (via the use of
     * <code>DistributedCommunicator#getTestHooks</code>).
     *
     * @param communicator
     */
    Marshalling(DistributedCommunicator communicator) {
        this.communicator = communicator;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T makeLongReference(T referent)
            throws IllegalArgumentException {
        if (referent == null) {
            return null;
        }

        try {
            LocationManager<T> lm = communicator
                    .findLocationManagerForObject(referent);
            return (T) lm.newLongReference();
        } catch (UnsupportedOperationException ex) {
            throw new IllegalArgumentException(
                    "Could not form long reference", ex);
        } catch (AutocloseableLockWrapper.CannotLockException ex) {
            /* The IllegalStateException is not documented: creation of test
               hooks is banned earlier in the shutdown sequence than creation
               of long references, so it can only happen due to a mistake in
               Mokapot itself. */
            throw new IllegalStateException(
                    "attempting to create a long reference as the communicator "
                    + "shuts down", ex);
        }
    }

    @Override
    public ObjectDescription describe(Object obj) {
        Class<?> actualClass = getActualClassInternal(obj);
        return describeExact(obj, actualClass);
    }

    /**
     * Static version of <code>describe</code>. Description of objects is not
     * inherently tied to a communicator. (The instance method is, however, also
     * needed because static methods cannot fulfil an interface.)
     *
     * @param obj The object to describe.
     * @return The description of the object.
     */
    static ObjectDescription describeStatic(Object obj) {
        Class<?> actualClass = getActualClassInternal(obj);
        return describeExact(obj, actualClass);
    }

    /**
     * Creates an object description from a standin's referent.
     *
     * @param <T> The actual type of the standin's referent.
     * @param standin The standin whose referent should be described.
     * @return A description of that referent, read-only and rewound to the
     * start.
     * @throws IOException If something goes wrong describing the standin
     */
    static <T> ObjectDescription describeStandin(Standin<T> standin)
            throws IOException {
        ObjectDescription od = new ObjectDescription(
                standin.descriptionSize(UNRESTRICTED));
        if (slowDebugOperationsEnabled()) {
            standin.verifiedDescribeInto(od, UNRESTRICTED);
        } else {
            standin.describeInto(od, UNRESTRICTED);
        }
        od.resetForRead();
        return od;
    }

    /**
     * Creates a description of the given object, explicitly passing its actual
     * class. Used to work around some issues with Java's type system.
     *
     * @param <T> The actual class of <code>obj</code>.
     * @param obj The object to describe. Must not be <code>null</code>. (This
     * must be verified by the caller; as this method is private and only
     * intended to be called by <code>describe</code>, duplicating the null
     * check it already does would be pointless.)
     * @param actualClass <code>T.class</code>, given explicitly due to Java's
     * type erasure rules.
     * @return A description of <code>obj</code>.
     */
    @SuppressWarnings("unchecked")
    private static <T> ObjectDescription describeExact(
            Object obj, Class<T> actualClass) {
        T o = (T) obj;
        ClassKnowledge<T> ck = knowledgeForClass(actualClass);

        if (ck instanceof NonCopiableKnowledge) {
            /* We have to use shallow description, not reference description. */
            Standin<T> oStandin;

            if (o instanceof Standin) {
                oStandin = (Standin<T>) o;
            } else {
                StandinFactory<T> sf = ck.getStandinFactory(STANDIN_WRAPPER);
                oStandin = sf.wrapObject(o);
            }

            try {
                return describeStandin(oStandin);
            } catch (IOException ex) {
                throw new DistributedError(ex, "describing object: "
                        + stringify(obj));
            }
        } else {
            /* If it's copiable, reference description and shallow description
               are identical. Use reference description, as the shallow
               description code can't handle special cases like arrays. */
            ObjectDescription.Size oSize
                    = ck.descriptionSize(() -> o, false);
            ObjectDescription rv = new ObjectDescription(oSize);
            try {
                ck.describeFieldInto(rv, o, false);
                rv.resetForRead();
                return rv;
            } catch (IOException ex) {
                throw new DistributedError(ex, "describing object: "
                        + stringify(obj));
            }
        }
    }

    @Override
    public ObjectDescription describeField(Object obj, Class<?> declaredType) {
        ClassKnowledge<?> ck = knowledgeForActualClass(obj);
        ObjectDescription.Size oSize
                = ck.descriptionSize(() -> obj, false);
        oSize = oSize.addBytes(ck.getClassNameDescription(declaredType).length);
        ObjectDescription rv = new ObjectDescription(oSize);
        try {
            rv.write(ck.getClassNameDescription(declaredType));
            ck.describeFieldInto(rv, obj, false);
            rv.resetForRead();
            return rv;
        } catch (IOException ex) {
            throw new DistributedError(ex, "describing for field: "
                    + stringify(obj));
        }
    }

    /**
     * Serialises a deeply copiable object structure into a sequence of bytes.
     * This is basically the operation that traditional Java serialisation
     * performs, except in the custom serialisation format that the rest of the
     * code uses.
     * <p>
     * You can use <code>readClassAndObject</code> to undo this operation,
     * producing a deep copy of the original object. (The two additional
     * parameters should be <code>null</code> when used for this purpose.)
     *
     * @param obj The object to serialise.
     * @return A byte array representing the object.
     * @throws IOException If something goes wrong in serialisation, or if the
     * given object is not actually deeply copiable.
     */
    byte[] describeToByteArray(Object obj) throws IOException {
        ObjectDescription description = describeField(obj, null);
        if (!description.listAllNoncopiableObjects().isEmpty()) {
            throw new IOException("Object of " + obj.getClass() + " is not deeply copiable");
        }
        ByteBuffer bb = description.readByteSlice(description.getWrittenLength());
        bb.rewind();
        byte[] rv = new byte[bb.limit()];
        bb.get(rv);
        return rv;
    }

    @Override
    public Object readClassAndObject(ReadableDescription description,
            Object parent, Class<?> expected) throws IOException {
        return rCAOStatic(description, parent, expected);
    }

    /**
     * Static version of <code>readClassAndObject</code>. (The communicator to
     * use, if one is necessary, is implied via <code>description</code>.)
     *
     * @param description The description to read a class and object from.
     * @param parent The object that will be used when the description specifies
     * a "back-reference to the parent".
     * @param expected The class that will be used when the description
     * specifies that "the class has its expected value".
     * @return An object, undescribed from the given description, of a class
     * that was also read from the description.
     * @throws IOException If the description cannot be read, or is corrupted.
     */
    static Object rCAOStatic(ReadableDescription description, Object parent,
            Class<?> expected) throws IOException {
        Class<?> objectClass;
        int length = description.readInt();
        /* See the constants in ClassNameDescriptions for explanations of the
           numbers used here. */
        if (length == NULL_DESCRIPTION_INT) {
            return null;
        } else if (length == -0x0A000001) {
            SerializedLambda sl = SERIALIZED_LAMBDA_KNOWLEDGE
                    .reproduce(description, false);
            try {
                return READ_RESOLVE_METHOD.invoke(sl);
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException ex) {
                throw new IOException(ex);
            }
        } else if (length == -0x0A000002) {
            return parent;
        } else if (length == -0x0A000003) {
            if (expected == null) {
                throw new IOException("Description encodes 'expected class', "
                        + "but there is no expected class");
            }
            objectClass = expected;
            while (Standin.class.isAssignableFrom(objectClass)) {
                objectClass = objectClass.getSuperclass();
            }
        } else if (length < 0) {
            objectClass = ClassNameDescriptions.specialCasedClass(length);
        } else {
            ByteBuffer slicedPortion = description.readByteSlice(length);
            try {
                objectClass = ClassNameDescriptions.fromByteSlice(slicedPortion);
            } catch (ClassNotFoundException ex) {
                throw new IOException(ex);
            }
        }
        return knowledgeForClass(objectClass).reproduce(
                description, false);
    }

    /**
     * Marshals a given object via a description. This is equivalent to calling
     * <code>describeField</code> on the object, and then marshalling the
     * resulting description.
     * <p>
     * Note that in the garbage-collection model used by this library, whenever
     * something is marshalled, the resulting object, and all copies of it, must
     * collectively be unmarshalled exactly once (i.e. either the returned
     * marshalled form itself is unmarshalled once and its copies aren't
     * unmarshalled at all, or the returned unmarshalled form isn't unmarshalled
     * but exactly one of its copies is, and only once). Additionally, the
     * virtual machine on which the unmarshalling occurs must be known in
     * advance.
     * <p>
     * The usual way to unmarshal a <code>MarshalledDescription</code> object
     * (such as the object returned by this function) is to read it using a
     * method such as <code>readClassAndObject(null)</code>, which will
     * unmarshal it as a side effect. (Because this function writes nothing to
     * the description other than a single class-and-object pair,
     * <code>readClassAndObject(null)</code> is therefore the exact opposite
     * operation.)
     * <p>
     * Note that the object will be returned as a standin, allowing it to be
     * written directly to an output stream without the need to create a
     * separate indirect standin. In most cases, this will be what you want.
     *
     * @param o The object to describe and marshal.
     * @param targetSystem The system where the return value, or a copy of it,
     * will eventually be unmarshalled. Can be <code>null</code> if the object
     * is deeply copiable (because in that situation, no special garbage
     * collection handling is required).
     * @return The marshalled form of the object.
     * @see
     * #readClassAndObject(xyz.acygn.mokapot.wireformat.ReadableDescription,
     * java.lang.Object, java.lang.Class)
     */
    MarshalledDescriptionStandin describeAndMarshal(
            Object o, CommunicationAddress targetSystem) {
        ObjectDescription desc = describeField(o, null);
        return new MarshalledDescriptionStandin(
                desc, communicator, targetSystem);
    }

    /**
     * Whether slow debug operations are enabled. This is a JVM-global setting.
     */
    private static boolean enableSlowDebugOperations = false;

    @Override
    public void setEnableSlowDebugOperations(boolean enableSlowDebugOperations) {
        Marshalling.enableSlowDebugOperations = enableSlowDebugOperations;
    }

    /**
     * Returns whether slow debug operations are enabled. Callers can use this
     * to determine whether to run a slow debugging version of an operation.
     * <p>
     * Note that it's possible for users to set this even with test hooks
     * disabled (via the use of a secondary communicator); thus, slow debug
     * operations should not do anything potentially insecure, or semantically
     * different from the normal version of the operation.
     *
     * @return Whether slow debug operations are enabled.
     */
    static boolean slowDebugOperationsEnabled() {
        return enableSlowDebugOperations;
    }

    @Override
    public CommunicationAddress createSecondaryCommunicator(
            String name, DebugMonitor debugMonitor) throws IOException {
        CommunicationEndpoint endpoint
                = new SecondaryEndpoint(communicator, name);
        DistributedCommunicator secondary
                = new DistributedCommunicator(endpoint, false, true);
        secondary.enableTestHooks();
        secondary.setDebugMonitor(debugMonitor);
        secondary.startCommunication();
        return secondary.getMyAddress();
    }
}
