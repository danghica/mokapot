package xyz.acygn.mokapot.util;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A value that initialises itself on first use. When <code>get()</code> is
 * called for the first time, a given method is called to get a value; calling
 * <code>get()</code> thereafter will return the same value.
 * <p>
 * This class is intentionally thread-safe; the initialisation will only ever
 * run once.
 *
 * @author Alex Smith
 * @param <T> The type of value lazily stored in the object.
 */
public class Lazy<T> implements Supplier<T> {

    /**
     * A stopwatch that runs only while no lazy initialiser is running. This
     * functionality can be used to create a stopwatch that excludes time spent
     * in run-once initialisation, via using this stopwatch as its time base.
     * Because initialisation can run in one thread at the same time as other
     * operations run in a different thread, the time-bases are thread-local
     * (even though stopwatches exist across threads).
     */
    public static final ThreadLocal<Stopwatch> TIME_BASE
            = ThreadLocal.withInitial(() -> new Stopwatch(null).start());

    /**
     * Initialiser for the object. This should only be accessed when
     * synchronised on <code>this</code>'s monitor, and is nulled out once the
     * object has been initialised (both to indicate that it's already been run,
     * and to save memory). Must not supply <code>null</code>.
     */
    private Supplier<T> initialiser;

    /**
     * The value that was recorded when the initialiser ran; returned by
     * <code>get()</code>. This is an atomic reference storing <code>null</code>
     * until the initialiser has run. Access to this field should first check to
     * see if it equals <code>null</code>; if it doesn't, use the stored value;
     * if it does, attempt to run the initialiser and check again.
     */
    private final AtomicReference<T> recordedValue
            = new AtomicReference<>(null);

    /**
     * Creates a lazy value with the given initialiser.
     *
     * @param initialiser The initialiser that will be used to produce the value
     * in question the first time that it is used. This will only ever be called
     * at most once (i.e. it'll be called the first time it's needed, and then
     * it will never be needed again because the returned value is remembered).
     * Cannot be <code>null</code>, and cannot return <code>null</code>.
     */
    public Lazy(Supplier<T> initialiser) {
        Objects.requireNonNull(initialiser);
        this.initialiser = initialiser;
    }

    /**
     * Returns the value that was returned by the initialiser. If the
     * initialiser has not run yet, runs it to produce a value to return. If it
     * has already run, returns the same value as it returned when it ran; the
     * initialiser will not be run a second time. (In other words, the value
     * returned by the initialiser is being memoised.)
     * <p>
     * Note that the initialiser will run on this thread, regardless of the
     * thread that created the <code>Lazy</code> object. Other than that,
     * though, this method is thread-safe and can be safely called even if
     * another thread is calling this method on the same object at the same
     * time.
     *
     * @return The value previously returned by the initialiser; or the value
     * newly returned by the initialiser, if no value was previously returned.
     */
    @Override
    public T get() {
        T value = recordedValue.get();
        if (value == null) {
            initialise();
            value = recordedValue.get();
        }
        return value;
    }

    /**
     * Force the initialiser to run and initialise the object, if it hasn't
     * already. (If the object has already been initialised, this has no
     * effect.)
     * <p>
     * Normally, you would run <code>get()</code> instead, which automatically
     * calls <code>initialise</code> if necessary.
     */
    private synchronized void initialise() {
        if (initialiser == null) {
            return;
        }
        try (DeterministicAutocloseable ac = TIME_BASE.get().pause()) {
            T value = initialiser.get();
            if (value == null) {
                throw new NullPointerException("initialiser returned null");
            }
            initialiser = null;
            recordedValue.set(value);
        }
    }

    /**
     * Resets this <code>Lazy</code> object to a state in which it was only just
     * constructed. This forgets the remembered value of the object, and allows
     * setting a new initialiser (in fact this is required, as there's a
     * guarantee that the initialiser is only called once, thus you need to
     * specify it again if you want it to be called again). The object will act
     * as if <code>.get()</code> had never been called on it (up until the next
     * time that <code>.get()</code> actually is called, at which point the new
     * initialiser will run and have its return value remembered, etc.).
     *
     * @param initialiser The initialiser that will be used to produce the value
     * in question the next time that it is used. This will only ever be called
     * at most once (i.e. it'll be called the first time it's needed, and then
     * it will never be needed again because the returned value is remembered).
     * Cannot be <code>null</code>, and cannot return <code>null</code>.
     * @return The remembered value of the object immediately before
     * <code>reset()</code> was called, or <code>null</code> if the value was
     * never remembered due to <code>.get()</code> never having been called.
     */
    public synchronized T reset(Supplier<T> initialiser) {
        Objects.requireNonNull(initialiser);
        if (this.initialiser != null) {
            this.initialiser = initialiser;
            return null;
        } else {
            this.initialiser = initialiser;
            return recordedValue.getAndSet(null);
        }
    }

    /**
     * Returns whether the initialiser has ever been needed. In other words,
     * whether <code>.get()</code> has ever been called on this
     * <code>Lazy</code> object.
     * <p>
     * Note that the results will only be instantaneously valid; if
     * <code>.get()</code> is called by another thread in parallel with this
     * call, the result may already have changed by the time the caller sees it.
     * As such, this method only gives meaningful results if it's known that no
     * other thread is using the <code>Lazy</code> value at the time. (A typical
     * use would be from a finalizer, to <code>close()</code> a
     * <code>Closeable</code> object only if it was constructed; a finalizer
     * will only run when no other thread has a reference to the object in
     * question.)
     *
     * @return <code>true</code> if <code>.get()</code> has been called.
     */
    public boolean isInitialised() {
        return recordedValue.get() != null;
    }
}
