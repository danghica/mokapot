package xyz.acygn.millr;


import xyz.acygn.millr.util.intArrayWrapper;

import java.util.Arrays;
import java.util.Random;

import static xyz.acygn.millr.util.intArrayWrapper.getintArrayWrapper;

/**
 * A version of the {@link ArrayInitialization} class which is directly implemented
 * using millr's ArrayWrapper classes.
 *
 * @author Marcello De Bernardi
 */
@SuppressWarnings("Duplicates")
public class WrapperInitializations implements Profilable {
    @Override
    public void run(int iterations) {
        main(new String[] {String.valueOf(iterations)});
    }


    public static void main(String[] args) {
        // process profiling parameters
        int iterations = (args != null && args.length != 0) ? Integer.parseInt(args[0]) : DEFAULT_ITERATIONS;

        // array used in operations
        intArrayWrapper numbers = getintArrayWrapper(10000);


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
     * Represents an entity capable of initializing an {@link intArrayWrapper} by some means.
     */
    private interface Initialization {
        intArrayWrapper initialize(intArrayWrapper array);
    }

    /**
     * Initializer for {@link intArrayWrapper} which uses a simple for loop
     */
    private static class ForLoopInitialization implements Initialization {
        private Random rng;

        ForLoopInitialization() {
            rng = new Random();
        }

        @Override
        public intArrayWrapper initialize(intArrayWrapper array) {
            for (int i = 0; i < array.size(); i++) {
                array.set(i, rng.nextInt());
            }
            return array;
        }
    }

    /**
     * Initializer for {@link intArrayWrapper} which uses a simple for loop, but modified
     * so that the length of the array wrapper is only queried once
     */
    private static class StaticForLoopInitialization implements Initialization {
        private Random rng;

        StaticForLoopInitialization() {
            rng = new Random();
        }

        @Override
        public intArrayWrapper initialize(intArrayWrapper array) {
            int size = array.size();

            for (int i = 0; i < size; i++) {
                array.set(i, rng.nextInt());
            }

            return array;
        }
    }

    /**
     * Initializer for {@link intArrayWrapper} which uses a simple for loop, but modified
     * so that the length of the array wrapper is never queried; a preset constant is
     * used instead.
     */
    private static class ConstantForLoopInitialization implements Initialization {
        private Random rng;

        ConstantForLoopInitialization() {
            rng = new Random();
        }

        @Override
        public intArrayWrapper initialize(intArrayWrapper array) {
            for (int i = 0; i < 10000; i++) {
                array.set(i, rng.nextInt());
            }

            return array;
        }
    }

    /**
     * Initializer for {@link intArrayWrapper} which uses the api method Arrays.setAll to
     * initialize a new array, and then returns a new wrapper for that array
     */
    private static class SetAllInitialization implements Initialization {
        @Override
        public intArrayWrapper initialize(intArrayWrapper array) {
            int[] storage = array.asArray();
            Arrays.setAll(storage, i -> i);

            return getintArrayWrapper(storage);
        }
    }

    /**
     * Initializer which uses the .clone() method.
     */
    private static class CloneInitialization implements Initialization {
        @Override
        public intArrayWrapper initialize(intArrayWrapper array) {
            return array.clone();
        }
    }
}
