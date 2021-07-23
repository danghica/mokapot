package xyz.acygn.mokapot.skeletons;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.function.Consumer;
import xyz.acygn.mokapot.markers.DistributedError;
import xyz.acygn.mokapot.wireformat.DescriptionOutput;
import xyz.acygn.mokapot.wireformat.ObjectDescription;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * A standin is a class that's used in place of another class, to transparently
 * add functionality. In the case of this library, we're adding the following
 * functionalities: the ability for the class's fields to be stored remotely on
 * another machine (implemented via running methods remotely whenever the
 * storage method extends <code>ForwardingStandinStorage</code>); the ability to
 * serialise the object into a "description" of that object (which holds the
 * values of each copiable field in serialised format, plus references to the
 * noncopiable fields); the ability to conditionally attach a finalizer that
 * saves the object's description in its location manager once it becomes
 * locally dead; and the ability to invoke methods indirectly via a "method
 * code" number. (Many of these abilities are implemented indirectly via the
 * <code>StandinStorage</code> class.)
 * <p>
 * At present, there are two main ways that this interface can be implemented;
 * by composition, or by inheritance. The inheritance-based method is more
 * efficient, and used in cases when we're creating a standin directly; the
 * standin class is created by code generation, extending an existing class and
 * implementing this interface. In cases where there's a need to apply the
 * operations normally implemented by a standin to an object that already
 * exists, a composition-based method can be used instead, in which a temporary
 * <code>IndirectStandin</code> object is created that applies its actions
 * indirectly to the original object. Note that in the latter case, not all
 * standin actions are guaranteed to have the correct semantics if any
 * references directly to the original object (rather than via the standin)
 * exist.
 * <p>
 * An inheritance-based standin is sometimes a long reference (whether it's a
 * long reference or a short reference is a status that can change dynamically
 * over the course of a program's execution). This status (long or short) is
 * stored indirectly via the standin's storage method.
 * <p>
 * Because the Standin interface sometimes has to be implemented retroactively
 * via editing it into classes from outside this library that were created with
 * no knowledge of it, every method should take an <code>Authorisation</code>
 * parameter in case an existing method happens to have the same name. Depending
 * on whether the method would be safe to call from arbitrary code, some methods
 * will verify the parameter, whereas others will allow a <code>null</code>
 * authorisation; this will be mentioned in the method documentation.
 * <p>
 * Many of the methods in this class have default implementations. These are
 * slow, because they access all the fields of the object by reflection.
 * Additionally, they may not work on classes that use a nonstandard format for
 * writing the description (although the most common special cases, enums and
 * lambdas, have special handling to keep them working). As such, classes that
 * use nonstandard formats will need a nonstandard standin to implement them,
 * that overrides all the methods dealing with wire formats.
 * <p>
 * Note that, however, the fact that the methods have default implementations
 * means that where a class wants to provide a nonstandard standin for
 * efficiency purposes (whose methods have the same effect as the standard
 * methods but are faster), it can override only those methods for which it has
 * an efficient implementation, and things will still work.
 * <p>
 * The minimal set of methods that need implementing is <code>getStorage</code>,
 * <code>setStorage</code>, <code>getReferent</code>, and <code>invoke</code>
 * (plus <code>getReferentClass</code> from <code>ProxyOrWrapper</code>). For a
 * composition-based standin, the storage-related methods (plus
 * <code>getReferent</code>) can be implemented via use of
 * <code>IndirectStandin</code>; <code>ReflectiveStandin</code> can be used to
 * implement the full set. For an inheritance-based standin, you will need to
 * implement all these methods manually (although <code>getReferent</code> is
 * very simple in such a case, merely returning <code>this</code>).
 *
 * @author Alex Smith
 * @param <T> The type that this object is standing in for.
 * @see StandinStorage
 */
public interface Standin<T> extends ProxyOrWrapper<T> {

    /**
     * Gets the mechanism that this standin uses to store its data. This should
     * be stored on a private mutable field of the standin, and can typically
     * simply be a direct getter method (that checks the provided authorisation
     * and then returns the field).
     * <p>
     * The default value of the standin storage field should be a new object of
     * class <code>TrivialStandinStorage</code>.
     *
     * @param auth An authorisation, for namespacing purposes; need not be
     * verified (no information in a <code>StandinStorage</code> object is
     * secret, although much of it is not really intelligible outside this
     * package) and thus can be <code>null</code>.
     * @return This standin's storage mechanism.
     */
    public StandinStorage<T> getStorage(Authorisation auth);

    /**
     * Sets the mechanism that this standin uses to store its data. This should
     * be stored on a private mutable field of the standin, and can typically
     * simply be a direct setter method (that checks the provided authorisation
     * and then returns the field). As an exception, standins for
     * <code>Copiable</code> objects will never have their standin storage set
     * (because copiable objects can't be long references, can't be
     * long-referenced, and can't be migrated), so this method will not be
     * called in this case and may error out if called.
     * <p>
     * Note that although this <i>method</i> should not have side effects, the
     * <i>field</i> <code>storage</code> must. Most notably, each method of the
     * standin whose purpose is to access the standin's data (i.e. the methods
     * of the superclass and the various implemented interfaces) should first
     * check to see if the field in question contains an object that implements
     * <code>ForwardingStandinStorage</code>. If it does, the method should
     * forward its parameters rather than just calling the method on the
     * referent (as would be used when in stored-in-self status).
     * <p>
     * This method must not be called after <code>dropResources</code>, unless
     * <code>undropResources</code> is called first. (However, note in general
     * that it's impossible to ensure that surrounding user code respects this
     * general contract; it's acceptable to raise an exception if the methods
     * are called in the wrong order, but not to rely on the methods being
     * called correctly for security guarantees.)
     * <p>
     * The standin storage object can also have other side effects (e.g. it can
     * have a nontrivial finalizer). This is handled automatically, though, and
     * need not be handled by the standin itself.
     * <p>
     * This method does not handle synchronization itself, and should not be
     * called directly unless you have the only reference to the standin (e.g.
     * because you just created it). Use <code>safeSetStorage</code> instead if
     * the standin is currently in use. (The convention is to use the monitor of
     * standin's storage in order to block/delay migration, in order to avoid
     * race conditions, which obviously needs care when replacing the storage
     * directly.)
     *
     * @param storage The new storage mechanism.
     * @param auth An authorisation, for namespacing and verification purposes;
     * cannot be <code>null</code> (and must be verified by the implementation
     * of this method using <code>Authorisation#verify</code>).
     * @throws UnsupportedOperationException If this class is
     * <code>Copiable</code>
     * @see #safeSetStorage(xyz.acygn.mokapot.skeletons.StandinStorage,
     * xyz.acygn.mokapot.skeletons.StandinStorage,
     * xyz.acygn.mokapot.skeletons.Authorisation)
     */
    public void setStorage(StandinStorage<T> storage, Authorisation auth)
            throws UnsupportedOperationException;

    /**
     * Sets the mechanism that this standin uses to store its data, in a
     * thread-safe way. Think of this like a test-and-set operation; if the old
     * storage does not have the expected value, an exception is thrown and the
     * storage remains unchanged.
     *
     * @param storage The new storage mechanism.
     * @param believedOldStorage The old storage mechanism that should be
     * verified; if this storage mechanism is not in use, no change will be
     * made.
     * @param auth An authorisation, for namespacing and verification purposes;
     * cannot be <code>null</code> (and must be verified by the implementation
     * of this method using <code>Authorisation#verify</code>).
     * @throws UnsupportedOperationException If this class is
     * <code>Copiable</code>
     * @throws ConcurrentModificationException If the given old storage
     * mechanism is not the standin's current storage mechanism
     */
    public default void safeSetStorage(StandinStorage<T> storage,
            StandinStorage<?> believedOldStorage,
            Authorisation auth) throws UnsupportedOperationException,
            ConcurrentModificationException {
        auth.verify();

        synchronized (believedOldStorage) {
            if (getStorage(null) != believedOldStorage) {
                throw new ConcurrentModificationException();
            }
            setStorage(storage, auth);
        }
    }

    /**
     * Calculates the size of a description of this object, excluding its class.
     * This is used to determine how much space to allocate when incorporating
     * this object into a description. Note that this method should assume that
     * the object will be stored as a complete description of its fields, even
     * if it's of a class that's more normally marshalled as a reference.
     * <p>
     * The size should not include any space needed for a special representation
     * of <code>null</code>, nor for the object's class. The size can be an
     * overestimate, in cases where an overestimate is much faster to calculate
     * than an exact value; it cannot, however, be too small.
     * <p>
     * Note that it is not correct in general to implement this method using
     * <code>ClassKnowledge#descriptionSize</code>; that method will return the
     * size of one reference in the case that the object would normally be
     * marshalled via reference, and will call this method to determine the size
     * in most other cases. In some special cases (e.g. enums), that method will
     * produce the correct answer without reference to this method, and can be
     * used in that case.
     *
     * @param auth An authorisation, for namespacing and perhaps verification
     * purposes; must be verified if describing the object could be used to
     * bypass security on its fields, and may be verified in other circumstances
     * too.
     * @return The size of the description.
     * @see
     * ExposedMethods#defaultStandinDescriptionSize(xyz.acygn.mokapot.skeletons.Standin,
     * xyz.acygn.mokapot.skeletons.Authorisation)
     */
    public default ObjectDescription.Size descriptionSize(Authorisation auth) {
        return ExposedMethods.getSingleton()
                .defaultStandinDescriptionSize(this, auth);
    }

    /**
     * Produces a description (that is, a semi-serialised shallow copy) of this
     * object, excluding its class. The description is formed via serialising
     * the object itself and (recursively) any copiable fields of the object,
     * except that noncopiable fields are not serialised and rather remain as
     * references, and returned via appending it to an existing object
     * description. The description can subsequently be used to produce either a
     * shallow copy of the object, or a marshalled form of the object.
     * <p>
     * This library will only call the method in situations where the standin is
     * in stored-in-self status (i.e. its fields are the canonical location in
     * which the object that the standin is standing in for its data).
     * <p>
     * Unlike with <code>ClassKnowledge#describeFieldInto</code> (which is
     * implemented, for copiable classes, in terms of this method), this should
     * always use a format that doesn't have a special case for
     * <code>null</code> (and can assume that the object is
     * non-<code>null</code>; obviously it is, because this method is being
     * called on it!) The caller will encode <code>null</code> (or its opposite)
     * itself if necessary.
     * <p>
     * The default implementation calls
     * <code>ClassKnowledge#defaultStandinDescription</code>.
     *
     * @param desc The description to append the description of this object to.
     * @param auth An authorisation, for namespacing and perhaps verification
     * purposes; must be verified if describing the object could be used to
     * bypass security on its fields, and may be verified in other circumstances
     * too.
     * @see
     * ExposedMethods#defaultStandinDescribeInto(xyz.acygn.mokapot.skeletons.Standin,
     * xyz.acygn.mokapot.wireformat.DescriptionOutput,
     * xyz.acygn.mokapot.skeletons.Authorisation)
     * @see #writeTo(java.io.DataOutput,
     * xyz.acygn.mokapot.skeletons.Authorisation)
     * @see
     * ExposedMethods#defaultStandinDescribeInto(xyz.acygn.mokapot.skeletons.Standin,
     * xyz.acygn.mokapot.wireformat.DescriptionOutput,
     * xyz.acygn.mokapot.skeletons.Authorisation)
     * @throws IOException If something goes wrong writing to the description
     */
    public default void describeInto(DescriptionOutput desc, Authorisation auth)
            throws IOException {
        ExposedMethods.getSingleton().defaultStandinDescribeInto(
                this, desc, auth);
    }

    /**
     * A version of <code>describeInto</code> with extra sanity checks. The
     * default implementation, which standin implementations are unlikely to
     * want to override, is to call <code>describeInto</code> but to verify
     * (after the data is written) that the amount of data actually written is
     * consistent with the return value of <code>descriptionSize</code>. (Note
     * that <code>descriptionSize can be an overestimate, and thus writing less
     * data than indicated is acceptable.)
     *
     * @param desc The description to write the output into.
     * @param auth An authorisation, for namespacing and perhaps verification
     * purposes; must be verified if describing the object could be used to
     * bypass security on its fields, and may be verified in other circumstances
     * too.
     * @throws IOException If something goes wrong writing to the description
     * @throws AssertionError If the attempt to write to the description wrote
     * too much data, as indicated by <code>descriptionSize</code>.
     */
    public default void verifiedDescribeInto(DescriptionOutput desc, Authorisation auth)
            throws IOException {
        ObjectDescription.Size position = desc.getPosition();
        ObjectDescription.Size expected = descriptionSize(auth);
        describeInto(desc, auth);
        ObjectDescription.Size actual = desc.getPosition().subtract(position);
        if (!actual.fitsWithin(expected)) {
            throw new AssertionError("description size mismatch, expected "
                    + expected + ", actual " + actual);
        }
    }

    /**
     * Writes a description of this object directly to a data output sink. This
     * excludes the object's class, and is an optional operation, that only
     * needs to be implemented in cases where the standin's referent is deeply
     * copiable. (It should not be called on objects which can't be completely
     * serialised down to a flat structure.)
     * <p>
     * The default implementation calls
     * <code>ClassKnowledge#defaultStandinDescription</code>.
     *
     * @param sink The sink to write the description to.
     * @param auth An authorisation, for namespacing and perhaps verification
     * purposes; must be verified if describing the object could be used to
     * bypass security on its fields, and may be verified in other circumstances
     * too.
     * @throws IOException If something goes wrong writing the description
     * @throws UnsupportedOperationException If this standin is not deeply
     * copiable
     * @see
     * ExposedMethods#defaultStandinWriteTo(xyz.acygn.mokapot.skeletons.Standin,
     * java.io.DataOutput, xyz.acygn.mokapot.skeletons.Authorisation)
     */
    public default void writeTo(DataOutput sink, Authorisation auth)
            throws UnsupportedOperationException, IOException {
        ExposedMethods.getSingleton().defaultStandinWriteTo(this, sink, auth);
    }

    /**
     * Replaces the content of this standin's referent with a reproduction of
     * the given description. In other words, we're effectively "overwriting"
     * the content of this standin. This method is typically called just before
     * the standin becomes the canonical place to store data for the object it's
     * standing in for (either because it's about to enter stored-in-self
     * status, or else because its local location manager wants to use it to
     * store data). Any encoding of the class (or of null) must have already
     * been stripped from the description.
     * <p>
     * If the referent has been dropped, this method cannot be called until the
     * referent is undropped. (Doing an drop, undrop and replace effectively
     * creates a new referent for the standin, which might or might not be the
     * same object in memory.)
     * <p>
     * The default implementation uses reflection to set the fields of the
     * referent individually from the description.
     *
     * @param wrappedClass <code>T.class</code>, the class that this standin
     * wraps. This is given explicitly so that the method can safely be called
     * from the constructor of a standin class, even if
     * <code>getReferentClass</code> is implemented by reading from a field.
     * @param description The description to reproduce.
     * @param auth An authorisation, for namespacing and verification purposes;
     * cannot be <code>null</code> (and must be verified by the implementation
     * of this method using <code>Authorisation#verify</code>).
     * @throws IOException If the description appears to be corrupted
     */
    public default void replaceWithReproduction(Class<T> wrappedClass,
            ReadableDescription description, Authorisation auth)
            throws IOException {
        ExposedMethods.getSingleton().defaultStandinReplaceWithReproduction(
                this, wrappedClass, description, auth);
    }

    /**
     * Invokes a method directly on the local referent of this standin. Unlike
     * using <code>getReferent</code>, the method is not intercepted by the
     * standin even if it normally would be (e.g. a method can be forwarded to
     * the location manager if called directly on the standin, and yet called on
     * the standin without forwarding if called indirectly via
     * <code>invoke</code>); this mechanism prevents an infinite forwarding loop
     * in the case where the standin forwards the method to the location
     * manager, but the location manager forwards it back to the original
     * standin.
     *
     * @param methodCode The method code of the method to run (calculated using
     * the actual class of the standin's referent, i.e. <code>T</code>).
     * @param methodArguments The arguments of the method.
     * @param auth An authorisation, for namespacing and verification purposes;
     * cannot be <code>null</code> (and must be verified by the implementation
     * of this method using <code>Authorisation#verify</code>).
     * @return The return value of the method.
     * @throws Throwable If the invoked method throws an exception, then this
     * method throws the same exception.
     */
    public Object invoke(long methodCode, Object[] methodArguments,
            Authorisation auth)
            throws Throwable;

    /**
     * Gets the underlying object behind this standin. For a composition-based
     * standin, this is a different object. For an inheritance-based object,
     * this is the standin itself; as such, it must return <code>this</code>.
     * This method can safely be used to determine whether a standin is
     * inheritance-based, via checking to see if the return value is equal to
     * the standin itself using reference equality.
     * <p>
     * Bear in mind that this is a very low-level operation; the object in
     * question might not be currently in use to store the standin's data, for
     * example. As such, it's probably useful only in cases where the referent
     * is already known to be local (e.g. when creating a location manager for
     * it).
     *
     * @param auth An authorisation, for namespacing and verification purposes;
     * cannot be <code>null</code> (and must be verified by the implementation
     * of this method using <code>Authorisation#verify</code>).
     * @return The local referent of this standin. This could be
     * <code>null</code>, if the standin has a dropped referent.
     */
    public T getReferent(Authorisation auth);

    /**
     * Drops any unnecessary references to data that this standin might
     * currently be holding. This is called by the location manager when the
     * standin isn't currently being used to store data (i.e.
     * <code>isStoredLocally()</code> in the storage manager is false).
     * <p>
     * It's to some extent up to the standin just how much it wants to drop.
     * Anything that won't be useful in the future (e.g. references in mutable
     * fields) almost certainly should be dropped; in the case of a
     * composition-based standin, its referent <i>must</i> be dropped (in order
     * that the caller can get better guarantees about sharing). An
     * inheritance-based standin might want to hold onto immutable fields so
     * that less time is spent loading them if the data is migrated back into
     * the standin, and/or so that it can call methods on the cached copy of the
     * object if they use only those fields.
     * <p>
     * After calling this method, <code>undropResources()</code> must be called
     * prior to any attempt to call <code>replaceWithReproduction()</code>.
     * <p>
     * The default implementation verifies the authorisation and the state of
     * the standin's storage and does nothing else. Calling into this default
     * implementation is a sensible way for implementations of this interface to
     * verify the parameters. The default implementation throws unchecked
     * exceptions upon violations of the calling convention;
     * <code>NullPointerException</code> if the authorisation is
     * <code>null</code>, <code>IllegalStateException</code> if the standin's
     * data is currently held locally, or whatever exception happened trying to
     * verify the authorisation. This is not part of the API contract of this
     * method, and implementations of this method may choose to report errors in
     * calling it via some other means.
     *
     * @param auth An authorisation, for namespacing and verification purposes;
     * cannot be <code>null</code> (and must be verified by the implementation
     * of this method using <code>Authorisation#verify</code>).
     * @see #undropResources(xyz.acygn.mokapot.skeletons.Authorisation)
     */
    public default void dropResources(Authorisation auth) {
        auth.verify();
    }

    /**
     * Restores the ability for the standin to have its referent replaced.
     * <p>
     * In some cases, dropping resources from a standin via
     * <code>dropResources</code> will leave it in a partially deallocated state
     * that cannot handle a replacement of its referent. In order to handle this
     * situation, after calling <code>dropResources</code>, the
     * <code>undropResources</code> method must be called before a subsequent
     * <code>replaceWithReproduction</code> or <code>setStorage</code>.
     * <p>
     * Due to the sequence in which standins are constructed, this method must
     * not examine or look at the standin's storage mechanism, which may be only
     * partially constructed at the time this method is called.
     * <p>
     * The default implementation does nothing (other than verifying the
     * provided authorisation).
     *
     * @param auth An authorisation, for namespacing and verification purposes;
     * cannot be <code>null</code> (and must be verified by the implementation
     * of this method using <code>Authorisation#verify</code>).
     * @see #dropResources(xyz.acygn.mokapot.skeletons.Authorisation)
     */
    public default void undropResources(Authorisation auth) {
        auth.verify();
    }

    /**
     * Gets a consumer that sets the values of successive fields of this
     * standin's referent via reflection. Because this operation is
     * <code>public</code> (it acts like a <code>protected</code> method but
     * that isn't legal in an interface), but can see into the internals of an
     * object, it requires an authorisation. This is used by the code that
     * implements standins in order to initialise the fields of the object,
     * including static and final fields.
     * <p>
     * Note: Do not rely on any particular internal implementation of this
     * method; it may be changed in the future to make it more efficient.
     *
     * @param auth The authorisation that determines that the act of setting is
     * being called from inside this package.
     * @return The setting consumer. Calling the <code>accept</code> method on
     * this consumer will set successive fields of the referent.
     */
    public default Consumer<Object>
            getSettingConsumerForReferent(Authorisation auth) {
        auth.verify();

        T referent = getReferent(auth);
        if (referent == null) {
            throw new IllegalStateException(
                    "getting setting consumer for a null (dropped?) referent");
        }

        return ExposedMethods.getSingleton().getSettingConsumerFor(
                referent, auth);
    }

    /**
     * Throws a DistributedError complaining about an unrecognised method code.
     * This method is provided mostly to allow standins to produce appropriate
     * error messages without needing to depend on the details of how
     * <code>DistributedError</code> works, and without needing to generate
     * complex bytecode to build the string and wrap it in the appropriate
     * exceptions.
     *
     * @param code The unrecognised method code.
     * @throws DistributedError Always.
     */
    public static void unrecognisedMethodCode(long code)
            throws DistributedError {
        throw new DistributedError(new NoSuchMethodException(),
                "Method code " + code + " does not correspond to a method");
    }
}
