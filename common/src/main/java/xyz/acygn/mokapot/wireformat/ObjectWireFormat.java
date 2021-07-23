package xyz.acygn.mokapot.wireformat;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PROTECTED;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isPublic;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import xyz.acygn.mokapot.markers.Copiable;
import xyz.acygn.mokapot.markers.NonCopiable;
import xyz.acygn.mokapot.util.ObjectMethodDatabase;
import xyz.acygn.mokapot.util.VMInfo;
import static xyz.acygn.mokapot.util.VMInfo.isClassNameInSealedPackage;
import static xyz.acygn.mokapot.wireformat.DescriptionOutput.PRIMITIVE_SIZES;
import static xyz.acygn.mokapot.wireformat.DescriptionOutput.sizeOfPrimitive;
import static xyz.acygn.mokapot.wireformat.MethodCodes.defaultMethodCode;
import static xyz.acygn.mokapot.wireformat.ObjectWireFormat.Technique.ELEMENT_BY_ELEMENT;
import static xyz.acygn.mokapot.wireformat.ObjectWireFormat.Technique.ENUM_INDEX;
import static xyz.acygn.mokapot.wireformat.ObjectWireFormat.Technique.FIELD_BY_FIELD;
import static xyz.acygn.mokapot.wireformat.ObjectWireFormat.Technique.LONG_REFERENCE;
import static xyz.acygn.mokapot.wireformat.ObjectWireFormat.Technique.PRIMITIVE;
import static xyz.acygn.mokapot.wireformat.ObjectWireFormat.Technique.READ_RESOLVE;
import static xyz.acygn.mokapot.wireformat.ObjectWireFormat.Technique.SPECIAL_CASE;
import static xyz.acygn.mokapot.wireformat.ObjectWireFormat.Technique.UNSERIALISABLE;
import static xyz.acygn.mokapot.wireformat.ObjectWireFormat.Transparent.getFormatDefiningClass;

/**
 * Information determining how an object is serialised for sending over a
 * network. This includes an <code>ObjectMethodDatabase</code>; a salt for
 * sending method codes; and what serialisation/marshalling technique is used
 * with the object.
 * <p>
 * Note that in some cases, the object wire format of a class is actually
 * defined by reference to the object wire format of a different class. (This
 * happens when the former class is intended to be a transparent wrapper, rather
 * than interpreted as a class in its own right.) As such, the type
 * <code>ObjectMethodDatabase&lt;T&gt;</code> is only an approximation, not the
 * actually true type.
 *
 * @author Alex Smith
 * @param <T> The actual class of objects described by the database.
 * @see MethodCodes
 */
public class ObjectWireFormat<T> extends ObjectMethodDatabase<T> {

    /**
     * A serialisation technique used to serialise objects.
     */
    public enum Technique {
        /**
         * The object is serialised via a "long reference". This forwards
         * methods to the original copy of the object (most likely on a
         * different machine). This is used with objects that are
         * <code>NonCopiable</code>, or implicitly so.
         */
        LONG_REFERENCE,
        /**
         * The object is a primitive. Primitives are serialised directly using
         * methods of <code>DataOutput</code>.
         *
         * @see java.io.DataOutput
         */
        PRIMITIVE,
        /**
         * The object is copied a field at a time. This is used with objects
         * that are <code>Copiable</code>, or implicitly so.
         */
        FIELD_BY_FIELD,
        /**
         * The object is copied an element at a time. This is only used for
         * arrays. Note that it produces incorrect results unless the array is
         * never modified after serialising or after deserialising, and thus
         * this is considered an unreliable method of serialisation.
         */
        ELEMENT_BY_ELEMENT,
        /**
         * The object is serialised via enum index. This obviously only works
         * for enum constants. Even then, it's not 100% reliable in the case
         * that the enum constants are mutable, but this case is sufficiently
         * silly that it's not worth worrying about.
         */
        ENUM_INDEX,
        /**
         * The object is serialised indirectly as a builder object. The method
         * `readResolve` on the builder object constructs the actual object in
         * use. This method is used for objects that are serialised via copying,
         * but cannot be constructed directly, such as lambdas.
         */
        READ_RESOLVE,
        /**
         * The object has a custom serialisation routine. At the moment, this is
         * reserved for <code>Class</code> and <code>String</code>.
         */
        SPECIAL_CASE,
        /**
         * The object cannot be serialised at all. For example, it belongs to a
         * synthetic class with no known factory method.
         */
        UNSERIALISABLE
    }

    /**
     * The technique via which this object is serialised.
     */
    private final Technique technique;

    /**
     * Whether the object is expected to serialise reliably. Unreliable
     * serialisation includes situations in which we might potentially have to
     * copy a cyclic list, wrap a <code>final</code> class (or one which has
     * package-private methods from outside its own package), or copy a
     * read/write object; none of these situations are guaranteed to work
     * reliably.
     * <p>
     * If an object contains fields whose declared classes do not serialise
     * reliably, or whose declared classes are abstract/interfaces, we assume
     * that we might potentially end up with something we can't handle in those
     * fields, and treat it as noncopiable unless stated otherwise.
     */
    private final boolean serialisesReliably;

    /**
     * The serialised size of this object. Can be <code>null</code> in cases
     * where the size varies or cannot be statically determined. This includes
     * the potential extra byte needed to store the possibility that the object
     * is <code>null</code> (unlike most size measurements, which exclude this),
     * and likewise is a size of "1 object" if the object is serialized by
     * reference.
     */
    private final ObjectDescription.Size serialisedSize;

    /**
     * Whether the object is a lambda. This is determined via seeing whether its
     * actual class (i.e. <code>about</code>) is a lambda class.
     */
    private final boolean lambda;

    /**
     * The salt used for producing default method codes for the class this
     * knowledge is about.
     */
    private final int salt;

    /**
     * The set of classes already considered when checking whether this class
     * can indirectly contain an instance of itself.
     */
    private static final ThreadLocal<Set<Class<?>>> OUTER_CLASSES
            = ThreadLocal.withInitial(() -> new HashSet<>());

    /**
     * Create a database of methods, fields and superclasses/interfaces for
     * objects with the given actual class, and calculate a method code salt.
     *
     * @param about The actual class that the resulting database should be
     * about.
     */
    @SuppressWarnings("unchecked")
    public ObjectWireFormat(Class<T> about) {

        /* This cast is "wrong", but Java's type system is incapable of handling
           the actual type we want, so this is the closest we can get. */
        super((Class<T>) getFormatDefiningClass(about));
        Class<T> newAbout = getAbout();

        /* Find a salt that produces distinct hashes for every method name in
           the set of method names. */
        int testSalt = 0;
        NEXT_SALT:
        for (;;) {
            testSalt++;
            Set<Long> usedCodes = new HashSet<>(getMethods().size());
            for (Method m : getMethods()) {
                long code = defaultMethodCode(m, testSalt);
                if (usedCodes.contains(code)) {
                    continue NEXT_SALT;
                }
                usedCodes.add(code);
            }
            break;
        }
        this.salt = testSalt;

        /* Is this a lambda class?

           TODO: This check is pretty fragile, depending as it does on JVM
           internals. Strangely, though, it works better in practice than
           some apparently less fragile techniques do. */
        this.lambda = newAbout.getName().contains("$$Lambda$")
                && !Arrays.stream(newAbout.getInterfaces()).anyMatch(e -> e.getCanonicalName().equals(
                xyz.acygn.mokapot.markers.ComeFromRetroLambda.class.getCanonicalName()));

        /* Search for obstacles to wrapping. */
        boolean canReliablyWrap = true;
        if (isFinal(newAbout.getModifiers())) {
            canReliablyWrap = false;
        }
        if (isPrivate(newAbout.getModifiers())) {
            canReliablyWrap = false;
        }
        if (!isPublic(newAbout.getModifiers())
                && isClassNameInSealedPackage(newAbout.getName())) {
            /* We can't reliably wrap a package-private class in the java.
               hierarchy because we can't create a wrapper in the same package;
               there's no way to inject classes into java.*. */
            canReliablyWrap = false;
        }
        for (Method m : getMethods()) {
            if ((m.getModifiers()
                    & (PRIVATE | PUBLIC
                    | PROTECTED | STATIC)) == 0
                    && Objects.equals(m.getDeclaringClass().getPackage(), newAbout.getPackage())) {
                /* If the method is not static, and is package-private (i.e.
                   not public, protected, or private), and is declared in a
                   different package from the object, then we can't override it
                   by wrapping the object's actual class.

                   TODO: Actually, if we have access to the package in question,
                   we could override it via adding multiple wrapping classes
                   that belong to different packages. That would be much more
                   complex, though, and possibly violate security invariants
                   assumed by the original code. */
                canReliablyWrap = false;
            }
        }

        /* Checks for special-cased classes. */
        DescriptionOutput.initSizes();
        if (PRIMITIVE_SIZES.containsKey(newAbout)) {
            this.serialisesReliably = true;
            this.technique = PRIMITIVE;
            this.serialisedSize = new ObjectDescription.Size(
                    sizeOfPrimitive(newAbout), 0);
            return;
        } else if (newAbout.equals(String.class) || newAbout.equals(Class.class)) {
            this.serialisesReliably = true;
            this.technique = SPECIAL_CASE;
            this.serialisedSize = null;
            return;
        } else if (newAbout.equals(Inet4Address.class)) {
            this.serialisesReliably = true;
            this.technique = SPECIAL_CASE;
            this.serialisedSize = new ObjectDescription.Size(5, 0);
            return;
        } else if (newAbout.equals(Inet6Address.class)) {
            this.serialisesReliably = true;
            this.technique = SPECIAL_CASE;
            this.serialisedSize = new ObjectDescription.Size(17, 0);
            return;
        } else if (newAbout.isArray()) {
            this.serialisesReliably = false;
            this.technique = ELEMENT_BY_ELEMENT;
            this.serialisedSize = null;
            return;
        } else if (newAbout.equals(BigInteger.class)
                || newAbout.equals(Optional.class)) {
            this.serialisesReliably = true;
            this.technique = FIELD_BY_FIELD;
            this.serialisedSize = null;
            return;
        } else if (newAbout.equals(HashSet.class)) {
            /* TODO: This is a hack needed to get Mokapot working in the short
               term. In the long term, we need to figure out what's causing this
               and fix it. */
            this.serialisesReliably = true;
            this.technique = LONG_REFERENCE;
            this.serialisedSize = new ObjectDescription.Size(0, 1);
            return;
        } else if (lambda) {
            /* Note: we can't have a mutable field in a class that's truly a
               lambda class, due to the algorithm used for generating such
               classes */
            this.serialisesReliably
                    = Serializable.class.isAssignableFrom(newAbout);
            this.technique = serialisesReliably
                    ? READ_RESOLVE : UNSERIALISABLE;
            /* TODO: It might theoretically be possible to work out a fixed,
               known size in some cases, but that would require knowledge of the
               internals of SerializedLambda and also of the lambda in question,
               which are hard or even impossible to extract. */
            this.serialisedSize = null;
            return;
        } else if (Enum.class.isAssignableFrom(newAbout)) {
            this.serialisesReliably = true;
            this.technique = ENUM_INDEX;
            /* An enum index is 4 bytes large. */
            this.serialisedSize = new ObjectDescription.Size(4, 0);
            return;
        } else if (NonCopiable.class.isAssignableFrom(newAbout)
                || Object.class.equals(newAbout)) {
            /* In this case, we don't need to answer any "how would this object
               look if it were copied?" scenarios; we know it won't be, so can
               short-circuit the rest of this code. Object is implicitly
               NonCopiable because there's no reason to use a non-extended
               Object except for its address. */
            this.serialisesReliably = canReliablyWrap;
            this.technique = LONG_REFERENCE;
            this.serialisedSize = new ObjectDescription.Size(0, 1);
            return;
        }

        /* Do a field-by-field scan of the object in order to figure out whether
           we can potentially copy it and what it would look like if we did. */
        String cannotCopyBecause = null;
        ObjectDescription.Size copiedSize;
        if (OUTER_CLASSES.get().contains(newAbout)) {
            /* We need to detect recursion in ObjectWireFormat construction,
               which we do using a thread-local set of objects currently being
               constructed. If we see this, copying the class is unreliable
               (as it could lead to an arbitrarily or even infinitely large
               serialisation). */
            cannotCopyBecause = "recursion in copiable fields";
            copiedSize = null;
        } else {
            copiedSize = new ObjectDescription.Size(0, 0);
            try {
                OUTER_CLASSES.get().add(newAbout);
                for (Field f : this.getInstanceFieldList()) {
                    Class<?> fType = f.getType();

                    if (!isFinal(f.getModifiers())) {
                        /* We can't reliably copy an object with mutable
                           fields. */
                        cannotCopyBecause = "field " + f + " is mutable";
                    } else if (fType.isAssignableFrom(newAbout)) {
                        /* We can't reliably copy an object that could contain
                           a reference to another object of the same class.
                           (Note that this isn't necessarily caught via the
                           anti-recursion checks when the declared type of the
                           field is something general like Object, so it has
                           to be checked separately.) */
                        cannotCopyBecause = "field " + f
                                + " could self-reference";
                    } else if (fType.isInterface()) {
                        /* Is it a functional interface? Is it a marker
                           interface? Either could store a lambda, thus is not
                           reliably copiable. */
                        int methodCount = 0;
                        for (Method m : f.getType().getMethods()) {
                            if (!isFinal(m.getModifiers())
                                    && isAbstract(m.getModifiers())) {
                                methodCount++;
                            }
                        }
                        if (methodCount < 2) {
                            cannotCopyBecause = "field " + f + " might store a lambda";
                        }
                    }

                    if (!isAbstract(fType.getModifiers())
                            && !fType.isInterface()) {
                        /* It's a concrete class. If the class is final and
                           reliably copiable, we know how large it is (the size
                           of that class, including the + 1 byte to specify
                           "not null"). If the class is noncopiable, we also
                           know how large it is (1 object). We can also check if
                           the class is reliably serialisable; if it isn't we
                           can't reliably copy this class (as that would require
                           serialising that one). */
                        ObjectWireFormat<?> fWireFormat
                                = new ObjectWireFormat<>(fType);
                        if (!fWireFormat.serialisesReliably) {
                            cannotCopyBecause = "field " + f
                                    + "'s actual type is concrete and unreliable";
                        }
                        if (fWireFormat.technique.equals(LONG_REFERENCE)
                                && NonCopiable.class.isAssignableFrom(fType)
                                && copiedSize != null) {
                            copiedSize = copiedSize.addObjects(1);
                        } else if (isFinal(fType.getModifiers())
                                && copiedSize != null) {
                            if (fWireFormat.technique.equals(LONG_REFERENCE)) {
                                copiedSize = copiedSize.addObjects(1);
                            } else if (fWireFormat.serialisedSize == null) {
                                copiedSize = null;
                            } else {
                                copiedSize = copiedSize.add(
                                        fWireFormat.serialisedSize);
                            }
                        } else {
                            /* We don't know how the field will be serialised
                               (an implicitly copiable class could be extended
                               by a noncopiable class, or an explicitly copiable
                               class by one with more fields), so we don't know
                               how large it will be. */
                            copiedSize = null;
                        }
                    } else {
                        /* The field is specified via interface, so could
                           potentially be any size; among other things, we'd
                           need to store the name of the class implementing the
                           interface.

                           TODO: We could actually be more precise if the
                           interface extends UnsafeToCopy, but that seems
                           unwise. */
                        copiedSize = null;
                    }
                }
            } finally {
                OUTER_CLASSES.get().remove(newAbout);
            }
        }

        /* Add the +1 byte for potential nullability. */
        if (copiedSize != null) {
            copiedSize = copiedSize.addBytes(1);
        }

        if (Throwable.class.isAssignableFrom(newAbout)
                || StackTraceElement.class.isAssignableFrom(newAbout)) {
            /* Special cases to avoid an infinite regress in error handling. */
            this.serialisesReliably = false;
            this.technique = FIELD_BY_FIELD;
            this.serialisedSize = copiedSize;
        } else if (Copiable.class.isAssignableFrom(newAbout)
                || newAbout.equals(SerializedLambda.class)
                || cannotCopyBecause == null) {
            /* Note: even if we think the serialisation is unreliable, we treat
               it as reliable if we've been told it is; this is necessary for
               cases where we have a special-cased format implemented via the
               use of hand-written serialisation code.

               SerializedLambda is overridden to "reliably copiable" because
               we know that the array it contains is treated as immutable. */
            this.serialisesReliably = true;
            this.technique = FIELD_BY_FIELD;
            this.serialisedSize = copiedSize;
        } else {
            this.serialisesReliably = canReliablyWrap;
            this.technique = LONG_REFERENCE;
            this.serialisedSize = new ObjectDescription.Size(0, 1);
        }
    }

    /**
     * Returns the salt used to produce default method codes for the objects
     * described by this class.
     *
     * @return The salt in use for the class.
     */
    public final int getMethodCodeSalt() {
        return salt;
    }

    /**
     * Returns the technique via which objects of the given actual class are
     * serialised.
     *
     * @return The serialisation technique.
     */
    public Technique getTechnique() {
        return technique;
    }

    /**
     * Returns whether objects of the given actual class are expected to
     * serialise and deserialise reliably.
     *
     * @return <code>true</code> if the objects are expected to serialise and
     * deserialise reliably; <code>false</code> if serialising and deserialising
     * an object may fail in reasonable circumstances.
     */
    public boolean serialisesReliably() {
        return serialisesReliably;
    }

    /**
     * Returns the number of bytes and objects an object of this actual class is
     * expected to serialise into. In some cases, this may be an upper bound
     * rather than an exact size (typically because a field turned out to be
     * <code>null</code>). There are also cases (e.g. objects containing fields
     * of type <code>String</code>) in which no maximum size can be determined;
     * this causes the function to be <code>null</code>.
     * <p>
     * Note that unlike most sizes seen in this library's API, the size in
     * question is measuring the object itself, not the total size of its
     * fields; if the object is serialised by reference, the size will be "1
     * reference"; and if the object is serialised field-by-field, an extra byte
     * will be added to allow for the possibility that the object is
     * <code>null</code> (in the case that the caller requested a nullable
     * size).
     *
     * @param nullable Whether the included size should include any potential
     * extra byte needed to indicate whether or not the object is
     * <code>null</code>.
     * @return The maximum (and typical) size of the serialised representation
     * of an object of this class; or <code>null</code> if no fixed maximum size
     * is known.
     */
    public ObjectDescription.Size getSerialisedSize(boolean nullable) {
        if (serialisedSize == null) {
            return null;
        }
        if (!nullable && (technique.equals(FIELD_BY_FIELD)
                || technique.equals(SPECIAL_CASE))) {
            return serialisedSize.addBytes(-1);
        }
        if (nullable && technique.equals(PRIMITIVE)
                && !getAbout().isPrimitive()) {
            /* e.g. int is never nullable, but Integer can be; we have the size
               recorded as though for int, add +1 for a potential null box */
            return serialisedSize.addBytes(1);
        }
        return serialisedSize;
    }

    /**
     * Returns whether this object method database describes a lambda class. In
     * such cases, the class must be serialised indirectly, as lambda classes
     * cannot be loaded via their name.
     *
     * @return <code>true</code> if this database describes a lambda class.
     */
    public boolean isLambda() {
        return lambda;
    }

    /**
     * Returns all the interfaces implemented by the class, except those which
     * could not be implemented by subclasses. The returned set will include all
     * public interfaces (even those implemented indirectly), and if the class
     * belongs to a package in which new classes can be created, also all
     * non-public interfaces which are in the same package as the class itself.
     *
     * @return A set of interfaces. The set is not guaranteed to be mutable or
     * thread-safe.
     */
    public Set<Class<?>> getAccessibleInterfaces() {
        boolean sealed = VMInfo.isClassNameInSealedPackage(getAbout().getName());
        return getInterfaces().stream().filter((Class<?> c)
                -> ((c.getModifiers() & (Modifier.PUBLIC)) != 0)
                || (!sealed && c.getPackage().equals(getAbout().getPackage())))
                .collect(Collectors.toSet());
    }

    /**
     * Marker interface, indicating that a class should be transparent to
     * ObjectWireFormat. This is intended so that proxy and wrapper classes can
     * have the same object wire format as the class they're proxying or
     * wrapping.
     */
    public static interface Transparent {

        /**
         * Given a class, returns which of its superclasses (or the class
         * itself) defines the object wire format of that class.
         *
         * @param <U> The class to inspect.
         * @param about <code>U.class</code>, given explicitly due to Java's
         * type erasure rules.
         * @return The class whose object wire format <code>about</code>'s
         * object wire format is defined in terms of.
         */
        static <U> Class<? super U> getFormatDefiningClass(Class<U> about) {
            Class<? super U> inTermsOf = about;
            while (Transparent.class.isAssignableFrom(inTermsOf)) {
//                if (ProxyOrWrapper.class.isAssignableFrom(inTermsOf)){
//                    ObjenesisStd obj = new ObjenesisStd();
//                    ObjectInstantiator<? super U>  instantiator =  obj.getInstantiatorOf(inTermsOf);
//                    Object o = instantiator.newInstance();
//                    inTermsOf = ((ProxyOrWrapper) o).getReferentClass(null);
//                }
//                else {
                inTermsOf = inTermsOf.getSuperclass();
//                }
            }
            return inTermsOf;
        }
    }
}
