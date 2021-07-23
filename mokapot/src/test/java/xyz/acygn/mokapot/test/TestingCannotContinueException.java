package xyz.acygn.mokapot.test;

/**
 * An exception thrown when a problem that prevents all further testing has been
 * discovered.
 *
 * @author Alex Smith
 */
public class TestingCannotContinueException extends RuntimeException {

    /**
     * Explicit serialisation version, as is required for a serialisable class
     * to be compatible between machines. The number was originally generated
     * randomly, and should be changed whenever the class's fields are changed
     * in an incompatible way.
     *
     * @see java.io.Serializable
     */
    private static final long serialVersionUID = 0x544e55d41975d719L;

    /**
     * Whether we've printed the "Bail out!\n" TAP line that specifies that
     * further testing is impossible.
     */
    private boolean bailedOut = false;

    /**
     * Creates a new TestingCannotContinueException(). Throwing the exception
     * from a test routine will cause the test runners to exit early, and
     * signify to the test driver that further testing is impossible.
     */
    public TestingCannotContinueException() {
    }

    /**
     * Prints a TAP directive to bail out of the test, if we have not already
     * done so.
     */
    void bailOut() {
        if (bailedOut) {
            return;
        }

        System.out.println("Bail out!");
        bailedOut = true;
    }
}
