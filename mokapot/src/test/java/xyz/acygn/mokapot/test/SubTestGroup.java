package xyz.acygn.mokapot.test;

/**
 * A test group made out of a number of smaller test groups. This contains the
 * functionality common to <code>ParallelTests</code> and
 * <code>SerialTests</code>.
 *
 * @author Alex Smith
 */
public abstract class SubTestGroup extends TestGroup {

    /**
     * Creates a group of tests, out of smaller test groups.
     *
     * @param testGroupName The name of the group of tests.
     * @param subTests The subtests to run.
     */
    public SubTestGroup(String testGroupName, TestGroup... subTests) {
        super(getInnerTestCount(subTests), testGroupName);
        this.subTests = subTests;
    }

    /**
     * Counts the total number of individual tests in the given test groups.
     * Split out from the constructor so that its <code>super</code> call can
     * appear as part of the first statement.
     *
     * @param subTests The test groups to count.
     * @return The total number of individual tests in those test groups.
     */
    private static int getInnerTestCount(TestGroup[] subTests) {
        int testCount = 0;
        for (TestGroup subTest : subTests) {
            testCount += subTest.getTestCount();
        }
        return testCount;
    }

    /**
     * The smaller groups that make up this larger group.
     */
    protected final TestGroup[] subTests;
}
