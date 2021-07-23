package xyz.acygn.mokapot;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isFinal;
import java.lang.reflect.UndeclaredThrowableException;
import static java.security.AccessController.doPrivileged;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import static xyz.acygn.mokapot.LengthIndependent.getActualClassInternal;
import xyz.acygn.mokapot.markers.DistributedError;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.util.DeterministicAutocloseable;
import xyz.acygn.mokapot.util.Lazy;
import static xyz.acygn.mokapot.util.TypeSafe.classCast;
import xyz.acygn.mokapot.wireformat.ClassNameDescriptions;
import static xyz.acygn.mokapot.wireformat.ClassNameDescriptions.EXPECTED_DESCRIPTION;
import xyz.acygn.mokapot.wireformat.DescriptionOutput;
import xyz.acygn.mokapot.wireformat.MethodCodes;
import static xyz.acygn.mokapot.wireformat.MethodCodes.defaultMethodCode;
import xyz.acygn.mokapot.wireformat.ObjectDescription;
import xyz.acygn.mokapot.wireformat.ObjectWireFormat;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * Information used in marshalling a given class. This includes cached results
 * of static analysis of the class, and information used in marshalling the
 * class (such as factory methods for objects of that class).
 * <p>
 * This information is stored in the form of methods that actually perform
 * marshalling from and unmarshalling into the class in question.
 *
 * @author Alex Smith
 * @param <T> The class which this class knowledge is about.
 */
abstract class ClassKnowledge<T> {

    /**
     * Lookup object for generating method handles with the access rights of
     * this class. Used to bypass security checks on method handle creation.
     */
    static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    /**
     * Produces the method code for the given method and actual class. If a
     * table of method codes already exists for the class (TODO: this is
     * currently unimplemented), that table will be used. Otherwise, the class's
     * knowledge will be consulted for the salt to use with that class, and the
     * default method code will be returned.
     *
     * @param m The method to obtain the code of.
     * @param c The actual class which namespaces the code.
     * @return The code.
     */
    static long methodCode(Method m, Class<?> c) {
        return defaultMethodCode(m, knowledgeForClass(c).getMethodCodeSalt());
    }

    /**
     * An object method database for <code>about</code>, with security checks
     * already performed. Most commonly used by serialisation/deserialisation to
     * write to the fields of the object directly; also used by the constructor
     * to produce method handles.
     */
    private final ObjectWireFormat<T> owf;

    /**
     * A map from method codes to method handles for an actual class that this
     * knowledge is about. Enables quick lookup of a method, given a code that
     * was, e.g., received over the network.
     */
    private final Map<Long, MethodHandle> methodMap;

    /**
     * The way the name of the class this knowledge is about appears in an
     * object description.
     *
     * @see #getClassNameDescription(java.lang.Class)
     */
    private final byte[] classNameDescription;

    /**
     * An override allowing a different standin factory to be used to create
     * standins for the class this knowledge is about.
     *
     * @see #setStandinFactoryOverride(xyz.acygn.mokapot.StandinFactory)
     */
    private StandinFactory<T> standinFactoryOverride = null;

    /**
     * Protected constructor. In addition to the behaviour of
     * <code>ObjectMethodDatabase</code>'s constructor, also creates a method
     * handle for each method, and a description of the class name.
     *
     * @param about The class that this knowledge is about.
     */
    protected ClassKnowledge(Class<T> about) {
        owf = doPrivileged((PrivilegedAction<ObjectWireFormat<T>>) () -> {
            ObjectWireFormat<T> rv = new ObjectWireFormat<>(about);
            rv.setAccessible();
            return rv;
        });

        methodMap = new HashMap<>(owf.getMethods().size());
        owf.getMethods().forEach((m) -> {
            long code = defaultMethodCode(m,
                    owf.getMethodCodeSalt());
            try {
                methodMap.put(code, LOOKUP.unreflect(m));
            } catch (IllegalAccessException ex) {
                throw new SecurityException(ex);
            }
        });
        classNameDescription = ClassNameDescriptions.describe(about);
    }

    /**
     * Returns the class that this knowledge is about.
     *
     * @return The class that the knowledge is about.
     */
    Class<T> getAbout() {
        return owf.getAbout();
    }

    /**
     * Returns the method code salt used by the class this knowledge is about.
     *
     * @return The method code salt.
     * @see ObjectWireFormat#getMethodCodeSalt()
     */
    final int getMethodCodeSalt() {
        return owf.getMethodCodeSalt();
    }

    /**
     * Returns a list of fields of objects whose actual type is the class that
     * this object is about. All security checks required to access the fields
     * will have been performed already. The order is consistent with that of
     * <code>ObjectMethodDatabase#getInstanceFieldList</code>.
     *
     * @return An immutable list of fields.
     */
    Iterable<Field> getInstanceFieldList() {
        return owf.getInstanceFieldList();
    }

    /**
     * Returns a list of methods of this class and its superclasses. All
     * security checks required to invoke the methods will have been called
     * already.
     *
     * @return An immutable set of methods.
     */
    Set<Method> getMethods() {
        return owf.getMethods();
    }

    /**
     * Finds the method with the given code that can be called on an object
     * whose actual class is the class this knowledge is about. The method will
     * already have had security checks overridden, allowing it to be invoked
     * via reflection even if it's private.
     *
     * @param code The method code of the desired method.
     * @return The method with the given code.
     * @throws NoSuchMethodException If no method has the given code
     */
    MethodHandle getMethodByCode(long code) throws NoSuchMethodException {
        return methodMap.get(code);
    }

    /**
     * Overrides the default selection of a standin factory for construction of
     * standins for this class. Would typically be called immediately after
     * creating the class knowledge, in cases where a custom pre-generated
     * standin class is available (to save having to generate a new one, or so
     * that the code works in cases where the default generated standin class
     * wouldn't).
     *
     * @param standinFactoryOverride The standin factory to use in preference to
     * standin factories located by default means, or <code>null</code> to
     * return to the default.
     */
    void setStandinFactoryOverride(StandinFactory<T> standinFactoryOverride) {
        this.standinFactoryOverride = standinFactoryOverride;
    }

    /**
     * Returns any override that exists for the standin factory for use with the
     * class this knowledge is about. There may not be an override, in which
     * case the return value will be <code>null</code>, recommending the use of
     * a default.
     *
     * @return The standin factory override, or <code>null</code> if there is no
     * such override.
     */
    StandinFactory<T> getStandinFactoryOverride() {
        return standinFactoryOverride;
    }

    /**
     * Returns the standin factory for use with the class this knowledge is
     * about. For many <code>ClassKnowledge</code> implementations, this will be
     * <code>null</code>, as the standins are only used temporarily for things
     * like serialisation, and in particular aren't used outside the class
     * knowledge hierarchy. If a standin factory is returned, it will be
     * suitable for creating standins for the purpose of data storage (i.e. long
     * references, wrappers for existing objects, and the like).
     * <p>
     * The default implementation of this method returns the standin factory
     * override if one exists, or else <code>null</code>. It should be
     * overridden in knowledge classes for which standins can sensibly be
     * created externally (i.e. <code>NonCopiableKnowledge</code>).
     *
     * @param purpose The purpose for which the standins are being created. (In
     * the case of a noncopiable class, different factories could be recommended
     * for different purposes. Most class knowledge implementations ignore this
     * argument, though.)
     * @return A standin factory, or <code>null</code> if the class is not one
     * for which standins should be created directly.
     */
    StandinFactory<T> getStandinFactory(
            NonCopiableKnowledge.StandinFactoryPurpose purpose) {
        return standinFactoryOverride;
    }

    /**
     * Cache of <code>ClassKnowledge</code> objects. Maps classes to the
     * corresponding knowledge for that class.
     */
    private static final Map<Class<?>, ClassKnowledge<?>> knowledgeCache
            = new ConcurrentHashMap<>();

    /**
     * Registers an exception to the normal marshalling rules for a given class.
     * The exception is given in the form of a <code>ClassKnowledge</code>
     * object that specifies the new marshalling rules. This has a permanent,
     * global effect; all future marshalling for the given class will be done
     * under the new rules.
     * <p>
     * This method is mostly used internally to set up special cases, but can
     * also be called explicitly by a user.
     *
     * @param knowledge The exception to add. (The class for which the exception
     * is being made will be inferred from the exception itself.)
     */
    static void registerMarshallingException(
            ClassKnowledge<?> knowledge) {
        knowledgeCache.put(knowledge.getAbout(), knowledge);
    }

    /**
     * Registers an exception to the normal marshalling rules for two classes.
     * This is used for primitives, which share class knowledge with the
     * corresponding boxed wrapper class.
     *
     * @param knowledge The exception to add. (One class for which an exception
     * is added will be the class this knowledge is about.)
     * @param otherClass The other class for which the exception is being made.
     */
    private static void register2MarshallingExceptions(
            ClassKnowledge<?> knowledge, Class<?> otherClass) {
        knowledgeCache.put(knowledge.getAbout(), knowledge);
        knowledgeCache.put(otherClass, knowledge);
    }

    /* Important note! All marshalling exceptions must be compatible with
       the wire formats specified in ObjectWireFormat. In general, they
       should only be used in situations which ObjectWireFormat already
       records as a special case (i.e. a marshalling technique of
       "primitive" or "special case"). */
    static {
        /* Built-in exceptions to marshalling rules: primitives */
        register2MarshallingExceptions(
                new PrimitivePOJOKnowledge.Bool(), boolean.class);
        register2MarshallingExceptions(
                new PrimitivePOJOKnowledge.Byte(), byte.class);
        register2MarshallingExceptions(
                new PrimitivePOJOKnowledge.Char(), char.class);
        register2MarshallingExceptions(
                new PrimitivePOJOKnowledge.Double(), double.class);
        register2MarshallingExceptions(
                new PrimitivePOJOKnowledge.Float(), float.class);
        register2MarshallingExceptions(
                new PrimitivePOJOKnowledge.Int(), int.class);
        register2MarshallingExceptions(
                new PrimitivePOJOKnowledge.Long(), long.class);

        /* Built-in exceptions to marshalling rules: other fundamental
           classes */
        registerMarshallingException(new SpecialCaseKnowledge.Class());
        registerMarshallingException(new SpecialCaseKnowledge.String());

        /* These can be part of a CommunicationAddress (thus a GlobalID), and
           therefore attempting to serialise them via any more complex mechanism
           than "immutable data" can lead to an infinite regress. Luckily, they
           can also be treated as POJOs. ObjectWireFormat has a "special case"
           override for these two classes specifically. */
        registerMarshallingException(new PrimitivePOJOKnowledge.IPv4());
        registerMarshallingException(new PrimitivePOJOKnowledge.IPv6());

        /* MarshalledDescription has a custom standin class, because it's using
           inherently noncopiable classes internally yet is copiable itself.
           Note that the class implements Copiable, so we aren't overriding the
           knowledge <i>class</i>, just the knowledge <i>object</i>. */
        final ClassKnowledge<MarshalledDescription> mdKnowledge
                = new CopiableKnowledge<>(MarshalledDescription.class);
        mdKnowledge.setStandinFactoryOverride(
                new MarshalledDescriptionStandin.Factory());
        registerMarshallingException(mdKnowledge);

        /* Built in special cases for class name descriptions */
        final BiConsumer<Integer, Class<?>> r
                = ClassNameDescriptions::addNewSpecialCaseDescription;
        r.accept(-0x0D100001, GlobalID.class);
        r.accept(-0x0D100002, CommunicationAddress.class);
        r.accept(-0x0D100003, LoopbackCommunicationAddress.class);
        r.accept(-0x0D100004, SiteLocalCommunicationAddress.class);
        r.accept(-0x0D100005, StaticInternetCommunicationAddress.class);
        r.accept(-0x0D100006, SecondaryEndpoint.Address.class);
        r.accept(-0x0D100007, OutboundOnlyCommunicationAddress.class);

        r.accept(-0x0D200001, MethodMessage.class);
        r.accept(-0x0D200002, OperationCompleteMessage.class);
        r.accept(-0x0D200003, GarbageCollectionMessage.class);
        r.accept(-0x0D200004, LocationManagerStatusMessage.class);
        r.accept(-0x0D200005, ThirdPartyMessage.class);
        r.accept(-0x0D200006, IdentityMessage.class);
        r.accept(-0x0D200007, InterruptMessage.class);
        r.accept(-0x0D200008, MigrationActionsMessage.class);
        r.accept(-0x0D200009, MigrationMessage.class);
        r.accept(-0x0D20000A, MigrationSynchronisationMessage.class);

        r.accept(-0x0D300001, MarshalledDescription.class);
        r.accept(-0x0D300002, MessageAddress.class);
        r.accept(-0x0D300003, MessageEnvelope.class);
        r.accept(-0x0D300004, MigrationActions.class);
        r.accept(-0x0D300005, MigrationMonitor.class);
        r.accept(-0x0D300006, ObjectLocation.class);
        r.accept(-0x0D300007, ReferenceValue.class);
        r.accept(-0x0D300008, ThreadProjectionTracker.ThreadHasEnded.class);
    }

    /**
     * Given an object, returns class knowledge for its actual class. This takes
     * standins into account, i.e. if the object is actually a standin, returns
     * knowledge for the class it's standing in for.
     *
     * @param <T> The declared class of the object. Provided for type-safety;
     * can be Object if there's no need to constrain the type of the resulting
     * class knowledge object.
     * @param obj The object whose class should be looked up.
     * @return Knowledge for the object's actual class.
     */
    static <T> ClassKnowledge<? extends T> knowledgeForActualClass(T obj) {
        return knowledgeForClass(getActualClassInternal(obj));
    }

    /**
     * Factory method for ClassKnowledge objects. Given a class, checks to see
     * if we already know about it; if we do, returns the existing knowledge; if
     * we don't, determine an appropriate knowledge class to use via using
     * <code>ObjectWireFormat</code>, then returns the information and also
     * stores it for future use. (In other words, this is a kind of memoised
     * constructor that automatically picks an appropriate implementation.)
     * <p>
     * This will pause <code>Lazy.TIME_BASE</code> while running, so that
     * accurate times are available for time measurements that wish to avoid
     * counting one-time initialisation operations.
     *
     * @param <T> The class to gain information about.
     * @param about <code>T.class</code>, given explicitly due to Java's type
     * erasure rules.
     * @return The knowledge for that class.
     */
    @SuppressWarnings("unchecked")
    static <T> ClassKnowledge<T> knowledgeForClass(Class<T> about) {
        Objects.requireNonNull(about);
        synchronized (ClassKnowledge.class) {
            if (!knowledgeCache.containsKey(about)) {
                try (DeterministicAutocloseable ac
                        = Lazy.TIME_BASE.get().pause()) {
                    ClassKnowledge<T> rv = newKnowledgeForClass(about);

                    knowledgeCache.put(about, rv);
                    return rv;
                }
            }
        }
        /* The unchecked cast is because we can't cast, say, Integer to int. */
        return (ClassKnowledge<T>) knowledgeCache.get(about);
    }

    /**
     * Creates a new ClassKnowledge value for the given class. This implements
     * the main bulk of <code>knowledgeForClass</code>, and is split out from it
     * to make the code more readable. It finds an appropriate implementation of
     * <code>ClassKnowledge</code> to use for the class via consulting
     * <code>ObjectWireFormat</code>, then constructs one.
     *
     * @param <T> The class to analyse.
     * @param about <code>T.class</code>, given explicitly due to Java's type
     * erasure rules.
     * @return A newly created <code>ClassKnowledge&lt;T&gt;</code>.
     * @throws IllegalArgumentException If <code>about</code> is a signpost
     * class or if it cannot be marshalled via any means
     */
    private static <T> ClassKnowledge<T> newKnowledgeForClass(
            Class<T> about) throws IllegalArgumentException {
        if (about == null) {
            throw new NullPointerException("creating knowledge for null");
        }

        /* Java seems to mark some special classes (e.g. arrays) as abstract
           and final, even though that combination makes no sense. Complain only
           if the class is actually abstract in the normal sense. */
        if (about.isInterface()
                || (isAbstract(about.getModifiers())
                && !isFinal(about.getModifiers())
                && !Enum.class.isAssignableFrom(about))) {
            throw new IllegalArgumentException(
                    Modifier.toString(about.getModifiers()) + " " + about
                    + " is abstract; only concrete classes have knowledge");
        }

        if (Standin.class.isAssignableFrom(about)) {
            /* Sanity check: when dealing with long references, we should have
               been given the class of the referenced object, not the class of
               the signpost. */
            throw new IllegalArgumentException(about
                    + " is a standin class; use the referenced class instead");
        }

        if (Enum.class.isAssignableFrom(about)
                && about.getEnumConstants() == null) {
            throw new IllegalArgumentException(about
                    + " is an enum constant class; use the enum class instead");
        }

        ObjectWireFormat<T> owf = doPrivileged(
                (PrivilegedAction<ObjectWireFormat<T>>) () -> new ObjectWireFormat<>(about));

        switch (owf.getTechnique()) {
            case LONG_REFERENCE:
                return new NonCopiableKnowledge<>(about);
            case PRIMITIVE:
                throw new IllegalArgumentException("primitive " + about
                        + " should have knowledge already");
            case FIELD_BY_FIELD:
                return owf.serialisesReliably()
                        ? new CopiableKnowledge<>(about)
                        : new UnreliablyCopiableKnowledge<>(about);
            case ELEMENT_BY_ELEMENT:
                return new ArrayKnowledge<>(about);
            case ENUM_INDEX: {
                @SuppressWarnings("unchecked")
                Class<? extends Enum<?>> enumAbout
                        = (Class<? extends Enum<?>>) classCast(
                                about, Enum.class, null);

                if (enumAbout != null) {
                    @SuppressWarnings("unchecked")
                    ClassKnowledge<T> enumKnowledge
                            = (ClassKnowledge<T>) new EnumKnowledge<>(enumAbout);
                    return enumKnowledge;
                } else {
                    throw new RuntimeException(about
                            + " is serialised by enum index, but isn't an enum");
                }
            }
            case READ_RESOLVE:
                if (owf.isLambda()) {
                    return new LambdaKnowledge<>(about);
                } else {
                    throw new DistributedError(new ClassCastException(),
                            "Don't know how to find the factory method for "
                            + about);
                }
            case SPECIAL_CASE:
                throw new IllegalArgumentException("special case " + about
                        + " should have knowledge already");
            case UNSERIALISABLE:
                throw new DistributedError(new ClassCastException(),
                        "Cannot figure out how to serialise " + about);
            default:
                throw new RuntimeException(
                        "unexpected serialisation technique: "
                        + owf.getTechnique().name());
        }
    }

    /**
     * Calculates the size within an object description that will be needed to
     * describe the value of a field holding either a value of the class that
     * this knowledge is about, or possibly <code>null</code>. (Note that the
     * <code>null</code> possibility can be ignored for non-nullable values like
     * primitives. If <code>nullable</code> is false, the value will not be null
     * and the encoding used for the value doesn't have to take into account the
     * possibility that the value might be null; this means that non-nullable
     * encodings may be shorter.) This is allowed to be an overestimate in cases
     * where working out the exact value would be considerably slower (e.g. if
     * an overestimate can be produced without forcing <code>lazyValue</code>,
     * but an exact value can't be).
     *
     * @param lazyValue The actual value that will be described, given lazily to
     * improve performance in the (fairly common) case where it's irrelevant.
     * Can be <code>null</code> to determine the size only if it's constant.
     * @param nullable Whether the encoding used in the description has to take
     * into account the possibility that the value might be null.
     * @return The size of the description, or an overestimate of the size of
     * the description, or <code>null</code> if the size is not constant and
     * <code>lazyValue</code> is <code>null</code>.
     */
    ObjectDescription.Size descriptionSize(
            Supplier<Object> lazyValue, boolean nullable) {
        /* Do we have a reasonably accurate upper bound already? */
        ObjectDescription.Size size = owf.getSerialisedSize(nullable);
        if (size != null) {
            return size;
        }
        if (lazyValue == null) {
            return null;
        }

        size = descriptionSizeOfObject(
                getAbout().cast(lazyValue.get()), nullable);
        return size;
    }

    /**
     * Calculates the size within an object description that will be needed to
     * describe the value of a field holding either a value of the class that
     * this knowledge is about, or possibly <code>null</code>. (Note that the
     * <code>null</code> possibility can be ignored for non-nullable values like
     * primitives. If <code>nullable</code> is false, the value will not be null
     * and the encoding used for the value doesn't have to take into account the
     * possibility that the value might be null; this means that non-nullable
     * encodings may be shorter.)
     * <p>
     * This method is used to implement <code>descriptionSize</code>, and called
     * only in cases where the object could potentially be arbitrarily large
     * (e.g. because it contains an array field, or it contains a field that
     * could potentially be copiable but is not <code>final</code>, and thus
     * could contain a generated class of arbitrary size).
     *
     * @param value The actual value that will be described.
     * @param nullable Whether the encoding used in the description has to take
     * into account the possibility that the value might be null.
     * @return The size of the description.
     */
    abstract ObjectDescription.Size descriptionSizeOfObject(
            T value, boolean nullable);

    /**
     * Mutates a given object description by appending a description of a field
     * whose actual class is the class that this knowledge is about. In the case
     * where the field's value is <code>null</code>, this can also be used in
     * the case where the field's <i>declared</i> class is the class that this
     * knowledge is about.
     * <p>
     * Note that a description of a field often also contains a description of
     * the actual class of the object stored in that field; if that's required,
     * it must be added by the caller.
     * <p>
     * The default implementation calls <code>writeFieldDescriptionTo</code>
     * using the given object description as a target. In cases where the class
     * is not deeply copiable, this will need to be overridden with an
     * alternative implementation.
     *
     * @param description The description to mutate.
     * @param fieldValue The value of the field in question.
     * @param nullable Whether to use an encoding that takes into account the
     * possibility that <code>fieldValue</code> might be null.
     * @throws IOException If something goes wrong writing into the description
     */
    void describeFieldInto(DescriptionOutput description,
            Object fieldValue, boolean nullable) throws IOException {
        writeFieldDescriptionTo(description, fieldValue, nullable);
    }

    /**
     * Attempts to write a description of the value of a field (whose actual
     * class is the class this knowledge is about) to a data output sink. This
     * is possible only in cases where the class is deeply copiable, i.e. it's
     * safe to write out the description recursively field-by-field.
     * <p>
     * As with <code>describeFieldInto</code>, this will not write out the
     * object's class (i.e. if its class needs to be communicated over the data
     * output sink too, the caller must do that), and this method can (if
     * <code>nullable</code> is <code>true</code>) also be used to write out a
     * <code>null</code> whose declared class is the class this knowledge is
     * about.
     *
     * @param sink The data output sink to write the description to.
     * @param fieldValue The value to write a description of.
     * @param nullable Whether to use an encoding that takes into account the
     * possibility that <code>fieldValue</code> might be null.
     * @throws IOException If something goes wrong while writing
     * @throws UnsupportedOperationException If this class is not deeply
     * copiable
     */
    abstract void writeFieldDescriptionTo(DataOutput sink,
            Object fieldValue, boolean nullable)
            throws IOException, UnsupportedOperationException;

    /**
     * Reproduce an object of the class that this knowledge is about from a
     * description. This will be a shallow copy of the object that was initially
     * described to produce the description. Any header specifying the class of
     * the object to reproduce will already have been read.
     *
     * @param description The description from which to reproduce the object.
     * @param nullable If set, must check for an encoding of <code>null</code>
     * in the description, and output <code>null</code> if it is found; note
     * that nullability can sometimes change the format of the description.
     * @return The newly constructed object.
     * @throws IOException If an error is noticed reading the description (note
     * that for efficiency reasons, not all errors will be caught)
     */
    abstract T reproduce(ReadableDescription description, boolean nullable)
            throws IOException;

    /**
     * Determines whether the marshalled form of objects of this class can be
     * reliably unmarshalled. Some marshalling forms are unreliable due to not
     * being fully general; the marshalling system will attempt to avoid these
     * forms if possible, via using references at the next level out.
     * <p>
     * Sometimes the result changes depending on whether we're talking about a
     * hypothetical object stored in a field of this class, or an actual known
     * class that inherits from this class. As such, this method has a parameter
     * so that the two different options can be distinguished.
     * <p>
     * This method should return <code>true</code> if the unmarshalled form of
     * an object is effectively interchangeable with the object itself, and
     * <code>false</code> if they differ or if there's a chance that the
     * marshalled form might not be possible to unmarshal.
     * <p>
     * Note that it is not possible in general to avoid unreliable unmarshals,
     * and thus even when reliable marshalling is not possible, class knowledge
     * should make a best-effort attempt at codifying how to marshal an object
     * so that it unmarshals under as many circumstances as possible.
     *
     * @param asField Whether we're talking about a hypothetical object stored
     * in a field of this class (<code>true</code>), or a class that inherits
     * from or implements this class (<code>false</code>)
     * @return Whether objects of this class can be reliably unmarshalled.
     */
    protected abstract boolean unmarshalsReliably(boolean asField);

    /**
     * Determines whether objects are unmarshalled as a copy of the original
     * object. If this is false, it means that they're unmarshalled to a
     * reference to the original object.
     * <p>
     * This method cannot be called if <code>T</code> is an interface (because
     * the answer would depend on the concrete class of the object used to
     * fulfil that interface).
     *
     * @return <code>true</code> if marshalling and unmarshalling an object of
     * the class we know about copies it; <code>false</code> if it instead
     * produces a reference.
     * @throws UnsupportedOperationException If <code>T</code> is an interface
     */
    protected abstract boolean unmarshalsAsCopy()
            throws UnsupportedOperationException;

    /**
     * Produces a description of the name of this class, suitable for storing in
     * an object description. This is a sequence of bytes that can be used to
     * uniquely reconstruct the class. For most classes, this is the class's
     * binary name preceded by its length, but some classes have a special-cased
     * representation.
     * <p>
     * The returned array should not be modified.
     *
     * @param expected The class that
     * <code>ClassNameDescription.EXPECTED_DESCRIPTION</code> would refer to in
     * the context that the resulting description will be unmarshalled.
     * @return The description, as a hardcoded byte array.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    byte[] getClassNameDescription(Class<?> expected) {
        /* about is not a standin, but expected might be. Find the underlying
           non-standin class behind expected, and then compare it to about. As
           an optimization, if expected isn't equal to and doesn't extend about,
           then there can't possibly be a match so don't bother looking up the
           class hierarchy. */
        if (expected != null && getAbout().isAssignableFrom(expected)) {
            Class<?> expectedNoStandin = expected;
            while (Standin.class.isAssignableFrom(expectedNoStandin)) {
                expectedNoStandin = expectedNoStandin.getSuperclass();
            }
            if (getAbout().equals(expectedNoStandin)) {
                return EXPECTED_DESCRIPTION;
            }
        }

        return classNameDescription;
    }

    /**
     * Returns the object wire format used by the class this knowledge is about.
     * This can also be used to get at an object method database for the class
     * efficiently (because <code>ObjectWireFormat</code> extends
     * <code>ObjectMethodDatabase</code>).
     *
     * @return The class's wire format.
     */
    ObjectWireFormat<T> getWireFormat() {
        return owf;
    }

    /**
     * Calls <code>Object#clone</code> on the given object. (This call cannot be
     * made directly because <code>Object#clone</code> is a
     * <code>protected</code> method.) This is a virtual call, i.e. if the
     * object overrides <code>Object#clone</code>, the overriding method will be
     * called.
     *
     * @param <T> The actual class of <code>original</code>.
     * @param original The object to clone.
     * @return The cloned object.
     * @throws CloneNotSupportedException If <code>Object#clone</code> throws a
     * <code>CloneNotSupportedException</code>
     */
    static <T> Object cloneObject(T original) throws CloneNotSupportedException {
        ClassKnowledge<? extends T> ck = knowledgeForActualClass(original);
        MethodHandle mh;
        try {
            mh = ck.getMethodByCode(MethodCodes.CLONE);
        } catch (NoSuchMethodException ex) {
            /* should be impossible, as everything extends <code>Object</code> */
            throw new RuntimeException(ex);
        }
        try {
            return mh.invoke(original);
        } catch (CloneNotSupportedException | RuntimeException | Error ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new UndeclaredThrowableException(ex);
        }
    }
}
