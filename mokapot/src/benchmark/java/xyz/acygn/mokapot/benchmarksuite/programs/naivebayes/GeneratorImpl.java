package xyz.acygn.mokapot.benchmarksuite.programs.naivebayes;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author Kelsey McKenna
 */
public class GeneratorImpl implements Generator {

    @Override
    public Set<Email> generate(int seed, int sampleSize, int numberOfFeatures) {
        HashSet<Email> trainingData = new HashSet<>();
        Random rng = new Random(seed);

        for (int i = 0; i < sampleSize; i++) {
            trainingData.add(new Email(numberOfFeatures, rng));
        }

        return trainingData;
    }

}
