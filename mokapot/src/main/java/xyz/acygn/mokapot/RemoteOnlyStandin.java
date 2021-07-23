package xyz.acygn.mokapot;

import xyz.acygn.mokapot.markers.DistributedError;
import xyz.acygn.mokapot.skeletons.Authorisation;
import xyz.acygn.mokapot.skeletons.ForwardingStandinStorage;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.skeletons.StandinStorage;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * Cut-down standin that can be sent over the network, but is not usable on the
 * local system.
 * <p>
 * When marshalling a local object to a remote system, this is done by first
 * creating a location manager for that object, and then sending a
 * <code>ReferenceValue</code> that talks about the location manager. However,
 * if the information we already have is the location manager itself (rather
 * than the object), converting the location manager to an object and back can
 * be inefficient.
 * <p>
 * In this situation, we instead create a very bare-bones standin that doesn't
 * implement most of a standin's features (e.g. it can't have methods invoked
 * via it, can't have its storage changed, etc.) but is just powerful enough to
 * have its reference value returned. All we need to be able to do is to track a
 * location manager; <code>ForwardingStandinStorage</code> is used for this
 * purpose. In fact, that class already has all the functionality we need, so we
 * simply extend it so that it implements the <code>Standin</code> interface.
 *
 * @author Alex Smith
 * @param <T> The location manager's referent.
 */
class RemoteOnlyStandin<T> extends ForwardingStandinStorage<T> implements Standin<T> {

    /**
     * Creates a "standin" that's actually just a thin wrapper around a location
     * manager.
     *
     * @param manager The location manager of the referenced object.
     */
    RemoteOnlyStandin(LocationManager<T> manager) {
        super(manager);
    }

    /**
     * Produces a string representation of this object. It simply refers to the
     * location manager's string representation.
     *
     * @return A string representation of this object.
     */
    @Override
    public String toString() {
        return "remote-only standin via " + getMethodsForwardedTo(this);
    }

    @Override
    public StandinStorage<T> getStorage(Authorisation auth) {
        return this;
    }

    @Override
    public void setStorage(StandinStorage<T> storage, Authorisation auth)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "A remote-only standin has fixed storage");
    }

    @Override
    public Object invoke(long methodCode, Object[] methodArguments,
            Authorisation auth) throws Throwable {
        throw new DistributedError(new UnsupportedOperationException(),
                "A remote-only standin is never stored-in-self, "
                + "so manipulating its referent does not make sense");
    }

    @Override
    public T getReferent(Authorisation auth) {
        /* We never have a referent. */
        return null;
    }

    @Override
    public Class<T> getReferentClass(Namespacer dummy) {
        return getMethodsForwardedTo(this).getObjectClass();
    }

    /**
     * A factory for creating remote-only standins pointing to a given type of
     * location manager.
     *
     * @param <T> The type of location manager target that's used.
     */
    public static class Factory<T> implements StandinFactory<T> {

        @Override
        public Standin<T> newFromDescription(ReadableDescription description)
                throws UnsupportedOperationException {
            throw new UnsupportedOperationException(
                    "A remote-only standin cannot be stored-in-self, thus "
                    + "its referent cannot be created from a description");
        }

        @Override
        public Standin<T> wrapObject(T t) throws UnsupportedOperationException {
            throw new UnsupportedOperationException(
                    "A remote-only standin cannot be stored-in-self, thus "
                    + "it cannot use an existing object as a referent");
        }

        @Override
        public Standin<T> standinFromLocationManager(LocationManager<T> lm) {
            return new RemoteOnlyStandin<>(lm);
        }
    }
}
