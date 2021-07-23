package xyz.acygn.mokapot.util;

import static java.lang.System.identityHashCode;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A concurrent map that uses weak references for both keys and values, and
 * compares keys by reference equality. If either a key or value becomes
 * deallocated (which is possible because this map does not hold either alive),
 * the corresponding map entry will be removed as a consequence.
 * <p>
 * <code>null</code> keys and values are not supported (as you couldn't usefully
 * construct weak references to them).
 *
 * @author Alex Smith
 * @param <K> The type of the map keys.
 * @param <V> The type of the map values.
 */
public class DoublyWeakConcurrentMap<K, V> implements ConcurrentMap<K, V> {

    /**
     * The reference queue that handles expiry of entries when their references
     * break.
     */
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();

    /**
     * The map entries, grouped by hash code. Note that the values of this map
     * are <i>immutable</i> sets; instead of editing the sets, you replace the
     * entire set. Such a set cannot contain two or more entries with the same
     * key unless all but one is expired.
     */
    private final ConcurrentMap<Integer, Set<Entry>> entries
            = new ConcurrentHashMap<>();

    /**
     * Handles removal of any broken references from the map. This should be
     * called before processing other map operations, to prevent broken
     * references using up memory forever.
     */
    @SuppressWarnings("NestedAssignment")
    private void runQueue() {
        Reference<?> ro;
        while (((ro = queue.poll())) != null) {
            if (ro instanceof DoublyWeakConcurrentMap.Entry.Ref) {
                ((DoublyWeakConcurrentMap.Entry.Ref) ro).expire();
            }
        }
    }

    /**
     * Finds the nonexpired entry in an entry set which has the given key, if
     * any. (Note that the entry may expire between this function determining it
     * to be non-expired and your attempt to use it; in that case, the set no
     * longer has a non-expired entry with the key in question, so you should
     * treat this case identically to a null return value.)
     *
     * @param set The set to look in. Can be <code>null</code>.
     * @param key The key to look for.
     * @return The non-expired set entry with the given key; or
     * <code>null</code>, if no such entry exists or <code>set</code> was
     * <code>null</code>.
     */
    private Entry findNonexpiredEntry(Set<Entry> set, Object key) {
        if (set == null) {
            return null;
        }
        return set.stream().filter((e) -> e.key.get() == key)
                .filter((e) -> !e.expired.get()).findAny().orElse(null);
    }

    /**
     * Atomically performs the given operation on the appropriate entry set for
     * the given key. That is, the state of <code>entries</code> will change
     * such that the old entry set associated with <code>key</code> will be
     * mapped to a new entry set associated with <code>key</code>.
     * <p>
     * To ensure atomicity, the operation in question may be run multiple times
     * with different entry sets (from different points in time), until a stable
     * value is found.
     *
     * @param <R> The type of the return value.
     * @param key The key that determines which entry set to use.
     * @param op The operation to perform; it will be given the existing state
     * of the entry, and returns the new state and the value to return from
     * <code>entrySetOperation</code>. (Note that the existing state could be
     * <code>null</code>, as could the new state.)
     * @return The second return value of the <code>op</code> call that actually
     * updated the entry set.
     */
    private <R> R entrySetOperation(Object key,
            Function<Set<Entry>, Pair<Set<Entry>, R>> op) {
        runQueue();
        int hc = identityHashCode(key);
        Set<Entry> oldEntrySet = entries.get(hc);
        while (true) {
            Pair<Set<Entry>, R> oprv = op.apply(oldEntrySet);
            Set<Entry> newEntrySet = oprv.getFirst();
            if (oldEntrySet == null && newEntrySet == null) {
                oldEntrySet = entries.get(hc);
                if (oldEntrySet == null) {
                    return oprv.getSecond();
                }
            } else if (oldEntrySet == null) {
                oldEntrySet = entries.putIfAbsent(hc, newEntrySet);
                if (oldEntrySet == null) {
                    return oprv.getSecond();
                }
            } else if (newEntrySet == null) {
                if (entries.remove(hc, oldEntrySet)) {
                    return oprv.getSecond();
                }
                oldEntrySet = entries.get(hc);
            } else {
                if (entries.replace(hc, oldEntrySet, newEntrySet)) {
                    return oprv.getSecond();
                }
                oldEntrySet = entries.get(hc);
            }
        }
    }

    /**
     * Adds the given key/value pair to the map, unless the key is already
     * associated with some value (and the previous value has not been
     * deallocated).
     *
     * @param key The key to add, if it isn't already there.
     * @param value The value to associate it with, if a change is necessary.
     * @return The value for the existing mapping, if there was one. (If a new
     * mapping was added, this will be <code>null</code>.)
     */
    @Override
    public V putIfAbsent(K key, V value) {
        return entrySetOperation(key, (eset) -> {
            Entry oldEntry = findNonexpiredEntry(eset, key);
            try {
                if (oldEntry != null) {
                    return new Pair<>(eset, oldEntry.getValueIfPresent());
                }
            } catch (Expirable.ExpiredException ex) {
                /* Looks like the entry has expired and not been cleared up
                   yet. We treat this the same as if it wasn't there. */
            }
            return new Pair<>(ImmutableSets.plusElement(eset,
                    new Entry(key, value)), null);
        });
    }

    /**
     * Removes a given key/value pair from the map, if the key is currently
     * mapped to the given value.
     *
     * @param key The key to remove.
     * @param oldValue The value it's expected to have.
     * @return <code>true</code> if the key/value pair existed (i.e. an entry
     * was removed).
     */
    @Override
    public boolean remove(Object key, Object oldValue) {
        return entrySetOperation(key, (eset) -> {
            Entry oldEntry = findNonexpiredEntry(eset, key);
            if (oldEntry == null) {
                return new Pair<>(eset, false);
            }
            try {
                if (oldEntry.getValueIfPresent().equals(oldValue)) {
                    return new Pair<>(ImmutableSets.minusElement(eset,
                            oldEntry), true);
                }
            } catch (Expirable.ExpiredException ex) {
                /* Looks like the entry has expired and not been cleared up
                   yet. We treat this the same as if it wasn't there. */
            }
            return new Pair<>(eset, false);
        });
    }

    /**
     * Alters the given key/value pair within the map, if the key is currently
     * mapped to the given value, by changing the value it's mapped to.
     *
     * @param key The key to remove.
     * @param oldValue The value it's expected to have.
     * @param newValue The value it will be given if it has the existing value.
     * @return <code>true</code> if the key/value pair existed (i.e. an entry
     * was replaced).
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return entrySetOperation(key, (eset) -> {
            Entry oldEntry = findNonexpiredEntry(eset, key);
            if (oldEntry == null) {
                return new Pair<>(eset, false);
            }
            try {
                if (oldEntry.getValueIfPresent().equals(oldValue)) {
                    return new Pair<>(ImmutableSets.plusElement(
                            ImmutableSets.minusElement(eset, oldEntry),
                            new Entry(key, newValue)), true);
                }
            } catch (Expirable.ExpiredException ex) {
                /* Looks like the entry has expired and not been cleared up
                   yet. We treat this the same as if it wasn't there. */
            }
            return new Pair<>(eset, false);
        });
    }

    /**
     * Alters the given key/value pair within the map, if the key is currently
     * mapped to any value, by changing the value it's mapped to.
     * <p>
     * Note that because a doubly weak map might have entries disappear at any
     * time, the usefulness of this method is somewhat limited (although not
     * entirely useless, as you can be sure that any existing mapping will be
     * removed one way or the other).
     *
     * @param key The key to remove.
     * @param newValue The value it will be given if it currently has some
     * value.
     * @return The value that was replaced, or <code>null</code> if no value was
     * replaced.
     */
    @Override
    public V replace(K key, V newValue) {
        return entrySetOperation(key, (eset) -> {
            Entry oldEntry = findNonexpiredEntry(eset, key);
            if (oldEntry == null) {
                return new Pair<>(eset, null);
            }
            try {
                /* Note: the conditional here is the ExpiredException on the
                   oldEntry.getValueIfPresent(). */
                return new Pair<>(ImmutableSets.plusElement(
                        ImmutableSets.minusElement(eset, oldEntry),
                        new Entry(key, newValue)),
                        oldEntry.getValueIfPresent());
            } catch (Expirable.ExpiredException ex) {
                /* Looks like the entry has expired and not been cleared up
                   yet. We treat this the same as if it wasn't there. */
            }
            return new Pair<>(eset, null);
        });
    }

    /**
     * Returns the number of non-broken mappings in this map. This is only
     * weakly consistent, because it's possible for the number of mappings to
     * change while this method is counting them (due to garbage collector
     * activity or concurrent calls).
     *
     * @return An approximation of the size of this map.
     */
    @Override
    public int size() {
        runQueue();
        /* note: don't check for expiry; that's a) slow, and b) unnecessary
           because runQueue will have removed any expired entries other than
           those which expired while we're counting, which might not have been
           counted anyway if the iteration happened in a different order */
        return entries.entrySet().stream().
                mapToInt((e) -> e.getValue().size()).sum();
    }

    /**
     * Returns whether at least one non-broken mapping exists in this map. If a
     * mapping is added while this method is executing, it may or may not be
     * considered for determining the return value. It's possible that a mapping
     * will break due to garbage collector activity while this method is
     * running, thus causing a <code>false</code> return value even though no
     * return values remain by the time the method returns.
     *
     * @return An approximation of the size of this map.
     */
    @Override
    public boolean isEmpty() {
        runQueue();
        /* Very simple: runQueue removed all the broken mappings from the entry
           sets, and entry sets are removed from the map when they empty, so
           this is actually accurate! */
        return entries.isEmpty();
    }

    /**
     * Returns whether the key in question is currently mapped to some value.
     * You should probably use <code>get()</code> instead, in order to prevent a
     * race with the garbage collector (if the garbage collector notices that
     * the value has become unreferenced while this method is returning, the key
     * will become unmapped but the return value will still be
     * <code>true</code>).
     *
     * @param key The key to check
     * @return <code>true</code> if the key is mapped to some value
     */
    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    /**
     * Returns whether some key maps to the value in question. Comparison on
     * values is done using <code>.equals()</code>.
     * <p>
     * This method is only weakly consistent, and is also very inefficient, as
     * the map does not contain a "reverse index" (and thus will have to seach
     * every key individually).
     *
     * @param value The value to look for.
     * @return <code>true</code> if some key maps to that value
     */
    @Override
    public boolean containsValue(Object value) {
        runQueue();
        return entries.entrySet().stream().anyMatch((e)
                -> e.getValue().stream().anyMatch((e2)
                        -> Objects.equals(value, e2.value.get())));
    }

    /**
     * Returns the value mapped to the given key.
     *
     * @param key The key to look for.
     * @return The value mapped to that key; or <code>null</code> if the mapping
     * does not exist (possibly because the value became unreferenced, thus
     * causing the entry to be removed from the hash table)
     */
    @Override
    public V get(Object key) {
        runQueue();
        Entry e = findNonexpiredEntry(entries.get(identityHashCode(key)), key);
        if (e != null) {
            try {
                return e.getValueIfPresent();
            } catch (Expirable.ExpiredException ex) {
                return null;
            }
        }
        return null;
    }

    /**
     * Causes a given key to map to a given value. This happens regardless of
     * whether or not an existing mapping exists for the key; if one does exist,
     * it'll be overwritten.
     * <p>
     * Bear in mind that unless the key and value are both being held alive by
     * other means, the new mapping may end up being deleted almost immediately
     * due to the key and/or value being deallocated.
     *
     * @param key The key to map.
     * @param newValue The new value to map it to.
     * @return The value that <code>key</code> previously mapped to, or
     * <code>null</code> if it did not previously have a mapping
     */
    @Override
    public V put(K key, V newValue) {
        return entrySetOperation(key, (eset) -> {
            Entry oldEntry = findNonexpiredEntry(eset, key);
            Set<Entry> newEntrySet = oldEntry == null ? eset
                    : ImmutableSets.minusElement(eset, oldEntry);
            newEntrySet = ImmutableSets.plusElement(
                    newEntrySet, new Entry(key, newValue));
            try {
                return new Pair<>(newEntrySet,
                        oldEntry == null ? null : oldEntry.getValueIfPresent());
            } catch (Expirable.ExpiredException ex) {
                return new Pair<>(newEntrySet, null);
            }
        });
    }

    /**
     * Removes any mappings that exist for the given key.
     *
     * @param key The key to remove mappings for.
     * @return The value that was previously mapped to the key, or
     * <code>null</code> if there was no such value.
     */
    @Override
    public V remove(Object key) {
        return entrySetOperation(key, (eset) -> {
            Entry oldEntry = findNonexpiredEntry(eset, key);
            Set<Entry> newEntrySet = oldEntry == null ? eset
                    : ImmutableSets.minusElement(eset, oldEntry);
            try {
                return new Pair<>(newEntrySet,
                        oldEntry == null ? null : oldEntry.getValueIfPresent());
            } catch (Expirable.ExpiredException ex) {
                return new Pair<>(newEntrySet, null);
            }
        });
    }

    /**
     * Adds all entries from the given map into this one.
     * <p>
     * This is not an atomic operation; the entries will be added one at a time.
     *
     * @param map The map containing the entries to add.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        runQueue();
        map.forEach(this::put);
    }

    /**
     * Deletes all entries from this map.
     */
    @Override
    public void clear() {
        /* No need to runQueue(); it'll be more efficient to just let the whole
           thing disappear (note that entries disappear from a reference queue
           if they themselves get deallocated) */
        entries.clear();
    }

    /**
     * Currently unimplemented.
     *
     * @return Never returns.
     * @throws UnsupportedOperationException Always.
     */
    @Override
    public Set<K> keySet() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "DoublyWeakConcurrentMap#keySet");
    }

    /**
     * Currently unimplemented.
     *
     * @return Never returns.
     * @throws UnsupportedOperationException Always.
     */
    @Override
    public Collection<V> values() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "DoublyWeakConcurrentMap#keySet");
    }

    /**
     * Currently unimplemented.
     *
     * @return Never returns.
     * @throws UnsupportedOperationException Always.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "DoublyWeakConcurrentMap#keySet");
    }

    /**
     * Performs an operation on every key/value pair in the map. Behaviour is
     * undefined if the map is changed during the operation. However, if an
     * element becomes deallocated during the operation, it will be silently
     * skipped over.
     *
     * @param f The operation to perform.
     */
    @Override
    public void forEach(BiConsumer<? super K, ? super V> f) {
        runQueue();
        entries.values().forEach((entrySet)
                -> entrySet.forEach((entry) -> {
                    try {
                        K key = entry.getKey();
                        V value = entry.getValue();
                        f.accept(key, value);
                    } catch (IllegalStateException ex) {
                        /* looks like it expired; just ignore it */
                    }
                }));
    }

    /**
     * A single entry in the doubly weak concurrent map. This enters "expired"
     * state when either the key or value reference breaks.
     */
    private class Entry implements Expirable, Map.Entry<K, V> {

        /**
         * The identity hash code of the key.
         */
        private final int keyIdentityHashCode;

        /**
         * The key stored within the entry.
         */
        private final Ref<K> key;

        /**
         * The value stored within the entry. Access to this field must be
         * synchronised on the entry's monitor.
         */
        private Ref<V> value;

        /**
         * Whether this entry's expiration has been processed.
         */
        private final AtomicBoolean expired = new AtomicBoolean(false);

        /**
         * Creates a new entry with a given key and value.
         *
         * @param key The key (as a strong reference).
         * @param value The value (as a strong reference).
         */
        Entry(K key, V value) {
            if (key == null) {
                throw new NullPointerException(
                        "key is null in DoublyWeakConcurrentMap");
            }
            if (value == null) {
                throw new NullPointerException(
                        "value is null in DoublyWeakConcurrentMap");
            }
            this.key = new Ref<>(key, queue);
            this.value = new Ref<>(value, queue);
            this.keyIdentityHashCode = identityHashCode(key);
        }

        /**
         * Returns the value, if it's still present. The value might be missing
         * due to the reference having broken or due to the key reference having
         * been broken (causing the value to be forgotten).
         *
         * @return The value stored in this entry. Cannot be <code>null</code>.
         * @throws ExpiredException If the value is no longer present, due to
         * the key or value reference breaking
         */
        private synchronized V getValueIfPresent() throws ExpiredException {
            if (expired.get()) {
                throw ExpiredException.SINGLETON;
            }
            V gotValue = value.get();
            if (gotValue == null) {
                throw ExpiredException.SINGLETON;
            }
            return gotValue;
        }

        @Override
        public void expire() throws ExpiredException {
            if (expired.getAndSet(true)) {
                throw ExpiredException.SINGLETON;
            }
            key.clear();
            value.clear();

            entries.computeIfPresent(keyIdentityHashCode, (kihc, eset)
                    -> ImmutableSets.minusElement(eset, this));
        }

        /**
         * Returns the key portion of this entry. (Note that this throws
         * <code>IllegalStateException</code>, not
         * <code>ExpiredException</code>, to conform to the
         * <code>Map.Entry</code> interface.)
         *
         * @return The key.
         * @throws IllegalStateException If the entry has expired
         */
        @Override
        public K getKey() throws IllegalStateException {
            K rv = this.key.get();
            if (rv == null || expired.get()) {
                throw new IllegalStateException("entry has expired");
            }
            return rv;
        }

        /**
         * Returns the value portion of this entry. (Note that this throws
         * <code>IllegalStateException</code>, not
         * <code>ExpiredException</code>, to conform to the
         * <code>Map.Entry</code> interface.)
         *
         * @return The value.
         * @throws IllegalStateException If the entry has expired
         */
        @Override
        public synchronized V getValue() {
            V rv = this.value.get();
            if (rv == null || expired.get()) {
                throw new IllegalStateException("entry has expired");
            }
            return rv;
        }

        /**
         * Alters the value portion of this entry in place. (Note that this
         * throws <code>IllegalStateException</code>, not
         * <code>ExpiredException</code>, to conform to the
         * <code>Map.Entry</code> interface.)
         *
         * @param newValue The new value to store.
         * @return The previous value.
         * @throws IllegalStateException If the entry has expired
         */
        @Override
        public synchronized V setValue(V newValue) {
            if (newValue == null) {
                throw new NullPointerException(
                        "newValue is null in DoublyWeakConcurrentMap.Entry#setValue");
            }
            K holdKeyAlive = key.get();
            V oldValue = value.get();
            if (holdKeyAlive == null || oldValue == null
                    || expired.get()) {
                throw new IllegalStateException("entry has expired");
            }
            /* note: at this point it's guaranteed that key and value were never
               enqueued and cannot be enqueued until after this code has run */
            value.clear();
            value = new Ref<>(newValue, queue);

            /* It's important to hold the key and value alive until the end of
               the method, to prevent a race against the garbage collector
               and the reference queue (we don't want the entry expiring, and
               then getting a non-cleared value reference after the expiration
               has been handled). */
            BackgroundGarbageCollection.volatileAccess(holdKeyAlive);
            BackgroundGarbageCollection.volatileAccess(oldValue);

            return oldValue;
        }

        /**
         * A WeakReference implementation that provides visibility to the
         * Entry's <code>expire</code> method.
         *
         * @param <T> The type of the reference target.
         */
        private class Ref<T> extends WeakReference<T> {

            /**
             * Constructs a weak reference that enqueues to a specific queue
             * when the reference breaks (if still allocated at that point).
             *
             * @param target The reference's target.
             * @param queue The queue on which to enqueue the reference when it
             * breaks.
             */
            private Ref(T target, ReferenceQueue<? super T> queue) {
                super(target, queue);
            }

            /**
             * Calls the <code>expire</code> method of the parent Entry. If the
             * parent entry has already expired, this has no effect.
             */
            private void expire() {
                try {
                    Entry.this.expire();
                } catch (ExpiredException ex) {
                    /* no need to do anything, the previous expire() call did */
                }
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
