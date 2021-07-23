package xyz.acygn.mokapot.benchmarksuite.programs.naivebayes.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.naivebayes.GeneratorImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.naivebayes.LearnerImpl;

public class NaiveBayesLocalBenchmark extends NaiveBayesBenchmark {
    public NaiveBayesLocalBenchmark() {
    }

    public ExampleType getType() {
        return ExampleType.LOCAL;
    }

    public int getNumberOfRequiredPeers() {
        return 0;
    }

    public void distribute() {
        this.learner = new LearnerImpl((new GeneratorImpl()).generate(seed, sampleSize, numberOfFeatures));
    }

    public void stop() throws IOException, NotBoundException {
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    public void fix(BenchmarkPhase phase) {
    }
}
