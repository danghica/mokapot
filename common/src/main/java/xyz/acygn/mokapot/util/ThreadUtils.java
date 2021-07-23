package xyz.acygn.mokapot.util;

import static java.lang.Thread.currentThread;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Utility class for threading-related static methods.
 *
 * @author Alex Smith
 */
public class ThreadUtils {

    /**
     * Performs a given operation, delaying interruptions until it's complete.
     * This is intended mostly for use with operations that complete quickly,
     * and might potentially end up having to be serialised due to thread
     * contention (but which don't block on their own); in such cases, it
     * performs appropriate handling of thread interruptions without changing
     * the semantics of the code.
     * <p>
     * If the operation throws an <code>InterruptedException</code>, it will be
     * retried from the start, and the current thread will be interrupted again
     * after it's finished. As such, this only works correctly if the parts of
     * the operation before the last point at which it can be blocked are
     * idempotent.
     * <p>
     * This function can also be used for longer-running operations, although in
     * such cases the semantics of any interruption will be changed (by delaying
     * it until after the operation has completed).
     *
     * @param operation The operation to perform.
     */
    public static void delayInterruptions(InterruptiblyRunnable operation) {
        int interruptedCount = 0;
        while (true) {
            try {
                operation.run();
                break;
            } catch (InterruptedException ex) {
                interruptedCount++;
            }
        }

        for (int i = 0; i < interruptedCount; i++) {
            currentThread().interrupt();
        }
    }

    /**
     * Performs a given operation that returns a value, delaying interruptions
     * until it's complete. This is intended mostly for use with operations that
     * complete quickly, and might potentially end up having to be serialised
     * due to thread contention (but which don't block on their own); in such
     * cases, it performs appropriate handling of thread interruptions without
     * changing the semantics of the code.
     * <p>
     * If the operation throws an <code>InterruptedException</code>, it will be
     * retried from the start, and the current thread will be interrupted again
     * after it's finished. As such, this only works correctly if the parts of
     * the operation before the last point at which it can be blocked are
     * idempotent.
     * <p>
     * This function can also be used for longer-running operations, although in
     * such cases the semantics of any interruption will be changed (by delaying
     * it until after the operation has completed).
     *
     * @param operation The operation to perform.
     */
    public static <T> T delayInterruptionsRv(InterruptibleSupplier<T> operation) {
        int interruptedCount = 0;
        T retval;
        while (true) {
            try {
                retval = operation.get();
                break;
            } catch (InterruptedException ex) {
                interruptedCount++;
            }
        }

        for (int i = 0; i < interruptedCount; i++) {
            currentThread().interrupt();
        }

        return retval;
    }

    /**
     * Throws the unchecked exception wrapped by the given throwable. The
     * intended use of this is to unpack an
     * <code>InvocationTargetException</code>, <code>ExecutionException</code>,
     * or the like into the exception that it's wrapping.
     *
     * @param throwable The exception (or other throwable) to unwrap.
     * @throws IllegalArgumentException If the exception does not wrap anything
     */
    public static void unwrapAndRethrow(Throwable throwable)
            throws IllegalArgumentException {
        Throwable cause = throwable.getCause();

        if (cause == null) {
            throw new IllegalArgumentException(
                    "Unwrapping an exception that does not wrap anything");
        }

        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        } else if (cause instanceof Error) {
            throw (Error) cause;
        } else {
            throw new UndeclaredThrowableException(cause);
        }
    }

    /**
     * Inaccessible constructor. This is a utility class and not meant to be
     * instantiated.
     */
    private ThreadUtils() {
    }

    /**
     * A functional interface representing operations that can potentially be
     * interrupted.
     */
    @FunctionalInterface
    public static interface InterruptiblyRunnable {

        /**
         * Performs an interruptible operation. The semantics are that if the
         * operation is interrupted, then the operation can be retried from the
         * start without affecting the behaviour of the code.
         * <p>
         * Ideally, the operation would not become blocked for any reason other
         * than thread contention. If the operation is capable of blocking
         * long-term, any thread that interrupts this thread during the
         * operation must be aware that the interruptions may be delayed.
         *
         * @throws InterruptedException If the thread is interrupted before the
         * operation has done anything non-idempotent
         */
        void run() throws InterruptedException;
    }

    /**
     * A functional interface representing operations that return a value and
     * can potentially be interrupted.
     *
     * @param <T> The type of values that are returned from the operation.
     */
    @FunctionalInterface
    public static interface InterruptibleSupplier<T> {

        /**
         * Performs an interruptible operation that returns a value. The
         * semantics are that if the operation is interrupted, then the
         * operation can be retried from the start without affecting the
         * behaviour of the code.
         * <p>
         * Ideally, the operation would not become blocked for any reason other
         * than thread contention. If the operation is capable of blocking
         * long-term, any thread that interrupts this thread during the
         * operation must be aware that the interruptions may be delayed.
         *
         * @return The value that should be returned from the operation.
         * @throws InterruptedException If the thread is interrupted before the
         * operation has done anything non-idempotent
         */
        T get() throws InterruptedException;
    }
}
