package xyz.acygn.mokapot.util;

import java.util.function.Consumer;

/**
 * A set that does not hold a strong reference to its elements. Any element that
 * becomes deallocated is automatically removed from the set. No synchronisation
 * is required when adding or removing set elements.
 * <p>
 * Set elements are compared with <code>==</code>, not <code>.equals()</code>,
 * i.e. elements are considered distinct if they have different addresses.
 *
 * @author Alex Smith
 * @param <T> The type of elements in the set.
 */
public class WeakConcurrentSet<T> {

    /**
     * The elements of the set. These are stored as the keys of a doubly weak
     * concurrent map. The values of the set are each <code>SINGLETON</code>
     * (because a doubly weak concurrent map does not permit <code>null</code>
     * elements); this will not cause elements to be freed because this class
     * itself hold the singleton alive.
     */
    private final DoublyWeakConcurrentMap<T, Object> elements = new DoublyWeakConcurrentMap<>();

    /**
     * The singleton element used for values of the map storing the data.
     */
    private final static Object SINGLETON = new Object();

    /**
     * Returns the number of elements in the set. This may be approximate due to
     * concurrent changes, and calculating it may be slow.
     *
     * @return The number of elements in the set.
     */
    public int size() {
        return elements.size();
    }

    /**
     * Returns whether the set is empty.
     *
     * @return <code>true</code> if the set is empty.
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * Returns whether the given object exists within the set. (Note that if the
     * object in question is not being held alive, it's possible that it will be
     * removed from the set by the garbage collector as soon as this method
     * stops running.)
     *
     * @param element The object to check.
     * @return <code>true</code> if <code>o</code> exists within the set.
     */
    public boolean contains(T element) {
        return elements.containsKey(element);
    }

    /**
     * Adds the given element to the set, if it isn't there already.
     *
     * @param element The element to add.
     * @return <code>true</code> if the element was added; <code>false</code> if
     * it was there already (via reference equality).
     */
    public boolean add(T element) {
        return elements.put(element, SINGLETON) == null;
    }

    /**
     * Removes the given element from the set.
     *
     * @param element The element to remove.
     * @return <code>true</code> if the element was removed; <code>false</code>
     * if the element was already missing from the set.
     */
    public boolean remove(T element) {
        return elements.remove(element) != null;
    }

    /**
     * Removes all elements from the set.
     */
    public void clear() {
        elements.clear();
    }

    /**
     * Runs a method on each element of the set.
     *
     * @param f The method to run.
     */
    public void forEach(Consumer<T> f) {
        elements.forEach((k, v) -> f.accept(k));
    }
}
