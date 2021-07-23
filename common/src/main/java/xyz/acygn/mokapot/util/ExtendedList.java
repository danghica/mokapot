package xyz.acygn.mokapot.util;

import java.util.AbstractList;
import java.util.List;

/**
 * An immutable list that's formed by adding one element to the end of an
 * existing list. The existing list should not be modified while the
 * <code>ExtendedList</code> is in use.
 *
 * @author Alex Smith
 * @param <T> The element type of the list.
 */
public class ExtendedList<T> extends AbstractList<T> {

    /**
     * The existing list being extended.
     */
    private final List<T> existing;
    /**
     * The element with which the list was extended.
     */
    private final T newElement;
    /**
     * The newly extended size of the list.
     */
    private final int size;

    /**
     * Create a new list, that extends an existing list by one element. The new
     * list is immutable, and the existing list should either be immutable, or
     * at least not mutated until after the <code>ExtendedList</code> is no
     * longer in use (i.e. if the existing list is mutated, the constructed list
     * will become unusable).
     *
     * @param existing The existing list to extend.
     * @param newElement The new element to append to the list.
     */
    public ExtendedList(List<T> existing, T newElement) {
        this.existing = existing;
        this.newElement = newElement;
        this.size = existing.size() + 1;
    }

    /**
     * Gets the element at the given index of the list.
     *
     * @param i The index at which to return the element.
     * @return The element at the given index.
     */
    @Override
    public T get(int i) {
        if (i == size - 1) {
            return newElement;
        } else {
            return existing.get(i);
        }
    }

    /**
     * Gets the size of this list. This will be 1 plus the number of elements in
     * the existing list.
     *
     * @return The size of the extended list.
     */
    @Override
    public int size() {
        return this.size;
    }
}
