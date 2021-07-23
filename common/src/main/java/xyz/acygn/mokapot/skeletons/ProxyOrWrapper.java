package xyz.acygn.mokapot.skeletons;

import xyz.acygn.mokapot.wireformat.ObjectWireFormat;

import javax.naming.Name;

/**
 * An interface that marks a class as acting like a proxy or wrapper for another
 * specific class. In other words, objects of the proxy-or-wrapper class
 * "pretend" to be objects of a referent class. The referent class must be a
 * specific, actual class (e.g. the object acts as though it were an instance of
 * the referent class, not one of its subclasses).
 * <p>
 * The proxy/wrapper class does not necessarily have to extend the referent
 * class (this may be impossible in some cases), although in most cases it will.
 *
 * @author Alex Smith
 * @param <T> The referent class.
 */
public interface ProxyOrWrapper<T> extends ObjectWireFormat.Transparent {

    /**
     * Gets the referent class of this proxy or wrapper class. That is, the
     * class that it's "pretending to have", i.e. the class that's being wrapped
     * or proxied.
     *
     * @param dummy A dummy parameter for namespacing purposes (i.e. preventing
     * this method having a naming clash with a method of the wrapped class).
     * Typically <code>null</code>, and must be ignored.
     * @return The referent class.
     * @see Object#getClass()
     */
    public Class<T> getReferentClass(Namespacer dummy);

    /**
     * The type of a dummy parameter used to prevent naming clashes between
     * <code>ProxyOrWrapper#getReferentClass()</code> and methods of other
     * classes that randomly happen to be named "getReferentClass". The
     * parameter is not actually used, so typically <code>null</code> is used
     * when an object of this class is needed.
     * <p>
     * As the objects of this interface are not actually used, it has no
     * functionality.
     */
    public static interface Namespacer {
    }


    final class NameSpacerInstance implements Namespacer{

    }

    Namespacer NAMESPACEROBJECT = new NameSpacerInstance();

}
