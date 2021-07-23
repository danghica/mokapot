package xyz.acygn.mokapot.benchmarksuite.programs.checkequality.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import xyz.acygn.mokapot.CopiableSupplier;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.checkequality.EqualityTestObject;
import xyz.acygn.mokapot.benchmarksuite.programs.checkequality.ValueHolder;

/**
 * @author Kelsey McKenna
 */
public class CheckEqualityLocalBenchmark extends CheckEqualityBenchmark {

    private EqualityTestObject localObject;
    private ValueHolder remoteObject;

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
        final CopiableSupplier<EqualityTestObject> objectSupplier = () -> new EqualityTestObject(0);

        localObject = objectSupplier.get();
        remoteObject = objectSupplier.get();
    }

    @Override
    public void executeAlgorithm() throws IOException {
        boolean equal = localObject.equals(remoteObject);
        boolean alias = localObject == remoteObject;

        assert equal;
        assert !alias;
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
