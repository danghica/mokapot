package xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;

import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist.FinderImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist.IntegerGenerator;
import xyz.acygn.mokapot.rmiserver.RMIServer;

public class TraverseLinkedListRMIBenchmark extends TraverseLinkedListBenchmark {
	@Override
	public ExampleType getType() {
		return ExampleType.RMI;
	}
	
    public int getNumberOfRequiredPeers() {
        return 1;
    }

    public void distribute() throws RemoteException {
        this.finder = servers.get(0).create(() -> new FinderImpl<>(new IntegerGenerator().generateTestList(listSize)));
    }

    public void executeAlgorithm() throws RemoteException {
        Integer largest = finder.findLargestIndex();
    }

    public void stop() throws IOException, NotBoundException {
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    public void fix(BenchmarkPhase phase) {
    }
}
