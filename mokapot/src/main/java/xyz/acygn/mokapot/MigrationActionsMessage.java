package xyz.acygn.mokapot;

import java.time.Duration;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper;

/**
 * A distributed message that returns a set of migration actions for an object.
 * This should typically be sent to the system which currently stores the
 * object's data (otherwise, the message is likely to simply be forwarded on to
 * the system were the object's data is stored).
 *
 * @author Alex Smith
 * @param <T> The actual type of the object for which migration actions are
 * being returned.
 */
class MigrationActionsMessage<T> extends SynchronousMessage<MigrationActions<T>>
        implements SynchronousMessage.BorrowOnly {

    /**
     * The object for which we're getting the migration actions. Because the
     * object is highly likely to already exist on the target system, there's no
     * real performance cost to using a long reference here (as opposed to a
     * reference value or object location), as this won't cause a new object to
     * be created on the target system. Meanwhile, using the general
     * serialisation mechanism rather than a custom, cut-down version will
     * handle garbage collection properties appropriately (by ensuring that the
     * object isn't deallocated until after the migration actions are created);
     * the BorrowOnly marker interface makes it clear that we aren't going to
     * hang onto the provided reference indefinitely (we only use it to find the
     * location manager, which must already exist).
     * <p>
     * The most efficient way to initially create this field is probably via use
     * of <code>RemoteOnlyStandin</code>.
     */
    private final T getActionsFor;

    /**
     * Creates a migration actions message that gets actions for the given
     * object. The object in question can be an actual real object, or (more
     * likely, as this message is unlikely to be constructed on the same machine
     * as the object itself) a standin that's acting as a long reference to it.
     *
     * @param getActionsFor The object for which to get actions. This must
     * either be a <code>T</code> (actual class, not just declared class) or a
     * <code>Standin&lt;T&gt;</code>.
     */
    MigrationActionsMessage(T getActionsFor) {
        this.getActionsFor = getActionsFor;
    }

    /**
     * Creates a migration actions message that gets actions for the object
     * managed by a given location manager. This is a shorthand for creating a
     * <code>RemoteOnlyStandin</code> and using it as a long reference to the
     * object (as, despite being simple, it is sufficiently powerful for the
     * recipient machine to be able to identify the object in question).
     *
     * @param lm The location manager of the object for which to get actions.
     */
    @SuppressWarnings("unchecked")
    MigrationActionsMessage(LocationManager<T> lm) {
        /* Note: this is an utter abuse of type erasure, and relies on the fact
           that although a RemoteOnlyStandin<T> is not a T, it will turn into a
           T after being sent over the network, and Java will let us store an
           object of the wrong type in getActionsFor meanwhile. */
        this((T) new RemoteOnlyStandin<T>(lm));
    }

    @Override
    protected MigrationActions<T> calculateReply() {
        LocationManager<T> lm;
        try {
            lm = getCommunicator()
                    .findLocationManagerForObject(getActionsFor);
        } catch (AutocloseableLockWrapper.CannotLockException ex) {
            /* It shouldn't be possible for this to fail; the object should be
               being held alive on the remote system, and it's obviously alive
               here, thus the location manager must already exist and thus we
               can't be past the point at which they can be created. */
            throw new RuntimeException(ex);
        }
        return new MigrationActions<>(lm);
    }

    @Override
    public Duration periodic() {
        return null;
    }
}
