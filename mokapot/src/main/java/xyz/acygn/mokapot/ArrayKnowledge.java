package xyz.acygn.mokapot;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import static java.lang.invoke.MethodHandles.arrayElementGetter;
import static java.lang.invoke.MethodHandles.arrayElementSetter;
import java.lang.reflect.Array;
import static java.lang.reflect.Modifier.isFinal;
import static xyz.acygn.mokapot.DescriptionWriter.DESCRIBE_FIELD_INTO;
import static xyz.acygn.mokapot.DescriptionWriter.WRITE_FIELD_DESCRIPTION_TO;
import static xyz.acygn.mokapot.wireformat.ClassNameDescriptions.NULL_DESCRIPTION;
import static xyz.acygn.mokapot.wireformat.ClassNameDescriptions.NULL_DESCRIPTION_INT;
import xyz.acygn.mokapot.wireformat.DescriptionOutput;
import xyz.acygn.mokapot.wireformat.ObjectDescription.Size;
import static xyz.acygn.mokapot.wireformat.ObjectDescription.Size.LENGTH_SIZE;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * Knowledge about array classes. In particular. this handles the special case
 * needed to create an <code>ObjectCopy</code> equivalent for an array (because
 * arrays aren't objects in the normal sense, and in particular don't store data
 * in fields).
 *
 * @author Alex Smith
 * @param <T> The array class to store knowledge about.
 */
class ArrayKnowledge<T> extends ClassKnowledge<T> {

    /**
     * The actual type of all elements of this array, in the (uncommon) case
     * where it's known at compile time. Will be <code>null</code> if the actual
     * type can be inconsistent. Examples where the types are known at compile
     * time include the situations where <code>T</code> is <code>final</code>,
     * and where <code>T</code> is a primitive.
     */
    private final Class<?> invariantComponentType;

    /**
     * The declared type of the array elements. Unlike
     * <code>invariantComponentType</code>, this is always known at compile
     * time.
     */
    private final Class<?> componentType;

    /**
     * Records whether this array's component type is an object type. (This
     * makes it possible to avoid recording nullability for each individual
     * element of an array of primitives.)
     */
    private final boolean componentsAreObjects;

    /**
     * Method that writes the content of an array of this type into an object
     * description.
     */
    private final MethodHandle arrayReader;

    /**
     * Method that reads the content of an array of this type from an object
     * description.
     */
    private final MethodHandle arrayWriter;

    /**
     * Creates a new object for storing knowledge about an array class.
     *
     * @param about The array class to store knowledge about.
     * @throws IllegalArgumentException If <code>about</code> is not an array
     * class
     */
    ArrayKnowledge(Class<T> about) throws IllegalArgumentException {
        super(about);
        if (!about.isArray()) {
            throw new IllegalArgumentException(
                    about + " is not an array class");
        }

        this.componentType = getAbout().getComponentType();
        if (isFinal(componentType.getModifiers())) {
            invariantComponentType = componentType;
        } else {
            invariantComponentType = null;
        }

        arrayReader = arrayElementGetter(about);
        arrayWriter = arrayElementSetter(about);

        componentsAreObjects = !(componentType.isPrimitive());
    }

    /**
     * Always returns false. We have no way to know that an array is read-only,
     * and an array unmarshals into a copy of itself, rather than a reference to
     * itself.
     *
     * @return <code>false</code>.
     */
    @Override
    public boolean unmarshalsReliably(boolean asField) {
        return false;
    }

    /**
     * Always returns true. It's impossible to produce a long reference to an
     * array, thus the unmarshalled form of an array must be a copy.
     *
     * @return <code>true</code>.
     */
    @Override
    public boolean unmarshalsAsCopy() {
        return true;
    }

    @Override
    Size descriptionSizeOfObject(T value, boolean nullable)
            throws NullPointerException {
        /* We start by recording the array length. For a null array, we record
           this as -1; thus, an array description is always at least 4 bytes
           long. Note that this also means that our nullable and non-nullable
           encodings for arrays are the same. */
        if (value == null) {
            return LENGTH_SIZE;
        }

        int length = Array.getLength(value);
        /* A length of 0 is another case where we can shortcut. */
        if (length == 0) {
            return LENGTH_SIZE;
        }

        if (invariantComponentType != null) {
            /* We need to store the length, plus the nullable value of each
               array element. As a shortcut, first see if we can calculate this
               without looking at the actual elements. */
            Size elementSize
                    = ClassKnowledge.knowledgeForClass(invariantComponentType)
                            .descriptionSize(null, true);
            if (elementSize != null) {
                return elementSize.scale(length).add(LENGTH_SIZE);
            }
            /* If that failed, we're going to have to add the size of each
               element individually. At this point, we know it isn't an array
               of primitives, so we can safely cast it to Object[]. */
            Object[] castValue = (Object[]) value;
            Size rv = LENGTH_SIZE;
            ClassKnowledge<?> elementKnowledge
                    = ClassKnowledge.knowledgeForClass(invariantComponentType);
            for (Object element : castValue) {
                rv = rv.add(elementKnowledge.descriptionSize(() -> element,
                        componentsAreObjects));
            }
            return rv;
        } else {
            /* We need to store the length, plus the value and type of each
               array element. */
            Object[] castValue = (Object[]) value;
            Size rv = LENGTH_SIZE;
            for (Object element : castValue) {
                if (element == null) {
                    /* The encoding of <code>null</code> is 4 bytes long. */
                    rv = rv.addBytes(4);
                } else {
                    ClassKnowledge<?> elementKnowledge
                            = ClassKnowledge.knowledgeForActualClass(element);
                    rv = rv.add(new Size(elementKnowledge.
                            getClassNameDescription(componentType).length, 0));
                    rv = rv.add(elementKnowledge.descriptionSize(
                            () -> element, false));
                }
            }
            return rv;
        }
    }

    /**
     * Code common to the implementations of <code>describeFieldInto</code> and
     * <code>writeFieldDescriptionTo</code>.
     *
     * @param <T> The type of <code>into</code>.
     * @param writer The method used to recursively write descriptions of the
     * array's elements.
     * @param into The place to write the description.
     * @param fieldValue The array to describe.
     * @param nullable Ignored; arrays do not have separate nullable and
     * non-nullable description formats.
     * @throws IOException If something goes wrong writing
     */
    private <T extends DataOutput> void describeInner(
            DescriptionWriter<T> writer, T into,
            Object fieldValue, boolean nullable) throws IOException {
        if (fieldValue == null) {
            into.writeInt(-1);
            return;
        }

        int length = Array.getLength(fieldValue);
        into.writeInt(length);

        try {
            if (invariantComponentType != null) {
                ClassKnowledge<?> elementKnowledge
                        = ClassKnowledge.knowledgeForClass(
                                invariantComponentType);
                for (int i = 0; i < length; i++) {
                    writer.describeTo(elementKnowledge, into,
                            arrayReader.invoke(fieldValue, i),
                            componentsAreObjects);
                }
            } else {
                for (int i = 0; i < length; i++) {
                    Object element = arrayReader.invoke(fieldValue, i);
                    if (element == null) {
                        into.write(NULL_DESCRIPTION);
                    } else {
                        ClassKnowledge<?> elementKnowledge
                                = ClassKnowledge.knowledgeForActualClass(
                                        element);
                        into.write(elementKnowledge.
                                getClassNameDescription(componentType));
                        writer.describeTo(elementKnowledge, into,
                                element, false);
                    }
                }
            }
        } catch (Throwable ex) {
            /* An unfortunate consequence of duck-typed methods is that you
               don't have information on checked exceptions, so even though they
               shouldn't be happening, we need a handler for them. Just wrap
               them in a type of exception we're expecting. */
            if (ex instanceof IOException) {
                throw (IOException) ex;
            } else if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new IOException(ex);
        }
    }

    @Override
    void describeFieldInto(DescriptionOutput description, Object fieldValue,
            boolean nullable) throws IOException {
        describeInner(DESCRIBE_FIELD_INTO,
                description, fieldValue, nullable);
    }

    @Override
    void writeFieldDescriptionTo(DataOutput sink, Object fieldValue,
            boolean nullable) throws IOException, UnsupportedOperationException {
        describeInner(WRITE_FIELD_DESCRIPTION_TO,
                sink, fieldValue, nullable);
    }

    @Override
    T reproduce(ReadableDescription description, boolean nullable)
            throws IOException {
        int length = description.readInt();
        /* Did we read the sentinel for null? */
        if (length == NULL_DESCRIPTION_INT) {
            return null;
        }

        @SuppressWarnings("unchecked")
        T rv = (T) Array.newInstance(getAbout().getComponentType(), length);

        try {
            if (invariantComponentType != null) {
                ClassKnowledge<?> elementKnowledge
                        = ClassKnowledge.knowledgeForClass(
                                invariantComponentType);
                for (int i = 0; i < length; i++) {
                    arrayWriter.invoke(rv, i,
                            elementKnowledge.reproduce(description,
                                    componentsAreObjects));
                }
            } else {
                for (int i = 0; i < length; i++) {
                    Object element = Marshalling.rCAOStatic(
                            description, rv, componentType);
                    arrayWriter.invoke(rv, i, element);
                }
            }

            return rv;
        } catch (Throwable ex) {
            if (ex instanceof IOException) {
                throw (IOException) ex;
            } else if (ex instanceof RuntimeException
                    || ex instanceof Error) {
                throw (RuntimeException) ex;
            }
            throw new IOException(ex);
        }
    }
}
