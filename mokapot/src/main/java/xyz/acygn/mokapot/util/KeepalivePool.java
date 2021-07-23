package xyz.acygn.mokapot.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.concurrent.TimeoutException;
import static xyz.acygn.mokapot.util.ThreadUtils.delayInterruptions;
import static xyz.acygn.mokapot.util.ThreadUtils.unwrapAndRethrow;

/**
 * An object which allows other objects to hold themselves alive for a given
 * length of time. The intended use is for this object to hold the only strong
 * reference to the object, and all other references to the object (apart from
 * short-lived references used to operate on the object temporarily) to be weak
 * references (with a broken reference and expired object being treated the same
 * way).
 *
 * @author Alex Smith
 * @param <K> The type of objects that this object will hold alive.
 */
public class KeepalivePool<K extends Expirable> {

    /**
     * The set of objects to keep alive, together with their expiry times.
     */
    private final Map<ObjectIdentity<K>, Optional<Future<?>>> objectsToKeepAlive
            = new HashMap<>();

    /**
     * The thread that handles the actual expiries.
     */
    private final Lazy<ScheduledExecutorService> timeoutThread
            = new Lazy<>(() -> newSingleThreadScheduledExecutor());

    /**
     * Keeps the given object alive for at least the given duration (unless the
     * object is manually expired earlier). Note that if this is called multiple
     * times with the same object, the later durations will overwrite the
     * earlier ones even if they would end earlier, and are measured relative to
     * the later calls (rather than the original call to this method).
     * <p>
     * When the duration (after allowing for any changes made to the duration
     * via repeated calls to this method) expires, the object will also be
     * expired (in addition to this keepalive pool ceasing to keep it alive).
     *
     * @param obj The object to keep alive.
     * @param duration The length of time for which to keep it alive. Ignored if
     * <code>unit</code> is <code>null</code>.
     * @param unit The units in which <code>duration</code> is measured. If
     * <code>null</code>, it specifies that the keepalive is not timed (and thus
     * that the keepalive lasts until manually removed).
     * @throws TimeoutException If the given object has already been expired by
     * this pool
     * @throws RejectedExecutionException If this keepalive pool has already
     * been shut down
     */
    public synchronized void keepAlive(K obj, long duration, TimeUnit unit)
            throws TimeoutException, RejectedExecutionException {
        Optional<Future<?>> newFuture;
        if (unit != null) {
            newFuture = Optional.of(timeoutThread.get().schedule(
                    () -> this.removeAndExpire(obj), duration, unit));
        } else {
            newFuture = Optional.empty();
        }
        Optional<Future<?>> maybeOldFuture = objectsToKeepAlive.put(
                new ObjectIdentity<>(obj), newFuture);
        if (maybeOldFuture != null && maybeOldFuture.isPresent()) {
            Future<?> oldFuture = maybeOldFuture.get();
            if (!oldFuture.cancel(false)) {
                /* We tried to set a new duration, but the old one had already
                   expired. That's something of a problem, and something that
                   we can't handle ourselves; the caller will have to do it. We
                   can at least cancel the new future to save a bit of memory
                   (it wouldn't do anything if executed anyway, which in turn
                   means that failing to cancel it is not a problem).

                   Note that the current state of things must be that the old
                   object future is currently running but has not yet gotten to
                   the point of removing the object (and is currently stuck
                   waiting for the monitor on the keepalive pool). So there's no
                   need to remove the new future from the pool; the old one is
                   about to. */
                newFuture.map((timer) -> timer.cancel(false));
                throw new TimeoutException(
                        "Attempt to keep an object alive came too late, it had already died");
            }
        }
    }

    /**
     * Removes the given object from the pool and expires it immediately. This
     * is a low-level method that does not cancel the future that would
     * otherwise expire it; the caller will have to do that themselves. (This
     * method is often called <i>from</i> the future, which will naturally mark
     * itself as complete when run.) Does not error out if the object is already
     * removed or already expired; just does as much as possible.
     *
     * @param obj The object to remove and expire.
     */
    private synchronized void removeAndExpire(K obj) {
        objectsToKeepAlive.remove(new ObjectIdentity<>(obj));

        try {
            obj.expire();
        } catch (Expirable.ExpiredException ex) {
            /* Looks like someone else expired it already; we have nothing to
               do. */
        }
    }

    /**
     * Ceases to keep the given object alive and expires it immediately, if it
     * hasn't been already. If the object isn't currently being held alive,
     * silently does nothing. The object having already expired is also not
     * treated as an error condition (so if an object ends up internally marking
     * itself as expired and then calls this method to remove itself from a
     * keepalive pool, things will still work).
     *
     * @param obj The object to expire.
     * @param skipExpire If <code>true</code>, assume that the object has
     * already expired and do not expire it, even if a failure to expire it
     * would break invariants. This should only be set to <code>true</code> in
     * the case where the caller is certain that the object has expired or is
     * expiring (e.g. because the caller is the object's <code>expire</code>
     * method).
     */
    public synchronized void expireNow(K obj, boolean skipExpire) {
        /* Remove the object from the keepalive pool. */
        Optional<Future<?>> oldFuture = objectsToKeepAlive.remove(
                new ObjectIdentity<>(obj));
        if (oldFuture == null) {
            /* The object wasn't there. We have nothing to do. */
            return;
        }

        if (skipExpire) {
            /* async cancel of the future - if it's already expired, we have
               nothing to do */
            if (oldFuture.isPresent()) {
                oldFuture.get().cancel(false);
            }
            objectsToKeepAlive.remove(new ObjectIdentity<>(obj));
        } else {
            accelerateFuture(obj, oldFuture);
        }
    }

    /**
     * "Accelerates" the given scheduled future, causing it to, effectively, run
     * immediately and synchronously. There are two possible ways this might be
     * done; either waiting for it to finish (if it's already executing or has
     * already executed), or else cancelling it and performing the task ourself.
     * <p>
     * This method should only be called from contexts which are already
     * synchronized. It can safely be called even in cases where the caller has
     * already removed the object from the map, as long as that's in the same
     * synchronized block as this method itself.
     *
     * @param obj The object that the future pertains to.
     * @param future The future to accelerate. This can also be
     * <code>Optional.empty()</code> if no future was created (because the
     * expiry does not happen on a timer), in which case this method will always
     * have to "cancel" the non-existing future and perform the same job
     * manually.
     */
    private void accelerateFuture(K obj, Optional<Future<?>> future) {
        /* We're expiring the object now, so cancel the scheduled expiry in
           the future so that it doesn't expire a second time. */
        if (future.isPresent()) {
            Future<?> f = future.get();
            if (!f.cancel(false)) {
                /* Looks like the object already has been or is being expired; so
               just wait for that operation to complete. */
                delayInterruptions(() -> {
                    try {
                        future.get().get();
                    } catch (ExecutionException ex) {
                        /* Pretend we were expiring it synchronously. */
                        unwrapAndRethrow(ex);
                    }
                });
                return;
            }
        }

        removeAndExpire(obj);
    }

    /**
     * Expires all objects in this pool immediately, empties the pool, and
     * prevents any further objects being added. The pool should be shutdown
     * before deallocation, in order to avoid leaving its timeout-handling
     * thread active until finalization.
     */
    public synchronized void shutdown() {
        List<ObjectIdentity<K>> allKeys
                = new ArrayList<>(objectsToKeepAlive.keySet());
        allKeys.stream().forEach((key) -> {
            accelerateFuture(key.dereference(), objectsToKeepAlive.get(key));
        });
        objectsToKeepAlive.clear();

        AccessController.doPrivileged((PrivilegedAction<?>) () -> {
            if (timeoutThread.isInitialised()) {
                timeoutThread.get().shutdown();
                /* Wait for expiry to complete. In theory, this should happen
                   instantly, because we accelerated all the remaining futures.
                   However, do it anyway so that we know about any bugs that
                   might cause stray futures to be left around. */
                delayInterruptions(() -> {
                    if (!timeoutThread.get().awaitTermination(3, SECONDS)) {
                        throw new RuntimeException(
                                "Final KeepalivePool expiry is taking too long");
                    }
                });
            }
            return null;
        });
    }
}
