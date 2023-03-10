package xyz.acygn.millr;


import java.util.Arrays;
import java.util.Random;

/**
 * A version of {@link ArrayInitialization} which uses a custom array wrapper
 * object rather than the millr array wrappers.
 *
 * @author Marcello De Bernardi
 */
@SuppressWarnings("Duplicates")
public class CustomWrapperInitializations implements Profilable {
    @Override
    public void run(int iterations) {
        main(new String[] {String.valueOf(iterations)});
    }

    public static void main(String[] args) {
        // process profiling parameters
        int iterations = (args != null && args.length != 0) ? Integer.parseInt(args[0]) : DEFAULT_ITERATIONS;

        // array used in operations
        integerArrayWrapper numbers = new integerArrayWrapper(10000);

        // array initializer classes
        Initialization forLoop = new ForLoopInitialization();
        Initialization staticForLoop = new StaticForLoopInitialization();
        Initialization constantForLoop = new ConstantForLoopInitialization();

        // repeatedly perform array initialization operations
        // array contents are printed to a dummy stream to ensure the values
        // placed into the arrays in the initializations are needed for something
        // preventing the JVM from concluding the initializations are unnecessary
        // and deciding not to carry them out
        for (int i = 0; i < iterations; i++)  {
            numbers = forLoop.initialize(numbers);

            numbers = staticForLoop.initialize(numbers);

            numbers = constantForLoop.initialize(numbers);
        }
    }


    /**
     * Represents a particular method for initializing the contents of an integerArrayWrapper.
     */
    private interface Initialization {
        integerArrayWrapper initialize(integerArrayWrapper array);
    }

    /**
     * Initialization of an integerArrayWrapper using a simple for loop
     */
    private static class ForLoopInitialization implements Initialization {
        private Random rng;

        ForLoopInitialization() {
            rng = new Random();
        }

        @Override
        public integerArrayWrapper initialize(integerArrayWrapper array) {
            for (int i = 0; i < array.length(); i++) {
                array.set(i, rng.nextInt());
            }
            return array;
        }
    }

    /**
     * Initialization of an integerArrayWrapper using a for loop, but changed
     * in that the size of the wrapper (which is constant) is obtained before
     * the loop begins.
     */
    private static class StaticForLoopInitialization implements Initialization {
        private Random rng;

        StaticForLoopInitialization() {
            rng = new Random();
        }

        @Override
        public integerArrayWrapper initialize(integerArrayWrapper array) {
            int size = array.length();

            for (int i = 0; i < size; i++) {
                array.set(i, rng.nextInt());
            }

            return array;
        }
    }

    /**
     * Initialization of an integerArrayWrapper using a for loop, but changed in
     * that the for loop's counter is a constant preset value, rather than related
     * to the size of the array-wrapper (it should be the same value though).
     */
    private static class ConstantForLoopInitialization implements Initialization {
        private Random rng;

        ConstantForLoopInitialization() {
            rng = new Random();
        }

        @Override
        public integerArrayWrapper initialize(integerArrayWrapper array) {
            for (int i = 0; i < 10000; i++) {
                array.set(i, rng.nextInt());
            }

            return array;
        }
    }


    /**
     * A simple wrapper for primitive integer arrays which wraps the most
     * basic array functionality.
     */
    private static class integerArrayWrapper {
        private final int[] storage;
        private final int length;

        integerArrayWrapper(int[] storage) {
            this.storage = storage;
            this.length = storage.length;
        }

        integerArrayWrapper(int length) {
            storage = new int[length];
            this.length = length;
        }

        int get(int index) {
            return storage[index];
        }

        void set(int index, int value) {
            storage[index] = value;
        }

        int length() {
            return length;
        }

        int[] array() {
            return this.storage;
        }
    }
}
