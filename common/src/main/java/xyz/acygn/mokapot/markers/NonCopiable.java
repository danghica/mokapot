package xyz.acygn.mokapot.markers;

/**
 * A marker interface warning that a given class should <i>not</i> be copied to
 * another system, even if doing so appears to be semantically safe. This is the
 * opposite of <code>Copiable</code>; no class should be tagged with both
 * interfaces (even indirectly).
 * <p>
 * The rules for a class to be acceptable as non-copiable are much more lenient
 * than the rules for it to be copiable: it must be a class which can be
 * extended (i.e. is not <code>final</code>, and not a special class such as a
 * primitive or an array). (This is because if something can't be copied, we
 * have to create a reference to it instead, and the class that the reference
 * itself belongs to can't normally be the same class as the original object as
 * its methods wouldn't abstract over the network, thus it has to be a subclass
 * instead.)
 *
 * @author Alex Smith
 * @see Copiable
 */
public interface NonCopiable {
}
