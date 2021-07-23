package xyz.acygn.mokapot.test;

/**
 * A <code>TestGroup</code> implementation that runs a given function. This can
 * be used as a simple way to bundle up tests that don't need to set up the
 * environment in any particular way into a test group.
 *
 * @author Alex Smith
 */
public class SimpleTest extends TestGroup {

    /**
     * The code to run the tests.
     */
    private final Tests tests;

    /**
     * Creates a new test group that simply runs specific code. The specified
     * code should report on success or failure of the tests via calling methods
     * on a provided <code>TestGroup</code> object.
     *
     * @param testCount The number of <code>ok</code> (or equivalent) calls that
     * <code>tests</code> will make.
     * @param testGroupName The name via which these tests should be reported in
     * TAP debug output.
     * @param tests The code to run the tests.
     */
    public SimpleTest(int testCount, String testGroupName, Tests tests) {
        super(testCount, testGroupName);
        this.tests = tests;
    }

    @Override
    protected void testImplementation() throws Exception {
        tests.runTests(this);
    }

    /**
     * The code that forms the body of a <code>SimpleTest</code> test group.
     */
    @FunctionalInterface
    public interface Tests {

        /**
         * Runs the tests that are part of the test group.
         *
         * @param group The <code>TestGroup</code> object via which success or
         * failure of the tests are reported (via <code>ok</code> and related
         * methods).
         * @throws Exception If something goes wrong during the testing; this
         * will add one test failure, and cause any tests that failed to run due
         * to the exception to be skipped
         */
        void runTests(TestGroup group) throws Exception;
    }
}
