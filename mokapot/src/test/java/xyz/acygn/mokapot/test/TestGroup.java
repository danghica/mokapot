package xyz.acygn.mokapot.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;
import xyz.acygn.mokapot.util.StringifyUtils;

/**
 * Class that implements a test within the TAP testing protocol. This is a
 * partial class implementation that implements the testing-related methods, but
 * not the main functionality of the class.
 * <p>
 * For information on TAP, see <a href="http://testanything.org">
 * testanything.org</a>.
 *
 * @author Alex Smith
 */
public abstract class TestGroup {

    /**
     * The number of individual tests that exist within this test. Used to
     * declare a test count in advance, so that a crash of the testbench can be
     * detected. When building up a test out of subtests, the number of
     * individual tests from each subtest are added together to produce this
     * number.
     */
    private final int testCount;

    /**
     * The number of the currently running test. When we setup running, the
     * previous test or test driver will let us know what the number should be.
     * Before then, this is meaningless.
     * <p>
     * In some cases, the test sequence is not known (because multiple tests are
     * running in parallel). In that case, this is set to -1.
     */
    private int currentTestNumber = -1;

    /**
     * The number of times <code>ok</code> has been called in the current test
     * run. Used to ensure that the correct number of <code>ok()</code> calls
     * are made by <code>runTest</code> in case of exception.
     */
    private int okCount = 0;

    /**
     * Whether the next test is a todo test (expected to fail due to the thing
     * it's testing being unimplemented).
     */
    private boolean todo = false;

    /**
     * The number of test failures over the lifetime of the program. Some test
     * runners may want to use this as a cheap method of reporting "everything
     * is OK" or "something is broken", without the use of an external TAP
     * parser.
     */
    private static int failCount = 0;

    /**
     * The number of test successes over the lifetime of the program. This can
     * provide a cheap method to determine whether the test plan is correct.
     */
    private static int successCount = 0;

    /**
     * The number of tests skipped over the lifetime of the program. Used when
     * providing a summary of the test status to the user. Skipped tests are
     * successful tests, so contribute to both <code>skipCount</code> and
     * <code>successCount</code>.
     */
    private static int skipCount = 0;

    /**
     * The number of todo tests over the lifetime of the program that, as
     * expected, failed. Used when providing a summary of the test status to the
     * user. These tests are considered successful, thus contribute to
     * <code>successCount</code> as well.
     */
    private static int todoCount = 0;

    /**
     * The number of todo tests over the lifetime of the program that,
     * unexpectedly, succeeded. Used when providing a summary of the test status
     * to the user. These tests are considered failures, thus contribute to
     * <code>failCount</code> as well.
     */
    private static int unexpectedCount = 0;

    /**
     * The number of tests specified in the plan. This is used to detect
     * mistakes in the test plan.
     */
    private static int plannedTestCount = -1;

    /**
     * The name of the test group this object represents. It will be included as
     * part of the test name for each of its individual tests.
     */
    private final String testGroupName;

    /**
     * Returns the name of this test group.
     *
     * @return The name of the test group.
     */
    public String getTestGroupName() {
        return testGroupName;
    }

    /**
     * Returns the number of individual tests within this test. This is 1 more
     * than the number supplied to the constructor, as there is one test within
     * <code>runTest</code> itself (that the test completed with no exceptions).
     *
     * @return The number of tests that will be run by <code>runTest</code>.
     */
    public int getTestCount() {
        return testCount + 1;
    }

    /**
     * A function which should be called when <code>ok()</code> will be called
     * indirectly via another object. This is used via test groups that wrap
     * smaller groups to ensure that test sequence accounting is performed
     * correctly; it both accounts for the test sequence in this object, and
     * returns the information necessary to account for the test sequence in the
     * other object.
     *
     * @param count The number of <code>ok()</code> calls that were indirectly
     * performed.
     * @return The test number (or possibly -1 if unknown) that should be used
     * for the first of the indirectly made <code>ok()</code> calls.
     */
    protected int indirectOk(int count) {
        final int oldCurrentTestNumber = currentTestNumber;
        okCount += count;
        if (currentTestNumber != -1) {
            currentTestNumber += count;
        }
        return oldCurrentTestNumber;
    }

    /**
     * Sets up the testing-related part of this object's state.
     *
     * @param testCount The number of individual tests run by this object.
     * @param testGroupName The name of the test group this object represents.
     */
    protected TestGroup(int testCount, String testGroupName) {
        this.testCount = testCount;
        this.testGroupName = testGroupName;
    }

    /**
     * Performs the actions that this object tests. This is the "inside" of the
     * test (i.e. performing the actions being tested), not the "outside"
     * (reporting the results).
     *
     * If you want to run the test with results reported, see
     * <code>runTest</code>.
     *
     * @throws Exception If an exception is thrown while testing; this will
     * cause the "ran without exception" subtest to report failure, and all
     * tests that have not yet run to be skipped
     * @see #runTest(int)
     */
    abstract protected void testImplementation() throws Exception;

    /**
     * Runs the test represented by this object. This performs the actions
     * defined in <code>testImplementation</code>, and makes a TAP report on
     * whether they ran without exception.
     *
     * @param initialTestNumber The TAP number of the first subtest. In cases
     * where this is not known, it can be provided as -1.
     */
    public void runTest(int initialTestNumber) {
        currentTestNumber = initialTestNumber;
        okCount = 0;

        try {
            testImplementation();
            ok(true, "no exception/error in test driver");
        } catch (TestingCannotContinueException ex) {
            ex.bailOut();
            throw ex;
        } catch (Exception | Error ex) {
            while (okCount < getTestCount() - 1) {
                skipTest("unknown test (skipped by exception/error)");
            }
            synchronized (System.out) {
                ok(false, "no exception/error in test driver");
                System.out.println("# Exception was: " + ex.toString());
                Throwable exc = ex;
                while (exc != null) {
                    if (exc.getStackTrace().length == 0) {
                        System.out.println("# (no stack trace available)");
                    }
                    for (StackTraceElement s : exc.getStackTrace()) {
                        System.out.println("# " + s.toString());
                    }
                    exc = exc.getCause();
                    if (exc != null) {
                        System.out.println("# caused by: " + exc.toString());
                    }
                }
                System.out.flush();
            }
        }

        if (okCount != getTestCount()) {
            throw new RuntimeException(
                    "Expected to see " + getTestCount()
                    + " ok() calls, saw " + okCount);
        }
    }

    /**
     * Adds a comment to the TAP output. This will have no effect, but can be
     * seen by a human who examines the verbose TAP output manually.
     *
     * @param comment The comment to add.
     */
    public void comment(String comment) {
        synchronized (System.out) {
            System.out.println("# " + comment);
            System.out.flush();
        }
    }

    /**
     * Implements an individual test. Each individual test should call this
     * function exactly once, specifying whether or not the test passed, and the
     * name of the individual test.
     *
     * @param passed True if the test passed; false if it failed.
     * @param name The name of the individual test, not including the name of
     * the test group containing it.
     */
    public void ok(boolean passed, String name) {
        /* This has to be synchronized on a global, because we don't want two
           tests running on different threads to try to print results at the
           same time and end up with a corrupted string, or a race on
           currentTestNumber */
        synchronized (System.out) {
            System.out.println(
                    (passed ? "ok " : "not ok ")
                    + (todo ? "# TODO " : "")
                    + (currentTestNumber > -1 ? currentTestNumber + " - " : "- ")
                    + testGroupName + ": " + name);
            if (currentTestNumber > -1) {
                currentTestNumber++;
            }
            if (passed ^ todo) {
                successCount++;
            } else {
                failCount++;
            }
            okCount++;
            if (todo) {
                if (passed) {
                    unexpectedCount++;
                } else {
                    todoCount++;
                }
            }
            todo = false;
            System.out.flush();
        }
    }

    /**
     * Implements an individual test that checks to see if a value is equal to
     * an expected value. This is a special case of <code>ok</code> that
     * produces more detailed diagnostic output.
     * <p>
     * Equality is defined via <code>Object#equals</code>, in most cases. If
     * both objects are <code>Serializable</code>, they additionally count as
     * equal if their serialised forms are identical. If both objects are
     * arrays, they additionally count as equal if they have the same length,
     * and corresponding objects compare as equal via
     * <code>Object#equals</code>.
     *
     * @param actual The value actually observed during testing.
     * @param expected The value that would be expected if the test succeeded.
     * @param testName The name of this individual test.
     * @return <code>true</code> if the test succeeded; <code>false</code> if it
     * failed
     */
    public boolean okEq(Object actual, Object expected, String testName) {
        boolean eq = Objects.equals(expected, actual);
        if (!eq && actual != null && actual instanceof Serializable
                && expected != null && expected instanceof Serializable) {
            ByteArrayOutputStream os1 = new ByteArrayOutputStream();
            ByteArrayOutputStream os2 = new ByteArrayOutputStream();
            try {
                ObjectOutputStream oos1 = new ObjectOutputStream(os1);
                ObjectOutputStream oos2 = new ObjectOutputStream(os2);
                oos1.writeObject(actual);
                oos2.writeObject(expected);
                oos1.flush();
                oos2.flush();
                eq = Arrays.equals(os1.toByteArray(), os2.toByteArray());
            } catch (IOException ex) {
                /* note: IOException includes NotSerializableException */
                eq = false;
            }
        }

        if (actual != null && actual.getClass().isArray()
                && expected != null && expected.getClass().isArray()
                && Array.getLength(actual) == Array.getLength(expected)) {
            eq = true;
            for (int i = 0; i < Array.getLength(expected); i++) {
                if (!Objects.equals(Array.get(expected, i),
                        Array.get(actual, i))) {
                    eq = false;
                }
            }
        }
        synchronized (System.out) {
            ok(eq, testName);
            if (!eq) {
                System.out.println("# Expected value: "
                        + StringifyUtils.toString(expected));
                System.out.println("# Actual value: "
                        + StringifyUtils.toString(actual));
            }
        }
        return eq;
    }

    /**
     * Skips an individual test. Called in circumstances where the test cannot
     * be run due to an earlier failure. This is an alternative to calling
     * <code>ok</code> for the test.
     *
     * @param name The name of the individual test, not including the name of
     * the test group containing it.
     * @see #ok(boolean, java.lang.String)
     */
    public void skipTest(String name) {
        /* Skipped tests aren't marked as TODO as well. */
        todo = false;
        ok(true, name + " # skip");
        skipCount++;
    }

    /**
     * Marks the next test as a TODO test. TODO tests are expected to fail, due
     * to the thing they're testing being unimplemented; thus, they won't report
     * a failing test suite if they fail to be OK, but an unexpected success
     * will cause an error return the same way as an unexpected failure will.
     * <p>
     * The TODO status is reset after one call to <code>ok</code> or a related
     * function (including <code>skipTest</code>).
     */
    public void markTodo() {
        todo = true;
    }

    /**
     * Plans this test group. A plan specifies the entire set of tests scheduled
     * to be run during the execution of the test script; calling this function
     * specifies <i>this</i> test group as the entire list of tests. TAP
     * requires exactly one plan to be made, at the very start or very end of
     * the testing. As such, this function may only reasonably be called once
     * total, across all <code>TestGroup</code> objects, during the lifetime of
     * the program.
     *
     * @throws IllegalStateException If this method is called more than once in
     * the lifetime of the program
     */
    public void plan() throws IllegalStateException {
        synchronized (System.out) {
            if (plannedTestCount != -1) {
                throw new IllegalStateException("TestGroup#plan() called twice");
            }
            System.out.println("1.." + this.getTestCount());
            plannedTestCount = this.getTestCount();
            System.out.flush();
        }
    }

    /**
     * Determines the current state of the testing system. Intended for use at
     * the end of the program, to determine an appropriate exit code.
     * <p>
     * The returned exit code is 0 ("success") if all tests succeeded and the
     * total number of tests run equals the number stated in the plan. If the
     * plan is missing or incorrect (i.e. does not equal the total number of
     * tests run up to this point), the return value is 127. Otherwise, the
     * return value is the number of failing tests, capped at 126.
     *
     * @param verbose If true, also print a summary of the testing status to
     * standard output, formatted as a TAP diagnostic.
     * @return An integer suitable for use as an exit code.
     */
    public static int getTestingStatus(boolean verbose) {
        if (verbose) {
            System.out.println("# Planned " + plannedTestCount + " tests, ran "
                    + (failCount + successCount) + " tests ("
                    + (failCount - unexpectedCount) + " fail, "
                    + (successCount - todoCount - skipCount) + " success, "
                    + skipCount + " skipped, "
                    + todoCount + " todo, "
                    + unexpectedCount + " unexpected success)");
        }
        if (failCount + successCount != plannedTestCount) {
            if (verbose) {
                System.out.println("# Bad plan! Check your test cases.");
            }
            return 127;
        } else {
            return (failCount > 126 ? 126 : failCount);
        }
    }
}
