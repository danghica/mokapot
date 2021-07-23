package xyz.acygn.mokapot.benchmarksuite.programs.naivebayes.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.naivebayes.Learner;

/**
 * A benchmark in which a remote {@link Learner} learns
 * from a set of test data.
 *
 * @author Kelsey McKenna
 */
public abstract class NaiveBayesBenchmark extends BenchmarkProgram {
    static final int sampleSize = 10;
    static final int seed = 15268;
    static final int numberOfFeatures = 10;
    Learner learner;

    public void executeAlgorithm() throws RemoteException {
        learner.learn(10);
    }

    public static void main(String[] args) throws IOException, NotBoundException {
        NaiveBayesBenchmark program = new NaiveBayesRMIBenchmark();
        program.setup(program instanceof NaiveBayesMokapotBenchmark ? 15239 : 0);
        program.distribute();
        program.executeAlgorithm();
        program.stop();
    }
}
