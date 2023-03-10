package xyz.acygn.millr.localsemantics.arrays;

import java.util.Arrays;


/**
 * A class representing some arbitrary API that takes arrays, operates on them, and may
 * hold pointers to them for an extended period of time. This class is intended to
 * behave as a mock of any Java API methods.
 *
 * @author Marcello De Bernardi
 */
class ArbitraryAPI {
    static private int[] arrayPointer;


    static void passArray(int[] array) {
        arrayPointer = array;
    }

    static int[] retrieveArray() {
        return arrayPointer;
    }

    static int[] sort(int[] array) {
        Arrays.sort(array);
        return array;
    }
}