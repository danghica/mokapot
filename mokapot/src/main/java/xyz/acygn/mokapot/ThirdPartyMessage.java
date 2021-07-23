package xyz.acygn.mokapot;

import java.time.Duration;
import static xyz.acygn.mokapot.ReferenceValue.newReferenceValueOf;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper;

/**
 * A message that asks a system permission to marshal one of its noncopiable
 * objects so that it can be sent to a third system. That is, the sender of the
 * message, the recipient of the object, and the current holder of the object
 * are three different systems. A marshalled reference can only be sent by or to
 * the current location of the object (so that the garbage collection
 * information is up-to-date and known by every object involved), thus a message
 * is needed to handle the indirect case.
 *
 * @author Alex Smith
 * @param <T> A class to which the object to be marshalled is known to belong.
 */
class ThirdPartyMessage<T> extends SynchronousMessage<ReferenceValue<T>> {

    /**
     * The object that this message is about. (The object itself won't be sent
     * across the network as we know it's hosted on the recipient; the use of an
     * object in this field effectively means that the existing mechanism for
     * sending an object "back to its caller" will be reused here.)
     */
    private final T object;

    /**
     * The system that the resulting reference value will eventually be
     * unmarshalled on.
     */
    private final CommunicationAddress targetSystem;

    /**
     * Creates a message requesting a reference value for the given object.
     *
     * @param object The object to request a reference value for.
     * @param targetSystem The system on which the resulting reference value
     * will be eventually unmarshalled.
     */
    ThirdPartyMessage(T object, CommunicationAddress targetSystem) {
        this.object = object;
        this.targetSystem = targetSystem;
    }

    /**
     * Marshals the object specified in the message into a
     * <code>ReferenceValue</code>, and returns the marshalled form. Note that
     * as usual for a function that marshals an object, it must be unmarshalled
     * exactly once, and on the system specified when constructing this message.
     *
     * @return The marshalled object.
     */
    @Override
    protected ReferenceValue<T> calculateReply() {
        LocationManager<T> lm;
        try {
            lm = getCommunicator()
                    .findLocationManagerForObject(object);
        } catch (AutocloseableLockWrapper.CannotLockException ex) {
            /* This shouldn't be possible; by the whole nature of this message,
               the object already exists on two systems, and thus must have a
               location manager already. */
            throw new RuntimeException(ex);
        }
        return newReferenceValueOf(lm, targetSystem);
    }

    @Override
    public Duration periodic() {
        return null;
    }

    /**
     * Produces a human-readable representation of this message.
     *
     * @return An appropriate representation of this message for debug output.
     */
    @Override
    public String toString() {
        return "request for GC weight for object "
                + DistributedMessage.safeStringify(object)
                + ", which will be sent to " + targetSystem;
    }
}
