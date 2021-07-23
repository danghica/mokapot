package xyz.acygn.mokapot.markers;

/**
 * An Error thrown as a response to an exception that occurred as a result of
 * attempting distributed communications. (This is an Error, because it's
 * analogous to a StackOverflowError or an OutOfMemoryError; the call stack
 * can't get any larger for reasons unrelated to the program that's running.)
 *
 * @author Alex Smith
 */
public class DistributedError extends Error {

    /**
     * Explicit serialisation version, as is required for a serialisable class
     * to be compatible between machines. The number was originally generated
     * randomly, and should be changed whenever the class's fields are changed
     * in an incompatible way.
     *
     * @see java.io.Serializable
     */
    private static final long serialVersionUID = 0x7c0fef8445386709L;

    /**
     * Creates a new DistributedError with the given cause and context.
     *
     * @param cause The low-level exception that triggered this
     * DistributedError.
     * @param context The situation we were in when the exception occurred.
     */
    public DistributedError(Throwable cause, String context) {
        super(context, cause);
    }

    /**
     * Returns the original, low-level exception that caused distributed
     * communications to fail.
     *
     * @return The cause of this error.
     */
    @Override
    public synchronized Throwable getCause() {
        return super.getCause();
    }
}
