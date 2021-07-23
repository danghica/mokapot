package xyz.acygn.mokapot;

import static java.util.Objects.requireNonNull;
import static xyz.acygn.mokapot.GlobalID.getCurrentThreadID;
import xyz.acygn.mokapot.markers.Copiable;
import xyz.acygn.mokapot.markers.DistributedError;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper;

/**
 * Information about where a message should be handled and what should be done
 * with the reply. This includes all of the following: synchrony information
 * (i.e. is there a reply?), routing information (i.e. how do we connect to the
 * virtual machine that should handle the message's reply?), sender information
 * (i.e. who sent the message?), and threading information (which thread should
 * run the message, and/or is blocking on the message?).
 * <p>
 * Note that whether a message needs a reply or not is a property of the
 * message; thus, the message and its address each have an independent opinion
 * on this subject. The two need to match, meaning not all messages can be
 * matched with all addresses, in a form of denormalisation. This allows a
 * message address to be decoded separately from the message itself, meaning
 * that the multithreading logic for address decoding can be considerably
 * simplified.
 *
 * @author Alex Smith
 */
final class MessageAddress implements Copiable {

    /**
     * The thread ID to run the message on.
     *
     * @see #getThreadID()
     */
    private final GlobalID threadID;

    /**
     * The sender of the message.
     *
     * @see #getReturnAddress()
     */
    private final CommunicationAddress senderAddress;

    /**
     * Whether the message requires a reply. If the message requires a reply,
     * the return address is the sender address. Otherwise, there is no sender
     * address.
     */
    private final boolean requiresReply;

    /**
     * Whether the recipient discards all references that it's given in the
     * message. If <code>true</code>, references within the message won't be
     * stored persistently on the recipient; they'll only live while the message
     * is being processed.
     */
    private final boolean temporaryOnRecipient;

    /**
     * Whether the message is unimportant. Unimportant messages are simply
     * discarded if they can't be decoded due to communicator shutdown. They are
     * also discarded if they refer to a thread that isn't already running on
     * the recipient.
     */
    private final boolean isUnimportant;

    /**
     * Creates a message address with typical default settings for synchronous
     * communication. The message will be sent on the global extension of the
     * current thread, and request a reply be sent to the given address; there
     * are no guarantees about what the recipient will do with pointers within
     * the message.
     *
     * @param returnAddress The communication address to which the reply should
     * be sent. (This should be the address of the communicator that constructs
     * the message being addressed.)
     * @param temporaryOnRecipient Information about what the recipient is
     * allowed to do with references within the message; if this is
     * <code>true</code>, these references may be borrowed but copies of them
     * may not be kept.
     * @param isUnimportant Whether the recipient is allowed to discard the
     * message because it's shutting down or because the named thread is not
     * alive.
     */
    MessageAddress(CommunicationAddress returnAddress,
            boolean temporaryOnRecipient, boolean isUnimportant) {
        threadID = getCurrentThreadID(returnAddress);
        senderAddress = returnAddress;
        requiresReply = true;
        this.temporaryOnRecipient = temporaryOnRecipient;
        this.isUnimportant = isUnimportant;
    }

    /**
     * Creates a new message address. If the message requires a reply, then the
     * reply address will be set to the address of the current distributed
     * communicator.
     *
     * @param threadID An identifier indicating the (global, cross-system)
     * thread to run the message on, which is also the thread that will receive
     * the reply. Cannot be <code>null</code> if the message requires a reply.
     * @param requiresReply Whether the message address should include a return
     * address.
     * @param senderAddress The communication address of the message sender. If
     * the message requires a reply, this will also be used as the return
     * address.
     * @param temporaryOnRecipient Information about what the recipient is
     * allowed to do with references within the messsage; if this is
     * <code>true</code>, these references may be borrowed but copies of them
     * may not be kept.
     * @param isUnimportant Whether the recipient is allowed to discard the
     * message because it's shutting down or because the named thread is not
     * alive.
     *
     * @see #getThreadID()
     * @see #getReturnAddress()
     */
    MessageAddress(GlobalID threadID, boolean requiresReply,
            CommunicationAddress senderAddress, boolean temporaryOnRecipient,
            boolean isUnimportant) {
        if (requiresReply) {
            requireNonNull(threadID);
        }
        this.threadID = threadID;
        this.requiresReply = requiresReply;
        this.senderAddress = senderAddress;
        this.temporaryOnRecipient = temporaryOnRecipient;
        this.isUnimportant = isUnimportant;
    }

    /**
     * Returns the return address of a message. This is where the return value
     * is sent, and where any exceptions that happen executing the message are
     * sent. Will be <code>null</code> if no return value is expected (either
     * because the message is asynchronous, or because this message's purpose is
     * to convey a return value and thus doesn't need a re-return value of its
     * own).
     *
     * @return The return address component of this address.
     */
    public CommunicationAddress getReturnAddress() {
        return requiresReply ? senderAddress : null;
    }

    /**
     * Returns the address of the sender of the message. This will be the same
     * as the return address, if there is one. However, even asynchronous or
     * lightweight messages (which do not have return addresses) will have
     * senders.
     *
     * @return The sender address component of this address.
     */
    public CommunicationAddress getSenderAddress() {
        return senderAddress;
    }

    /**
     * Returns whether the references stored within the message are forbidden
     * from surviving the processing of the message.
     *
     * @return <code>true</code> if references within the message do not survive
     * after the message is processed.
     */
    public boolean isTemporaryOnRecipient() {
        return temporaryOnRecipient;
    }

    /**
     * Returns whether the address is for an unimportant message. (This
     * information is stored in the address, because a communicator that's
     * shutting down might not be able to decode the message body and might yet
     * need to know whether to drop the message or not.)
     *
     * @return <code>true</code> if the message is unimportant
     */
    public boolean isUnimportant() {
        return isUnimportant;
    }

    /**
     * Places the given reply into an envelope and sends it to the reply address
     * stated in this address.
     *
     * @param reply The reply to send.
     * @param communicator The distributed communicator that received the
     * original message and is now sending a reply to it.
     */
    public void sendReply(OperationCompleteMessage reply,
            DistributedCommunicator communicator) {
        try {
            communicator.
                    sendMessageAsync(reply, threadID, getReturnAddress());
        } catch (AutocloseableLockWrapper.CannotLockException ex) {
            /* This shouldn't happen; the function responsible for processing
               the message we're sending the reply to should be holding the
               communicator alive. */
            throw new DistributedError(ex, "sending OCM" + inDebugOutput());
        }
    }

    /**
     * Checks whether this reply address specifies that a reply be sent back to
     * the message's original sender.
     *
     * @return Whether a message using this address will send a reply.
     */
    public boolean sendsReply() {
        return requiresReply;
    }

    /**
     * Returns the thread on which to run a message. The thread need not exist
     * on the sender; if this is a fresh global ID, a new thread pool task will
     * be created to handle the message (although if the message has a reply,
     * this should typically be a thread that exists on the sender or else the
     * reply will need to be handled asynchronously on the sender). This can be
     * <code>null</code>, but only in circumstances where the message is
     * <i>very</i> lightweight (specifically: decodes and runs without blocking
     * in a short period of time, and without causing any further messages or
     * network activity; in particular, there can't be a return value).
     *
     * @return The thread ID component of this address.
     */
    public GlobalID getThreadID() {
        return threadID;
    }

    /**
     * Verifies that the information in this address is suitable for sending the
     * specified message.
     *
     * @param message The message to verify.
     * @throws IllegalArgumentException If the address is incompatible with the
     * message (e.g. the message expects a return address and the address
     * doesn't)
     */
    void verifyCompatibility(DistributedMessage message)
            throws IllegalArgumentException {
        if (message.sendsReply() && !requiresReply) {
            throw new IllegalArgumentException(
                    "Sending a message that needs a reply without a reply address");
        } else if (!message.sendsReply() && requiresReply) {
            throw new IllegalArgumentException(
                    "Providing a reply address for a message that does not use one");
        } else if (!message.lightweightSafe() && threadID == null) {
            throw new IllegalArgumentException(
                    "Sending a non-lightweight message with no thread ID");
        }
    }

    /**
     * Returns a suitable string to append to the debug output for a message
     * envelope, to describe where the envelope is addressed to.
     *
     * @return A string that can be appended to a message to produce debug
     * output for that message.
     */
    public String inDebugOutput() {
        if (threadID == null) {
            return ", lightweight";
        } else if (!requiresReply) {
            return ", async on thread " + threadID.toString();
        } else {
            return ", synchronous on thread " + threadID.toString();
        }
    }
}
