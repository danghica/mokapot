package xyz.acygn.mokapot.markers;

/**
 * A marker interface specifying that an object can be safely replaced with a
 * copy/clone without changing the semantics of the program. If an object is
 * explicitly marked as <code>Copiable</code>, then the distributed
 * communication system will always maintain a copy of the object on each
 * virtual machine that needs it, rather than trying to maintain a reference to
 * it.
 * <p>
 * As such, an object should only be marked as <code>Copiable</code> if this
 * copying behaviour will not affect the program's functionality. In addition,
 * because the object may be copied back and forth repeatedly, large objects
 * should not be marked as <code>Copiable</code> for efficiency reasons.
 * <p>
 * The copying will be done field by field. If the fields are
 * <code>Copiable</code> themselves (or effectively copiable, such as
 * <code>String</code> and primitives), they will be copied too, recursively,
 * meaning that the <code>Copiable</code> portion of the object will be deeply
 * copied. If a field is not <code>Copiable</code>, it must be an object
 * reference; this will be copied as another reference to the same object, i.e.
 * a shallow copy is performed in this case.
 * <p>
 * Due to implementation details of Java, if a <code>Copiable</code> interface
 * might potentially hold an object generated via a lambda expression, the
 * interface must also be declared <code>Serializable</code>. (Otherwise, it's
 * impossible to extract enough information from the lambda to reconstruct it
 * <p>
 * Note that if you declare a class as <code>Copiable</code>, you should
 * seriously consider it as <code>Serializable</code> too if it happens to obey
 * the restrictions of <code>Serializable</code> (i.e. all fields are themselves
 * <code>Serializable</code>, or can be made so). An object can be
 * <code>Copiable</code> even if it cannot be serialised via the built-in Java
 * mechanism (e.g. if it contains references to nonserialisable objects), and
 * thus <code>Copiable</code> does not require <code>Serializable</code>, but
 * the combination can be significantly more efficient than
 * <code>Copiable</code> on its own.
 *
 * @author Alex Smith
 * @see NonCopiable
 */
public interface Copiable {
}
