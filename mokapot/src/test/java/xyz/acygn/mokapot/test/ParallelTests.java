package xyz.acygn.mokapot.test;

/**
 * Runs multiple smaller tests in parallel. The main work of this class is
 * managing the resulting changes to the test sequence.
 * <p>
 * The main reason to use this is for testing client/server situations, and
 * other situations where two things need to run at the same time (interacting
 * with each other) and you want to test them both; using it merely for
 * efficiency is not recommended, as if there is a failure in the testbench, it
 * will be harder to determine what happened than if the tests ran in sequence.
 *
 * @author Alex Smith
 */
public class ParallelTests extends SubTestGroup {

    /**
     * Creates a group of tests, out of smaller groups that are run in parallel.
     *
     * @param testGroupName The name of the group of tests.
     * @param subTests The subtests to run.
     */
    public ParallelTests(String testGroupName, TestGroup... subTests) {
        super(testGroupName, subTests);
    }

    /**
     * Starts each subtest running in parallel, then waits for them to complete.
     *
     * @throws java.lang.InterruptedException If the main thread is interrupted
     * while waiting for the test threads to complete
     */
    @Override
    protected void testImplementation() throws InterruptedException {
        @SuppressWarnings("unchecked")
        Thread[] testThreads = new Thread[subTests.length];

        /* Create and setup a thread for each subtest... */
        for (int i = 0; i < subTests.length; i++) {
            /* Java can only close over final variables, so do it like this: */
            final int j = i;
            testThreads[j] = new Thread() {
                @Override
                public void run() {
                    subTests[j].runTest(-1);
                }
            };
            indirectOk(subTests[j].getTestCount());
            testThreads[j].start();
        }

        /* ...then wait for them all to complete. */
        for (Thread t : testThreads) {
            t.join();
        }
    }
}
