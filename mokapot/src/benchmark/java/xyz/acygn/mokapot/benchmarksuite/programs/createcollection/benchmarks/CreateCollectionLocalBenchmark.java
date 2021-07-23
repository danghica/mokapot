package xyz.acygn.mokapot.benchmarksuite.programs.createcollection.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.HashSet;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.createcollection.GeneratorImpl;

/**
 * @author Kelsey McKenna
 */
public class CreateCollectionLocalBenchmark extends CreateCollectionBenchmark {

    @Override
    public Benchmarkable.ExampleType getType() {
        return Benchmarkable.ExampleType.LOCAL;
    }

    @Override
    public int getNumberOfRequiredPeers() {
        return 0;
    }

    @Override
    public void distribute() {
        collections = new HashSet<>();
        generator = new GeneratorImpl();
    }

    @Override
    public void executeAlgorithm() throws IOException {
        for (int i = 0; i < NUMBER_OF_COLLECTIONS; i++) {
            collections.add(generator.generate(SEED, COLLECTION_MAX_SIZE));
        }
    }

    @Override
    public void stop() throws IOException, NotBoundException {
        // Nothing to do
    }

    @Override
    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    @Override
    public void fix(BenchmarkPhase phase) {
        // Nothing to do
    }

}
