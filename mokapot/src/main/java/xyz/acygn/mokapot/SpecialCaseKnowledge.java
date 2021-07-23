package xyz.acygn.mokapot;

import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import xyz.acygn.mokapot.wireformat.ClassNameDescriptions;
import static xyz.acygn.mokapot.wireformat.ClassNameDescriptions.NULL_DESCRIPTION;
import static xyz.acygn.mokapot.wireformat.ClassNameDescriptions.NULL_DESCRIPTION_INT;
import xyz.acygn.mokapot.wireformat.ObjectDescription.Size;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * Knowledge of how to handle special cases in the marshalling system. These
 * "special cases" are single specific classes that are immutable (i.e.
 * references to them act like values do), and cannot be serialised
 * field-by-field for technical reasons. This includes primitives (which
 * actually are value types and which don't have fields); strings (which are
 * much more complex internally than we ant in a serialisation format); classes
 * (which often need abbreviation and may need special techniques to load them
 * at the other side); and IP addresses (which are used internally in the
 * marshalling mechanism, and thus marshalling them normally could cause an
 * infinite regress).
 *
 * @author Alex Smith
 * @param <T> The class which this knowledge is about.
 */
abstract class SpecialCaseKnowledge<T extends Serializable>
        extends ClassKnowledge<T> {

    /**
     * Creates class knowledge for the given class, which must be one of the
     * recognised special cases.
     *
     * @param about The class to create the knowledge about.
     */
    SpecialCaseKnowledge(java.lang.Class<T> about) {
        super(about);
    }

    /**
     * Always returns true. Marshalling and unmarshalling these objects is
     * always reliable, due to the special cases used to ensure this.
     *
     * @return Whether <code>about</code> is not an array class.
     */
    @Override
    public boolean unmarshalsReliably(boolean asField) {
        return !getAbout().isArray();
    }

    /**
     * Always returns true. Marshalling and unmarshalling these objects produces
     * a copy, because they're treated as value types.
     *
     * @return <code>true</code>
     */
    @Override
    public boolean unmarshalsAsCopy() {
        return true;
    }

    /**
     * Special-cased knowledge about the class <code>String</code>.
     */
    static class String extends SpecialCaseKnowledge<java.lang.String> {

        /**
         * Encoding used to encode strings in object descriptions.
         */
        static final Charset STRING_CHARSET;

        static {
            STRING_CHARSET = Charset.forName("UTF-8");
        }

        /**
         * Constructs the object. As usual for special-cased knowledge, all
         * objects of this type are effectively identical.
         */
        String() {
            super(java.lang.String.class);
        }

        /**
         * Calculates the length a string will have after serialising it.
         *
         * @param str The string to calculate the serialised size of.
         * @param nullable Ignored; a <code>null</code> String can be
         * represented in no more bytes than a null (i.e. zero-length) String.
         * @return The number of bytes in the string, including the length
         * header.
         */
        @Override
        Size descriptionSizeOfObject(java.lang.String str, boolean nullable) {
            /* A <code>null</code> string is encoded using a length value of
               -1 (much like a <code>null</code> array). */
            if (str == null) {
                return Size.LENGTH_SIZE;
            }
            return Size.LENGTH_SIZE.addBytes(str.getBytes(STRING_CHARSET).length);
        }

        /**
         * Writes the length and content of a string to the given data output
         * sink. The string will be encoded appropriately, and the length will
         * be expressed as the number of bytes in the encoded string (so that
         * decoding knows where to stop reading).
         *
         * @param sink The sink to write to.
         * @param fieldValue The string to write.
         * @param nullable Ignored (existing only to comply with the
         * <code>ClassKnowledge</code> API).
         * @throws IOException If there is no more space in the object
         * description
         */
        @Override
        void writeFieldDescriptionTo(DataOutput sink,
                Object fieldValue, boolean nullable) throws IOException {
            if (fieldValue == null) {
                sink.writeInt(-1);
                return;
            }
            java.lang.String str = (java.lang.String) fieldValue;
            byte[] strBytes = str.getBytes(STRING_CHARSET);
            sink.writeInt(strBytes.length);
            sink.write(strBytes);
        }

        /**
         * Reads the length and content of a string from the given object
         * description. The string will be assumed to have been encoded
         * appropriately; the length is the number of bytes in the encoded
         * content.
         *
         * @param description The description to write to.
         * @param nullable Ignored (existing only to comply with the
         * <code>ClassKnowledge</code> API).
         * @return The string that was read.
         * @throws IOException If the description is detected to be corrupted
         */
        @Override
        java.lang.String reproduce(ReadableDescription description,
                boolean nullable) throws IOException {
            int length = description.readInt();
            if (length == -1) {
                return null;
            }
            ByteBuffer b = description.readByteSlice(length);
            return STRING_CHARSET.decode(b).toString();
        }
    }

    /**
     * Special-cased knowledge about the class <code>Class</code>.
     */
    static class Class extends SpecialCaseKnowledge<java.lang.Class<?>> {

        /**
         * Constructs the object. As usual for special-cased knowledge, all
         * objects of this type are effectively identical.
         */
        @SuppressWarnings("unchecked")
        Class() {
            /* Why is it so hard to create an object of class
               Class<Class<?>>? */
            super((java.lang.Class<java.lang.Class<?>>) (java.lang.Class<?>) java.lang.Class.class);
        }

        /**
         * Calculates the length of the serialised form of a class name.
         *
         * @param c The class to calculate the serialised size of.
         * @param nullable Ignored; there is no clash between the representation
         * of a <code>null</code> <code>Class</code> and that of actual
         * <code>Class</code> objects.
         * @return The length of the class name.
         */
        @Override
        Size descriptionSizeOfObject(java.lang.Class<?> c, boolean nullable) {
            if (c == null) {
                /* Null, as a class, has a description that's a single integer,
                   i.e. 4 bytes long. */
                return new Size(4, 0);
            }

            ClassKnowledge<?> ck = ClassKnowledge.knowledgeForClass(c);
            return new Size(ck.getClassNameDescription(null).length, 0);
        }

        /**
         * Writes a description of a given class to a data output sink.
         *
         * @param sink The sink to write to.
         * @param fieldValue The class to store.
         * @param nullable Ignored (existing only to comply with the
         * <code>ClassKnowledge</code> API).
         * @throws IOException If there is no more space in the object
         * description
         */
        @Override
        void writeFieldDescriptionTo(DataOutput sink,
                Object fieldValue, boolean nullable) throws IOException {
            if (fieldValue == null) {
                sink.write(NULL_DESCRIPTION);
                return;
            }

            java.lang.Class<?> c = (java.lang.Class) fieldValue;
            ClassKnowledge<?> ck = ClassKnowledge.knowledgeForClass(c);
            sink.write(ck.getClassNameDescription(null));
        }

        /**
         * Reads a class from a description.
         *
         * @param description The description to read from.
         * @param nullable Ignored (existing only to comply with the
         * <code>ClassKnowledge</code> API).
         * @return The class read from the description.
         * @throws IOException If something goes wrong (e.g. the class not
         * existing, or the description being detected as corrupted)
         */
        @Override
        java.lang.Class<?> reproduce(ReadableDescription description,
                boolean nullable) throws IOException {
            int length = description.readInt();
            if (length == NULL_DESCRIPTION_INT) {
                return null;
            } else if (length < 0) {
                return ClassNameDescriptions.specialCasedClass(length);
            } else {
                ByteBuffer b = description.readByteSlice(length);
                try {
                    return ClassNameDescriptions.fromByteSlice(b);
                } catch (ClassNotFoundException ex) {
                    throw new IOException(ex);
                }
            }
        }
    }
}
