package xyz.acygn.millr.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import xyz.acygn.mokapot.skeletons.ProxyOrWrapper;

/**
 * A class that acts, in most respects, identically to a Java array. However,
 * being an actual class, rather than a primitive, makes it possible to inherit
 * from, intercept methods of, etc..
 *
 * @author Thomas Cuvillier
 */
public final class byteArrayWrapper implements ArrayWrapper {

    /**
     * The array that stores this array wrapper's data.
     */
    private final byte[] storage;
    private final int length;
    private final static WeakHashMap<byte[], WeakReference<byteArrayWrapper>> MAP = new WeakHashMap<>();

    /**
     * Creates a new array wrapper, with the given length and type.
     *
     * @param length The number of elements in the wrapped array. This cannot be
     * changed after the array wrapper is created.
     */
    private byteArrayWrapper(final int length) {
        storage = new byte[length];
        this.length = length;
    }

    public final static byteArrayWrapper getbyteArrayWrapper(final int length) {
        final byteArrayWrapper newArrayWrapper = new byteArrayWrapper(length);
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
    private byteArrayWrapper(final byte[] storage) {
        if (MAP.containsKey(storage) && MAP.get(storage).get() != null) {
            throw new RuntimeException("Attemp to create a booleanArrayWrapper with a boolean array with a already corresponding BooleanArrayWrapper");
        }
        this.storage = storage;
        this.length = storage.length;
    }

    public final static synchronized byteArrayWrapper getbyteArrayWrapper(final byte[] storage) {
        if (storage == null) {
            return null;
        }
            if (MAP.containsKey(storage) && MAP.get(storage).get() != null) {
                return MAP.get(storage).get();
            } else {
                byteArrayWrapper newArrayWrapper = new byteArrayWrapper(storage);
                MAP.put(storage, new WeakReference<>(newArrayWrapper));
                return newArrayWrapper;
            }
        
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
     */
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
    public final byte[] asArray() {
        return storage;
    }

    /**
     * Returns the element at the given array index. Note that an array can (and
     * does by default) contain <code>null</code> elements.
     *
     * @param i The index at which to retrieve the element.
     * @return The element at the given index.
     * @throws ArrayIndexOutOfBoundsException If the index is negative, or
     * greater than or equal to the number of elements in the array
     */
    public final byte get(final int i) {
        return storage[i];
    }

    /**
     * Returns the capacity of this array.
     *
     * @return The number of elements that can be stored in the array.
     */
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
     * greater than or equal to the size of the array.
     * @throws ArrayStoreException If this array wrapper was produced via
     * casting it from an array wrapper whose component type was a subtype of
     * <code>T</code>, and <code>e</code> is not an instance of that subtype
     */
    public final void set(final int i, final byte e) {
        storage[i] = e;
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
        if (o == null || !(o instanceof byteArrayWrapper)) {
            return false;
        }
        return ((byteArrayWrapper) o).asArray() == storage;
    }

    @Override
    public final boolean isEquals(final Object o) {
        if (o == null || !(o instanceof byteArrayWrapper)) {
            return false;
        }
        return ((byteArrayWrapper) o).asArray() == storage;
    }

    @Override
    public final byteArrayWrapper clone() {
        return getbyteArrayWrapper(this.storage.clone());
    }

    @Override
    public final Class getReferentClass(final ProxyOrWrapper.Namespacer dummy) {
        return storage.getClass();
    }
   
        public final void update(Object updatedArray){
        System.arraycopy(updatedArray, 0, storage, 0, length);
    }

//     @Override
//    public void setArray(Object o) {
//        byte[] f = (byte[]) o;
//        if (f.length != storage.length){
//            throw new RuntimeException("messing with arrays length");
//        }
//        else{
//            for (int i = 0; i< f.length; i++){
//                storage[i] = f[i];
//            }
//        }
//    }
}
