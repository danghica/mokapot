package xyz.acygn.millr.util;

import xyz.acygn.mokapot.skeletons.ProxyOrWrapper;

import java.lang.reflect.Array;
import java.lang.ClassCastException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.WeakHashMap;


/**
 * A class that acts, in most respects, identically to a Java array. However,
 * being an actual class, rather than a primitive, makes it possible to inherit
 * from, intercept methods of, etc..
 *
 * @param <T> The component type of the array.
 * @author Thomas Cuvillier
 */
public final class ObjectArrayWrapper<T> implements ArrayWrapper {

    /**
     * The array that stores this array wrapper's data.
     */
    //   private final Class<T> componentType;
    private final Object[] storage;

    private final int length;

    private final static WeakHashMap<Object[], WeakReference<ObjectArrayWrapper>> MAP = new WeakHashMap<>();

    /**
     * Creates a new array wrapper, with the given length and type.
     *
     * @param length        The number of elements in the wrapped array. This cannot be
     *                      changed after the array wrapper is created.
     * @param componentType The component type of the created array.
     * @param <T>           The class of the components of the array.
     */
    @SuppressWarnings("unchecked")
    private <T> ObjectArrayWrapper(final int length, final Class<T> componentType) {
        storage = (Object[]) Array.newInstance(componentType, length);
        this.length = length;
        //    this.componentType = componentType;
    }

    public final static synchronized <T> ObjectArrayWrapper<T> getObjectArrayWrapper(final int length, final Class<T> componentType) {
        final ObjectArrayWrapper newArrayWrapper = new ObjectArrayWrapper(length, componentType);
        MAP.put(newArrayWrapper.asArray(), new WeakReference<>(newArrayWrapper));
        return newArrayWrapper;
    }

    private <T> ObjectArrayWrapper(final Class<T> componentType, final int... dims) {
        try {
            storage = (Object[]) Array.newInstance(componentType, dims);
            length = dims[0];
        }
        catch (Throwable e){
            System.out.println(Arrays.toString(dims));
            throw e;
        }
    }

    public final static synchronized <T> ObjectArrayWrapper<T> getObjectArrayWrapper(final Class<T> componentType, final int[] dims) {
        final ObjectArrayWrapper newArrayWrapper = new ObjectArrayWrapper(componentType, dims);
        MAP.put(newArrayWrapper.asArray(), new WeakReference<>(newArrayWrapper));
        return newArrayWrapper;
    }

    /**
     * Creates a new wrapper, with the given component type and storage array.
     * This can be used to create two <code>ArrayWrappers</code> with the same
     * storage but different component types.
     *
     * @param storage The storage array.
     */
    private ObjectArrayWrapper(final Object[] storage) {
        if (MAP.containsKey(storage) && MAP.get(storage).get() != null) {
            throw new RuntimeException("Trying to create an ArrayWrapper for an array whose wrapper is already present");
        } else {
            this.storage = storage;
            this.length = storage.length;
        }
    }

    public static synchronized ObjectArrayWrapper getObjectArrayWrapper(final Object[] storage) {
        if (storage == null) {
            return null;
        }
        if (MAP.containsKey(storage)) {
            // We store the value to make sure it won't get garbage collected.
            ObjectArrayWrapper value = MAP.get(storage).get();
            if (value != null) {
                return value;
            }
        }
        final ObjectArrayWrapper newArrayWrapper = new ObjectArrayWrapper(storage);
        MAP.put(storage, new WeakReference<>(newArrayWrapper));
        return newArrayWrapper;
    }

    /**
     * Creates a new array, by casting an existing array wrapper to a new
     * wrapper whose component type is a supertype of the existing array
     * wrapper's component type. Note that doing this may potentially cause
     * <code>ArrayStoreException</code>s when an attempt is made to write an
     * object of the wrong type into the resulting cast array (Java arrays are
     * covariant, and mutable covariant collections are not fully type-safe.)
     *
     * @param existingWrapper The wrapper to cast.
     * @param componentType The new component type to use for the new wrapper.
     * //
     */
//    public <T> ArrayWrapper(ArrayWrapper existingWrapper,
//            Class componentType<T>) {
//        this(existingWrapper.asArray(), componentType);
//    }

    /**
     * Returns the array wrapped by this array wrapper. Note that unlike
     * <code>AbstractCollection#toArray</code>, the resulting array is shared
     * with the internals of the array wrapper; modifying the returned array
     * directly, and modifying the array via the array wrapper, will both change
     * both the returned array and the wrapped array.
     * <p>
     * The array is returned as an <code>Object</code>, because not every array
     * type can be cast to <code>Object[]</code> (which is what <code>T[]</code>
     * would erase to).
     *
     * @return The array wrapped by this array wrapper.
     */
    @Override
    public final Object[] asArray() {
        return storage;
    }

    /**
     * Returns the element at the given array index. Note that an array can (and
     * does by default) contain <code>null</code> elements.
     *
     * @param i The index at which to retrieve the element.
     * @return The element at the given index.
     * @throws ArrayIndexOutOfBoundsException If the index is negative, or
     *                                        greater than or equal to the number of elements in the array
     */
    //   @Override
    @SuppressWarnings("unchecked")
    public final Object get(final int i) {
        final Object object = storage[i];
        if (object != null) {
            final Class c = object.getClass();
            if (c.isArray()) {
                switch (c.getComponentType().getName()) {
                    case "boolean":
                        return booleanArrayWrapper.getbooleanArrayWrapper((boolean[]) object);
                    case "byte":
                        return byteArrayWrapper.getbyteArrayWrapper((byte[]) object);
                    case "char":
                        return charArrayWrapper.getcharArrayWrapper((char[]) object);
                    case "double":
                        return doubleArrayWrapper.getdoubleArrayWrapper((double[]) object);
                    case "float":
                        return floatArrayWrapper.getfloatArrayWrapper((float[]) object);
                    case "long":
                        return longArrayWrapper.getlongArrayWrapper((long[]) object);
                    case "short":
                        return shortArrayWrapper.getshortArrayWrapper((short[]) object);
                    case "int":
                        return intArrayWrapper.getintArrayWrapper((int[]) object);
                    default:
                        return getObjectArrayWrapper((Object[]) object);
                }
            }
            return object;
        } else {
            return null;
        }
    }

    /**
     * Returns the capacity of this array.
     *
     * @return The number of elements that can be stored in the array.
     */
//    @Override
    public final int size() {
        return length;
    }

    /**
     * Changes an element of this array.
     *
     * @param i The array index to change.
     * @param e The new element at that index.
     * @return The old value of the element that was changed.
     * @throws ArrayIndexOutOfBoundsException If <code>i</code> is negative, or
     *                                        greater than or equal to the size of the array.
     * @throws ArrayStoreException            If this array wrapper was produced via
     *                                        casting it from an array wrapper whose component type was a subtype of
     *                                        <code>T</code>, and <code>e</code> is not an instance of that subtype
     */
//    @Override
    public final void set(final int i, final Object e) {
        if (e instanceof ArrayWrapper) {
            storage[i] = ((ArrayWrapper) e).asArray();
        } else {
            storage[i] = e;
        }
    }

    /**
     * Hash code consistent with reference equality of the wrapped array.
     *
     * @return The identity hash code of the wrapped array.
     * @see System#identityHashCode(java.lang.Object)
     */
    @Override
    @SuppressWarnings("ArrayHashCode")
    public final int hashCode() {
        return storage.hashCode();
    }

    /**
     * Reference equality of the wrapped array.
     *
     * @param o The object to compare to.
     * @return <code>true</code> if <code>o</code> is the same reference as
     * <code>this</code>
     */
    @Override
    public final boolean equals(final Object o) {
        if (o == null || !(o instanceof ObjectArrayWrapper)) {
            return false;
        }
        return ((ObjectArrayWrapper) o).asArray() == (storage);
    }

    @Override
    public final boolean isEquals(final Object o) {
        if (o == null || !(o instanceof ObjectArrayWrapper)) {
            return false;
        }
        return ((ObjectArrayWrapper) o).asArray() == storage;
    }

    public final ObjectArrayWrapper millrClone() {
        return getObjectArrayWrapper(this.storage.clone());
    }

    @Override
    public final Class getReferentClass(final ProxyOrWrapper.Namespacer dummy) {
        return storage.getClass(); //To change body of generated methods, choose Tools | Templates.
    }

    public final void cast(final Class<?> Cl) throws ClassCastException {
        if (!Cl.isInstance(storage)) {
            throw new ClassCastException();
        }
    }

    @Override
    public final void update(final Object updatedArray) {
        System.arraycopy(updatedArray, 0, storage, 0, length);
    }


    public static final void update(Object potentialUpdatedArray, Object potentialArrayWrapper) {
        if (potentialArrayWrapper == null) {
            potentialArrayWrapper = null;
            return;
        }
        Class clazz = potentialArrayWrapper.getClass();
        if (clazz.equals(booleanArrayWrapper.class))
            ((booleanArrayWrapper) potentialArrayWrapper).update((boolean[]) potentialArrayWrapper);
        else if (clazz.equals(byteArrayWrapper.class))
            ((byteArrayWrapper) potentialArrayWrapper).update((byte[]) potentialArrayWrapper);
        else if (clazz.equals(charArrayWrapper.class))
            ((charArrayWrapper) potentialArrayWrapper).update((char[]) potentialArrayWrapper);
        else if (clazz.equals(doubleArrayWrapper.class))
            ((doubleArrayWrapper) potentialArrayWrapper).update((double[]) potentialArrayWrapper);
        else if (clazz.equals(floatArrayWrapper.class))
            ((floatArrayWrapper) potentialArrayWrapper).update((float[]) potentialArrayWrapper);
        else if (clazz.equals(intArrayWrapper.class))
            ((intArrayWrapper) potentialArrayWrapper).update((int[]) potentialArrayWrapper);
        else if (clazz.equals(longArrayWrapper.class))
            ((longArrayWrapper) potentialArrayWrapper).update((long[]) potentialArrayWrapper);
        else if (clazz.equals(shortArrayWrapper.class))
            ((shortArrayWrapper) potentialArrayWrapper).update((short[]) potentialArrayWrapper);
        else if (clazz.equals(ObjectArrayWrapper.class))
            ((ObjectArrayWrapper) potentialArrayWrapper).update((Object[]) potentialArrayWrapper);

    }

}
