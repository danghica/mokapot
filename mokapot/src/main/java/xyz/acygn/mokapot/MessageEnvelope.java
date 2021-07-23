package xyz.acygn.mokapot;

import java.time.temporal.ChronoUnit;
import xyz.acygn.mokapot.markers.Copiable;
import xyz.acygn.mokapot.util.Lazy;
import xyz.acygn.mokapot.util.Stopwatch;

/**
 * A marshalled <code>DistributedMessage</code>, together with information on
 * the intended recipient of the message. The purpose of this class is to allow
 * the routing information on the message (which can be decoded quickly) to be
 * decoded separately on the distributed communicator thread, allowing the main
 * work of decoding the message to be carried out by the recipient of the
 * message.
 * <p>
 * This class also serves a secondary purpose: error handling for the case where
 * the message isn't correctly understood. Messages (even synchronous messages)
 * are sent asynchronously (to avoid the need to set up a stream to receive the
 * response), so if the caller is expecting a response, it needs to be sent one
 * whether there's an error or not.
 *
 * @author Alex Smith
 */
/* Note: this class must be <code>final</code> and <code>Copiable</code> so that
   <code>DistributedSendConnection</code> can reliably pick the correct
   class knowledge to serialise it. */
final class MessageEnvelope implements Copiable {

    /**
     * The message's routing information. Includes information about the thread
     * on which the message should be run, and the location of the message's
     * reply (if any).
     */
    private final MessageAddress address;

    /**
     * The marshalled form of the message being sent.
     */
    private final MarshalledDescriptionStandin marshalledMessage;

    /**
     * Returns the return address of this envelope.
     *
     * @return The location to which a reply to this message should be sent, and
     * the thread that it should run on and that replies should be sent to.
     */
    public MessageAddress getAddress() {
        return address;
    }

    /**
     * Unmarshals the message inside the envelope, then processes it. Should be
     * called exactly once (because any marshalled value should be unmarshalled
     * exactly once); one exception is if the message expected to be contained
     * in the envelope is an <code>OperationCompleteMessage</code>, in which
     * case <code>getOCMReturnValue</code> (which also unmarshals the message)
     * should be called instead. Note that this should be run only on the thread
     * suggested in the message's return address (unless it does not specify a
     * thread), and (unless the message's return address indicates that it's
     * intended to run in a lightweight fashion) can be highly time-consuming,
     * can block, etc., and thus is unsafe to run on the same thread that
     * accepts messages over the network.
     * <p>
     * If the message requests a reply, and something goes wrong trying to
     * process it, the resulting exception will be sent as a reply rather than
     * being rethrown. If the message does not request a reply, any exceptions
     * will be sent to the async exception handler. In other words, this doesn't
     * produce an exception under any circumstances.
     *
     * @param monitor The debug monitor to record the received message on. Can
     * be <code>null</code> if this feature is not required.
     * @param communicator The distributed communicator to which the message was
     * addressed. (The caller must ensure that the message is being handled by
     * the right communicator; this parameter would normally be the caller's
     * <code>this</code>.)
     * @param requestTracking Whether, in the reply message, we should request
     * that the system we're returning to sets up a thread projection tracker
     * for the purpose.
     */
    @SuppressWarnings("UseSpecificCatch")
    public void processMessage(DebugMonitor monitor,
            DistributedCommunicator communicator, boolean requestTracking) {
        if (requestTracking && !address.sendsReply()) {
            throw new IllegalArgumentException("requesting a thread tracker"
                    + " in a reply that doesn't exist");
        }
        try {
            Stopwatch timer = new Stopwatch(Lazy.TIME_BASE.get()).start();
            marshalledMessage.setUnmarshalCommunicator(communicator);
            marshalledMessage.setTemporaryOnRecipient(
                    address.isTemporaryOnRecipient());
            DistributedMessage message
                    = (DistributedMessage) Marshalling.rCAOStatic(
                            marshalledMessage, null, null);

            if (monitor != null) {
                monitor.newMessage(asMessageInfo(communicator,
                        timer.time(ChronoUnit.NANOS), message, null));
            }
            message.process(this, communicator, requestTracking);
        } catch (Throwable ex) {
            if (address.sendsReply()) {
                address.sendReply(new OperationCompleteMessage(
                        ex, true, requestTracking), communicator);
            } else {
                communicator.asyncExceptionHandler(ex);
            }
        }
    }

    /**
     * Unmarshal the message inside the envelope, then extracts the return value
     * it contains. Message replies (<code>OperationCompleteMessage</code>) have
     * to be treated differently from other messages to prevent the stack
     * growing indefinitely. As such, if an
     * <code>OperationCompleteMessage</code> is received (which can be
     * determined from the fact that it was sent as a followup to a message, but
     * was nonetheless marked as asynchronous), this alternate method of
     * unmarshalling the envelope should be used in preference to
     * <code>processMessage</code> (which cannot process the message because the
     * behaviour of an <code>OperationCompleteMessage</code> is to remove
     * elements from the call stack, something that can only be done by the
     * caller).
     *
     * @param monitor The debug monitor to record the received message on. Can
     * be <code>null</code> if this feature is not required.
     * @param communicator The distributed communicator to which the envelope
     * was addressed.
     * @return The return value of the message inside the envelope.
     * @throws Throwable If the message indicates the presence of an exception
     * rather than a successful completion of the operation, this method throws
     * that exception
     */
    Object getOCMReturnValue(DebugMonitor monitor,
            DistributedCommunicator communicator) throws Throwable {
        Stopwatch timer = new Stopwatch(Lazy.TIME_BASE.get()).start();
        marshalledMessage.setUnmarshalCommunicator(communicator);
        marshalledMessage.setTemporaryOnRecipient(
                address.isTemporaryOnRecipient());
        DistributedMessage message
                = (DistributedMessage) Marshalling.rCAOStatic(
                        marshalledMessage, null, null);
        if (monitor != null) {
            monitor.newMessage(asMessageInfo(
                    communicator, timer.time(ChronoUnit.NANOS), message, null));
        }

        return ((OperationCompleteMessage) message).getReturnValue(
                communicator, getAddress());
    }

    /**
     * Marshals the given message, then creates a new envelope for the purpose
     * of sending it. Note that the envelope and message must have matching
     * ideas as to whether the message expects a reply.
     *
     * @param address Information about what thread to run the message on and
     * where to send any reply.
     * @param message The message that will be sent.
     * @param sourceCommunicator The communicator that is sending the message.
     * @param targetSystem The system that the message will be sent to. Note
     * that this information is not stored on the envelope, and used only for
     * the purpose of marshalling the message. Can be <code>null</code> if the
     * message is deeply copiable.
     */
    MessageEnvelope(MessageAddress address, DistributedMessage message,
            DistributedCommunicator sourceCommunicator,
            CommunicationAddress targetSystem) {
        address.verifyCompatibility(message);
        this.address = address;
        this.marshalledMessage = sourceCommunicator.getMarshalling()
                .describeAndMarshal(message, targetSystem);
    }

    /**
     * Produces a string representation of this envelope.
     *
     * @return A string representation of the envelope.
     */
    @Override
    public String toString() {
        return "[" + marshalledMessage.toString() + "]" + address.inDebugOutput();
    }

    /**
     * Produces information suitable for a debug monitor that describes this
     * message.
     *
     * @param communicator The distributed communicator that is processing the
     * message (the sender if <code>sentTo</code> is valid, the recipient if
     * <code>sentTo</code> is <code>null</code>).
     * @param marshalTimeNanos The length of time spent marshalling or
     * unmarshalling the message, in nanoseconds.
     * @param message The message that was sent or received.
     * @param sentTo If just about to send the message, the system it's just
     * about to be sent to; if the message was just received, <code>null</code>.
     *
     * @return A <code>MessageInfo</code> describing the message.
     */
    DebugMonitor.MessageInfo asMessageInfo(
            DistributedCommunicator communicator, long marshalTimeNanos,
            DistributedMessage message, Communicable sentTo) {
        return new DebugMonitor.MessageInfo(message,
                sentTo != null ? communicator.getMyAddress()
                        : address.getSenderAddress(),
                sentTo != null ? sentTo : communicator.getMyAddress(),
                sentTo != null, address.getThreadID(),
                marshalledMessage.getWrittenLength(),
                marshalledMessage.getMarshalledNoncopiableObjects().length,
                marshalTimeNanos);
    }
}
