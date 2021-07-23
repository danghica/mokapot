package xyz.acygn.mokapot;

/**
 * A distributed message that does not have a reply. Used to avoid the need to
 * repetitively specify no handling of message replies.
 * <p>
 * This class can be used both for lightweight messages (those which are decoded
 * and executed immediately on the acceptor thread), and heavyweight messages
 * (those which create a new thread to decode and execute them), as long as no
 * reply is sent in either case. (Lightweight messages never have replies
 * anyway; heavyweight messages might or might not have a reply.)
 *
 * @author Alex Smith
 */
abstract class AsynchronousMessage implements DistributedMessage {

    @Override
    public void process(MessageEnvelope envelope,
            DistributedCommunicator communicator, boolean requestTracking) {
        if (requestTracking) {
            throw new IllegalArgumentException(
                    "Asynchronous messages have no replies, "
                    + "cannot request tracking");
        }
        process(communicator);
    }

    /**
     * Performs the operations required by this distributed message.
     *
     * @param communicator The distributed communicator to which the message was
     * sent.
     */
    protected abstract void process(DistributedCommunicator communicator);

    /**
     * Always returns false. Asynchronous messages have no replies by
     * definition.
     *
     * @return <code>false</code>.
     */
    @Override
    public boolean sendsReply() {
        return false;
    }
}
