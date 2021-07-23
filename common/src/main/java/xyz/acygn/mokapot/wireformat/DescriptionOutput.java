package xyz.acygn.mokapot.wireformat;

import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * An interface representing objects that support the same write operations as
 * an <code>ObjectDescription</code> does. This is used to allow writing of
 * descriptions to things other than <code>ObjectDescription</code> objects.
 *
 * @author Alex Smith
 */
public interface DescriptionOutput extends DataOutput {

    /**
     * Appends a reference to an object to the noncopiable part of the
     * description.
     *
     * @param obj The object to append.
     * @throws IOException If this description is full or read-only
     */
    void writeNoncopiableObject(Object obj) throws IOException;

    /**
     * Records/remembers a position within the output. This is relative to an
     * arbitrary offset, but two of these can be subtracted from each other to
     * get an exact measurement of the amount of data that was written from one
     * to the next.
     *
     * @return A position (measured in bytes + objects), relative to some
     * arbitrary location.
     */
    public ObjectDescription.Size getPosition();

    /**
     * A map from each primitive (both boxed and unboxed) to its size, in bytes.
     *
     * Note that this map is not initially intialized (because interfaces can't
     * have static initializers), and must be initialized manually using
     * <code>initSizes()</code>.
     */
    static final Map<Class<?>, Integer> PRIMITIVE_SIZES
            = new HashMap<>();

    /**
     * Initializes the <code>PRIMITIVE_SIZES</code> map. This method is
     * idempotent, so can be safely called before any use of the map.
     */
    static void initSizes() {
        synchronized (DescriptionOutput.class) {
            if (PRIMITIVE_SIZES.isEmpty()) {
                PRIMITIVE_SIZES.put(boolean.class, 1);
                PRIMITIVE_SIZES.put(Boolean.class, 1);
                PRIMITIVE_SIZES.put(byte.class, 1);
                PRIMITIVE_SIZES.put(Byte.class, 1);
                PRIMITIVE_SIZES.put(char.class, 2);
                PRIMITIVE_SIZES.put(Character.class, 2);
                PRIMITIVE_SIZES.put(double.class, 8);
                PRIMITIVE_SIZES.put(Double.class, 8);
                PRIMITIVE_SIZES.put(float.class, 4);
                PRIMITIVE_SIZES.put(Float.class, 4);
                PRIMITIVE_SIZES.put(int.class, 4);
                PRIMITIVE_SIZES.put(Integer.class, 4);
                PRIMITIVE_SIZES.put(long.class, 8);
                PRIMITIVE_SIZES.put(Long.class, 8);
            }
        }
    }

    /**
     * Calculates the number of bytes a primitive class will take up in the
     * description, upon writing to it.
     *
     * @param primitiveClass A Java primitive class.
     * @return The size of that class when serialised, in bytes.
     * @throws IllegalArgumentException If <code>primitiveClass</code> is not a
     * primitive class
     */
    static int sizeOfPrimitive(Class<?> primitiveClass)
            throws IllegalArgumentException {
        initSizes();
        Integer size = PRIMITIVE_SIZES.get(primitiveClass);
        if (size == null) {
            throw new IllegalArgumentException(
                    primitiveClass + " is not a primitive class");
        }
        return size;
    }
}
