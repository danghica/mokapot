package xyz.acygn.mokapot.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for timing how long an operation takes to occur. When created,
 * a stopwatch is showing a time of 0 and is stopped. When started, the time on
 * the stopwatch increases as real time passes (1 second of realtime passing = 1
 * second of time passed on the stopwatch). When stopped, the time on the
 * stopwatch stays steady even as real time passes.
 * <p>
 * This class is thread-safe; you can share a single stopwatch between multiple
 * threads safely.
 *
 * @author Alex Smith
 */
public class Stopwatch {

    /**
     * The amount of time that was recorded on the stopwatch prior to the most
     * recent time it was started. This always measures in nanoseconds,
     * regardless of the value of <code>units</code>. Access to this field must
     * be synchronised on the stopwatch's monitor.
     */
    private long time = 0;

    /**
     * When the stopwatch was last set to RUNNING state. If the stopwatch has
     * never been in RUNNING state, the value is meaningless. If the stopwatch
     * is not <i>currently</i> in RUNNING state, the value (although currently
     * well-defined) should not be used, as the meaning of the field in this
     * case may change in future. Access to this field must be synchronised on
     * the stopwatch's monitor.
     */
    private long startedAt;

    /**
     * The stopwatch that this stopwatch measures time against. This can be
     * <code>null</code>, to use <code>System.nanoTime</code> as the time base.
     */
    private final Stopwatch timeBase;

    /**
     * 0 if the stopwatch is running. 1 means that it's stopped; more than 1
     * means that it's recursively stopped. Access to this field must be
     * synchronised on the stopwatch's monitor.
     */
    private int stopCount = 1;

    /**
     * Creates a new stopwatch, which is (non-recursively) stopped and has a
     * time of 0.
     *
     * @param timeBase If <code>null</code>, the stopwatch counts in real time
     * (using <code>System.nanoTime</code>); otherwise, it runs only while the
     * given stopwatch is running (this provides a method for starting and
     * stopping stopwatches as a group via starting or stopping a shared
     * <code>timeBase</code>).
     */
    public Stopwatch(Stopwatch timeBase) {
        this.timeBase = timeBase;
    }

    /**
     * Returns the time, as measured by <code>timeBase</code>.
     *
     * @return A time in nanoseconds, relative to some arbitrary point in time.
     */
    private long getTimebaseTime() {
        if (timeBase == null) {
            return System.nanoTime();
        } else {
            return timeBase.time(ChronoUnit.NANOS);
        }
    }

    /**
     * Starts the stopwatch. Has no effect if it's already running.
     * <p>
     * If the stopwatch is recursively stopped, this will undo one call to
     * <code>recursiveStop()</code> without actually starting the stopwatch.
     *
     * @return The stopwatch itself, i.e. <code>this</code>.
     */
    public synchronized Stopwatch start() {
        if (stopCount == 0) {
            return this;
        }

        stopCount--;
        if (stopCount == 0) {
            startedAt = getTimebaseTime();
        }

        return this;
    }

    /**
     * Stops the stopwatch. Has no effect if it's already stopped or recursively
     * stopped.
     *
     * @return The stopwatch itself, i.e. <code>this</code>.
     */
    public synchronized Stopwatch stop() {
        if (stopCount != 0) {
            return this;
        } else {
            return recursiveStop();
        }
    }

    /**
     * Stops the stopwatch, even if it's already stopped. If a stopwatch is
     * "double-stopped", it will need to be started twice before it will run
     * again (likewise, a "triple-stopped" stopwatch will need to be started
     * three times, and so on).
     * <p>
     * The intended purpose of this method is to allow a method to pause a
     * stopwatch while it's running, even if the stopwatch is already stopped
     * (either manually or because another method is doing the same thing.)
     *
     * @return The stopwatch itself, i.e. <code>this</code>.
     */
    public synchronized Stopwatch recursiveStop() {
        if (stopCount == 0) {
            long newTime = time(ChronoUnit.NANOS);
            stopCount++;
            time = newTime;
        } else {
            stopCount++;
        }

        return this;
    }

    /**
     * Recursively stops the stopwatch, returning an object that starts it
     * again.
     * <p>
     * This method is equivalent to <code>recursiveStop</code> except for the
     * return value. The intended use is to allow a try-with-resources block to
     * pause a running stopwatch.
     *
     * @return An object that calls <code>start()</code> on this stopwatch when
     * autoclosed.
     */
    public DeterministicAutocloseable pause() {
        recursiveStop();
        return this::start;
    }

    /**
     * Returns the time on the stopwatch, in the given units. This can be called
     * whether the stopwatch is running or stopped, and will not stop or start
     * it.
     *
     * @param units The units with which to return the time. Must be in the
     * range of nanoseconds to days inclusive.
     * @return The time on the stopwatch, in the requested units.
     * @throws IllegalArgumentException If <code>units</code> is shorter than a
     * nanosecond or longer than a day
     */
    public synchronized long time(ChronoUnit units) {
        long curTime = time;
        if (stopCount == 0) {
            curTime += getTimebaseTime() - startedAt;
        }

        switch (units) {
            case DAYS:
                return Duration.ofNanos(curTime).toDays();
            case HOURS:
                return Duration.ofNanos(curTime).toHours();
            case MINUTES:
                return Duration.ofNanos(curTime).toMinutes();
            case SECONDS:
                return Duration.ofNanos(curTime).getSeconds();
            case MILLIS:
                return Duration.ofNanos(curTime).toMillis();
            case MICROS:
                return curTime / 1000; // there isn't a toMicros
            case NANOS:
                return curTime;
            default:
                throw new IllegalArgumentException(
                        "Cannot measure time in " + units);
        }
    }
}
