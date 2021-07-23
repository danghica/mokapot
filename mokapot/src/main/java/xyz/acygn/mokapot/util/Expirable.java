package xyz.acygn.mokapot.util;

import java.util.function.Supplier;

/**
 * An interface describing objects that can be atomically "effectively
 * deallocated" even while there are still references to them. "Expiring" an
 * object allows it to do cleanup, and the object will thereafter be treated as
 * nonexistent; all method calls on it (including <code>expire()</code> itself)
 * should respond by throwing <code>ExpiredException</code>.
 * <p>
 * Once the object has expired, it's not supposed to be used any more; any
 * methods on the object should return exceptions or failure codes. The basic
 * idea here is that it's possible to store an object in a map with weak values,
 * and treat a missing key, a key with a deallocated value, and a key with an
 * expired value all identically; this means that the object can effectively be
 * removed from the map via expiring it, without any risk of race conditions
 * between removing an expired entry from the map and adding a new entry to
 * replace it. (A naive algorithm which simply removes the object from the map
 * would risk accidentally removing the newly added object instead.)
 *
 * @author Alex Smith
 * @see KeepalivePool
 * @see ExpirableMap
 */
public interface Expirable {

    /**
     * Causes this object to perform any cleanup required, then permanently mark
     * itself as expired. The object is thereafter useless, serving only to
     * inform anyone who tries to use a reference to the existing object that
     * they need to use a different object instead.
     * <p>
     * Like all methods on an expirable object, this method has no effect on an
     * expired object, and simply throws an exception. Similarly, if two or more
     * attempts are made to call this method concurrently on a previously
     * non-expired object, exactly one must succeed, with the rest throwing the
     * exception. This can be most easily implemented by making the
     * implementation of this method <code>synchronized</code>, but there may be
     * more efficient ways to implement it special cases.
     * <p>
     * Note that it's possible that other methods will start considering the
     * object expired before this method returns; in concurrent cases, what's
     * important is that there's a point in time, during the execution of this
     * message, past which the object counts as expired, and before which the
     * object counts as not expired.
     *
     * @throws ExpiredException If this is not the first call to this method (if
     * it's called multiple times concurrently, one such call must be chosen to
     * be the "first call")
     */
    public void expire() throws ExpiredException;

    /**
     * Attempts to run the given method on the given expirable object, running
     * alternative code if the object is null or expired.
     *
     * @param <A> The class of the object.
     * @param <R> The return value of the method.
     * @param on The object to run on.
     * @param method The method to run.
     * @param or The code to run instead, if the object is null or expired.
     * @return The return value of the method.
     */
    public static <A extends Expirable, R> R runOr(
            A on, ExpirableMethod<A, R> method, Supplier<R> or) {
        if (on == null) {
            return or.get();
        }
        try {
            return method.invoke(on);
        } catch (ExpiredException ex) {
            return or.get();
        }
    }

    /**
     * Attempts to run the given method on the given expirable object, running
     * alternative code if the object is null or expired.
     *
     * @param <A> The class of the object.
     * @param on The object to run on.
     * @param method The method to run.
     * @param or The code to run instead, if the object is null or expired.
     */
    public static <A extends Expirable> void runOr(
            A on, ExpirableVoidMethod<A> method, Runnable or) {
        if (on == null) {
            or.run();
        } else {
            try {
                method.invoke(on);
            } catch (ExpiredException ex) {
                or.run();
            }
        }
    }

    /**
     * An interface representing a method that can be called on an Expirable
     * object and that has no return value.
     *
     * @param <A> The class of object that the method is called on.
     */
    @FunctionalInterface
    public interface ExpirableVoidMethod<A extends Expirable> {

        /**
         * Runs this method on a given object.
         *
         * @param on The object to run this method on.
         * @throws ExpiredException If the object has expired
         */
        void invoke(A on) throws ExpiredException;
    }

    /**
     * An interface representing a method that can be called on an Expirable
     * object and that has a return value.
     *
     * @param <A> The class of object that the method is called on.
     * @param <R> The method's return value.
     */
    @FunctionalInterface
    public interface ExpirableMethod<A extends Expirable, R> {

        /**
         * Runs this method on a given object.
         *
         * @param on The object to run this method on.
         * @return The return value of the method.
         * @throws ExpiredException If the object has expired
         */
        R invoke(A on) throws ExpiredException;
    }

    /**
     * Exception thrown when an attempt is made to call a method on an expired
     * object.
     */
    public static class ExpiredException extends Exception {

        /**
         * A single, repeatedly used <code>ExpiredException</code>.
         *
         * Because ExpiredExceptions' stack traces are rarely relevant (it's a
         * checked exception that's supposed to always be caught), it's fastest
         * to construct the exception once and then throw the same exception
         * repeatedly.
         */
        public static final ExpiredException SINGLETON
                = new ExpiredException();

        /**
         * Explicit serialisation version, as is required for a serialisable
         * class to be compatible between machines. The number was originally
         * generated randomly, and should be changed whenever the class's fields
         * are changed in an incompatible way.
         *
         * @see java.io.Serializable
         */
        private static final long serialVersionUID = 0xcd8f6d6131518c22L;
    }
}
