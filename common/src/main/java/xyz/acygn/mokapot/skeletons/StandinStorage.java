package xyz.acygn.mokapot.skeletons;

/**
 * The method via which a standin currently accesses its storage. A standin can
 * either store its data directly on its referent ("stored in self" status), or
 * store its data indirectly via a location manager.
 * <p>
 * A standin storage object is immutable and intended to be stored in a mutable
 * field of a standin. The class also contains a number of methods whose purpose
 * is to centralise implementations of common operations performed on standins,
 * both to reduce code duplication (in generated code, but still), and to allow
 * classes from outside the package to indirectly implement methods that refer
 * to package-private classes like <code>LocationManager</code>.
 * <p>
 * A standin's storage method is used to determine whether it's currently a
 * short or long reference. The class <code>ForwardingStandinStorage</code> is
 * used for the storage methods for which the corresponding standin is a long
 * reference (this rule makes it possible to check the length of the reference
 * using a single <code>instanceof</code> rather than needing a method call).
 *
 * @author Alex Smith
 * @param <T> The referent of the corresponding standin.
 * @see Standin
 */
public interface StandinStorage<T> {

    /**
     * Returns whether the fields of the standin are known to be stored locally.
     * This is always the case if the standin is currently a short reference,
     * and could potentially be the case even if the object is currently a long
     * reference.
     * <p>
     * Note that as immutable fields of the object cannot be changed, they can
     * safely be stored in more than one place. This method returns
     * <code>true</code> if a local copy of every field is available, even if
     * there may also be copies of the fields in question elsewhere.
     * <p>
     * It is legal for this method to return <code>false</code> even if the
     * fields of the standin happen to be stored locally; this can occur in
     * cases where the fields are being accessed indirectly and their current
     * location is not known.
     *
     * @return <code>true</code> only if the object's mutable fields are stored
     * on the current virtual machine.
     */
    public abstract boolean certainlyLocal();

    /**
     * Returns the object which handles methods being forwarded from the
     * standin. If a method is called on the standin (without being called in a
     * way that "bypasses" forwarding, such as via
     * <code>InvokeByCode#invoke</code>), and forwarding is enabled (i.e. the
     * standin storage implementation is <code>ForwardingStandinStorage</code>),
     * then the method will be called indirectly via
     * <code>ForwardingStandinStorage#forwardMethodCall</code>, which in turn
     * forwards the object to an <code>InvokeByCode</code> object. This method
     * makes it possible to get at the underlying forwarding object.
     * <p>
     * Standin storages which do not inherit from
     * <code>ForwardingStandinStorage</code> do not forward method invocations.
     * However, it is possible that the storage will need to remember a
     * previously used forwarding object so that it can be used again in the
     * future, or that such an object will be created in advance. These standin
     * storages will also implement this method so that the "dormant" forwarding
     * object can be accessed.
     * <p>
     * In a previous version of this code, a return value of <code>null</code>
     * was an explicit guarantee that no forwarding objects were currently
     * associated with the standin (and any that had been in the past had been
     * deallocated). The current guarantee is a bit subtler: it guarantees that
     * no forwarding objects are currently in use with this standin object (if
     * any were in use, they should have set the storage to indicate that), but
     * makes no guarantee about any forwarding objects that might currently be
     * in use with this standin object's <i>referent</i>. The distinction only
     * matters if two standins to the same object exist at the same time (and if
     * the standin is not its own referent!).
     *
     * @param standin The standin that this standin storage method is managing
     * storage for; given explicitly to avoid backreferences in the object graph
     * in cases where they're unnecessary.
     * @return The forwarding object used by this standin (whether or not it is
     * currently being used for anything), or <code>null</code> if it does not
     * have one.
     */
    public abstract InvokeByCode<T> getMethodsForwardedTo(Standin<T> standin);
}
