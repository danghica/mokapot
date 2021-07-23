package xyz.acygn.mokapot.util;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A map for which the values are <code>Expirable</code>, and automatically
 * created when required. If not accessed for a certain length of time, they
 * will expire and be effectively reset to the default (possibly with side
 * effects); expired entries are treated as nonexistent (whether it was this map
 * that expired them or a call to <code>expire()</code> independently of this
 * map).
 * <p>
 * Because this map conceptually has infinitely many elements, it cannot
 * implement all the operations of the <code>Map</code> interface, and as such
 * does not support standard collections interfaces.
 *
 * @author Alex Smith
 * @param <K> The type of the map's keys.
 * @param <V> The type of the map's values.
 */
public class ExpirableMap<K, V extends Expirable> {

    /**
     * A map of keys to values. An element is only present here if the key
     * exists in the map, the corresponding value is not a broken reference, and
     * the referent of the broken value is not expired. An absent value,
     * deallocated value, and expired value should all be treated identically.
     * <p>
     * The semantics of this are that values can be added to the map in slots
     * that are absent/deallocated/expired, but if a value is valid, it cannot
     * be replaced in the map unless it is removed first, and can only be
     * removed from the map by expiring it.
     */
    private final ConcurrentMap<K, WeakReference<V>> weakMap
            = new ConcurrentHashMap<>();

    /**
     * Keepalive pool for values. Because the <code>weakMap</code> field does
     * not hold a strong reference to the values, something else will have to,
     * and that's this pool. The pool is also used to handle timeouts of the
     * values.
     * <p>
     * While the map is empty, this can be <code>null</code>. Methods should be
     * sure to recreate it if necessary.
     */
    private final Lazy<KeepalivePool<V>> keepalives
            = new Lazy<>(KeepalivePool::new);

    /**
     * The duration to keep entries alive for after each access.
     */
    private final long duration;

    /**
     * The units of <code>duration</code>. This can be <code>null</code>, for a
     * conceptually infinite duration.
     */
    private final TimeUnit units;

    /**
     * Creates a new expirable map with the given configuration.
     *
     * @param duration The length of time for which a value has to be unused
     * before it is expired, implicitly reverting the map entry to the default.
     * Ignored if <code>units</code> is null.
     * @param units The units in which <code>duration</code> is measured. Can be
     * <code>null</code>, to turn off expiration-over-time behaviour altogether.
     */
    public ExpirableMap(long duration, TimeUnit units) {
        this.duration = duration;
        this.units = units;
    }

    /**
     * Expires every entry in the map, resetting the map to its default. This
     * method should be called when the map is no longer in use (even if it's
     * naturally emptied); it makes it possible to clean up a thread, in
     * addition to the obvious savings of memory used to track map elements.
     */
    public synchronized void clear() {
        /* Concurrency properties: we can't add to weakMap() while we're in a
           synchronized block, so don't need to worry about that; method calls
           on the values are safe due to the atomicity of expiredness; so as
           long as we "effectively clear" the map using shutdown(), which
           expires everything, we can safely subsequently clear the map. */
        KeepalivePool<V> oldPool = keepalives.reset(KeepalivePool::new);
        if (oldPool != null) {
            oldPool.shutdown();
        }
        weakMap.clear();
    }

    /**
     * Greatly improves performance if called from the expire() method of the
     * corresponding value. Note that the value has to know that it belongs to
     * this map, and what its key is, so this might not always be possible;
     * however, it makes it possible to free memory for the values that are no
     * longer used.
     * <p>
     * It's very important that the value given is the exact, specific object
     * that expired; don't access it via the key, as doing so is vulnerable to
     * race conditions.
     *
     * @param expiredKey The key of the value that expired.
     * @param expiredValue The value that expired.
     */
    public synchronized void informOfExpiry(K expiredKey, V expiredValue) {
        keepalives.get().expireNow(expiredValue, true);
        WeakReference<V> valueRef = weakMap.get(expiredKey);
        if (valueRef != null) {
            V storedValue = valueRef.get();
            if (storedValue == null || storedValue == expiredValue) {
                weakMap.remove(expiredKey);
            }
        }
    }

    /**
     * Places the given value into the map at the given key. The old value
     * there, if any, will be expired.
     *
     * @param key The key whose value should be replaced.
     * @param value The new replacement value.
     */
    public synchronized void replace(K key, V value) {
        WeakReference<V> valueRef = weakMap.get(key);
        if (valueRef != null) {
            V oldValue = valueRef.get();
            if (oldValue != null) {
                try {
                    oldValue.expire();
                } catch (Expirable.ExpiredException ex) {
                    /* do nothing; it's already expired */
                }
            }
        }
        weakMap.put(key, new WeakReference<>(value));
    }

    /**
     * Runs the given method on the value corresponding to the given key. The
     * method may (and probably will) mutate the value in question. If there is
     * no value corresponding to the key, then a specified supplier will instead
     * be used to create a new value for that key (with no method called on the
     * new value unless the supplier calls it itself before returning the
     * value).
     * <p>
     * As a side effect, this also resets the timeout on any existing value that
     * might be associated with the key. (If it expired as a consequence of
     * calling the method, then the timeout won't be reset if the map is aware
     * of the expiry. It comes to the same thing either way, as expired objects
     * are removed from the map whenever an attempt is made to operate on them.)
     *
     * @param key The key for the value to run the method on.
     * @param method The method to run.
     * @param defaultValue A supplier for the new value to use, if the existing
     * value is nonexistent, deallocated or expired. May be or return
     * <code>null</code>, in which case no changes will be made to the map (i.e.
     * this method will <code>method</code> if possible and otherwise do
     * nothing).
     * @return The value on which the method was run.
     */
    public V runMethodOn(K key, Expirable.ExpirableVoidMethod<V> method,
            Supplier<V> defaultValue) {
        Expirable.ExpirableMethod<V, V> methodAndKeepAlive = (v) -> {
            method.invoke(v);
            /* It's possible that the method will immediately expire the object
               it's operating on. In that case, the corresponding field of the
               map will be nulled. Check that field to see if it contains the
               value v that we're expecting; if it does, we can keep v alive, if
               it didn't, this object must have expired and thus there's no
               point in keeping it alive. (There's a race condition in which the
               object expires for an unrelated reason after we check the field;
               in that case, we end up keeping it alive pointlessly and
               unreferencedly until the timeout, which is harmless apart from
               consuming memory.) */
            try {
                WeakReference<V> currentValue = weakMap.get(key);
                if (currentValue != null && currentValue.get() == v) {
                    keepalives.get().keepAlive(v, duration, units);
                }
            } catch (TimeoutException ex) {
                /* Looks like we managed to call the method, and <i>then</i> the
                   object expired through timeout before we could keep it alive
                   (otherwise we would have got an ExpiredException earlier). In
                   that case, we might or might not have a problem, but the
                   current state is as good as it's ever going to be, so just
                   swallow the exception and hope. */
            }
            return v;
        };

        /* First, try to run the method directly without synchronization. A
           valid value can't be modified without expiring it (which will forever
           thereafter cause the <i>resulting value</i> to be expired, and thus
           is atomic "into the future"), so it's safe to do this
           unsynchronized. */
        WeakReference<V> valueRef = weakMap.get(key);
        V value = valueRef == null ? null : valueRef.get();
        return Expirable.runOr(value, methodAndKeepAlive, () -> {
            /* The value expired before the method ran, or else was null all
               along. Try again, this time with synchronization; if it fails
               again, we'll still be in the same synchronized block and thus
               will know for certain that the slot is still empty, deallocated
               or expired. (It's possible for it to change from expired to
               deallocated behind our back, but that doesn't matter; the two
               states are equivalent.) */
            synchronized (this) {
                WeakReference<V> valueRef2 = weakMap.get(key);
                V value2 = valueRef2 == null ? null : valueRef2.get();
                return Expirable.runOr(value2, methodAndKeepAlive, () -> {
                    /* OK, the value isn't valid, and we have a lock that lets
                       us ensure that nobody else will try to change the slot in
                       the map. So we can put the default value into the map
                       now. */
                    if (defaultValue == null) {
                        return null;
                    }
                    V newValue = defaultValue.get();
                    if (newValue == null) {
                        return null;
                    }
                    weakMap.put(key, new WeakReference<>(newValue));
                    try {
                        /* We also need to keep the new value alive. Note that
                           we need to do this inside this synchronized block, in
                           case we're racing against clear(). */
                        keepalives.get().keepAlive(newValue, duration, units);
                    } catch (TimeoutException ex) {
                        /* The new value wasn't in keepalives before, so it
                           shouldn't be possible for this to happen. */
                        throw new RuntimeException(ex);
                    }
                    return newValue;
                });
            }
        });
    }

    /**
     * Runs a given operation on, at least, all keys that currently correspond
     * to valid values. This will also run the operation on some keys
     * corresponding to expired values, unless this object has been informed of
     * the expiry of the value via a method call, by the garbage collector, or
     * because it expired the value itself.
     *
     * @param operation The operation to run.
     */
    public synchronized void forEachKey(Consumer<K> operation) {
        weakMap.forEach((k, v) -> {
            if (v.get() != null) {
                operation.accept(k);
            }
        });
    }
}
