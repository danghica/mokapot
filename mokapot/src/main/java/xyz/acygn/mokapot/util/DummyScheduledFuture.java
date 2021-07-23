package xyz.acygn.mokapot.util;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A scheduled feature that does nothing. Used to act as a placeholder in places
 * where a scheduled future is required.
 *
 * @author Alex Smith
 */
public class DummyScheduledFuture implements ScheduledFuture<Void> {

    /**
     * Returns 0. A dummy scheduled future has effectively already expired.
     *
     * @param unit Ignored.
     * @return 0
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return 0;
    }

    /**
     * Returns whether the given future has expired. (A dummy scheduled future
     * has expired by definition, so it's equal to expired futures and less than
     * non-expired futures.)
     *
     * @param other The future to check.
     * @return 0 if that future has a zero or negative delay; -1 otherwise.
     */
    @Override
    public int compareTo(Delayed other) {
        return other.getDelay(TimeUnit.SECONDS) > 0 ? -1 : 0;
    }

    /**
     * Has no effect. Returns <code>false</code>, to indicate that the scheduled
     * future had effectively already been cancelled.
     *
     * @param mayInterruptIfRunning Ignored.
     * @return <code>false</code>.
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    /**
     * Always returns true.
     *
     * @return <code>true</code>
     */
    @Override
    public boolean isCancelled() {
        return true;
    }

    /**
     * Always returns true.
     *
     * @return <code>true</code>
     */
    @Override
    public boolean isDone() {
        return true;
    }

    /**
     * Always throws an exception.
     *
     * @return Never returns.
     * @throws CancellationException Always.
     */
    @Override
    public Void get() throws CancellationException {
        throw new CancellationException();
    }

    /**
     * Always throws an exception.
     *
     * @param timeout Ignored.
     * @param unit Ignored.
     * @return Never returns.
     * @throws CancellationException Always.
     */
    @Override
    public Void get(long timeout, TimeUnit unit) throws CancellationException {
        throw new CancellationException();
    }
}
