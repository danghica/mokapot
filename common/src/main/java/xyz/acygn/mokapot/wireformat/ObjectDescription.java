package xyz.acygn.mokapot.wireformat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import xyz.acygn.mokapot.util.DataByteBuffer;
import static xyz.acygn.mokapot.util.StringifyUtils.stringify;

/**
 * An object's class, plus a flattened version of an object's fields. Any
 * primitive or array-typed fields are converted into a sequence of bytes; any
 * <code>Copiable</code> (or implicitly copiable) fields recursively have their
 * class and fields included in the object description; any noncopiable fields
 * are kept (at this stage) as references.
 * <p>
 * The resulting data structure consists of two parts: a sequence of bytes
 * containing the copiable portion of the object, and a sequence of references
 * containing the noncopiable portion. The description can be converted to a
 * marshalled form of the object via converting each reference to a
 * <code>ReferenceValue</code>. Alternatively, it can be converted to a shallow
 * copy of the original object via reversing the process used to construct it.
 * (This differs from a serialisation of an object, which would be converted to
 * a <i>deep</i> copy of the object upon being deserialised.)
 * <p>
 * An object description is initially mutable and write-only, a "partial object
 * description" that contains the object description so far. (In other words,
 * it's more like a <code>StringBuilder</code> than a <code>String</code>.) Once
 * writing to it is complete, calling <code>close()</code> makes it immutable
 * and read-only, instead; it's now a fully complete description with a read
 * cursor.
 * <p>
 * An object description cannot expand once created; the caller should calculate
 * the size of description needed before constructing it.
 *
 * @author Alex Smith
 */
public class ObjectDescription
        extends DataByteBuffer implements ReadableDescription, DescriptionOutput {

    /**
     * The character set into which Java identifiers are encoded in order to
     * store them in descriptions. Currently US-ASCII, but stored in a
     * centralised location to make it easy to change for compatibility with
     * non-English identifiers.
     */
    public static final Charset IDENTIFIER_CHARSET;

    static {
        IDENTIFIER_CHARSET = Charset.forName("US-ASCII");
    }

    /**
     * The subset of the object description that's expressed as a sequence of
     * references. These are (inherently copiable) references to noncopiable
     * objects.
     */
    private final Object[] references;

    /**
     * The first index in <code>references</code> that isn't currently filled
     * in. Used to determine where to write to it, when writing an object that
     * requires adding references.
     */
    private int referencesWriteCursor;

    /**
     * The index in <code>references</code> that we're currently reading. Used
     * to determine where to read from it. This is -1 while this is still a
     * partial description, and set to a nonnegative number when it's completed;
     * from that point onwards, the description is read-only.
     */
    private int referencesReadCursor;

    /**
     * Creates a new object description. It will originally be empty, allowing
     * data to be written to it.
     *
     * @param size The amount of data that will be written to the object
     * description. This may be an overestimate.
     */
    public ObjectDescription(Size size) {
        super(size.getByteCount());
        references = new Object[size.getObjectCount()];
        referencesWriteCursor = 0;
        referencesReadCursor = -1;
    }

    /**
     * Makes this object description readable and read-only, and resets the read
     * pointers. Used once writing to the object description is complete, or
     * once reading the object description is complete (to allow it to be read
     * again).
     */
    @Override
    public void resetForRead() {
        super.resetForRead();
        referencesReadCursor = 0;
    }

    /**
     * Checks to see whether this description is rewound. That is, that the
     * description has been placed into read-only mode, and the read cursor is
     * currently at the start of its data (indicating that none of it was read
     * since the previous reset) in both the copiable and noncopiable portions.
     * <p>
     * This method differs from the method in the superclass, which only checks
     * the noncopiable portion.
     *
     * @return Whether this description is rewound.
     */
    @Override
    public boolean isRewound() {
        return super.isRewound() && referencesReadCursor == 0;
    }

    /**
     * Appends a reference to an object to the noncopiable part of the
     * description.
     *
     * @param obj The object to append.
     * @throws IOException If this description is full or read-only
     */
    @Override
    public void writeNoncopiableObject(Object obj) throws IOException {
        if (referencesReadCursor != -1) {
            throw new IOException("Cannot write to a read-only description");
        }
        try {
            references[referencesWriteCursor] = obj;
            referencesWriteCursor++;
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IOException("Out of capacity for objects", ex);
        }
    }

    /**
     * Reads an object from the noncopiable portion of this description. This
     * operation can only be done reliably if the actual class of the object to
     * read is already known. Note that if the wrong class is given, it may not
     * be possible to detect this fact (although a
     * <code>ClassCastException</code> may well be thrown in cases where it is
     * detectable).
     *
     * @param <T> The class of the object to read.
     * @param asClass <code>T.class</code>, given explicitly due to Java's type
     * erasure rules.
     * @return The object that was read.
     * @throws IOException If the end of the description has been reached, or if
     * the description is write-only
     */
    @Override
    public <T> T readNoncopiableObject(Class<T> asClass) throws IOException {
        try {
            T o = asClass.cast(references[referencesReadCursor]);
            referencesReadCursor++;
            return o;
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Reads the entire list of noncopiable objects. This is intended for use
     * only by serialisation algorithms that serialise the entire object
     * description in one go, and may well be hard to use in other contexts.
     * (Note that it doesn't use the cursors, like most other methods do, so it
     * can only be used when focusing on the entire description as a whole, and
     * isn't useful to build up a description in a compositional way.)
     *
     * @return An unmodifiable list of noncopiable objects.
     */
    public List<?> listAllNoncopiableObjects() {
        return Collections.unmodifiableList(Arrays.asList(references));
    }

    /**
     * Produces a debug dump of the bytes that make up this description.
     *
     * @return A string representation of the description.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("object description: ");
        sb.append(super.toString());
        if (referencesWriteCursor == 0) {
            return sb.toString();
        }

        sb.append(" + [object list: ");

        for (int i = 0; i < referencesWriteCursor; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(stringify(references[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public Size getPosition() {
        return new Size(getWrittenLength(), referencesReadCursor == -1
                ? referencesWriteCursor : referencesReadCursor);
    }

    /**
     * The size of an object description. This is measured as a number of bytes
     * for the copiable portion, plus a number of objects for the noncopiable
     * portion.
     * <p>
     * <code>ObjectDescription.Size</code> objects are immutable, and act like
     * value types.
     */
    public static class Size {

        /**
         * The size of an empty object description (that is, no bytes and no
         * objects).
         */
        public static final Size ZERO = new Size(0, 0);

        /**
         * The size of a description of a length. Lengths are currently
         * represented as integers, so this is 4 bytes long.
         */
        public static final Size LENGTH_SIZE = new Size(4, 0);

        /**
         * The number of bytes in the copiable portion of the description.
         */
        private final int byteCount;

        /**
         * The number of objects in the noncopiable portion of the description.
         */
        private final int objectCount;

        /**
         * Creates a new object description size, specifying each field
         * individually.
         *
         * @param byteCount The number of bytes in the copiable portion of the
         * description.
         * @param objectCount The number of references to noncopiable objects in
         * the description.
         */
        public Size(int byteCount, int objectCount) {
            this.byteCount = byteCount;
            this.objectCount = objectCount;
        }

        /**
         * Returns the size of the copiable portion of the description.
         *
         * @return A number of bytes.
         */
        public int getByteCount() {
            return byteCount;
        }

        /**
         * Returns the size of the noncopiable portion of the description.
         *
         * @return A number of object references.
         */
        public int getObjectCount() {
            return objectCount;
        }

        /**
         * Calculates this size, multiplied by an integer.
         *
         * @param factor The integer to multiply by.
         * @return A newly constructed <code>ObjectDescription.Size</code> which
         * is <code>factor</code> times as large.
         */
        public Size scale(int factor) {
            return new Size(byteCount * factor, objectCount * factor);
        }

        /**
         * Calculates the sum of this size and another size.
         *
         * @param other The other size to add.
         * @return A newly constructed <code>ObjectDescription.Size</code> which
         * contains the sum of the two sizes.
         */
        public Size add(Size other) {
            return new Size(byteCount + other.byteCount,
                    objectCount + other.objectCount);
        }

        /**
         * Calculates the difference of this size and another size.
         *
         * @param other The other size to subtract from this size.
         * @return A newly constructed <code>ObjectDescription.Size</code> which
         * contains the difference of the two sizes.
         */
        public Size subtract(Size other) {
            return new Size(byteCount - other.byteCount,
                    objectCount - other.objectCount);
        }

        /**
         * Calculates the sum of this size and the given number of bytes.
         *
         * @param count The number of bytes to add.
         * @return This size, plus <code>count</code> bytes in the copiable
         * portion.
         */
        public Size addBytes(int count) {
            return new Size(byteCount + count, objectCount);
        }

        /**
         * Calculates the sum of this size and the given number of objects.
         *
         * @param count The number of objects to add.
         * @return This size, plus <code>count</code> objects in the noncopiable
         * portion.
         */
        public Size addObjects(int count) {
            return new Size(byteCount, objectCount + count);
        }

        /**
         * Returns a value calculated so that two identical sizes have identical
         * hashes.
         *
         * @return This size's hash.
         */
        @Override
        public int hashCode() {
            return this.byteCount * 19 + this.objectCount * 65537;
        }

        /**
         * Value equality of sizes.
         *
         * @param obj The object to compare to.
         * @return Whether the other object is a size, and the same size as this
         * one.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final Size other = (Size) obj;
            return this.byteCount == other.byteCount
                    && this.objectCount == other.objectCount;
        }

        /**
         * Outputs a debug description of this object.
         *
         * @return A string representing the object.
         */
        @Override
        public String toString() {
            return "Size(" + byteCount + ", " + objectCount + ")";
        }

        /**
         * Returns whether this size is not larger in any dimension than the
         * given size.
         *
         * @param compareTo The size to compare to.
         * @return <code>false</code> if this size has more bytes than
         * <code>compareTo</code> and/or more objects than
         * <code>compareTo</code>; otherwise <code>true</code>.
         */
        public boolean fitsWithin(Size compareTo) {
            return byteCount <= compareTo.byteCount
                    && objectCount <= compareTo.objectCount;
        }
    }
}
