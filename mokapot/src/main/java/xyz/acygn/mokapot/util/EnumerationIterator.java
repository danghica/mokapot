package xyz.acygn.mokapot.util;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * A wrapper for an <code>Enumeration</code> that causes it to implement the
 * interfaces <code>Iterator</code> and <code>Iterable</code>. This is used in
 * situations where an iterator is needed (e.g. for a <code>for</code> loop) but
 * only an enumeration is available.
 *
 * @author Alex Smith
 * @param <E> The element type of the enumeration.
 */
public class EnumerationIterator<E> implements Iterator<E>, Iterable<E> {

    /**
     * The enumeration being wrapped.
     */
    private final Enumeration<E> enumeration;

    /**
     * Creates an iterator from an enumeration.
     *
     * @param enumeration The enumeration from which to create the iterator.
     */
    public EnumerationIterator(Enumeration<E> enumeration) {
        this.enumeration = enumeration;
    }

    /**
     * Returns whether more elements are available.
     *
     * @return <code>true</code> if more elements are available;
     * <code>false</code> if the last element has been read.
     */
    @Override
    public boolean hasNext() {
        return enumeration.hasMoreElements();
    }

    /**
     * Returns the first element that has not been returned yet.
     *
     * @return The first element that has not already been returned via a call
     * to <code>next</code>.
     */
    @Override
    public E next() {
        return enumeration.nextElement();
    }

    /**
     * Returns the iterator itself. This "fills in the gap" between the
     * <code>Iterator</code> and <code>Iterable</code> interfaces.
     *
     * @return <code>this</code>.
     */
    @Override
    public Iterator<E> iterator() {
        return this;
    }
}
