package xyz.acygn.mokapot.benchmarksuite.programs.naivebayes;

import java.io.Serializable;
import java.util.Random;

/**
 * @author Marcello De Bernardi
 */
public class Email implements Serializable {

    private boolean spam;
    private boolean[] features;


    Email(int numberOfFeatures, Random rng) {
        spam = rng.nextInt(2) == 1;
        features = new boolean[numberOfFeatures];

        for (int i = 0; i < features.length; i++) {
            features[i] = rng.nextInt(2) == 1;
        }
    }

    boolean isSpam() {
        return spam;
    }

    boolean hasFeature(int i) {
        return features[i]; // fixme
    }

}
