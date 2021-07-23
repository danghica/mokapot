package xyz.acygn.mokapot.benchmarksuite.programs.validanagram.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.validanagram.CheckerImpl;

public class ValidAnagramLocalBenchmark extends ValidAnagramBenchmark {
    public ValidAnagramLocalBenchmark() {
    }

    public ExampleType getType() {
        return ExampleType.LOCAL;
    }

    public int getNumberOfRequiredPeers() {
        return 1;
    }

    public void distribute() throws IOException {
        super.distribute();
        this.firstChecker = new CheckerImpl();
        this.secondChecker = new CheckerImpl();
        this.firstChecker.pair(this.secondChecker);
        this.secondChecker.pair(this.firstChecker);
    }

    public void stop() throws IOException, NotBoundException {
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    public void fix(BenchmarkPhase phase) {
    }
}
