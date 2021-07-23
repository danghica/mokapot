package xyz.acygn.mokapot.util;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A set of <code>Expirable</code> objects that are interchangeable. Its main
 * purpose is to delegate method calls to an arbitrary object within the set
 * that happens to not have expired yet.
 * <p>
 * The set is actually a map; the objects have keys that can be used to tell
 * them apart, e.g. to expire a specific object.
 * <p>
 * The set itself expires if an attempt is made to delegate a method call to the
 * set, but it turns out that the set is empty or entirely expired. (As usual
 * for data structures that work on expirable objects, expired and nonexistent
 * objects are treated as equivalent.)
 *
 * @author Alex Smith
 * @param <K> The type of keys that can be used to identify a particular object
 * in the set.
 * @param <T> The type of alternative objects in the set.
 */
public class ExpirableAlternatives<K, T extends Expirable>
        implements Expirable {

    /**
     * The objects stored within the map.
     */
    private final ConcurrentMap<K, T> objects = new ConcurrentHashMap<>();

    /**
     * Whether this set is itself expired. Used to avoid race conditions
     * between, e.g., expire and add.
     */
    private final AtomicBoolean expired = new AtomicBoolean(false);

    /**
     * Expires the set itself, and every item in it.
     *
     * @throws ExpiredException If the set is expired
     */
    @Override
    public void expire() throws ExpiredException {
        if (expired.getAndSet(true)) {
            throw ExpiredException.SINGLETON;
        }

        objects.values().forEach((x) -> {
            try {
                x.expire();
            } catch (ExpiredException ex) {
                /* nothing to do */
            }
        });

        /* We may as well save a bit of memory. */
        objects.clear();
    }

    /**
     * Adds an element to the set.
     *
     * @param key A unique identifier by which the element can be subsequently
     * identified. The same identifier should never be used twice, not even if
     * its original user was removed from the set or expired.
     * @param element The element to add.
     * @throws ExpiredException If the set is expired.
     */
    public void add(K key, T element) throws ExpiredException {
        if (expired.get()) {
            throw ExpiredException.SINGLETON;
        }
        objects.put(key, element);
        if (expired.get()) {
            /* The set must have expired while we were adding the item; act as
               though the add happened first, i.e. remove the item and expire
               it */
            objects.remove(key);
            try {
                element.expire();
            } catch (ExpiredException ex) {
                /* notionally swallowed by our own expire() */
            }
        }
    }

    /**
     * Calls the given method on an arbitrary element of the set. The method
     * will be called on elements in turn, until one of them runs it
     * successfully (i.e. without throwing <code>ExpiredException</code>). If it
     * turns out that all elements in the set are expired (or the set is empty),
     * the set itself expires.
     *
     * @param m The method to run.
     * @throws Expirable.ExpiredException If this set has expired already, or if
     * it expires as a result of the call
     */
    public void callOnSomething(ExpirableVoidMethod<T> m)
            throws ExpiredException {
        if (expired.get()) {
            throw ExpiredException.SINGLETON;
        }
        /* the concurrent expiry behaviour here is plausible */

        Iterator<Map.Entry<K, T>> iterator = objects.entrySet().iterator();
        while (iterator.hasNext()) {
            T t = iterator.next().getValue();
            try {
                m.invoke(t);
                return;
            } catch (ExpiredException ex) {
                /* Save memory by removing expired objects from the set. */
                iterator.remove();
            }
        }

        expire();
        throw ExpiredException.SINGLETON;
    }

    /**
     * Calls a given method on a particular object within the set.
     *
     * @param k The object on which to call the method.
     * @param m The method to call.
     * @throws ExpiredException If the object is expired, or the set as a whole
     * is expired, or the object does not exist within the set
     */
    public void callOnSpecific(K k, ExpirableVoidMethod<T> m)
            throws ExpiredException {
        /* We don't need to check expired; if the set is actually expired k will
           not be found in it (or will have expired). */

        T t = objects.get(k);
        if (t == null) {
            throw ExpiredException.SINGLETON;
        }
        m.invoke(t);
    }
}
