package xyz.acygn.mokapot.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A concurrent map for which entries are removed when their values are
 * deallocated. The map holds its keys alive, but not its values. (Keys are
 * compared for equality using <code>.equals()</code>, as normal for a
 * <code>Map</code>, and should generally be assumed to be immutable and
 * copiable.)
 * <p>
 * The intended use of this class is to form an index that makes it possible to
 * find existing objects via searching on some value that's permanently
 * associated with them (perhaps the value of a <code>final</code> field),
 * whilst allowing the objects in question to be deallocated normally once
 * they're no longer in use.
 *
 * @author Alex Smith
 * @param <K> The type of the map keys.
 * @param <V> The type of the map values.
 */
public class WeakValuedConcurrentMap<K, V> implements ConcurrentMap<K, V> {

    /**
     * The map entries.
     */
    private final ConcurrentMap<K, EntryReference<K, V>> entries
            = new ConcurrentHashMap<>();

    /**
     * A reference queue that is used to clear up the keys corresponding to
     * deallocated values.
     */
    private final ReferenceQueue<V> queue = new ReferenceQueue<>();

    /**
     * Cleans up any map entries corresponding to deallocated values.
     */
    @SuppressWarnings("NestedAssignment")
    private void runQueue() {
        Reference<?> deadValue;
        while (((deadValue = queue.poll())) != null) {
            if (!(deadValue instanceof EntryReference)) {
                throw new RuntimeException("A WeakValuedConcurrentMap's queue "
                        + "should contain only EntryReferences");
            }
            @SuppressWarnings("unchecked")
            EntryReference<K, V> ref = (EntryReference) deadValue;
            entries.remove(ref.key, ref);
        }
    }

    /**
     * Associates the given key with a value, unless the key is already
     * associated with some non-deallocated value. This method guarantees that
     * an existing value won't be deallocated by the time it returns (via
     * returning the value in question), and thus the mapping will definitely
     * exist (one way or another) at the time it returns (unless a different
     * value is concurrently added by another thread, and that value gets
     * deallocated.)
     *
     * @param key The key to add, if it's not already there.
     * @param newValue The value to give it.
     * @return If the key/value pairing was added, <code>null</code>; otherwise,
     * the value that was stored alongside the key.
     */
    @Override
    public V putIfAbsent(K key, V newValue) {
        runQueue();
        EntryReference<K, V> old = entries.putIfAbsent(
                key, new EntryReference<>(key, newValue, queue));
        if (old == null) {
            return null;
        }
        V oldValue = old.get();
        if (oldValue == null) {
            /* Looks like we were racing against the garbage collector.
               We should be able to try again safely. (Note that we have to
               store old.get() in a variable; the correctness of this relies
               on preventing it being deallocated between the test-for-null and
               the return.) */
            return putIfAbsent(key, newValue);
        }
        return oldValue;
    }

    /**
     * Removes the given key/value pair from the map, if the given key does in
     * fact map to the given value.
     *
     * @param key The key to remove.
     * @param expectedValue The expected value for that key.
     * @return <code>true</code> if the expected key/value pair was found and
     * removed; <code>false</code> otherwise.
     */
    @Override
    public boolean remove(Object key, Object expectedValue) {
        runQueue();
        EntryReference<K, V> old = entries.get(key);
        if (old == null) {
            return false;
        }
        V actualValue = old.get();
        /* If the old value's been deallocated, it can't possibly be
           <code>value</code> (which is currently allocated by definition). */
        if (actualValue == null) {
            return false;
        }
        if (!actualValue.equals(expectedValue)) {
            return false;
        }
        if (entries.remove(key, old)) {
            return true;
        }
        /* Looks like someone concurrently changed the map while we were trying
           to determine if the entry was removable. Try again with the new
           values.*/
        return remove(key, expectedValue);
    }

    /**
     * If the given key is currently mapped to the given value, map it to a
     * different given value instead.
     *
     * @param key The key whose value should be replaced.
     * @param expectedValue The value that that key should currently have, if
     * it's going to be replaced.
     * @param newValue The value that it should be newly given.
     * @return <code>true</code> if a replacement was made.
     */
    @Override
    public boolean replace(K key, V expectedValue, V newValue) {
        runQueue();
        EntryReference<K, V> old = entries.get(key);
        if (old == null) {
            return false;
        }
        V actualValue = old.get();
        /* If the old value's been deallocated, it can't possibly be
           <code>value</code> (which is currently allocated by definition). */
        if (actualValue == null) {
            return false;
        }
        if (!actualValue.equals(expectedValue)) {
            return false;
        }
        if (entries.replace(key, old,
                new EntryReference<>(key, newValue, queue))) {
            return true;
        }
        /* Looks like someone concurrently changed the map while we were trying
           to determine if the entry was removable. Try again with the new
           values.*/
        return replace(key, expectedValue, newValue);
    }

    /**
     * If the given key is currently mapped to any value, map it to a given
     * value.
     *
     * @param key The key whose value should be replaced.
     * @param newValue The value it should be newly given.
     * @return <code>null</code> if no replacement was made because the key was
     * unmapped, or else the previous value.
     */
    @Override
    public V replace(K key, V newValue) {
        runQueue();
        EntryReference<K, V> old = entries.get(key);
        if (old == null) {
            return null;
        }
        V actualValue = old.get();
        if (actualValue == null) {
            /* Looks like the value got deallocated since we called runQueue().
               Try again to get a more up-to-date version of the map. */
            return replace(key, newValue);
        }
        if (entries.replace(key, old,
                new EntryReference<>(key, newValue, queue))) {
            return actualValue;
        }
        /* Someone concurrently changed the entry in question. Try again. */
        return replace(key, newValue);
    }

    /**
     * Returns an estimate for the number of entries in the map. This may be an
     * approximation due to changes in the map while the method is counting
     * (either due to concurrent calls to the map or due to the garbage
     * collector removing map entries when the values are deallocated).
     *
     * @return An estimate of the number of entries in the map.
     */
    @Override
    public int size() {
        runQueue();
        return entries.size();
    }

    /**
     * Returns whether or not the map contains any entries.
     *
     * @return <code>false</code> if the map contains entries, <code>true</code>
     * if it is empty
     */
    @Override
    public boolean isEmpty() {
        runQueue();
        return entries.isEmpty();
    }

    /**
     * Returns whether the map contains any entries with a given key. Because
     * this method does not return the corresponding value (thus keeping it
     * alive), it's possible that concurrent changes to the map, either due to
     * calls made by other threads or due to the garbage collector removing
     * key/value pairs for deallocated values, will cause the result to be
     * outdated before the caller can use it.
     *
     * @param key The key to check.
     * @return Whether the map contains any value corresponding to that key.
     */
    @Override
    public boolean containsKey(Object key) {
        runQueue();
        return entries.containsKey(key);
    }

    /**
     * Returns whether the map contains any entries with a given value. This
     * method requires scanning the whole map, and thus is inefficient.
     *
     * @param value The value to check.
     * @return Whether the map contains any key corresponding to that value.
     */
    @Override
    public boolean containsValue(Object value) {
        runQueue();
        return entries.entrySet().stream().anyMatch(
                ((e) -> value.equals(e.getValue().get())));
    }

    /**
     * Returns the value corresponding to a given key.
     *
     * @param key The key to check.
     * @return The corresponding value, or <code>null</code> if no value exists
     * for that key.
     */
    @Override
    public V get(Object key) {
        runQueue();
        EntryReference<K, V> old = entries.get(key);
        if (old == null) {
            return null;
        }
        V actualValue = old.get();
        if (actualValue == null) {
            /* Looks like the value got deallocated since we called runQueue().
               Try again to get a more up-to-date version of the map. */
            return get(key);
        }
        return actualValue;
    }

    /**
     * Sets the value for a given key, regardless of whether a previous value
     * existed.
     *
     * @param key The key to set the value for.
     * @param newValue The value to set it to.
     * @return The previous value that the key mapped to, or <code>null</code>
     * if there was no previous mapping.
     */
    @Override
    public V put(K key, V newValue) {
        runQueue();
        EntryReference<K, V> old
                = entries.put(key, new EntryReference<>(key, newValue, queue));
        if (old == null) {
            return null;
        }
        return old.get();
    }

    /**
     * Removes any mapping for the given key.
     *
     * @param key The key to remove the value for.
     * @return The previous value that the key mapped to, or <code>null</code>
     * if there was no previous mapping.
     */
    @Override
    public V remove(Object key) {
        runQueue();
        EntryReference<K, V> old = entries.remove(key);
        if (old == null) {
            return null;
        }
        return old.get();
    }

    /**
     * Adds all entries of the given map to this map. Note that this operation
     * is not atomic, in the sense that if concurrent changes are being made to
     * this map, there's no guarantee on how they interleave with the additions.
     *
     * @param m The map to add the entries from.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        runQueue();
        m.forEach(this::put);
    }

    /**
     * Removes all entries from this map.
     */
    @Override
    public void clear() {
        entries.clear();
    }

    /**
     * Returns a set of keys of this map. Deleting from the set will delete the
     * corresponding map entries. Note that entries may disappear from the set
     * "spontaneously" due to values being deleted by the garbage collector, and
     * concurrent changes to the map might or might not be reflected in the set.
     *
     * @return A set view of the current set of keys of the map. (This is the
     * set of keys itself, not a copy, and thus can be operated on directly.)
     */
    @Override
    public Set<K> keySet() {
        runQueue();
        return entries.keySet();
    }

    /**
     * Unimplemented method.
     *
     * @return Never returns normally.
     * @throws UnsupportedOperationException Always
     */
    @Override
    public Collection<V> values() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "WeakValuedConcurrentMap#values is unimplemented");
    }

    /**
     * Unimplemented method.
     *
     * @return Never returns normally.
     * @throws UnsupportedOperationException Always
     */
    @Override
    public Set<Entry<K, V>> entrySet() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "WeakValuedConcurrentMap#entrySet is unimplemented");
    }

    /**
     * A weak reference to a value within the concurrent map. This tracks the
     * corresponding key, allowing the key/value pair to be removed from the map
     * once the value is deallocated.
     *
     * @param <K> The type of the map keys via which the reference is located
     * within the map.
     * @param <V> The type of the values that the reference is referencing.
     */
    private static class EntryReference<K, V> extends WeakReference<V> {

        /**
         * The key corresponding to this value.
         */
        private final K key;

        /**
         * Creates a new entry reference. This constructor does not add the new
         * reference to the map; the caller will have to do that.
         *
         * @param key The map key where the entry reference will be stored.
         * @param referent The object to which a weak reference should be
         * created.
         * @param queue The queue on which the new entry reference should be
         * enqueued.
         */
        private EntryReference(K key, V referent, ReferenceQueue<V> queue) {
            super(referent, queue);
            this.key = key;
        }
    }

    /**
     * Explicit delegation to Object#hashCode().
     * <p>
     * TODO: This is a temporary workaround for a bug in
     * <code>StandinGenerator</code> and should not be necessary.
     *
     * @return This object's identity hash code.
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Explicit delegation to Object#equals().
     * <p>
     * TODO: This is a temporary workaround for a bug in
     * <code>StandinGenerator</code> and should not be necessary.
     *
     * @param o The object to compare to.
     * @return <code>this == o</code>.
     */
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
