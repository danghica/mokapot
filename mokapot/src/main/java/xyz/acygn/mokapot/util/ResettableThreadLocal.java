package xyz.acygn.mokapot.util;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * An object that conceptually holds a different value on different threads, but
 * where a "thread" can be a subset of a Java thread. This is intended for use
 * with thread pools; when taking an idle thread from the pool, it should be
 * treated as though it were a new thread, so all thread-local values on the
 * thread will need to be reset. As such, this is similar to Java's built-in
 * <code>ThreadLocal</code>, but allows a thread to do a simultaneous reset of
 * all its thread-local values without necessarily having a reference to those
 * values.
 *
 * @author Alex Smith
 * @param <T> The type of the value held within this object.
 * @see ThreadLocal
 */
public class ResettableThreadLocal<T> extends ThreadLocal<T> {

    /**
     * The thread name before it was temporarily changed.
     */
    private static final ThreadLocal<Optional<String>> originalThreadName;

    /**
     * A weak set of all <code>ResettableThreadLocal</code> values whose value
     * has been changed on this thread.
     */
    private static final ThreadLocal<Map<ResettableThreadLocal<?>, Void>> alteredThreadLocals;

    static {
        alteredThreadLocals = ThreadLocal.withInitial(() -> new WeakHashMap<>());
        originalThreadName = ThreadLocal.withInitial(Optional::empty);
    }

    /**
     * Alters the current thread's name until next time its resettable
     * thread-local variables are reset.
     *
     * @param name The name to temporarily give the thread.
     */
    public static void setName(String name) {
        Thread currentThread = Thread.currentThread();
        if (!originalThreadName.get().isPresent()) {
            originalThreadName.set(Optional.of(currentThread.getName()));
        }

        currentThread.setName(name);
    }

    /**
     * Resets the contents of this thread-local storage slot to their initial
     * value.
     * <p>
     * If the value was customized, and is <code>Expirable</code>, it will be
     * expired before the value is reset.
     */
    @Override
    public void remove() {
        expireValue();
        /* Note that we don't have to check concurrency here; a concurrent
           access to a thread-local value is impossible by definition. */
        alteredThreadLocals.get().remove(this);
        super.remove();
    }

    /**
     * Changes the value of this thread-local storage to another value.
     * <p>
     * If the old value is <code>Expirable</code>, it will be expired before the
     * new value is set.
     *
     * @param value The new value to set.
     */
    @Override
    public void set(T value) {
        expireValue();
        alteredThreadLocals.get().put(this, null);
        super.set(value);
    }

    /**
     * Resets the value of this thread-local storage to its original value,
     * without keeping track of the fact. This is used to avoid concurrent
     * updates to the <code>alteredThreadLocals</code> sets, but leaves the sets
     * in an inconsistent state (which must therefore be fixed by the caller; as
     * a private method, this is only called from callsites that have
     * appropriate code to fix the invariants).
     */
    private void directRemove() {
        super.set(null);
        super.remove();
    }

    /**
     * If the current value of this thread-local is <code>Expirable</code>,
     * expire it. This allows thread death to be observed via the use of
     * expirable resettable thread locals, while avoiding races against any
     * finalizers that may be using the same mechanism.
     */
    private void expireValue() {
        T currentValue = get();
        if (currentValue instanceof Expirable) {
            try {
                ((Expirable) currentValue).expire();
            } catch (Expirable.ExpiredException ex) {
                // nothing to do, it seems like it was expired earlier
            }
        }
    }

    /**
     * Resets the current thread's slot in all
     * <code>ResettableThreadLocal</code> objects to its default value. (In
     * other words, we're acting as though the current thread has become a new
     * thread, and thus should use a different slot for all its thread-local
     * storage.)
     * <p>
     * If any such values are <code>Expirable</code>, they will be expired
     * before being reset.
     */
    public static void reset() {
        alteredThreadLocals.get().keySet().forEach((tl) -> tl.directRemove());
        alteredThreadLocals.get().clear();
        originalThreadName.get().ifPresent((name) -> {
            Thread.currentThread().setName(name);
            originalThreadName.remove();
        });
    }
}
