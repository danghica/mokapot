
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
public final class shortArrayWrapper  implements ArrayWrapper {

       /**
     * The array that stores this array wrapper's data.
     */
    final private short[] storage;
    
    final private int length;
    
    private final static WeakHashMap<short[],   WeakReference<shortArrayWrapper>> MAP = new WeakHashMap<>();
    /**
     * Creates a new array wrapper, with the given length and type.
     *
     * @param length The number of elements in the wrapped array. This cannot be
     * changed after the array wrapper is created.
     */
    private shortArrayWrapper(final int length) {
        storage = new short[length];
        this.length = length;       
    }
    
    public final static synchronized shortArrayWrapper getshortArrayWrapper(int length){
        final shortArrayWrapper newWrapper = new shortArrayWrapper(length);
        MAP.put(newWrapper.asArray(),  new WeakReference<>(newWrapper));
        return newWrapper;
    }
    /**
     * Creates a new wrapper, with the given component type and storage array.
     * This can be used to create two <code>ArrayWrappers</code> with the same
     * storage but different component types.
     *
     * @param storage The storage array.
     */
    private shortArrayWrapper(final short[] storage) {
        if (MAP.containsKey(storage) && MAP.get(storage).get()!=null){
            throw new RuntimeException("Attempt to create a shortArrayWrapper for a short-array with an already exiting shortArrayWrappper");
        }
        else{
            this.storage = storage;
            this.length = storage.length;
        }
    }
    
    public final  static synchronized shortArrayWrapper getshortArrayWrapper(final short[] storage){
        if (storage==null){
            return null;
        }
        if (MAP.containsKey(storage) && MAP.get(storage).get()!= null){
            return MAP.get(storage).get();
        }
        else{
            final shortArrayWrapper newWrapper = new shortArrayWrapper(storage);
            MAP.put(storage,  new WeakReference<>(newWrapper));
            return newWrapper;
        }
    }
    public final short[] asArray() {
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
    public final short get(final int i) {
        return storage[i];
    }

    /**
     * Returns the capacity of this array.
     *
     * @return The number of elements that can be stored in the array.
     */

    public final int size() {
        return storage.length;
    }

    public final void  set(final int i, final short e) {
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
        if (o == null || !(o instanceof shortArrayWrapper)) {
            return false;
        }

        return ((shortArrayWrapper) o).asArray() == storage;
    }
    
    @Override
    public final boolean isEquals(final Object o){
        if (o == null || !(o instanceof longArrayWrapper)) {
            return false;
        }
        return ((shortArrayWrapper) o).asArray() == storage;
    }
    
    
    @Override
    public final shortArrayWrapper clone(){
        return getshortArrayWrapper(this.storage.clone());
    }
   
      @Override
    public final Class getReferentClass(final ProxyOrWrapper.Namespacer dummy) {
        return storage.getClass();
    }
    
//    @Override
//    public void setArray(Object o) {
//        short[] f = (short[]) o;
//        if (f.length != storage.length){
//            throw new RuntimeException("messing with arrays length");
//        }
//        else{
//            for (int i = 0; i< f.length; i++){
//                storage[i] = f[i];
//            }
//        }
//    }
    
        public final void update(final Object updatedArray){
        System.arraycopy(updatedArray, 0, storage, 0, length);
    }
}

    
