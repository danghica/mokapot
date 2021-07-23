package xyz.acygn.mokapot.benchmarksuite.programs.checkequality.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;

import xyz.acygn.mokapot.CopiableSupplier;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.checkequality.EqualityTestObject;
import xyz.acygn.mokapot.benchmarksuite.programs.checkequality.ValueHolder;
import xyz.acygn.mokapot.rmiserver.RMIServer;

/**
 * @author Kelsey McKenna
 */
public class CheckEqualityRMIBenchmark extends CheckEqualityBenchmark {

    private EqualityTestObject localObject;
    private ValueHolder remoteObject;

	@Override
	public ExampleType getType() {
		return ExampleType.RMI;
	}

    @Override
    public int getNumberOfRequiredPeers() {
        return 1;
    }

    @Override
    public void distribute() throws RemoteException {
        final CopiableSupplier<EqualityTestObject> objectSupplier = () -> new EqualityTestObject(0);

        localObject = objectSupplier.get();
        remoteObject = servers.get(0).create(objectSupplier);
    }

    @Override
    public void executeAlgorithm() throws IOException {
        boolean equal = localObject.equals(remoteObject);
        boolean alias = localObject == remoteObject;

        assert equal;
        assert !alias;
    }

    @Override
    public void stop() throws RemoteException {
        fix(BenchmarkPhase.POST_EXECUTION);
    }

    @Override
    public boolean requiresFix(BenchmarkPhase phase) {
        /* The method does not look at current state for two reasons:
        1) it is not possible for the example program itself to
        track whether an object has been exported without performing
        some tracking action in the distribute() or executeAlgorithm()
        methods, and this would skew benchmarking results.
        2) RMI does not offer a way to check if an object is exported,
        so the only way to check is to try exporting and seeing if an
        exception is thrown for the object already being exported. In
        that case, one might as well just go directly for fix(),
        and catch the potential NoSuchObjectException.
         */
        return (phase == BenchmarkPhase.WHILE_EXECUTION_BOTTOM);
    }

    @Override
    public void fix(BenchmarkPhase phase) {
        // Nothing to do
    }

}
