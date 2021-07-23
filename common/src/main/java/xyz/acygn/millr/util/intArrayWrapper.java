package xyz.acygn.millr.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import xyz.acygn.mokapot.markers.Copiable;
import xyz.acygn.mokapot.skeletons.ProxyOrWrapper;

/**
 * A class that acts, in most respects, identically to a Java array. However,
 * being an actual class, rather than a primitive, makes it possible to inherit
 * from, intercept methods of, etc..
 *
 * @author Thomas Cuvillier
 */
public final class intArrayWrapper implements ArrayWrapper, Copiable {

    /**
     * The array that stores this array wrapper's data.
     */
    final private int[] storage;
    final private int length;
    private final static WeakHashMap<int[], WeakReference<intArrayWrapper>> MAP = new WeakHashMap<>();

    /**
     * Creates a new array wrapper, with the given length and type.
     *
     * @param length The number of elements in the wrapped array. This cannot be
     * changed after the array wrapper is created.
     */
    private intArrayWrapper(int length) {
        storage = new int[length];
        this.length = length;

    }

    public static final intArrayWrapper getintArrayWrapper(final int length) {
        final intArrayWrapper newWrapper = new intArrayWrapper(length);
        MAP.put(newWrapper.asArray(), new WeakReference<>(newWrapper));
        return newWrapper;
    }

    /**
     * Creates a new wrapper, with the given component type and storage array.
     * This can be used to create two <code>ArrayWrappers</code> with the same
     * storage but different component types.
     *
     * @param storage The storage array.
     */
    private intArrayWrapper(final int[] storage) {
        if (MAP.containsKey(storage) && MAP.get(storage).get() != null) {
            throw new RuntimeException("Attempt to create a intArrayWrapper for a int-array with an already exiting intArrayWrappper");
        } else {
            this.storage = storage;
            this.length = storage.length;
        }
    }

    public final static synchronized intArrayWrapper getintArrayWrapper(final int[] storage) {
        if (storage == null) {
            return null;
        }
            if (MAP.containsKey(storage) && MAP.get(storage).get() != null) {
                return MAP.get(storage).get();
            } else {
                final intArrayWrapper newWrapper = new intArrayWrapper(storage);
                MAP.put(storage, new WeakReference<>(newWrapper));
                return newWrapper;
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
    public final int[] asArray() {
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
    public final int get(final int i) {
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
    public final void set(final int i, final int e) {
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
        if (o == null || !(o instanceof intArrayWrapper)) {
            return false;
        }
        return ((intArrayWrapper) o).asArray() == storage;
    }

    @Override
    public final boolean isEquals(final Object o) {
        if (o == null || !(o instanceof intArrayWrapper)) {
            return false;
        }
        return ((intArrayWrapper) o).asArray() == storage;
    }

    @Override
    public final intArrayWrapper clone() {
        return getintArrayWrapper(this.storage.clone());
    }

    @Override
    public final Class getReferentClass(final ProxyOrWrapper.Namespacer dummy) {
        return int[].class;
    }
    
    
    
   
    /**
     * This method has 0 test and exception for speed purposes. 
     * @param updatedArray 
     */
    public final void update(final Object updatedArray){
        System.arraycopy(updatedArray, 0, storage, 0, length);
    }

}
