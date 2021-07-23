package xyz.acygn.mokapot;

import java.time.Duration;
import xyz.acygn.mokapot.markers.NonCopiable;
import xyz.acygn.mokapot.markers.NonMigratable;

/**
 * An interface representing objects that monitor the messages sent to or from a
 * distributed communicator. This is effectively a "listener" class that can be
 * registered onto a distributed communicator to make it possible to monitor the
 * communications going via that communicator, typically for debugging purposes.
 * <p>
 * Debug monitors are also used to convey warning-level events (in other words,
 * something that has probably been compensated for automatically but is
 * sub-optimal and should be looked at).
 * <p>
 * It's possible that messages will appear to be sent or received slightly out
 * of order. This typically happens when one message "overtakes" another, i.e.
 * it starts to be sent after the other message has already started, but
 * finishes being sent before the other message has finished. The typical cause
 * of this is that a connection needs to be established/re-established to send
 * the overtaken message, and the overtaking message is needed to actually
 * establish or re-establish the connection.
 *
 * @author Alex Smith
 * @see DistributedCommunicator
 */
public interface DebugMonitor {

    /**
     * A listener called whenever a warning occurs. A warning is a situation in
     * which something is clearly wrong, but it has been automatically
     * compensated for or might not be a problem.
     *
     * @param message A string describing the warning in natural language.
     */
    public void warning(String message);

    /**
     * A listener called when a new message is sent or received via the
     * distributed communicator.
     *
     * @param messageInfo Information about the message that was sent or
     * received.
     */
    public void newMessage(MessageInfo messageInfo);

    /**
     * Information about a single message that was sent and/or received. A debug
     * monitor can process this to learn more.
     */
    public static class MessageInfo implements NonCopiable, NonMigratable {

        /**
         * The message that was sent/received.
         */
        private final DistributedMessage message;

        /**
         * The system that sent the message.
         */
        private final CommunicationAddress sender;

        /**
         * The system that received the message.
         */
        private final Communicable recipient;

        /**
         * Whether the message was sent from the communicator on which this
         * debug monitor is installed. (If this is <code>false</code>, the
         * message was received by the communicator on which this debug monitor
         * is installed.)
         */
        private final boolean outbound;

        /**
         * The thread on which the message was requested to run. Can be
         * <code>null</code> if the message is lightweight, and thus can safely
         * be run on any thread.
         */
        private final GlobalID threadID;

        /**
         * The number of bytes that make up the part of the message that
         * describes copiable data. Does not include the message address, or any
         * references to noncopiable objects being sent as part of the message.
         */
        private final int bytes;

        /**
         * The number of noncopiable objects to which references are being sent
         * as part of the message.
         */
        private final int objs;

        /**
         * The length of time spent marshalling (if a send) or unmarshalling (if
         * a recieve) the message, in nanoseconds.
         */
        private final long marshalTimeNanos;

        /**
         * Produces a string description of the message that was sent.
         *
         * @return A string describing the message, in an approximately
         * human-readable form.
         */
        public String getMessage() {
            return message.toString();
        }

        /**
         * Specifies which communicator sent the message, if known.
         *
         * @return The communication address of the communicator that sent the
         * message. If this is unknown, the message returns <code>null</code>.
         */
        public CommunicationAddress getSender() {
            return sender;
        }

        /**
         * Specifies which communicator received the message.
         *
         * @return The communication address of the communicator that received
         * the message.
         */
        public Communicable getRecipient() {
            return recipient;
        }

        /**
         * Specifies whether the message was sent by the same communicator that
         * reported the message to its debug monitor.
         *
         * @return <code>true</code> if the communicator is reporting a message
         * send; <code>false</code> if the communicator is reporting the receipt
         * of a message.
         */
        public boolean isOutbound() {
            return outbound;
        }

        /**
         * Specifies which thread the message pertains to. Many messages will
         * pertain to a specific global thread (e.g. the message that implements
         * <code>DistributedCommunicator#runRemotely</code> will cause the
         * calling thread to be suspended on the caller's JVM while its
         * counterpart runs on the recipient's JVM). Messages that run on the
         * same thread as each other will return the same <code>GlobalID</code>
         * object from this method.
         * <p>
         * Some messages are handled asynchronously, in the background on a
         * thread of their own. In such cases, you will likely see a thread ID
         * from this method that was never used before and is never used again.
         * <p>
         * Some messages can be handled very quickly, by the recipient's message
         * handling loop, and thus are not associated with a specific thread. In
         * such cases, this method will return <code>null</code>.
         *
         * @return A thread ID for the thread. This is an opaque object, but
         * supports being compared with <code>.equals()</code> and converted to
         * a string with <code>.toString()</code>.
         */
        public Object getThreadID() {
            return threadID;
        }

        /**
         * Returns the number of bytes of data that were sent as part of the
         * message. This does <i>not</i> include: the message address; or any
         * bytes needed to describe non-copiable objects to which references are
         * being sent as part of the message.
         *
         * @return The number of bytes in the copiable-data portion of the
         * message.
         */
        public int getCopiableDataBytes() {
            return bytes;
        }

        /**
         * Returns the number of non-copiable objects to which references were
         * sent as part of the message. In other words, these are objects that
         * were not directly mentioned in the message due to being non-copiable,
         * but which are referenced in the message; if the objects do not happen
         * to exist on the recipient already, they will be referenced on there
         * by a long reference when they arrive.
         *
         * @return The number of bytes in the noncopiable-reference portion of
         * the message.
         */
        public int getNoncopiableReferenceCount() {
            return objs;
        }

        /**
         * Gets the amount of time it took to marshal or unmarshal the message.
         * If this information is about the sending of a message, the return
         * value will explain how long the body of the message took to encode
         * (not counting any time spent actually sending the message, or in
         * producing the message headers/address). If this information is about
         * the receipt of a message, the return value will explain how long the
         * body of the message took to decode (again, not counting any time
         * spent actually receiving the message or decoding its address).
         *
         * @return The time, in nanoseconds.
         */
        public long getMarshalTimeNanos() {
            return marshalTimeNanos;
        }

        /**
         * Gets the rate at which this message is expected to repeat itself.
         * Some messages are sent repeatedly to inform a remote communicator
         * that a particular condition still exists (e.g. that an object is
         * still allocated). These messages need to be treated differently by
         * profilers, because the quantity of such messages depends on how fast
         * or slow the system you're running on is (the slower the system, the
         * longer such a condition will persist, and therefore the more messages
         * need to be sent).
         *
         * @return The approximate interval before this message will be sent
         * again if the situation it describes still persists; or
         * <code>null</code>, if the message is not expected to be repeated.
         */
        public Duration getRepeatRate() {
            return message.periodic();
        }

        /**
         * Creates a new <code>MessageInfo</code> object from its individual
         * fields.
         *
         * @param message The message that was sent or received.
         * @param sender The communication address that sent the message.
         * @param recipient The communication address that received the message.
         * @param outbound <code>true</code> if it's the sender of the message
         * that logged the message to the debug monitor.
         * @param threadID The thread ID to which the message pertains.
         * @param bytes The number of bytes in the copiable part of the message
         * (not counting the address).
         * @param objs The number of objects in the noncopiable part of the
         * message.
         * @param marshalTime The length of time the message took to marshal or
         * unmarshal, in nanoseconds.
         */
        MessageInfo(DistributedMessage message, CommunicationAddress sender,
                Communicable recipient, boolean outbound,
                GlobalID threadID, int bytes, int objs, long marshalTime) {
            this.message = message;
            this.sender = sender;
            this.recipient = recipient;
            this.outbound = outbound;
            this.threadID = threadID;
            this.bytes = bytes;
            this.objs = objs;
            this.marshalTimeNanos = marshalTime;
        }

        /**
         * Produces a human-readable summary of this message-sending event.
         *
         * @return A human-readable string describing some of the more important
         * pieces of information within this <code>MessageInfo</code> object.
         * Not all its fields will be represented in the return value.
         */
        @Override
        public String toString() {
            return "[" + message.toString() + "]"
                    + (outbound ? " > " + recipient : " < " + sender)
                    + " on thread "
                    + (threadID == null ? "(communicator)" : threadID);
        }
    }
}
