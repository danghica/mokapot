package xyz.acygn.mokapot;

import java.io.Serializable;
import xyz.acygn.mokapot.markers.Copiable;

/**
 * A Runnable that's safe to copy and serialisable. This interface exists mostly
 * for the benefit of <code>DistributedCommunicator#runRemotely</code>,
 * allowing lambdas to be automatically placed in a context in which they can be
 * reconstructed on another machine, without the need for the user to write a
 * complex cast.
 * <p>
 * Note that because this interface extends <code>Copiable</code>, it places
 * certain requirements on objects that conform to it; specifically, they must
 * not have any mutable state (because the other end of the network connection
 * may see a stale copy of the object, rather than an up-to-date original). This
 * is rarely an issue with lambdas (because captured variables have to be
 * effectively final according to Java syntax), but may become relevant if
 * writing a more complex object manually.
 * <p>
 * Be aware that the requirement to avoid mutable state only goes one level;
 * it's OK for an immutable field of the <code>CopiableRunnable</code> to be a
 * reference to something mutable (it's just not OK for a field to be mutable
 * itself).
 *
 * @author Kelsey McKenna
 * @see DistributedCommunicator#runRemotely(xyz.acygn.mokapot.CopiableRunnable,
 * xyz.acygn.mokapot.CommunicationAddress)
 */
public interface CopiableRunnable
        extends Runnable, Copiable, Serializable {
}
