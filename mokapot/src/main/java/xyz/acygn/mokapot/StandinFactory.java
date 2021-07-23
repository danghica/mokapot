package xyz.acygn.mokapot;

import java.io.IOException;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * A class that creates standins for objects of a given class. There are four
 * possible ways to create a standin (from a description, from an existing
 * object, from a location manager, or from scratch); a standin factory must
 * implement at least one of the first three, but whether or not they can all be
 * provided depends on what functionalities the standin provides. (Creating a
 * standin entirely from scratch is not something that's ever done by this
 * package; it's a useful thing for a user program to be able to do but this
 * will be handled via an annotation processor, not via methods at runtime, due
 * to the difficulty in selecting an appropriate constructor.)
 *
 * @author Alex Smith
 * @param <T> The class for which the standins are created.
 */
interface StandinFactory<T> {

    /**
     * Creates a new standin from a description of the object it stands in for.
     * The standin will initially be stored-in-self; the description will be
     * used to produce its referent. Any encoding of <code>null</code>, or of
     * the class that the object belongs to, must have already been removed from
     * the description (e.g. by reading it).
     *
     * @param description The description of the new standin's referent.
     * @return A stored-in-self standin whose referent matches the given
     * description (or <code>null</code> if this is a description of
     * <code>null</code>).
     * @throws IOException If something goes wrong reading the description
     * @throws UnsupportedOperationException If this sort of standin cannot
     * create objects from a description
     */
    Standin<T> newFromDescription(ReadableDescription description)
            throws IOException, UnsupportedOperationException;

    /**
     * Creates a new standin whose referent is an existing object. The new
     * standin will have stored-in-self status (because its data is stored in
     * its referent, i.e. the existing object "becomes part of" the standin).
     *
     * @param t The object to wrap.
     * @return A standin whose referent is the given object.
     * @throws UnsupportedOperationException If this sort of standin cannot use
     * an external referent
     */
    Standin<T> wrapObject(T t) throws UnsupportedOperationException;

    /**
     * Creates a new standin whose referent is an existing object. The new
     * standin will have stored-in-self status (because its data is stored in
     * its referent, i.e. the existing object "becomes part of" the standin).
     * <p>
     * This version of the method uses no check on the type of the argument,
     * rather than a static check, to get around issues with Java's type system.
     * (If an object of the wrong type is provided, the code may appear to work
     * but break at some random later point, so the caller should verify this).
     * The default implementation simply calls <code>wrapObject</code>.
     *
     * @param t The object to wrap.
     * @return A standin whose referent is the given object.
     * @throws UnsupportedOperationException If this sort of standin cannot use
     * an external referent
     */
    @SuppressWarnings("unchecked")
    default Standin<T> castAndWrapObject(Object t)
            throws UnsupportedOperationException {
        return wrapObject((T) t);
    }

    /**
     * Creates a new standin which forwards all method calls to a location
     * manager. The standin will have a dropped referent (i.e. <i>not</i>
     * stored-in-self status).
     *
     * @param lm The location mananger which handles method calls on the
     * standin.
     * @return A standin that forwards method calls to the given object.
     * @throws UnsupportedOperationException If this sort of standin cannot use
     * arbitrary standin storages.
     */
    Standin<T> standinFromLocationManager(LocationManager<T> lm)
            throws UnsupportedOperationException;

    /**
     * The type of factory method for creating standin factories. Each (standin
     * technique, referent class) pair typically needs its own standin factory.
     * Each standin technique thus has a standin factory factory, used to create
     * the various standin factories for standins that use that technique.
     * <p>
     * I would normally just use a plain <code>Function</code> here, rather than
     * the excessive levels of indirection, but there is unfortunately a need
     * here to represent the relationship between the types of the function's
     * input and output values and so I needed to put an extra class here in
     * order to put the quantifier in the right place. Sorry.
     */
    @FunctionalInterface
    public interface Factory {

        /**
         * Creates a new standin factory, that creates standins with the given
         * referent class. Note that this method is allowed and expected to fail
         * in cases where, e.g., the class that the standin would need to have
         * does not already exist and there is no way to generate it; the
         * failure is always indicated by exception, however, so that relevant
         * information about why is available.
         *
         * @param <T> The referent class.
         * @param referentClass <code>T.class</code>, given explicitly due to
         * Java's type erasure rules.
         * @return A standin factory that creates <code>Standin&lt;T&gt;</code>
         * objects.
         * @throws xyz.acygn.mokapot.StandinFactory.CannotConstructException If
         * something went wrong creating the factory
         */
        <T> StandinFactory<T> metafactory(Class<T> referentClass)
                throws CannotConstructException;
    }

    /**
     * An exception thrown when a standin metafactory cannot construct the
     * factory.
     */
    public static class CannotConstructException extends Exception {

        /**
         * Serialisation versioning information. Was originally generated
         * randomly, and should be changed to a new random number whenever the
         * class is changed in an incompatible way.
         */
        private static final long serialVersionUID = 0xd7468c3b56f98af4L;

        /**
         * Creates a new exception explaining why a standin could not be
         * constructed.
         *
         * @param ex The reason the standin could not be constructed.
         */
        public CannotConstructException(Throwable ex) {
            super(ex);
        }
    }
}
