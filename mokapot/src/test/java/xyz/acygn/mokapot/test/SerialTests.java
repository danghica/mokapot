package xyz.acygn.mokapot.test;

/**
 * Runs multiple smaller tests in series. This class adjusts the test sequence
 * accordingly. Combining a number of test groups into a single group not only
 * makes it more convenient to run, it also allows them to be planned correctly
 * within a single program.
 * <p>
 * In general, if you have the choice of running tests in parallel or in series,
 * you should run them in series; it prevents the tests interfering with each
 * other, and produces a more detailed test sequence.
 *
 * @author Alex Smith
 */
public class SerialTests extends SubTestGroup {

    /**
     * Creates a group of tests, out of smaller groups that are run in series.
     *
     * @param testGroupName The name of the group of tests.
     * @param subTests The subtests to run, in the sequence that they run.
     */
    public SerialTests(String testGroupName, TestGroup... subTests) {
        super(testGroupName, subTests);
    }

    /**
     * Starts each subtest running in sequence, waiting for each to complete
     * before running the next.
     */
    @Override
    protected void testImplementation() {
        for (TestGroup group : subTests) {
            group.runTest(indirectOk(group.getTestCount()));
        }
    }
}
