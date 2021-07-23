package xyz.acygn.mokapot.skeletons;

/**
 * Storage for a standin that causes it to act like a regular, local Java
 * object. This is used for standins that have never had a remote reference to
 * them, and aims for maximum efficiency of direct accesses to the standin.
 *
 * @author Alex Smith
 * @param <T> The class that the standin is standing in for.
 */
public class TrivialStandinStorage<T> implements StandinStorage<T> {

    /**
     * Always returns true. This storage method is only used with standins which
     * store their data directly, and direct is a special case of local.
     *
     * @return <code>true</code>.
     */
    @Override
    public boolean certainlyLocal() {
        return true;
    }

    /**
     * Always returns <code>null</code>. <code>TrivialStandinStorage</code>
     * should not be used with a standin while there is a need to associate a
     * forwarding object with it. (You can use
     * <code>ForwardingStandinStorage</code> to place a forwarding object
     * actively in use and forwarding for a standin. If you need to associate
     * extra data with an existing standin, such as a dormant forwarding object,
     * but not actively use the forwarding object in question, create a new
     * <code>StandinStorage</code> subclass to remember it.)
     *
     * @return <code>null</code>.
     */
    @Override
    public InvokeByCode<T> getMethodsForwardedTo(Standin<T> standin) {
        return null;
    }
}
