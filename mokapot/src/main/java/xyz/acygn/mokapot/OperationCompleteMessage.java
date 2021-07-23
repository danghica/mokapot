package xyz.acygn.mokapot;

import static java.lang.Thread.currentThread;
import java.time.Duration;
import static java.util.Arrays.copyOfRange;
import static xyz.acygn.mokapot.Authorisations.UNRESTRICTED;
import xyz.acygn.mokapot.skeletons.Standin;

/**
 * A distributed message sent when a synchronous operation completes. This is
 * used to prevent the call stack accumulating indefinitely (like it would in
 * continuation-passing style); the convention is that receiving an
 * OperationCompleteMessage causes the method that was waiting for an operation
 * to complete to return.
 *
 * @author Alex Smith
 */
class OperationCompleteMessage extends AsynchronousMessage {

    /**
     * The value returned by the operation. This should not have been explicitly
     * marshalled; it will be marshalled along with the
     * <code>OperationCompleteMessage</code> itself.
     */
    private final Object retval;

    /**
     * Whether <code>retval</code> is a "regular" sort of return value, or an
     * exception.
     */
    private final boolean isException;

    /**
     * Whether the thread being returned from was in interrupted status at the
     * time.
     */
    private final boolean isInterrupted;

    /**
     * Whether the thread being returned from is newly being kept alive, and
     * thus requires a thread projection tracker to handle its shutdown.
     */
    private final boolean requestTracking;

    /**
     * Creates a new OperationCompleteMessage, containing the specified return
     * value. This can either be a regular return value, or an exception. Note
     * that the message must be created by the thread which is about to return,
     * and the thread must be running as a consequence of a synchronous
     * DistributedMessage operation.
     * <p>
     * The return value should not have been explicitly marshalled, and will be
     * marshalled along with the <code>OperationCompleteMessage</code> itself.
     *
     * @param retval The return value of the operation, or the exception thrown
     * by the operation.
     * @param isException Whether or not the method threw an exception. (This is
     * used to interpret the value of <code>retval</code>.)
     * @param requestTracking Whether the system we're returning to needs to set
     * up a new thread projection tracker. This is necessary in cases where
     * there is residual thread-local state from the operation, as the same
     * thread will potentially need to be reused. Exception: if a thread
     * projection tracker already exists, there will be no need to create a new
     * one.
     * @throws ClassCastException if <code>isException</code> but
     * <code>retval</code> is not <code>Throwable</code>
     */
    OperationCompleteMessage(Object retval, boolean isException,
            boolean requestTracking) throws ClassCastException {
        this.isException = isException;
        if (isException) {
            Throwable exn = (Throwable) retval;

            /* If we threw an exception, fill in its stack trace before sending
               it, as otherwise the stack trace gets lost. For whatever reason,
               fillInStackTrace doesn't work, but getStackTrace does.

               We also handle a special case: if exn is a StackOverflowError, we
               might end up overflowing the stack again trying to serialize it.
               So make it a bit less heavily nested. */
            Throwable t = exn;
            while (t != null && (!(t instanceof Standin)
                    || ((Standin) t).getStorage(UNRESTRICTED)
                            .certainlyLocal())) {
                t.getStackTrace();
                t = t.getCause();
                if (t instanceof StackOverflowError) {
                    StackTraceElement[] stel = t.getStackTrace();
                    stel = copyOfRange(stel, 0, 20);
                    t.setStackTrace(stel);
                }
            }

            this.retval = exn;
        } else {
            this.retval = retval;
        }

        isInterrupted = Thread.interrupted();
        this.requestTracking = requestTracking;
    }

    /**
     * Whether this message is unimportant. It's unimportant if it's an attempt
     * to report that a thread has finished (because if the receiving thread
     * doesn't exist, we don't care that it's finished).
     *
     * @return <code>true</code> if we're returning a
     * <code>ThreadHasEnded</code> object
     */
    @Override
    public boolean isUnimportant() {
        return !isException
                && retval instanceof ThreadProjectionTracker.ThreadHasEnded;
    }

    /**
     * Normally throws an exception. Unlike other distributed messages, the
     * effect of the message is handled by the distributed communicator
     * directly, in order to ensure that the stack unwinds correctly. This means
     * that the message itself is just a marker.
     * <p>
     * Because the message cannot process itself (because the caller needs to
     * unwind the stack appropriately), it generally throws an exception if run.
     * <p>
     * However, there's one potential case in which this call can legitimately
     * happen: if two communicators both simultaneously decide a thread is
     * unnecessary. One communicator will shut down the thread, the other will
     * recreate it to tell it that it needs to be shut down again, and the new
     * thread will therefore end up with an
     * <code>OperationCompleteMessage</code> as is first message. In this case,
     * obviously, the correct thing to do is to allow the the thread to exit,
     * which we can do simply via doing nothing (thus allowing the thread to
     * exit naturally).
     *
     * @throws IllegalStateException Always, unless this is being used to send a
     * "thread has ended" signal
     */
    @Override
    public void process(DistributedCommunicator communicator)
            throws IllegalStateException {
        if (isUnimportant()) {
            return;
        }

        throw new IllegalStateException(
                "this message should not have been processed directly");
    }

    /**
     * Returns the return value of the synchronous distributed communication
     * that was completed by this message.
     * <p>
     * This method will also restore the interruption status of the current
     * thread from the time the message object was created (thus allowing an
     * interruption to propagate "backwards" from a callee that does not handle
     * it to the caller).
     * <p>
     * This method will also set up a thread projection tracker, if one is
     * required.
     *
     * @param communicator The distributed communicator that just received this
     * message.
     * @param address The addressing information on the envelope via which this
     * message just arrived.
     * @return The return value of the operation.
     * @throws Throwable If the operation did not complete normally, but rather
     * threw an exception, this method throws the same exception
     */
    Object getReturnValue(DistributedCommunicator communicator,
            MessageAddress address) throws Throwable {
        if (isInterrupted) {
            currentThread().interrupt();
        }
        if (requestTracking) {
            ThreadProjectionTracker tpt = communicator.getTPTForCurrentThread();
            if (tpt != null) {
                tpt.addProjection(address.getSenderAddress());
            } else {
                communicator.sendMessageAsync(
                        new OperationCompleteMessage(
                                new ThreadProjectionTracker.ThreadHasEnded(),
                                false, false),
                        address.getThreadID(), address.getSenderAddress());
            }
        }
        if (isException) {
            throw (Throwable) retval;
        }
        return retval;
    }

    /**
     * Produces a human-readable string describing this message.
     *
     * @return A human-readable version of the message.
     */
    @Override
    public String toString() {
        String tail = DistributedMessage.safeStringify(retval);
        if (isInterrupted) {
            tail += ", and interrupt";
        }
        if (requestTracking) {
            tail += ", requesting tracking";
        }
        if (isException) {
            return "throw exception " + tail;
        } else {
            return "return value " + tail;
        }
    }

    /**
     * Always returns false. This method needs to be sent to a specific thread
     * due to its unwinding behaviour.
     *
     * @return <code>false</code>.
     */
    @Override
    public boolean lightweightSafe() {
        return false;
    }

    @Override
    public Duration periodic() {
        return null;
    }
}
