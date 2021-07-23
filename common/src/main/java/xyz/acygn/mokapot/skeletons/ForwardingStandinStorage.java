package xyz.acygn.mokapot.skeletons;

import static xyz.acygn.mokapot.util.StringifyUtils.stringify;

/**
 * A standin storage method that forwards method invocations to another object
 * (perhaps on another virtual machine). In other words, this causes a standin
 * to work like a long reference. The standin's "local referent" is unused in
 * this situation.
 * <p>
 * Note that unlike most standin storage methods (which are used internally
 * only), this class is public and its name is part of the API for a standin;
 * standins check to see if their storage is an instance of this class in order
 * to determine whether to forward methods.
 * <p>
 * Because standins may need to implement their own finalization behaviour,
 * users of forwarding standin storages may assume that they do not have a
 * finalizer (and classes extending this class should not override
 * <code>Object#finalize</code> for this reason). It would be possible to
 * enforce this by adding a <code>final</code>, no-op, finalizer, but doing so
 * would cause efficiency issues in compilers that noticed that there was a
 * custom finalizer but not that it had no effect, and so at the moment there is
 * no automated enforcement of this.
 *
 * @author Alex Smith
 * @param <T> The type of the referent of the long reference.
 */
public class ForwardingStandinStorage<T> implements StandinStorage<T> {

    /**
     * The object via which method calls on this standin will be forwarded. This
     * field exists both to allow for the actual forwarding, and to ensure that
     * the standin will hold the forwarding object alive (it's possible that
     * nothing else will be holding the forwarding object alive).
     * <p>
     * It is possible that the forwarding object associated with a given standin
     * referent will be stored nowhere else but in this field; it's an
     * appropriate place for "attaching" information to a standin.
     */
    private final InvokeByCode<T> forwardingObject;

    /**
     * Creates a standin storage method that forwards method invocations to a
     * given forwarding object.
     *
     * @param forwardingObject The forwarding object to use for the standin.
     */
    public ForwardingStandinStorage(InvokeByCode<T> forwardingObject) {
        this.forwardingObject = forwardingObject;
    }

    @Override
    public boolean certainlyLocal() {
        return false;
    }

    @Override
    public InvokeByCode<T> getMethodsForwardedTo(Standin<T> standin) {
        return forwardingObject;
    }

    /**
     * Forwards a method call to the object that's storing the data for the
     * standin using this storage. Note that this method must be
     * <code>public</code> for technical reasons. However, because the method
     * code could conceptually refer to a non-public method, the forwarding
     * object should check to make sure that the caller actually has permissions
     * to call the method in question; that cannot be enforced by
     * <code>ForwardingStandinStorage</code> itself.
     *
     * @param methodCode The method code of the method to run.
     * @param methodArguments The arguments on which to run the method.
     * @return The return value of the method.
     * @throws Throwable If the forwarded method throws an exception, this
     * method throws the same exception
     */
    public Object forwardMethodCall(long methodCode, Object[] methodArguments)
            throws Throwable {
        return forwardingObject.invoke(methodCode, methodArguments);
    }

    /**
     * Produces a string representation of this standin storage. This will be
     * based on that of the forwarding object.
     *
     * @return A string representation of this object.
     */
    @Override
    public String toString() {
        return "ForwardingStandinStorage{"
                + stringify(forwardingObject) + "}";
    }
}
