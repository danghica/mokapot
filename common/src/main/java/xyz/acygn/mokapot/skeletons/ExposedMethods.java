package xyz.acygn.mokapot.skeletons;

import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Consumer;
import xyz.acygn.mokapot.wireformat.DescriptionOutput;
import xyz.acygn.mokapot.wireformat.ObjectDescription;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * Methods from the internals of the distributed system that might need to be
 * called by generated code. In general, you should not be calling these methods
 * yourself unless you're manually writing things like standins (which would
 * normally be generated via code generation). They are public only for
 * technical reasons, so that the internals of the distributed system can ensure
 * that the methods it provides conforms to the right interface.
 * <p>
 * This class does not contain the definitions of the methods, only their
 * signatures. The definitions are set at runtime using
 * <code>setSingleton</code>, which allows the core of the distributed system to
 * set the methods even though it belongs to a different package; the methods
 * are called via the singleton in question. (The getter,
 * <code>getSingleton</code>, is not available outside this package; it's used
 * by the default implementations of some methods in <code>Standin</code>.)
 *
 * @author Alex Smith
 */
public abstract class ExposedMethods {

    /**
     * Default method for calculating the size of the description of the object
     * represented by a standin. This is the method that
     * <code>Standin#descriptionSize</code> uses for its default implementation
     * (although it can be overridden). The size does not include any size
     * needed to describe the class of the object, nor any extra space needed to
     * give a representation of <code>null</code>.
     *
     * @param <T> The type of the standin's referent.
     * @param standin The standin whose description's size is being measured.
     * @param auth An authorisation, for namespacing and verification purposes.
     * @see Standin#descriptionSize(xyz.acygn.mokapot.skeletons.Authorisation)
     * @return The size of the description.
     */
    public abstract <T> ObjectDescription.Size defaultStandinDescriptionSize(
            Standin<T> standin, Authorisation auth);

    /**
     * A default implementation of <code>Standin#writeTo</code>. See that method
     * for more information.
     *
     * @param <T> The actual class of the standin's referent.
     * @param standin The standin to describe.
     * @param into The place to write the description.
     * @param auth The authorisation that allows this method to ensure it's
     * being run from inside the package; it will be verified to ensure that
     * it's valid.
     * @throws IOException If something goes wrong writing the description
     * @see Standin#writeTo(java.io.DataOutput,
     * xyz.acygn.mokapot.skeletons.Authorisation)
     */
    public abstract <T> void defaultStandinWriteTo(Standin<T> standin,
            DataOutput into, Authorisation auth) throws IOException;

    /**
     * A default implementation of <code>Standin#describeInto</code>. See that
     * method for more information.
     *
     * @param <T> The actual class of the standin's referent.
     * @param standin The standin to describe.
     * @param into The place to write the description.
     * @param auth The authorisation that allows this method to ensure it's
     * being run from inside the package; it will be verified to ensure that
     * it's valid.
     * @throws IOException If something goes wrong writing the description
     * @see Standin#writeTo(java.io.DataOutput,
     * xyz.acygn.mokapot.skeletons.Authorisation)
     */
    public abstract <T> void defaultStandinDescribeInto(Standin<T> standin,
            DescriptionOutput into, Authorisation auth) throws IOException;

    /**
     * A default implementation of <code>Standin#replaceWithReproduction</code>.
     * See that method for more information.
     *
     * @param <T> The type of the standin's referent.
     * @param standin The standin whose referent should be replaced by a
     * reproduction.
     * @param referentClass <code>T.class</code>, the class of the standin's
     * referent. This is given explicitly so that the method is usable even from
     * <code>standin</code>'s constructor.
     * @param description The description to reproduce.
     * @param auth An authorisation, for namespacing and verification purposes;
     * cannot be <code>null</code>.
     * @throws IOException If the description appears to be corrupted
     */
    public abstract <T> void defaultStandinReplaceWithReproduction(
            Standin<T> standin, Class<T> referentClass,
            ReadableDescription description, Authorisation auth)
            throws IOException, IllegalStateException, SecurityException;

    /**
     * Gets a consumer that sets successive fields of an object via reflection.
     * <p>
     * Note: Do not rely on any particular internal implementation of this
     * method; it may be changed in the future to make it more efficient.
     *
     * @param o The object whose fields should be set by the returned consumer.
     * @param auth An authorisation; this will be checked to ensure that the
     * caller is allowed to perform this (rather wide-ranging) action.
     * @return The setting consumer.
     */
    public abstract Consumer<Object> getSettingConsumerFor(
            Object o, Authorisation auth);

    /**
     * Singleton object implementing the exposed methods.
     */
    private static ExposedMethods singleton = null;

    /**
     * Returns an object implementing the methods of this class.
     *
     * @return The singleton <code>ExposedMethods</code> object.
     */
    static ExposedMethods getSingleton() {
        return singleton;
    }

    /**
     * Defines the methods of this class.
     *
     * @param singleton The singleton obejct that implements the exposed
     * methods.
     */
    public static void setSingleton(ExposedMethods singleton) {
        if (ExposedMethods.singleton == null) {
            ExposedMethods.singleton = singleton;
        } else {
            throw new IllegalStateException(
                    "ExposedMethods can be initialised only once");
        }
    }
}
