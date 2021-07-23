package xyz.acygn.mokapot.wireformat;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * An object that can be read the same way as an <code>ObjectDescription</code>.
 * In other words, it can read a sequence of bytes (representing the copiable
 * portion of the description) as various data types, and also return objects
 * from a separate sequence of objects.
 * <p>
 * Unlike <code>ObjectDescription</code> itself, this class does not necessarily
 * need to have the data stored concretely in fields ready to return instantly;
 * reading from it can generate the required data on the fly, and may have side
 * effects.
 *
 * @author Alex Smith
 */
public interface ReadableDescription extends DataInput {

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
     * @throws IOException If there is no object to read, or if something goes
     * wrong trying to read the object
     */
    <T> T readNoncopiableObject(Class<T> asClass) throws IOException;

    /**
     * Reads a number of bytes from the description, outputting them as a
     * <code>ByteBuffer</code>. This might potentially share its backing storage
     * with the description itself, so is more efficient than using a byte array
     * in cases where it's possible to operate on buffers directly.
     *
     * @param length The number of bytes to read.
     * @return A buffer holding the read bytes.
     * @throws java.io.IOException If there is no capacity in the buffer
     */
    ByteBuffer readByteSlice(int length) throws IOException;
}
