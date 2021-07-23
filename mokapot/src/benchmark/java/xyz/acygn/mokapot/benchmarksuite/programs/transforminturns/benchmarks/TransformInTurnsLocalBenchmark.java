package xyz.acygn.mokapot.benchmarksuite.programs.transforminturns.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.transforminturns.TransformerImpl;

public class TransformInTurnsLocalBenchmark extends TransformInTurnsBenchmark {
    public TransformInTurnsLocalBenchmark() {
    }

    public ExampleType getType() {
        return ExampleType.LOCAL;
    }

    public int getNumberOfRequiredPeers() {
        return 0;
    }

    public void distribute() throws IOException {
        super.distribute();
        this.localTransformer = new TransformerImpl();
        this.remoteTransformer = new TransformerImpl();
        this.localTransformer.pair(this.remoteTransformer);
        this.remoteTransformer.pair(this.localTransformer);
    }

    public void stop() throws IOException, NotBoundException {
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    public void fix(BenchmarkPhase phase) {
    }
}
