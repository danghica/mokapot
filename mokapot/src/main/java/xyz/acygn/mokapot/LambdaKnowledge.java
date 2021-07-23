package xyz.acygn.mokapot;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import xyz.acygn.mokapot.markers.DistributedError;
import static xyz.acygn.mokapot.wireformat.ClassNameDescriptions.SYNTHETIC_DESCRIPTION;
import xyz.acygn.mokapot.wireformat.DescriptionOutput;
import xyz.acygn.mokapot.wireformat.ObjectDescription;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * Knowledge about how to make a copy of an object belonging to a lambda class
 * (that is, a lambda or method reference). The fundamental problem here is that
 * lambda classes don't even exist until their first use, and in particular, may
 * not exist on the destination system.
 * <p>
 * Lambdas (and method references, which are basically the same) are implemented
 * like this:
 * <ul>
 * <li>At compile time, the lambda's body is made into a function (which takes
 * any captured values as arguments), and the part of the code which requires
 * the "value of the lambda" obtains it via a dynamic method call (again, giving
 * the captured values as arguments).</li>
 * <li>The first time this dynamic method call is called, the information in the
 * class's bootstrap methods is used to lazily generate a <code>CallSite</code>
 * object (typically using <code>LambdaMetafactory#altMetaFactory</code>. This
 * object contains information about how to generate lambdas of the given
 * structure. All dynamic method calls will then go via the call site.</li>
 * <li>Each subsequent time the dynamic method is called, it uses the
 * <code>CallSite</code> object to retrieve a <code>MethodCode</code> object,
 * then calls a factory method for the lambda class via the
 * <code>MethodCode</code>.</li>
 * </ul>
 * <p>
 * As such, the problem is that values of the lambda class can only be
 * constructed via a factory method; the factory method has no set name and may
 * not (probably will not) exist until the first lambda is constructed, but can
 * be returned via the use of the lambda's <code>CallSite</code>; the
 * <code>CallSite</code> <i>also</i> does not exist until the first lambda is
 * constructed, but may be returned via use of the lambda's bootstrap method.
 * The bootstrap method does always exist (within the constant pool of the
 * lambda's "capturing class"), but there's no easy way to figure out what
 * bootstrap method was used for any given lambda.
 * <p>
 * If the lambdas are declared as <code>Serialisable</code>, they have a
 * (private, but invocable via reflection) <code>writeReplace</code> method that
 * will produce a <code>SerialisedLambda</code> object that is meant to be
 * unique to the corresponding bootstrap method (and also contains the captured
 * arguments). <code>SerialisedLambda#readResolve</code> can then be used in
 * order to call the corresponding <code>CallSite</code> (first constructing it
 * if need be) and get a new lambda that's a shallow copy of the old one. This
 * has some problems, though: a) <code>SerialisedLambda</code> contains a large
 * number of redundant arguments that make it somewhat inefficient; b) at least
 * some Java compilers are capable of generating identical
 * <code>SerialisedLambda</code> objects even when the original lambdas differ
 * in minor respects, and will therefore choose the incorrect call site.
 * <p>
 * The current implementation of this class uses <code>writeReplace</code> and
 * <code>readResolve</code> anyway, but this is seen as unsatisfactory.
 * Hopefully it will eventually be possible to find a better solution.
 *
 * @author Alex Smith
 * @param <T> The class that this knowledge is about.
 */
class LambdaKnowledge<T> extends ClassKnowledge<T> {

    /**
     * Cached class knowledge for the serialized version of this class.
     */
    static final ClassKnowledge<SerializedLambda> SERIALIZED_LAMBDA_KNOWLEDGE
            = ClassKnowledge.knowledgeForClass(SerializedLambda.class);

    /**
     * The readResolve method of class <code>SerializedLambda</code>.
     */
    static final Method READ_RESOLVE_METHOD;

    static {
        try {
            READ_RESOLVE_METHOD = AccessController.doPrivileged(
                    (PrivilegedExceptionAction<Method>) () -> {
                        Method rv = SerializedLambda.class.getDeclaredMethod(
                                "readResolve");
                        rv.setAccessible(true);
                        return rv;
                    });
        } catch (PrivilegedActionException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * The writeReplace method of class <code>T</code>.
     */
    private final Method writeReplaceMethod;

    /**
     * Creates new knowledge about how to copy a class via using its
     * <code>writeReplace</code> method.
     *
     * @param about The class that this knowledge is about.
     */
    LambdaKnowledge(Class<T> about) {
        super(about);

        /* Find the writeReplace method. */
        try {
            writeReplaceMethod = AccessController.doPrivileged(
                    (PrivilegedExceptionAction<Method>) () -> {
                        Method writeReplace = about.getDeclaredMethod("writeReplace");
                        writeReplace.setAccessible(true);
                        return writeReplace;
                    });
        } catch (PrivilegedActionException ex) {
            throw new IllegalArgumentException(
                    "lambda is not writeReplace-able", ex.getCause());
        }
    }

    /**
     * Gets the value of an object of this class after passing through
     * <code>writeReplace()</code>.
     *
     * @param original The original object.
     * @return The replaced object.
     */
    private SerializedLambda getWriteReplaced(T original) {
        try {
            if (original == null) {
                return null;
            }

            return (SerializedLambda) writeReplaceMethod.invoke(original);

        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | ClassCastException ex) {
            throw new DistributedError(ex, "writeReplace on: " + original);
        }
    }

    /**
     * Always returns true. If we can unmarshal the fields of the object
     * reliably, the object itself can also be unmarshalled reliably.
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
     * Returns a code indicating that this knowledge is about a class generated
     * at runtime that does not have a fixed name. This overrides the name that
     * would normally be generated by <code>ClassNameDescriptions</code>.
     *
     * @param expected Ignored.
     * @return <code>ClassNameDescriptions.SYNTHETIC_DESCRIPTION</code>.
     */
    @Override
    byte[] getClassNameDescription(Class<?> expected) {
        return SYNTHETIC_DESCRIPTION;
    }

    /**
     * Calculates the size needed to describe an object of the class that this
     * knowledge is about. Because lambda classes are synthetic and generated
     * lazily at runtime, this will not be a description of the lambda itself,
     * but rather a description of how to create it.
     *
     * @param value The object to measure the size of.
     * @param nullable Should be <code>false</code>; there's no way to know that
     * a <code>null</code> "should" belong to the class this knowledge is about,
     * because it's a synthetic class created at runtime. As such, the only way
     * we'd know we're describing an object of this class is if it is
     * non-<code>null</code>.
     * @return The size of the description, or <code>null</code> if an upper
     * bound was requested but is not available.
     */
    @Override
    ObjectDescription.Size descriptionSizeOfObject(
            T value, boolean nullable) {
        return SERIALIZED_LAMBDA_KNOWLEDGE.descriptionSize(
                () -> getWriteReplaced(value), nullable);
    }

    /**
     * Appends a description of a field with the given value to the given object
     * description. Note that this isn't quite just a copy of
     * <code>writeFieldDescriptionTo</code>; although a
     * <code>SerializedLambda</code> is normally deeply copiable, it isn't when
     * the list of captured arguments includes noncopiable values.
     *
     * @param description The description to append to.
     * @param fieldValue The field to describe.
     * @param nullable Whether to use a description format that takes into
     * account the possibility that the lambda might be <code>null</code>.
     * @throws IOException If something goes wrong writing the description
     */
    @Override
    void describeFieldInto(DescriptionOutput description, Object fieldValue,
            boolean nullable) throws IOException {
        SerializedLambda s = getWriteReplaced(getAbout().cast(fieldValue));
        SERIALIZED_LAMBDA_KNOWLEDGE.describeFieldInto(description, s, nullable);
    }

    @Override
    void writeFieldDescriptionTo(DataOutput sink, Object fieldValue,
            boolean nullable) throws IOException {
        SerializedLambda s = getWriteReplaced(getAbout().cast(fieldValue));
        SERIALIZED_LAMBDA_KNOWLEDGE.writeFieldDescriptionTo(sink, s, nullable);
    }

    /**
     * Throws an exception. Because lambda classes don't always exist, and don't
     * have fixed names, a description should not mention them directly.
     *
     * @param description Ignored.
     * @param nullable Ignored.
     * @return Never returns normally.
     * @throws UnsupportedOperationException Always
     */
    @Override
    T reproduce(ReadableDescription description, boolean nullable)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "Lambda types should not appear in descriptions");
    }
}
