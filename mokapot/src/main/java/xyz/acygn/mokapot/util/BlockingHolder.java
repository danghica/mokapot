package xyz.acygn.mokapot.util;

import java.util.Objects;

/**
 * An atomic reference for which attempts to read it can block on attempts to
 * write it. In other words, this either references an object, or is empty (like
 * <code>Optional</code>); but if empty, attempts to read it will be delayed
 * until it is written (like <code>BlockingQueue</code>). You can also think of
 * this object as a <code>Promise</code> that can be used multiple times.
 * <p>
 * All methods of this class are thread-safe.
 *
 * @author Alex Smith
 * @param <T> The type of object being held.
 */
public class BlockingHolder<T> {

    /**
     * The value being held. <code>null</code> means that no value is held.
     */
    private T value;

    /**
     * Creates a new blocking holder, with no value initially held.
     */
    public BlockingHolder() {
        this.value = null;
    }

    /**
     * Creates a new blocking holder, with an object initially held.
     *
     * @param value The value to hold. Cannot be <code>null</code>.
     */
    public BlockingHolder(T value) {
        Objects.requireNonNull(value, "a BlockingHolder cannot hold null");
        this.value = value;
    }

    /**
     * Holds an object in this blocking holder. There must not be an object in
     * the blocking holder already.
     *
     * @param value The value to hold. Cannot be <code>null</code>.
     * @throws IllegalStateException If a value is held already
     */
    public synchronized void hold(T value) throws IllegalStateException {
        Objects.requireNonNull(value, "a BlockingHolder cannot hold null");

        if (this.value != null) {
            throw new IllegalStateException("object already held");
        }

        this.value = value;
        this.notifyAll();
    }

    /**
     * Changes the value held in the blocking holder. To help prevent race
     * conditions in the calling code, the expected current value must be
     * specified: if a different value is held, this method acts like a get
     * rather than a set, returning the value without changing it.
     *
     * @param oldValue The value expected to be found in the blocking holder;
     * this will be compared with the actual value using reference equality
     * (<code>==</code>). Can be <code>null</code>, indicating an expectation
     * that no value will be found, in order to produce an equivalent of
     * <code>hold</code> that uses its return value to express failure rather
     * than an exception.
     * @param newValue The value that will be placed into the blocking holder if
     * the expectation in <code>oldValue</code> is correct. Can be
     * <code>null</code>, to remove the value from the blocking holder if the
     * expectation specified by <code>oldValue</code> is correct.
     * @return The value that was previously held, or <code>null</code> if no
     * value was previously held. (If this <code>== oldValue</code>, then the
     * blocking holder now holds <code>newValue</code>; otherwise, the value was
     * not changed.)
     */
    public synchronized T setIf(T oldValue, T newValue) {
        if (oldValue != this.value) {
            return this.value;
        }

        this.value = newValue;
        if (oldValue == null) {
            this.notifyAll();
        }

        return oldValue;
    }

    /**
     * Returns the value in the holder, blocking until a value is stored there.
     * While blocking occurs, garbage collector effort will be directed towards
     * running finalizers (because one intended use of this class is to
     * implement "return values" for finalizers).
     *
     * @return The value stored in the holder. Will not be <code>null</code>
     * (because <code>null</code> cannot be stored in the holder, and if the
     * holder is empty, the method will not return until it is nonempty).
     * @throws InterruptedException If the calling thread is interrupted while
     * waiting for a value to be stored in the holder.
     */
    public synchronized T blockingGet() throws InterruptedException {
        while (this.value == null) {
            BackgroundGarbageCollection.perform(
                    BackgroundGarbageCollection.Operation.FINALIZE);
            this.wait(5000);
        }
        return this.value;
    }
}
