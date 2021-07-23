package xyz.acygn.mokapot.util;

import java.util.function.Consumer;

/**
 * A class representing objects that contain a single, mutable value. Typically
 * used to implement interior mutability of values which need to be shallowly
 * immutable (e.g. due to Java's requirement that values captured by a closure
 * must be effectively final).
 *
 * @author Alex Smith
 * @param <V> The type of the value being held.
 */
public class Holder<V> implements Consumer<V> {

    /**
     * The value stored within the object.
     */
    private V value;

    /**
     * Creates a new holder. The value will be initially null.
     */
    public Holder() {
        value = null;
    }

    /**
     * Creates a new holder with a given initial value.
     *
     * @param value The initial value to hold.
     */
    public Holder(V value) {
        this.value = value;
    }

    /**
     * Returns the currently held value.
     *
     * @return The currently held value.
     */
    public V getValue() {
        return value;
    }

    /**
     * Holds a new value. The old value will be forgotten from the Holder
     * (although it will be returned from this method.)
     *
     * @param value The new value to hold.
     * @return The previous value.
     */
    public V setValue(V value) {
        V rv = this.value;
        this.value = value;
        return rv;
    }

    /**
     * Holds a new value. The old value will be forgotten. The new value (that
     * was just passed to this method) will be returned, allowing this method to
     * be used to temporarily store a value in a Holder without needing to make
     * a copy of it in a variable first.
     *
     * @param value The new value to hold.
     * @return <code>value</code>.
     */
    public V setAndGet(V value) {
        this.value = value;
        return value;
    }

    /**
     * Holds a new value. The old value will be forgotten. This method exists so
     * that a Holder can implement the standard <code>Consumer</code> interface.
     *
     * @param value The new value to hold.
     */
    @Override
    public void accept(V value) {
        setValue(value);
    }
}
