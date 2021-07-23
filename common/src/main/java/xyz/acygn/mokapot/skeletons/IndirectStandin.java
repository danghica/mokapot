package xyz.acygn.mokapot.skeletons;

import java.io.IOException;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

// TODO: This documentation is a) more internals-dependent than it shuuld be;
// b) wrong.
/**
 * Abstract implementation of composition-based standins. In other words, a
 * standin object that's separate from the object holding its data. These
 * objects cannot be used to proxy calls to methods of a class (as they have the
 * wrong type), but can be used as wrappers to allow methods of
 * <code>Standin</code> to be called on arbitrary objects, or to proxy calls to
 * interface methods.
 * <p>
 * As long as the standin remains in stored-in-self status, the standin can be
 * converted to a reference simply by taking its referent (it's a short
 * reference, not a long reference, but it's still a valid reference). This
 * means that composition-based standins, whilst less general than
 * inheritance-based standins, can nonetheless be used as the standin via which
 * a location manager records the location of a local object. The only operation
 * a location manager might want to do, but cannot do with a composition-based
 * standin, is to convert the standin away from stored-in-self status (i.e.
 * migrate the object).
 * <p>
 * "Generalising" a composition-based standin to a form of standin that allows
 * all operations can be done indirectly via describing the standin, and taking
 * the location manager offline (allowing a new inheritance-based standin to be
 * created), but only if the referent is locally dead. This is not a condition
 * that's currently detected by the standin code (note that it's easy to detect
 * whether the <i>standin</i> is locally dead, but that's the opposite of what
 * you'd want). Thus, at present, an object that's managed via a
 * composition-based standin cannot be migrated.
 * <p>
 * In most cases, a composition-based standin will be of a specialised class
 * that was generated (ahead of time or just in time) to have the fastest
 * possible implementation for a given actual class of referent. In cases where
 * (for whatever reason) code can't be generated, it's possible to use
 * <code>ReflectiveStandin</code>, but note that this will likely be
 * considerably slower.
 *
 * @author Alex Smith
 * @param <T> The class this standin is standing in for, i.e. the actual class
 * of the standin's referent.
 */
public abstract class IndirectStandin<T> implements SeizeableStandin<T> {

    /**
     * This standin's storage method. Determines how to access the data within
     * the standin (e.g. via the location manager, or via the referent).
     * <p>
     * This needs to be very frequently accessed by the resulting standins, and
     * without authorisation. As such, this is one of the few cases in which a
     * field is not <code>private</code>.
     */
    @SuppressWarnings("ProtectedField")
    protected StandinStorage<T> storage;

    /**
     * This standin's referent. Can sometimes be <code>null</code>, but only if
     * the standin is not currently being used to hold data (i.e. it's
     * forwarding messages out via a location manager, and that location manager
     * isn't forwarding them back to this standin).
     * <p>
     * This field need to be very frequently accessed by the resulting standins,
     * and without authorisation. As such, this is one of the few cases in which
     * a field is not <code>private</code>.
     */
    @SuppressWarnings("ProtectedField")
    protected T referent;

    /**
     * Initializes this standin's referent. Note that this object might not be
     * continuously referenced by the standin; the standin will drop its
     * reference to the referent if taken out of stored-in-self status.
     *
     * @param referent The referent which this standin forwards methods to when
     * in stored-in-self status.
     * @param auth An authorisation to create the standin. This is included as a
     * precaution in case there's some way to get access to secure methods
     * within the package via a crafted standin implementation that disobeys the
     * invariants.
     */
    protected IndirectStandin(T referent, Authorisation auth) {
        auth.verify();

        this.referent = referent;

        /* Sanity check */
        if (referent instanceof Standin) {
            throw new IllegalArgumentException(
                    "standin referents cannot be standins themselves");
        }

        storage = new TrivialStandinStorage<>();
    }

    /**
     * Creates a new standin, likewise with a new referent, via reproducing a
     * description. This can, if necessary, be used simply as a method of
     * converting a description into a referent (via creating the standin, then
     * taking the referent and dropping the standin).
     *
     * @param referentClass <code>T.class</code>, given explicitly due to Java's
     * type erasure rules.
     * @param description The description that's reproduced to create the
     * referent.
     * @param auth The authorisation that the caller has to call this
     * constructor. It must have the capabilities to construct objects without
     * using their constructor and to write to unwritable fields.
     * @throws IOException If something goes wrong reading from the description
     */
    protected IndirectStandin(Class<T> referentClass,
            ReadableDescription description, Authorisation auth)
            throws IOException {
        auth.verify();
        this.referent = constructNewReferent(referentClass, auth);
        storage = new TrivialStandinStorage<>();
        this.replaceWithReproduction(referentClass, description, auth);
    }

    /**
     * Creates a new standin with a dropped referent. Note that as a dropped
     * referent cannot have a trivial standin storage, the standin storage to
     * use cannot fall back to the default and thus must be provided by the
     * caller.
     *
     * @param storage The standin storage that this standin will initially use.
     * @param auth The authorisation that the caller has to call this
     * constructor; cannot be <code>null</code>.
     */
    protected IndirectStandin(ForwardingStandinStorage<T> storage,
            Authorisation auth) {
        auth.verify();
        this.referent = null;
        this.storage = storage;
    }

    @Override
    public StandinStorage<T> getStorage(Authorisation auth) {
        return storage;
    }

    @Override
    public void setStorage(StandinStorage<T> storage, Authorisation auth) {
        auth.verify();
        /* Note a special case here: if the storage is null (meaning that we've
           been initialised without being constructed properly), we allow the
           storage to be set even with no referent. That helps when constructing
           standins without using their constructors. */
        if (referent == null && this.storage != null) {
            throw new IllegalStateException(
                    "setStorage with dropped referent (old storage: "
                    + this.storage + ", new storage: " + storage + ")");
        }
        this.storage = storage;
    }

    @Override
    public T getReferent(Authorisation auth) {
        auth.verify();
        return referent;
    }

    /**
     * Drops the referent, causing this standin to no longer hold its own data.
     * Naturally, this should only be called if the standin's own data is no
     * longer in use; you'll need to change the storage method first. The
     * storage method can't be changed back until the standin has been given a
     * new referent using <code>replaceWithReproduction</code>.
     *
     * @param auth The authorisation that the caller has to call this method;
     * cannot be <code>null</code>.
     */
    @Override
    public void dropResources(Authorisation auth) {
        if (referent == null) {
            throw new IllegalStateException("drop twice without undrop");
        }
        SeizeableStandin.super.dropResources(auth);
        referent = null;
    }

    /**
     * Undrops the referent, allowing this standin to have a new referent
     * constructed via reproducing a description. This is a necessary precursor
     * to being able to change the standin's referent to stored-in-self status,
     * if the referent was previously dropped.
     *
     * @param auth The authorisation that the caller has to call this method;
     * cannot be <code>null</code>.
     */
    @Override
    public void undropResources(Authorisation auth) {
        if (referent == null) {
            referent = constructNewReferent(getReferentClass(null), auth);
        } else {
            throw new IllegalStateException("drop/undrop not correctly paired");
        }
    }

    /**
     * Causes this standin to start using an existing object as its referent.
     * Can only be called if the standin currently has no referent. May be
     * called while the standin's storage is uninitialised.
     * <p>
     * This method is intended for use when constructing indirect standins
     * without use of their constructor.
     *
     * @param referent The object to use as the referent.
     * @param auth The authorisation that the caller has to call this method;
     * cannot be <code>null</code>.
     */
    @Override
    public void seizeReferent(T referent, Authorisation auth) {
        auth.verify();
        if (this.referent != null) {
            throw new IllegalStateException(
                    "seizing a referent while we already have one");
        }
        this.referent = referent;
    }

    /**
     * Creates a new uninitialised referent directly, without using its
     * constructor. This is common code extracted from
     * <code>ensureReferent</code> and from the description-based constructor.
     *
     * @param <T> The actual class of the new referent.
     * @param referentClass <code>T.class</code>, given explicitly due to Java's
     * type erasure rules.
     * @param auth The capability that makes it possible to construct the
     * referent in question. Also serves to prevent naming clashes between this
     * method and static methods in classes that extend this class.
     * @return The newly constructed referent.
     */
    private static <T> T constructNewReferent(Class<T> referentClass,
            Authorisation auth) {
        return auth.classInstantiator.instantiate(referentClass);
    }
}
