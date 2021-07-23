package xyz.acygn.mokapot.skeletons;

/**
 * An interface defining the API for invoking methods on objects via method
 * code. This is used in cases where standins need to forward method calls to
 * another object.
 *
 * Each instance of this interface is responsible for keeping track itself of
 * the object on which the methods will be invoked, i.e. classes implementing
 * this interface will also have a reference to the object in question. The
 * object need not necessarily be local, nor even need to continuously exist (in
 * some cases, it may be loaded/created lazily when the first method call on it
 * is made). However, the instance does need to know what class it has (in an
 * invariant way, i.e. merely knowing a superclass is not enough), because
 * method codes are defined relative to classes rather than being global.
 *
 * @param <T> The actual class of the object on which the methods are being
 * invoked.
 * @author Alex Smith
 */
public interface InvokeByCode<T> {

    /**
     * Runs the given method on the appropriate object. (Classes which implement
     * this method specify for themselves what the appropriate object is.)
     * <p>
     * If the object already exists on the local virtual machine, the method
     * will be called on the object with no further indirection. Otherwise, the
     * method may need to be run via some indirect method (e.g. first loading
     * the object from a serialisation, or contacting the actual host of the
     * object over a network).
     * <p>
     * Note that because this method is public and because
     * <code>Standin#getStorage</code> is public (and not authenticated), but
     * <code>methodCode</code> could reasonably refer to a non-public method,
     * it's possible that there will be attempts to call this method
     * maliciously. In insecure environments, the implementation of this method
     * should verify that the caller has appropriate permissions to call the
     * method in question on the object in question.
     *
     * @param methodCode The method code of the method to run.
     * @param methodParams The parameters of the method to run.
     * @return The return value of the invoked method.
     * @throws Throwable If invoking the method on the object throws an
     * exception, this method throws the same exception.
     */
    Object invoke(long methodCode, Object[] methodParams) throws Throwable;

    /**
     * Returns the actual class of the object on which <code>invoke</code> will
     * invoke methods. This must be constant and known in advance, even if the
     * object itself does not exist or is not locally available.
     *
     * @return <code>T.class</code>.
     */
    Class<T> getObjectClass();
}
