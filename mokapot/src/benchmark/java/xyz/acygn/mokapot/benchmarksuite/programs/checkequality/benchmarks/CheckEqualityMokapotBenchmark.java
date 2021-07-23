package xyz.acygn.mokapot.benchmarksuite.programs.checkequality.benchmarks;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.ArrayList;

import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.CopiableSupplier;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkData;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Utilities;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.checkequality.EqualityTestObject;
import xyz.acygn.mokapot.benchmarksuite.programs.checkequality.ValueHolder;


import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase.WHILE_DISTRIBUTION_TOP;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase.WHILE_EXECUTION_TOP;

/**
 * @author Kelsey McKenna
 */
public class CheckEqualityMokapotBenchmark extends CheckEqualityBenchmark {
    private EqualityTestObject localObject;
    private ValueHolder remoteObject;

	@Override
	public ExampleType getType() {
		return ExampleType.MOKAPOT;
	}

    @Override
    public int getNumberOfRequiredPeers() {
        return 1;
    }


    @Override
    public void distribute() throws IOException {
        final CopiableSupplier<EqualityTestObject> objectSupplier = () -> new EqualityTestObject(0);

        localObject = objectSupplier.get();
        remoteObject = communicator.runRemotely(objectSupplier, remotes.get(0));
        assert Utilities.isStoredRemotely(remoteObject);
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
        remoteObject = null;

        garbageCollect();
        communicator.stopCommunication();
    }

    @Override
    public boolean requiresFix(BenchmarkPhase phase) {
        return phase == WHILE_DISTRIBUTION_TOP || phase == WHILE_EXECUTION_TOP;
    }

    @Override
    public void fix(BenchmarkPhase phase) {
        switch (phase) {
            case WHILE_DISTRIBUTION_TOP:
                garbageCollect();
                break;
            case WHILE_EXECUTION_TOP:
                garbageCollect();
                break;
        }
    }

    private void garbageCollect() {
        for (int i = 0; i < 2; i++) {
            System.gc();
            System.runFinalization();
            communicator.runRemotely(() -> {
                System.gc();
                System.runFinalization();
            }, remotes.get(0));
        }
    }

}
