package xyz.acygn.mokapot.test;

import java.util.function.BooleanSupplier;

/**
 * A "test group" that serves as a position to bail out upon failure. It will
 * evaluate a given function, and if it returns true, cancel the entire test.
 *
 * @author Alex Smith
 */
public class TestBailOutPoint extends TestGroup {

    /**
     * A function that determines whether it's reasonable to continue testing.
     * If it returns false at this point in the testing, the entire test will
     * exit.
     */
    private final BooleanSupplier okToContinue;

    /**
     * Creates a new point at which it's possible to bail out of a test.
     *
     * @param okToContinue The function that will run to determine whether
     * continuing is acceptable or not.
     */
    public TestBailOutPoint(BooleanSupplier okToContinue) {
        super(0, "continue testing");
        this.okToContinue = okToContinue;
    }

    /**
     * If <code>okToContinue</code> returns <code>false</code>, abandons
     * testing. Otherwise does nothing.
     */
    @Override
    protected void testImplementation() {
        if (!okToContinue.getAsBoolean()) {
            throw new TestingCannotContinueException();
        }
    }
}
