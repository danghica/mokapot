package xyz.acygn.mokapot.benchmarksuite.programs.finder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Kelsey McKenna
 */
public class StringDataGeneratorImpl implements StringDataGenerator {

    private static final char[] alphabet = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
            'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
            's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    private static final int maxWordLength = 10;
    private static final long seed = 12576;
    private Random generator = new Random(seed);

    /**
     * Create a list with generated randomly words.
     *
     * @param size The number of words that are going to be added to the list.
     * @return A list of randomly generated words.
     */
    public List<String> generateRandomWordList(int size) {
        ArrayList<String> result = new ArrayList<>();

        for (int k = 0; k < size; k++) {
            int wordLength = 1 + generator.nextInt(maxWordLength);
            result.add(generateRandomWord(wordLength));
        }

        return result;
    }

    public String generateRandomWord(int wordLength) {
        final StringBuilder result = new StringBuilder();

        for (int i = 0; i < wordLength; i++) {
            int index = generator.nextInt(alphabet.length);
            result.append(alphabet[index]);
        }

        return result.toString();
    }

}
