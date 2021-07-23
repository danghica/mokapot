package xyz.acygn.mokapot;

import java.time.Duration;
import xyz.acygn.mokapot.markers.Copiable;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.util.StringifyUtils;
import static xyz.acygn.mokapot.util.StringifyUtils.stringify;

/**
 * A message that can be sent via a DistributedCommunicator. The message is
 * basically just a wrapper for the code to run when the message is received. A
 * message can also have a return value, which is marshalled and sent back to
 * the original sender; not all messages have a return value (messages with no
 * return value are called "asynchronous", because they can be run at any time
 * after being sent and the sender will not be informed when they're complete).
 * <p>
 * Distributed messages are intended to be immutable once created, and most
 * users assume that they can be freely sent from machine to machine. The
 * process of sending will, however, be to marshal the message itself; this
 * means that the message will be sent field by field, with
 * <code>Copiable</code> values copied, and other values replaced via long
 * references at the destination (marshalled references in transit). As such, a
 * distributed message must be designed so that this transformation is possible.
 *
 * @author Alex Smith
 */
interface DistributedMessage extends Copiable {

    /**
     * The operation to perform on the system that receives the message.
     *
     * @param envelope The envelope in which the message arrived. This parameter
     * can be used to, e.g., determine where to send a reply to the message.
     * @param communicator The communicator that is processing the message (and
     * to which the message was addressed).
     * @param requestTracking If <code>true</code>, any reply that is sent to
     * the message should contain a request to add a thread projection tracker
     * to the thread on the calling system from which it was sent.
     */
    public abstract void process(MessageEnvelope envelope,
            DistributedCommunicator communicator, boolean requestTracking);

    /**
     * Checks whether this message will send a reply back to the original
     * caller.
     *
     * @return Whether the message will send a reply.
     */
    public boolean sendsReply();

    /**
     * Checks whether this message is safe to run in a "lightweight" way. A
     * lightweight message is one that is run directly by the network-handling
     * thread of the communicator (blocking the receipt of new messages in the
     * process). As such, it's not safe to run a message like this unless it's
     * very simple (in particular, it must run quickly, with no network access,
     * and with no followup messages being sent).
     *
     * @return Whether the message is safe to run in a lightweight way.
     */
    public boolean lightweightSafe();

    /**
     * Checks whether this message is unimportant. An unimportant message will
     * be ignored if it asks to run on a nonexistent thread, or if the recipient
     * is not currently handling messages. An important message (the opposite)
     * will create a new thread to run on if the mentioned thread is
     * nonexistent, and requires error handling if the recipient is not
     * currently handling messages.
     *
     * @return <code>true</code> if the message is unimportant.
     */
    public default boolean isUnimportant() {
        return false;
    }

    /**
     * The time interval at which this message is periodically sent.
     * Informational only (i.e. setting this does nothing to actually
     * <i>cause</i> the message to be sent at the given period). Used to
     * distinguish one-off messages from things like "keep-alive" messages.
     *
     * @return The time interval of the message, or <code>null</code> if it's a
     * one-off message.
     */
    Duration periodic();

    /**
     * Creates a string representation of the given object, without triggering
     * network calls. Used to prevent recursive network calls in the debug code.
     *
     * @param o The object to produce a string representation of.
     * @return A string representation of the object, using local information.
     */
    static String safeStringify(Object o) {
        /* TODO: This doesn't work in the case where we have /nested/ long
           references inside a short reference. */
        if (o != null && o instanceof Standin
                && !((Standin) o).getStorage(Authorisations.UNRESTRICTED)
                        .certainlyLocal()) {
            return standinStringify((Standin<?>) o);
        }
        return stringify(o);
    }

    /**
     * Creates a string representation of a standin with remote data storage,
     * without network access. Split out from <code>safeStringify</code> due to
     * limitations of Java's type system.
     *
     * @param <T> The type of the standin's referent.
     * @param standin The standin to stringify.
     * @return A string representation of the standin.
     */
    static <T> String standinStringify(Standin<T> standin) {
        if (standin == null) {
            return "null";
        }
        return standin.getClass().getName() + "{"
                + StringifyUtils.toString(
                        standin.getStorage(Authorisations.UNRESTRICTED)) + "}";
    }
}
