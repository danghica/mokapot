package xyz.acygn.millr;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;


/**
 * A class useful for profiling the performance of array sorting operations in
 * milled applications. Array sorting using different approaches is performed
 * repeatedly.
 *
 * @author Marcello De Bernardi
 */
public class ArraySorting implements Profilable {
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

        // create sorting method objects
        SortMethod arraySort = new ArraysApiSort();
        SortMethod streamSort = new StreamApiSort();

        // repeatedly apply operations
        for (int i = 0; i < iterations; i++) {
            numbers = arraySort.sort(numbers);

            numbers = streamSort.sort(numbers);
        }
    }


    /**
     * A SortMethod object provides some way of sorting an array.
     */
    private interface SortMethod {
        int[] sort(int[] array);
    }

    /**
     * A SortMethod which uses Arrays.sort
     */
    private static class ArraysApiSort implements SortMethod {
        @Override
        public int[] sort(int[] array) {
            Arrays.sort(array);
            return array;
        }
    }

    /**
     * A SortMethod which uses Stream.sorted
     */
    private static class StreamApiSort implements SortMethod {
        @Override
        public int[] sort(int[] array) {
            return Arrays.stream(array).sorted().toArray();
        }
    }
}
