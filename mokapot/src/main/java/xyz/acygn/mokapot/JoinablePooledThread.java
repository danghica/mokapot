package xyz.acygn.mokapot;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import static xyz.acygn.mokapot.util.ThreadUtils.delayInterruptions;

/**
 * A PooledThread which additionally supports a <code>join()</code> operation
 * and optional return value.
 *
 * @author Alex Smith
 * @param <T> The type of the return value. This can be <code>Void</code> if no
 * return value is needed.
 */
class JoinablePooledThread<T> extends PooledThread {

    /**
     * The queue used to communicate the return value.
     */
    private final BlockingQueue<Wrapper<T>> joinQueue
            = new LinkedBlockingQueue<>();

    /**
     * The code that calculates the value we need.
     */
    private final Supplier<T> supplier;

    /**
     * A boolean tracking whether <code>join()</code> has been called.
     */
    private final AtomicBoolean joined = new AtomicBoolean(false);

    /**
     * Creates a joinable thread pool task where the task to run is specified
     * explicitly, and returns a value.
     *
     * @param code The code to run.
     */
    public JoinablePooledThread(Supplier<T> code) {
        super();
        supplier = code;
    }

    /**
     * Creates a joinable thread pool task where the task to run is specified
     * explicitly, but returns no value.
     *
     * @param code The code to run.
     */
    public JoinablePooledThread(Runnable code) {
        super();
        supplier = () -> {
            code.run();
            return null;
        };
    }

    @Override
    protected void run() {
        joinQueue.add(new Wrapper<>(supplier.get()));
    }

    /**
     * Waits for this task to end, then returns the value (if any). This will
     * block if the task has not yet ended (including if it has not yet
     * started). If the task does not naturally return a value, returns
     * <code>null</code>.
     * <p>
     * This method should only be called once.
     *
     * @return The value returned by the task, if any.
     * @throws IllegalThreadStateException If <code>join()</code> has already
     * been called once
     */
    public T join() throws IllegalThreadStateException {
        if (joined.getAndSet(true)) {
            throw new IllegalThreadStateException(
                    "JoinablePooledThread#join() may only be called once");
        }

        final Wrapper<T> rv = new Wrapper<>(null);
        delayInterruptions(() -> {
            rv.value = joinQueue.take().value;
        });
        return rv.value;
    }

    /**
     * Waits for this task to end, then returns the value (if any). This will
     * block if the task has not yet ended (including if it has not yet
     * started), up to a given length of time. If the task does not naturally
     * return a value, returns <code>null</code>.
     * <p>
     * This method should only be called once. (If the thread does not return
     * within the given length of time, its return value will be lost.)
     *
     * @param waitFor The maximum amount of time to wait before execution will
     * continue despite the continued execution of the task.
     * @param units The units in which <code>waitFor</code> is measured.
     * @return The value returned by the task, if any; or <code>null</code> if
     * the task failed to end in time.
     * @throws IllegalThreadStateException If <code>join()</code> has already
     * been called once
     */
    public T join(int waitFor, TimeUnit units) throws IllegalThreadStateException {
        if (joined.getAndSet(true)) {
            throw new IllegalThreadStateException(
                    "JoinablePooledThread#join() may only be called once");
        }

        final Wrapper<T> rv = new Wrapper<>(null);
        delayInterruptions(() -> {
            Wrapper<T> rvWrapped = joinQueue.poll(waitFor, units);
            if (rvWrapped == null) {
                /* nothing we can do; the thread can however still exit
                   naturally, as <code>LinkedBlockingQueue</code> is
                   asynchronous */
            } else {
                rv.value = rvWrapped.value;
            }
        });
        return rv.value;
    }

    /**
     * A wrapper that's capable of holding a value of type T. This is useful to
     * ensure that <code>null</code> values are never placed into a blocking
     * queue that might not be able to handle them.
     *
     * @param <T> The type held by the wrapper.
     */
    private static class Wrapper<T> {

        /**
         * Creates a new wrapper around the given value.
         *
         * @param value The initial value.
         */
        Wrapper(T value) {
            this.value = value;
        }

        /**
         * The value held by the wrapper.
         */
        private T value;
    }
}
