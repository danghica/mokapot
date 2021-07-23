package xyz.acygn.mokapot.benchmarksuite.programs.finder.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.finder.StringDataGeneratorImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.finder.StringFinderImpl;

/**
 * @author Kelsey McKenna
 */
public class FindStringLocalBenchmark extends FindStringBenchmark {

    @Override
    public ExampleType getType() {
        return ExampleType.LOCAL;
    }

    @Override
    public int getNumberOfRequiredPeers() {
        return 0;
    }

    @Override
    public void distribute() throws IOException {
        generator = new StringDataGeneratorImpl();
        words = generator.generateRandomWordList(listSize);
        wordToFind = generator.generateRandomWord(10);

        finder = new StringFinderImpl();
    }

    @Override
    public void stop() throws IOException, NotBoundException {
        //not needed locally
    }

    @Override
    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    @Override
    public void fix(BenchmarkPhase phase) {
    }

}
