package xyz.acygn.mokapot.test.bytecode;

/**
 * One of the simplest possible classes which has an inner class. Used for
 * experiments with extending inner classes.
 *
 * @author Alex Smith
 */
public class InnerBaseClass {

    /**
     * Returns this object's identity hash code.
     *
     * @return This object's identity hash code.
     * @see System#identityHashCode(java.lang.Object)
     */
    private int identityHashCode() {
        return System.identityHashCode(this);
    }

    /* There are two version of the inner class. Here's the version that doesn't
       cause a compile error in DerivedFromInnerClass: */
    /**
     * A dummy inner class used to ensure that
     * <code>DerivedFromInnerClass</code> can compile.
     */
    static class Inner {

        /**
         * Dummy method with the same signature as that in the other definition.
         */
        void printHashCodeReport() {
        }
    }

    /* and here's one with similar bytecode but a very different definition: */
    /**
     * A private inner class.
     */
    private class Inner2 {

        /**
         * Prints some information about the hash code of the inner and outer
         * objects.
         */
        void printHashCodeReport() {
            System.out.println("inner hash code: " + hashCode()
                    + ", outer hash code: " + identityHashCode());
        }
    }

    /* To use this test properly, first compile the code, then swap the names of
       Inner and Inner2 and compile again, thus causing an apparently
       incompatible class change. (The aim is to see how the JVM reacts.) */
}
