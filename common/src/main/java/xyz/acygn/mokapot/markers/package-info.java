/**
 * Marker interfaces used by the distributed communication system. These are
 * typically interfaces that you implement to convey a particular message; for
 * example, you could use <code>implements Copiable</code> to let the system
 * know that it can safely make copies of an object without changing the
 * semantics of your program.
 * <p>
 * The marker interfaces are:
 * <dl>
 * <dt>Copiable</dt>
 * <dd>Specifies that an object can safely be replaced by a copy. This often
 * makes the computation more efficient by allowing a copy of it to be kept on
 * every system, reducing network traffic (although often increasing memory
 * usage).</dd>
 * <dt>NonCopiable</dt>
 * <dd>Overrides copiability inference, to prevent a class being copied even if
 * it looks as though it safely could be. This can be useful for classes where
 * the objects are very large, or in cases where the addresses of objects are
 * important for distinguishing them.</dd>
 * <dt>NonMigratable</dt>
 * <dd>Specifies that objects of a class are inherently tied to one system, and
 * should not be moved to other systems. This is sometimes needed for
 * correctness when calling static methods (which run on the same system as is
 * storing the <code>this</code> of the instance method that called them), in
 * cases where the static methods would act differently on different systems
 * (e.g. <code>System.out</code> would print to a different system's standard
 * output stream).</dd>
 * </dl>
 * <p>
 * This class also contains error/exception classes used by the distributed
 * communication system.
 *
 * @see xyz.acygn.mokapot.markers.Copiable
 * @see xyz.acygn.mokapot.markers.NonCopiable
 * @see xyz.acygn.mokapot.markers.NonMigratable
 */
package xyz.acygn.mokapot.markers;
