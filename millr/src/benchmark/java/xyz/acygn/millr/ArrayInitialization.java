package xyz.acygn.millr;

import java.util.Arrays;
import java.util.Random;

/**
 * A class useful for profiling the performance of milled array initializations. Repeatedly
 * performs initialization operations.
 *
 * @author Marcello De Bernardi
 */
@SuppressWarnings("Duplicates")
public class ArrayInitialization implements Profilable {
    @Override
    public void run(int iterations) {
        main(new String[] {String.valueOf(iterations)});
    }


    public static void main(String[] args) {
        // process profiling parameters
        int iterations = (args != null && args.length != 0) ? Integer.parseInt(args[0]) : DEFAULT_ITERATIONS;

        // array used in operations
        int[] numbers = new int[10000];

        // array initializer classes
        Initialization forLoop = new ForLoopInitialization();
        Initialization staticForLoop = new StaticForLoopInitialization();
        Initialization constantForLoop = new ConstantForLoopInitialization();
        Initialization setAll = new SetAllInitialization();
        Initialization clone = new CloneInitialization();

        // repeatedly perform array initialization operations
        // array contents are printed to a dummy stream to ensure the values
        // placed into the arrays in the initializations are needed for something
        // preventing the JVM from concluding the initializations are unnecessary
        // and deciding not to carry them out
        for (int i = 0; i < iterations; i++)  {
            numbers = forLoop.initialize(numbers);

            numbers = staticForLoop.initialize(numbers);

            numbers = constantForLoop.initialize(numbers);

            numbers = setAll.initialize(numbers);

            numbers = clone.initialize(numbers);
        }
    }


    /**
     * An interface representing some mechanism for array initialization.
     */
    private interface Initialization {
        int[] initialize(int[] array);
    }

    /**
     * Initialization of an array using a simple for loop.
     */
    private static class ForLoopInitialization implements Initialization {
        private Random rng;

        ForLoopInitialization() {
            rng = new Random();
        }

        @Override
        public int[] initialize(int[] array) {
            for (int i = 0; i < array.length; i++) array[i] = rng.nextInt();
            return array; // redundant, but this way conforms to interface
        }
    }

    /**
     * Initialization of an array using a simple for loop, where the upper iteration
     * bound is stored in a local variable before the loop starts.
     */
    private static class StaticForLoopInitialization implements Initialization {
        private Random rng;

        StaticForLoopInitialization() {
            rng = new Random();
        }

        @Override
        public int[] initialize(int[] array) {
            int size = array.length;

            for (int i = 0; i < size; i++) {
                array[i] = rng.nextInt();
            }

            return array;
        }
    }

    /**
     * Initialization of an array using a simple for loop, where the upper iteration
     * bound is specified as a constant in the source code.
     */
    private static class ConstantForLoopInitialization implements Initialization {
        private Random rng;

        ConstantForLoopInitialization() {
            rng = new Random();
        }

        @Override
        public int[] initialize(int[] array) {
            for (int i = 0; i < 10000; i++) {
                array[i] = rng.nextInt();
            }

            return array;
        }
    }

    /**
     * Initialization of an array using Arrays.setAll.
     */
    private static class SetAllInitialization implements Initialization {
        @Override
        public int[] initialize(int[] array) {
            Arrays.setAll(array, i -> i);
            return array; // redundant, but this way conforms to interface
        }
    }

    /**
     * Initialization of an array using Object.clone
     */
    private static class CloneInitialization implements Initialization {
        @Override
        public int[] initialize(int[] array) {
            return array.clone();
        }
    }
}
