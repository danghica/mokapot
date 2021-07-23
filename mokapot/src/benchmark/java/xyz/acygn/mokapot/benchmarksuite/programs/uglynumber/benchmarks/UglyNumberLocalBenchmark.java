package xyz.acygn.mokapot.benchmarksuite.programs.uglynumber.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.uglynumber.CheckerImpl;

public class UglyNumberLocalBenchmark extends UglyNumberBenchmark {
    public ExampleType getType() {
        return ExampleType.LOCAL;
    }

    public int getNumberOfRequiredPeers() {
        return 0;
    }

    public void distribute() {
        this.checker = new CheckerImpl();
    }

    public void stop() throws IOException, NotBoundException {
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    public void fix(BenchmarkPhase phase) {
    }
}
