package xyz.acygn.mokapot.skeletons;

/**
 * An interface that marks a class as acting like another class when seen by the
 * user program, but not when seen by the distributed architecture. This makes
 * it possible to create a class that acts in one way, but which sends over the
 * network another way.
 * <p>
 * This library will not treat the class specially, except in methods of
 * <code>LengthIndependent</code> (which return how the class "should look" to
 * the user's program, rather than how they look to this library internally).
 * <p>
 * This particular marker is not likely to be of use to most end users, but some
 * programs that generate code for use with the distributed communication system
 * may need to use it in their generated code. As such, it's marked as
 * <code>public</code> so that they can use it.
 *
 * @author Alex Smith
 * @param <T> The referent class.
 */
public interface ArtificialWrapper<T> extends ProxyOrWrapper<T> {
}
