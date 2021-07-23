package xyz.acygn.mokapot;

import java.lang.ref.WeakReference;
import java.util.ConcurrentModificationException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import xyz.acygn.mokapot.skeletons.InvokeByCode;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.skeletons.StandinStorage;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper;
import xyz.acygn.mokapot.util.BackgroundGarbageCollection;
import xyz.acygn.mokapot.util.BlockingHolder;
import xyz.acygn.mokapot.util.DeterministicAutocloseable;
import static xyz.acygn.mokapot.util.ThreadUtils.delayInterruptionsRv;

/**
 * A reference to a standin that "tightens" when it's the strongest remaining
 * reference to the object.
 * <p>
 * The basic idea of a loose/tight reference is as follows: the reference can be
 * changed between a "loose" state and a "tight" state at will, and holds the
 * object alive in either state; whenever there are no strong or tight
 * references left to a connected group of objects, all loose references to
 * those objects automatically (and simultaneously and atomically) become tight.
 * This allows observation into whether other references exist to an object, by
 * loosening a reference and then checking to see whether it has become tight
 * again.
 * <p>
 * There are three technical restrictions on this implementation, which
 * unfortunately limit its utility somewhat:
 * <ul>
 * <li>the reference must go to an object that implements
 * <code>Standin</code>;</li>
 * <li>the interaction with other garbage collector hooks is weird: the list of
 * reference strength now goes strong &gt; tight &gt; weak &gt; finalizing &gt;
 * loose &gt; phantom, i.e. weak references to an object will break at the same
 * time that loose references tighten, and finalizers also run at this
 * time;</li>
 * <li>loose references require altering the object's standin storage, so only
 * one loose reference can exist to any given object, and the object cannot use
 * custom standin storages while such a reference exists.</li>
 * </ul>
 * <p>
 * If this class is extended to implement <code>InvokeByCode</code>, it
 * integrates this information with the referenced standin. As such, such an
 * extension must use the same value for <code>&lt;T&gt;</code>; it is
 * semantically incorrect to extend this class to implement
 * <code>InvokeByCode&lt;U&gt;</code> for some <code>U</code> other than
 * <code>T</code>.
 *
 * @author Alex Smith
 * @param <T> The class that the referenced standin is standing in for (i.e. the
 * referenced object's class extends <code>Standin&lt;T&gt;</code>).
 */
abstract class LooseTightStandinReference<T> {

    /**
     * A holder for the referenced standin, typically filled only when the
     * reference is tight. This is how we keep the reference alive when the
     * reference is tight.
     * <p>
     * While an object is transitioning from tight to loose, this holder is
     * filled (this doesn't create a "double hold" error condition because the
     * fact it's holding the object alive prevents the reference from
     * tightening; during this period, the loose/tight reference acts like a
     * combination of a strong and a loose reference, which has the same
     * semantics as a tight reference, thus the change effectively happens
     * atomically).
     * <p>
     * While an object is transitioning from loose to tight, the holder is
     * unfilled but treated as filled, so attempts to read the reference block
     * until the transition has finished.
     * <p>
     * Accesses to this field, other than filling it when empty, should be
     * guarded by <code>loosenTightenLock</code>.
     */
    private final BlockingHolder<Standin<T>> tightReference;

    /**
     * A weak reference to the object, if the reference is loose. This field is
     * what determines whether the reference is loose or tight; if it holds a
     * living weak reference, the reference is loose, and if it holds nothing or
     * a broken weak reference, the reference is tight.
     * <p>
     * Access to this field should be guarded by <code>loosenTightenLock</code>.
     */
    private WeakReference<Standin<T>> looseReference = null;

    /**
     * A lock used to prevent races between loosen, tighten, and get operations.
     */
    private final ReadWriteLock loosenTightenLock = new ReentrantReadWriteLock();

    /**
     * Creates a new loose/tight reference to the given object, initially in
     * tight state.
     *
     * @param target The object to reference.
     */
    LooseTightStandinReference(Standin<T> target) {
        this.tightReference = new BlockingHolder<>(target);
    }

    /**
     * Partially initializes the loose/tight reference. The resulting object is
     * not usable until <code>finishInitialization</code> is called.
     * <p>
     * This constructor exists for the benefit of derived classes which need to
     * create cyclic structures in their constructors, allowing a breaking of
     * the "chicken and egg" pattern.
     */
    protected LooseTightStandinReference() {
        this.tightReference = new BlockingHolder<>();
    }

    /**
     * Finishes initializing this partially constructed object. This should be
     * called only as the first method call on this object, and should be called
     * if and only if the no-argument constructor was used; typically, it would
     * be called from a derived class constructor (thus the <code>final</code>).
     *
     * @param target The object to reference.
     */
    protected void finishInitialization(Standin<T> target) {
        this.tightReference.setIf(null, target);
    }

    /**
     * Gets the target of this reference. This can be called whether the
     * reference is loose, tight, or transitioning between the two states.
     *
     * @return The standin referenced by this reference.
     */
    Standin<T> get() {
        try (DeterministicAutocloseable da
                = new AutocloseableLockWrapper(loosenTightenLock.readLock(),
                        "LooseTightStandinReference#get")) {
            return getInner();
        }
    }

    /**
     * Checks whether this reference is currently loose or tight.
     *
     * @return <code>true</code> if the reference is tight
     */
    boolean isTight() {
        try (DeterministicAutocloseable da
                = new AutocloseableLockWrapper(loosenTightenLock.readLock(),
                        "LooseTightStandinReference#get")) {
            return looseReference == null || looseReference.get() == null;
        }
    }

    /**
     * Gets the target of this reference, without locking. Used in cases where
     * the caller already holds an appropriate lock.
     *
     * @return The standin referenced by this reference.
     */
    private Standin<T> getInner() {
        Standin<T> standin = (looseReference == null ? null : looseReference.get());
        if (standin != null) {
            return standin;
        }

        return delayInterruptionsRv(() -> tightReference.blockingGet());
    }

    /**
     * Loosens this reference. If the reference is already loose, this is a
     * no-op. Otherwise, it's only valid if the standin's storage is currently
     * <code>LooseTightStandinReference.TightStorage</code>. As a side effect,
     * the standin's storage object will be changed to a standin storage object
     * that belongs to a different class but has the same user-visible
     * functionality (although because it belongs to a different class, this
     * prevents a double-loosening of the standin storage).
     *
     * @throws IllegalStateException if the standin's storage is not
     * <code>TightStorage</code>, or if its <code>TightStorage</code> refers to
     * some other loose/tight standin reference.
     */
    void loosen() throws IllegalStateException {
        loosenInner(true);
    }

    /**
     * Attempts to loosen this reference. This acts like <code>loosen</code> in
     * those cases where it could succeed, but does nothing rather than throwing
     * an exception if the standin storage is incorrect or the reference is
     * already loose.
     *
     * @return <code>false</code> if the standin's storage is not
     * <code>TightStorage</code> (including the case where the reference is
     * already loose), or if its <code>TightStorage</code> refers to some other
     * loose/tight standin reference.
     */
    boolean maybeLoosen() {
        return loosenInner(false);
    }

    /**
     * Code implementing <code>loosen</code> and <code>maybeLoosen</code>.
     *
     * @param exceptionOnFailure If <code>true</code>, acts like
     * <code>loosen</code>, otherwise like <code>maybeLoosen</code>.
     * @return <code>true</code> on success, <code>false</code> on failure.
     */
    private boolean loosenInner(boolean exceptionOnFailure) {
        while (true) {
            try (DeterministicAutocloseable da
                    = new AutocloseableLockWrapper(loosenTightenLock.writeLock(),
                            "LooseTightStandinReference#loosen")) {
                Standin<T> standin = getInner();
                StandinStorage<T> oldStorage = standin.getStorage(null);

                if (!(oldStorage instanceof TightStorage)) {
                    if (exceptionOnFailure) {
                        throw new IllegalStateException(
                                "Loosening a loose/tight reference with "
                                + "the wrong storage");
                    }
                    return false;
                }
                TightStorage<T> castOldStorage = (TightStorage<T>) oldStorage;
                if (castOldStorage.getRef() != this) {
                    if (exceptionOnFailure) {
                        throw new IllegalStateException(
                                "Loosening a loose/tight reference whose storage "
                                + "references a different loose/tight reference");
                    }
                    return false;
                }

                /* Try to change the object's storage. In race conditions
                   (against other storage sets), we need to go back to the start
                   of the method and try again. */
                LooseStorage<T> newStorage = new LooseStorage<>(this, standin);
                try {
                    standin.safeSetStorage(newStorage, oldStorage,
                            Authorisations.UNRESTRICTED);
                } catch (ConcurrentModificationException ex) {
                    continue;
                }

                /* Now that the storage is changed, and before the storage has
                   a chance to be deallocated via any means, change our own
                   reference from strong to weak. */
                looseReference = new WeakReference<>(standin);
                if (tightReference.setIf(standin, null) != standin) {
                    throw new RuntimeException(
                            "a loose reference was using TightStorage");
                }
                BackgroundGarbageCollection.volatileAccess(standin);
                break;
            }
        }
        return true;
    }

    /**
     * Tightens this reference. Unlike <code>loosen()</code>, this can be called
     * if the reference is already tight, in which case it does nothing.
     */
    void tighten() {
        while (true) {
            try (DeterministicAutocloseable da
                    = new AutocloseableLockWrapper(loosenTightenLock.writeLock(),
                            "LooseTightStandinReference#tighten")) {
                Standin<T> standin = getInner();
                StandinStorage<T> oldStorage = standin.getStorage(null);

                /* If looseReference is null or broken, there's nothing to do;
                   either the reference is tight right now, or it's currently
                   being tightened by finalizer (it can't be being tightened
                   or loosened manually because we're holding the lock). */
                if (looseReference == null) {
                    return;
                }
                if (looseReference.get() == null) {
                    return;
                }

                if (!(oldStorage instanceof LooseStorage)) {
                    throw new IllegalStateException(
                            "Loosening a loose/tight reference with "
                            + "the wrong storage");
                }
                LooseStorage<T> castOldStorage = (LooseStorage<T>) oldStorage;
                if (castOldStorage.getRef() != this) {
                    throw new IllegalStateException(
                            "Loosening a loose/tight reference whose storage "
                            + "references a different loose/tight reference");
                }

                /* Try to change the object's storage. In race conditions
                   (against other storage sets), we need to go back to the start
                   of the method and try again. */
                TightStorage<T> newStorage = new TightStorage<>(this);
                try {
                    standin.safeSetStorage(newStorage, oldStorage,
                            Authorisations.UNRESTRICTED);
                } catch (ConcurrentModificationException ex) {
                    continue;
                }

                /* Now that the storage is changed, and before the storage has
                   a chance to be deallocated via any means, change our own
                   reference from weak to strong. */
                looseReference = null;
                tightReference.hold(standin);
                BackgroundGarbageCollection.volatileAccess(standin);
                break;
            }
        }
    }

    /**
     * Base class for standin storage used by a loose/tight reference. This
     * specifies the functionality that must used while the reference is loose,
     * and may also be used to get identical functionality even while the
     * reference is tight (as the base class for a different storage class).
     * <p>
     * The storage is certainly-local, and uses this
     * <code>LooseTightStandinReference</code> as a dormant forwarding object if
     * it happens to support <code>InvokeByCode</code>, otherwise there is no
     * forwarding object.
     *
     * @param <T> The <code>T</code> of the associated loose/tight standin
     * reference.
     */
    private static abstract class Storage<T> implements StandinStorage<T> {

        /**
         * The reference with which this storage is associated.
         */
        private final WeakReference<LooseTightStandinReference<T>> refRef;

        /**
         * Initializes the fields of the base class.
         *
         * @param ref The reference with which this storage is associated.
         */
        protected Storage(LooseTightStandinReference<T> ref) {
            this.refRef = new WeakReference<>(ref);
        }

        @Override
        public boolean certainlyLocal() {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public InvokeByCode<T> getMethodsForwardedTo(Standin<T> standin) {
            LooseTightStandinReference<T> ref = this.refRef.get();
            if (ref instanceof InvokeByCode) {
                return (InvokeByCode<T>) ref;
            }
            return null;
        }

        /**
         * Returns the loose/tight standin reference associated with this
         * storage. Might return <code>null</code>, if it's been deallocated;
         * the storage does not hold the reference alive.
         *
         * @return The reference with which the standin storage is associated.
         */
        LooseTightStandinReference<T> getRef() {
            return refRef.get();
        }
    }

    /**
     * Standin storage usable for the referenced object while this reference is
     * tight. This has the same functionality as <code>LooseStorage</code> (i.e.
     * that defined by <code>Storage</code>), but no garbage-collection-related
     * behaviour. In order to set the reference to loose, the standin must
     * already be using <code>TightStorage</code>, which will be set to
     * <code>LooseStorage</code> as part of the loosening behaviour; likewise,
     * when a loose reference becomes tight, the storage will become
     * <code>TightStorage</code>.
     *
     * @param <T> The class that the referenced standin is standing in for.
     */
    static class TightStorage<T> extends Storage<T> {

        /**
         * Creates a standin storage with the same functionality as
         * <code>LooseStorage</code> but no garbage-collection-related
         * properties.
         *
         * @param ref The loose/tight standin reference with which this storage
         * will be associated.
         */
        TightStorage(LooseTightStandinReference<T> ref) {
            super(ref);
        }
    }

    /**
     * Standin storage used for the referenced object while this reference is
     * loose. Note that the reference itself is referenced only weakly from the
     * storage; if the reference dies first, this class will act equivalently to
     * <code>TrivialStandinStorage</code>. (This is the same behaviour as that
     * of <code>TightStorage</code>.)
     *
     * @param <T> The class that the referenced standin is standing in for.
     */
    private static class LooseStorage<T> extends Storage<T> {

        /**
         * The standin with which this storage is associated. A
         * <code>LooseStorage</code> object relies on a reference cycle between
         * it and the standin to ensure that they are deallocated at the same
         * time.
         */
        private final Standin<T> standin;

        /**
         * Creates a new standin storage for a standin referenced by a loose
         * reference. Note that each <code>LooseStorage</code> object can be
         * used only with one standin.
         *
         * @param ref The loose reference itself (i.e. a loose/tight reference
         * in loose mode).
         * @param standin The standin with which this storage will be used.
         */
        LooseStorage(LooseTightStandinReference<T> ref, Standin<T> standin) {
            super(ref);
            this.standin = standin;
        }

        /**
         * Tightens the loose/tight reference, and reconnects broken internal
         * references to the referenced standin. In particular, we need to set
         * <code>tightReference</code> to the referenced object, despite that
         * object being less than weakly reachable, which is the reason that a
         * finalizer is required.
         * <p>
         * This method does nothing unless the storage is actually in use; this
         * allows a detached or unused <code>LooseStorage</code> object to be
         * safely deallocated.
         *
         * @throws Throwable If <code>Object#finalize</code> throws an
         * exception, this method throws the same exception
         */
        @Override
        protected void finalize() throws Throwable {
            super.finalize();

            LooseTightStandinReference<T> ref = getRef();
            TightStorage<T> newStorage = new TightStorage<>(ref);

            try {
                /* Were we in use? If so, replace ourselves with the
                   TightStorage object we just created. */
                standin.safeSetStorage(newStorage, this,
                        Authorisations.UNRESTRICTED);
            } catch (ConcurrentModificationException ex) {
                /* We're not in use; do nothing. */
                return;
            }

            /* tightReference was an empty holder, but should be full, so fill
               it now. This should only be called once per loosening, but we
               ensured that with the (atomic) safeSetStorage call above. */
            ref.tightReference.hold(standin);
        }
    }
}
