package xyz.acygn.millr;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

/**
 * A class useful for profiling the performance of array operations in
 * milled applications. Array searching using different approaches is performed
 * repeatedly.
 *
 * @author Marcello De Bernardi
 */
public class ArraySearching implements Profilable {
    @Override
    public void run(int iterations) {
        main(new String[]{String.valueOf(iterations)});
    }


    public static void main(String[] args) {
        // process profiling parameters
        int iterations = (args != null && args.length != 0) ? Integer.parseInt(args[0]) : DEFAULT_ITERATIONS;

        // array used in operations
        int[] numbers = new int[1000];
        Random rng = new Random();
        Arrays.setAll(numbers, i -> rng.nextInt(200));
        int target;
        boolean found = false;

        // search methods
        SearchMethod linearApproach = new LinearSearch();
        SearchMethod containsApproach = new ApiContains();
        SearchMethod streamApproach = new ApiStream();

        for (int i = 0; i < iterations; i++) {
            target = rng.nextInt(200);

            found = linearApproach.search(numbers, target);

            found = containsApproach.search(numbers, target);

            found = streamApproach.search(numbers, target);
        }

    }


    /**
     * A SearchMethod object provides a way of searching through an array.
     */
    private interface SearchMethod {
        boolean search(int[] array, int key);
    }

    /**
     * A SearchMethod which uses a for loop to perform linear search.
     */
    private static class LinearSearch implements SearchMethod {
        @Override
        public boolean search(int[] array, int key) {
            for (int value : array) {
                if (value == key) return true;
            }
            return false;
        }
    }

    /**
     * A SearchMethod which uses the list method .contains
     */
    private static class ApiContains implements SearchMethod {
        @Override
        public boolean search(int[] array, int key) {
            return Arrays.asList(array).contains(key);
        }
    }

    /**
     * A SearchMethod which uses the stream method stream.anyMatch
     */
    private static class ApiStream implements SearchMethod {
        @Override
        public boolean search(int[] array, int key) {
            return Arrays.stream(array).anyMatch(value -> value == key);
        }
    }
}
