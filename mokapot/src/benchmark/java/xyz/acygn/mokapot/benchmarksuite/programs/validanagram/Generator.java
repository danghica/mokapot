package xyz.acygn.mokapot.benchmarksuite.programs.validanagram;

import java.util.Random;

/**
 * Generates a long, pseudorandom String for use in the anagram checking example.
 */
public class Generator {
    private Random rng;

    /**
     * Creates a new string generator wrapping a pseudorandom number
     * generator with the given seed.
     *
     * @param seed seed value for generator
     */
    public Generator(int seed) {
        rng = new Random(seed);
    }

    /**
     * Generates a string of the given length using the provided seed.
     *
     * @param length length of string
     */
    public String generateString(int length) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++) {
            result.append((char) (rng.nextInt(26) + 97));
        }

        return result.toString();
    }

}
