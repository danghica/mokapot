package xyz.acygn.mokapot.test.bytecode;

import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * A trivial class whose purpose is to have particularly simple bytecode so that
 * other classes can be easily compared to it. Also used as a base class from
 * which to derive derived classes.
 *
 * @author Alex Smith
 */
public class TrivialTestClass implements NonCopiable {

    /**
     * An integer used to make trivial test objects stateful.
     */
    private final int data;

    /**
     * Creates a new TrivialTestClass object with the given data.
     *
     * @param data The data to store in the trivial test object.
     */
    public TrivialTestClass(int data) {
        this.data = data;
    }

    /**
     * Returns this trivial test object's data.
     *
     * @return The data specified when constructing the trivial test object.
     */
    public int getData() {
        return data;
    }

    /**
     * Checks to see whether this object's data is equal to the given data.
     *
     * @param data The data to compare to.
     * @return <code>true</code> if the two datas are equal.
     */
    public boolean dataEquals(int data) {
        return this.data == data;
    }
}
