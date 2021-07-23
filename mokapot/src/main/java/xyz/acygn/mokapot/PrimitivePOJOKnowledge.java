package xyz.acygn.mokapot;

import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.net.Inet4Address;
import java.net.Inet6Address;
import xyz.acygn.mokapot.wireformat.ObjectDescription;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * Knowledge about specific classes whose objects' fields are nothing but
 * immutable primitives. Notably, this includes unboxed primitives, boxed
 * primitives, and a few more complex classes that would cause an infinite
 * regress if not special-cased (due to being used internally within the
 * marshalling system).
 *
 * @author Alex Smith
 * @param <T> The boxed representation of the class in question.
 */
abstract class PrimitivePOJOKnowledge<T extends Serializable>
        extends SpecialCaseKnowledge<T> {

    /**
     * Protected constructor. Used to provide information about the primitive
     * POJO in question.
     *
     * @param about The class that this knowledge is about.
     */
    protected PrimitivePOJOKnowledge(java.lang.Class<T> about) {
        super(about);
    }

    /**
     * Always throws an exception. Primitives and POJOs have a fixed size, so we
     * shouldn't be inspecting an object to determine how large they are.
     *
     * @param value Ignored.
     * @param nullable Ignored.
     * @return Never returns.
     * @throws IllegalStateException Always
     */
    @Override
    ObjectDescription.Size descriptionSizeOfObject(T value, boolean nullable) {
        throw new IllegalStateException(
                "Inspecting an object to determine a primitive's size");
    }

    /**
     * Writes a primitive POJO of the type that this class is about to a data
     * output sink.
     *
     * @param sink The sink to write into.
     * @param fieldValue The primitive POJO to write.
     * @param nullable Whether to use an extra byte at the start of the
     * description to allow a distinct representation for <code>null</code>
     * @throws IOException If there is not enough room in the description
     */
    @Override
    void writeFieldDescriptionTo(DataOutput sink,
            Object fieldValue, boolean nullable) throws IOException {
        if (nullable) {
            sink.writeBoolean(fieldValue != null);
            if (fieldValue == null) {
                return;
            }
        }

        T castValue = getAbout().cast(fieldValue);
        describeNonNullFields(sink, castValue);
    }

    /**
     * Reproduces the primitive POJO by reading it from a description.
     *
     * @param description The description to read from.
     * @param nullable Whether the description is written in a format that has a
     * separate representation for <code>null</code>.
     * @return The reconstructed primitive POJO.
     * @throws IOException If the description is detected as being corrupted
     */
    @Override
    T reproduce(ReadableDescription description, boolean nullable)
            throws IOException {
        if (nullable) {
            if (!description.readBoolean()) {
                return null;
            }
        }
        return reproduceNonNullFields(description);
    }

    /**
     * Code to write the fields of the primitive POJO this class is about to a
     * data output sink. The default definition of
     * <code>describeFieldInto</code> is implemented in terms of this method,
     * which is used to contain the code that varies from one POJO to another
     * (<code>describeFieldInto</code> contains the common code).
     *
     * @param sink The data output sink to write to.
     * @param obj The object to describe. Will not be <code>null</code>.
     * @throws IOException If something goes wrong while describing (e.g. lack
     * of room in the description)
     */
    protected abstract void describeNonNullFields(
            DataOutput sink, T obj) throws IOException;

    /**
     * Code to read the fields of the primitive POJO this class is about from an
     * object description. The default definition of <code>reproduce</code> is
     * implemented in terms of this method, which is used to contain the code
     * that varies from one POJO to another (<code>reproduce</code> contains the
     * common code).
     *
     * @param description The description to describe into.
     * @return The reproduced object.
     * @throws java.io.IOException If something goes wrong while describing
     * (e.g. lack of room in the description)
     */
    protected abstract T reproduceNonNullFields(ReadableDescription description)
            throws IOException;

    /**
     * Special-cased class knowledge for <code>bool</code> and
     * <code>Boolean</code>.
     */
    static class Bool extends PrimitivePOJOKnowledge<Boolean> {

        /**
         * Constructs the object. As usual for special-cased knowledge, all
         * objects of this type are effectively identical.
         */
        Bool() {
            super(Boolean.class);
        }

        @Override
        protected void describeNonNullFields(
                DataOutput sink, Boolean obj)
                throws IOException {
            sink.writeBoolean(obj);
        }

        @Override
        protected Boolean reproduceNonNullFields(ReadableDescription description)
                throws IOException {
            return description.readBoolean();
        }
    }

    /**
     * Special-cased class knowledge for <code>byte</code> and
     * <code>Byte</code>.
     */
    static class Byte extends PrimitivePOJOKnowledge<java.lang.Byte> {

        /**
         * Constructs the object. As usual for special-cased knowledge, all
         * objects of this type are effectively identical.
         */
        Byte() {
            super(java.lang.Byte.class);
        }

        @Override
        protected void describeNonNullFields(
                DataOutput sink, java.lang.Byte obj)
                throws IOException {
            sink.writeByte(obj);
        }

        @Override
        protected java.lang.Byte reproduceNonNullFields(
                ReadableDescription description) throws IOException {
            return description.readByte();
        }
    }

    /**
     * Special-cased class knowledge for <code>char</code> and
     * <code>Character</code>.
     */
    static class Char extends PrimitivePOJOKnowledge<Character> {

        /**
         * Constructs the object. As usual for special-cased knowledge, all
         * objects of this type are effectively identical.
         */
        Char() {
            super(Character.class);
        }

        @Override
        protected void describeNonNullFields(
                DataOutput sink, Character obj)
                throws IOException {
            sink.writeChar(obj);
        }

        @Override
        protected Character reproduceNonNullFields(
                ReadableDescription description) throws IOException {
            return description.readChar();
        }
    }

    /**
     * Special-cased class knowledge for <code>double</code> and
     * <code>Double</code>.
     */
    static class Double extends PrimitivePOJOKnowledge<java.lang.Double> {

        /**
         * Constructs the object. As usual for special-cased knowledge, all
         * objects of this type are effectively identical.
         */
        Double() {
            super(java.lang.Double.class);
        }

        @Override
        protected void describeNonNullFields(
                DataOutput sink, java.lang.Double obj)
                throws IOException {
            sink.writeDouble(obj);
        }

        @Override
        protected java.lang.Double reproduceNonNullFields(
                ReadableDescription description) throws IOException {
            return description.readDouble();
        }
    }

    /**
     * Special-cased class knowledge for <code>float</code> and
     * <code>Float</code>.
     */
    static class Float extends PrimitivePOJOKnowledge<java.lang.Float> {

        /**
         * Constructs the object. As usual for special-cased knowledge, all
         * objects of this type are effectively identical.
         */
        Float() {
            super(java.lang.Float.class);
        }

        @Override
        protected void describeNonNullFields(
                DataOutput sink, java.lang.Float obj)
                throws IOException {
            sink.writeFloat(obj);
        }

        @Override
        protected java.lang.Float reproduceNonNullFields(
                ReadableDescription description) throws IOException {
            return description.readFloat();
        }
    }

    /**
     * Special-cased class knowledge for <code>int</code> and
     * <code>Integer</code>.
     */
    static class Int extends PrimitivePOJOKnowledge<Integer> {

        /**
         * Constructs the object. As usual for special-cased knowledge, all
         * objects of this type are effectively identical.
         */
        Int() {
            super(Integer.class);
        }

        @Override
        protected void describeNonNullFields(
                DataOutput sink, Integer obj)
                throws IOException {
            sink.writeInt(obj);
        }

        @Override
        protected Integer reproduceNonNullFields(ReadableDescription description)
                throws IOException {
            return description.readInt();
        }
    }

    /**
     * Special-cased class knowledge for <code>long</code> and
     * <code>Long</code>.
     */
    static class Long extends PrimitivePOJOKnowledge<java.lang.Long> {

        /**
         * Constructs the object. As usual for special-cased knowledge, all
         * objects of this type are effectively identical.
         */
        Long() {
            super(java.lang.Long.class);
        }

        @Override
        protected void describeNonNullFields(
                DataOutput sink, java.lang.Long obj)
                throws IOException {
            sink.writeLong(obj);
        }

        @Override
        protected java.lang.Long reproduceNonNullFields(
                ReadableDescription description) throws IOException {
            return description.readLong();
        }
    }

    /**
     * Special-cased class knowledge for <code>Inet4Address</code>.
     */
    static class IPv4 extends PrimitivePOJOKnowledge<Inet4Address> {

        /**
         * Constructs the object. As usual for special-cased knowledge, all
         * objects of this type are effectively identical.
         */
        IPv4() {
            super(Inet4Address.class);
        }

        @Override
        protected void describeNonNullFields(
                DataOutput sink, Inet4Address obj)
                throws IOException {
            sink.write(obj.getAddress());
        }

        @Override
        protected Inet4Address reproduceNonNullFields(
                ReadableDescription description) throws IOException {
            byte[] addr = new byte[4];
            description.readFully(addr);
            return (Inet4Address) Inet4Address.getByAddress(addr);
        }
    }

    /**
     * Special-cased class knowledge for <code>Inet4Address</code>.
     */
    static class IPv6 extends PrimitivePOJOKnowledge<Inet6Address> {

        /**
         * Constructs the object. As usual for special-cased knowledge, all
         * objects of this type are effectively identical.
         */
        IPv6() {
            super(Inet6Address.class);
        }

        @Override
        protected void describeNonNullFields(
                DataOutput sink, Inet6Address obj)
                throws IOException {
            sink.write(obj.getAddress());
        }

        @Override
        protected Inet6Address reproduceNonNullFields(
                ReadableDescription description) throws IOException {
            byte[] addr = new byte[16];
            description.readFully(addr);
            return (Inet6Address) Inet6Address.getByAddress(addr);
        }
    }
}
