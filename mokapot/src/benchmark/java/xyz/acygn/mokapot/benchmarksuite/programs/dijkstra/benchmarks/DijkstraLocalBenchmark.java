package xyz.acygn.mokapot.benchmarksuite.programs.dijkstra.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.dijkstra.DijkstraImpl;

/**
 * @author Kelsey McKenna
 */
public class DijkstraLocalBenchmark extends DijkstraBenchmark {

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
        dij = new DijkstraImpl();
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
