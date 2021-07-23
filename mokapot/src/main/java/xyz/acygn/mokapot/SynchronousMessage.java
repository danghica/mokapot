package xyz.acygn.mokapot;

/**
 * A distributed message that communicates information back to the caller when
 * complete. This is intended to block the corresponding thread in the caller
 * until the message has completed, i.e. it gives synchronous behaviour,
 * treating the local threads on the caller and recipient as if they were part
 * of a single global thread that crosses between systems.
 *
 * @author Alex Smith
 * @param <T> The return type of the message.
 */
abstract class SynchronousMessage<T> implements DistributedMessage {

    /**
     * The envelope in which this message was sent. Could be <code>null</code>,
     * if the message has not yet been sent or if processing of the message has
     * not yet started.
     */
    private transient MessageEnvelope sentInEnvelope;

    /**
     * The communicator to which this message was sent.
     */
    private transient DistributedCommunicator communicator;

    /**
     * Returns the communicator to which this message was sent. This is intended
     * for use by <code>calculateReply()</code> to know which communicator it
     * should use (in those cases where more than one exists).
     *
     * @return The communicator.
     */
    protected DistributedCommunicator getCommunicator() {
        return communicator;
    }

    /**
     * Processes this message, returning a reply to send back to the caller. If
     * this message throws an exception, it will be returned to the caller in
     * lieu of a reply.
     * <p>
     * In cases where, for synchrony reasons or to verify the lack of an
     * exception, a reply is needed but its content is not relevant,
     * <code>null</code> will serve as a suitable default reply. (Note that in
     * general there are no restrictions on the reply at all, other than that it
     * must belong to a marshallable type and be storable in a variable of type
     * <code>T</code>; it's perfectly viable to assign some meaning to a
     * <code>null</code> return.)
     *
     * @return The return value that should be sent back to the caller.
     * @throws Throwable If this message throws an exception, the exception will
     * be communicated back to the caller (and most likely thrown there)
     */
    protected abstract T calculateReply() throws Throwable;

    /**
     * Runs code specific to this message, then sends the corresponding reply
     * back to the original system.
     *
     * @param envelope The envelope in which the message was sent.
     * @param communicator The distribute communicator to which the message was
     * sent.
     * @param requestTracking Whether the reply should include a request to set
     * up a new thread projection tracker.
     */
    @Override
    public void process(MessageEnvelope envelope,
            DistributedCommunicator communicator, boolean requestTracking) {
        Object returnValue;
        boolean returnValueIsException;

        sentInEnvelope = envelope;
        this.communicator = communicator;

        try {
            returnValue = calculateReply();
            returnValueIsException = false;
        } catch (Throwable ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
            returnValue = ex;
            returnValueIsException = true;
        }

        envelope.getAddress().sendReply(new OperationCompleteMessage(
                returnValue, returnValueIsException, requestTracking),
                getCommunicator());
    }

    /**
     * Returns the envelope in which this message was sent. The envelope might
     * not exist yet (e.g. the message hasn't been sent yet) or might not be
     * known (e.g. processing of the message has not yet started), in which case
     * this method returns <code>null</code>.
     * <p>
     * This method is intended to be called only from
     * <code>calculateReply</code>, during which time the envelope in question
     * will always be valid.
     *
     * @return The envelope in which the message was sent.
     */
    protected MessageEnvelope getSentInEnvelope() {
        return sentInEnvelope;
    }

    /**
     * Always returns true. Messages implemented using this class send a reply
     * by definition.
     *
     * @return <code>true</code>.
     */
    @Override
    public boolean sendsReply() {
        return true;
    }

    /**
     * Always returns false. A message that requires a reply cannot be
     * lightweight.
     *
     * @return <code>false</code>.
     */
    @Override
    public boolean lightweightSafe() {
        return false;
    }

    /**
     * A marker interface specifying that references within the message must not
     * be retained by the recipient; processing the message may look at the
     * references, but copies of them may not exist past the time the message
     * finishes running.
     */
    public interface BorrowOnly {
    }
}
