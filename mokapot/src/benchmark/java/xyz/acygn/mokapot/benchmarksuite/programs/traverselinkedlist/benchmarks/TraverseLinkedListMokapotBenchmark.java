package xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist.benchmarks;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.ArrayList;

import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkData;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Utilities;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist.FinderImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist.IntegerGenerator;

public class TraverseLinkedListMokapotBenchmark extends TraverseLinkedListBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.MOKAPOT;
	}

    public int getNumberOfRequiredPeers() {
        return 1;
    }

    public void distribute() throws RemoteException {
        IntegerGenerator generator = communicator.runRemotely(IntegerGenerator::new, remotes.get(0));
        assert Utilities.isStoredRemotely(generator);

        this.linkedList = generator.generateTestList(listSize);
        assert Utilities.isStoredRemotely(linkedList);

        this.finder = communicator.runRemotely(() -> new FinderImpl<>(), remotes.get(0));
        assert Utilities.isStoredRemotely(finder);

    }

    public void executeAlgorithm() throws RemoteException {
        Integer largest = finder.findLargestIndex(linkedList);
    }

    public void stop() throws IOException, NotBoundException {
        this.finder = null;
        this.linkedList.clear();
        this.linkedList = null;
        communicator.stopCommunication();
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return phase == BenchmarkPhase.WHILE_EXECUTION_TOP || phase == BenchmarkPhase.WHILE_DISTRIBUTION_TOP;
    }

    public void fix(BenchmarkPhase phase) {
        if (phase == BenchmarkPhase.WHILE_DISTRIBUTION_TOP) {
            this.finder = null;
            if (this.linkedList != null) {
                try {
                    this.linkedList.clear();
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }

            this.linkedList = null;
        }

    }
}
