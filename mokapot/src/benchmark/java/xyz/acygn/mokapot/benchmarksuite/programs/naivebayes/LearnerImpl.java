package xyz.acygn.mokapot.benchmarksuite.programs.naivebayes;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Kelsey McKenna
 */
public class LearnerImpl implements Learner {

    private final Set<Email> trainingData;
    private List<Double> results;

    public LearnerImpl(Set<Email> trainingData) {
        this.trainingData = trainingData;
    }

    @Override
    public void learn(int numberOfFeatures) {
        Set<Email> spam = new HashSet<>();
        Set<Email> notSpam = new HashSet<>();
        List<Double> probabilities = new ArrayList<>();

        for (Email email : trainingData) {
            if (email.isSpam()) spam.add(email);
            else notSpam.add(email);
        }

        // learn from data
        for (int i = 0; i < numberOfFeatures; i++) {
            int counterInSpam = 0;
            int counterNotInSpam = 0;

            for (Email email : spam) {
                if (email.hasFeature(i)) counterInSpam++;
            }
            for (Email email : notSpam) {
                if (email.hasFeature(i)) counterNotInSpam++;
            }

            double probability =
                    (double) counterInSpam / (double) spam.size() *                     //  P(feature | spam)
                            (double) spam.size() / (double) trainingData.size() /       //  P(spam)
                            (((double) counterInSpam + counterNotInSpam) /              //  P(feature)
                                    (double) trainingData.size());

            probabilities.add(probability);
        }

        this.results = probabilities;
    }

    @Override
    public List<Double> getResults() throws RemoteException {
        return results;
    }

}
