package xyz.acygn.mokapot;

import java.time.Duration;

/**
 * A message requesting the interruption of a thread. Note that this runs on a
 * different thread (not the thread being interrupted).
 *
 * @author Alex Smith
 */
class InterruptMessage extends SynchronousMessage<Boolean> {

    /**
     * The global ID of the thread to interrupt.
     */
    private final GlobalID threadID;

    /**
     * Creates a new message to interrupt a thread.
     *
     * @param threadID The thread to interrupt.
     */
    InterruptMessage(GlobalID threadID) {
        this.threadID = threadID;
    }

    /**
     * Attempts to interrupt the thread <code>threadID</code>. This might not
     * succeed (most likely because the thread returned and is no longer running
     * on this system).
     *
     * @return <code>true</code> if the attempt to interrupt the thread
     * succeeded, <code>false</code> if an interruption was not attempted.
     */
    @Override
    protected Boolean calculateReply() {
        return getCommunicator().interruptThreadById(threadID);
    }

    @Override
    public Duration periodic() {
        return null;
    }
}
