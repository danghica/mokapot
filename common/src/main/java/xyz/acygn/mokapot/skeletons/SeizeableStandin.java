package xyz.acygn.mokapot.skeletons;

/**
 * A type of standin capable of gaining a referent at a time after construction.
 * Typically used for generated classes, which can't have their constructors
 * called in the normal manner, and thus use a separate genesis and
 * initialisation phase.
 * <p>
 * This exists as a separate interface to avoid the need for all indirect
 * standins to extend <code>IndirectStandin</code>. (Although this would be
 * convenient, occasionally some unrelated class is needed as the superclass of
 * the standin so that it can be stored in variables of the appropriate type.)
 * <p>
 * This interface is public for technical reasons; it potentially needs to be
 * implemented on objects in arbitrary packages. However, it should not be used
 * by an end user.
 *
 * @author Alex Smith
 * @param <T> The type which this standin stands in for.
 */
public interface SeizeableStandin<T> extends Standin<T> {

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
    void seizeReferent(T referent, Authorisation auth);
}
