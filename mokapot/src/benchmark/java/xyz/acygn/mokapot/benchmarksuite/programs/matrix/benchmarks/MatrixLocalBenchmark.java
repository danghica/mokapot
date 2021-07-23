package xyz.acygn.mokapot.benchmarksuite.programs.matrix.benchmarks;

import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.matrix.MatrixImpl;

public class MatrixLocalBenchmark extends MatrixBenchmark {
    public MatrixLocalBenchmark() {
    }

    public ExampleType getType() {
        return ExampleType.LOCAL;
    }

    public int getNumberOfRequiredPeers() {
        return 0;
    }

    public void distribute() {
        this.matrixA = new MatrixImpl(this.createRandomMatrix(size, seed1));
        this.matrixB = new MatrixImpl(this.createRandomMatrix(size, seed2));
    }

    public void stop() {
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    public void fix(BenchmarkPhase phase) {
    }
}
