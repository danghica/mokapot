package xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist.FinderImpl;
import xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist.IntegerGenerator;

public class TraverseLinkedListLocalBenchmark extends TraverseLinkedListBenchmark {

    public ExampleType getType() {
        return ExampleType.LOCAL;
    }

    public int getNumberOfRequiredPeers() {
        return 0;
    }

    public void distribute() throws RemoteException {
        this.linkedList = (new IntegerGenerator()).generateTestList(listSize);
        this.finder = new FinderImpl<>();
    }

    public void executeAlgorithm() throws RemoteException {
        Integer largest = this.finder.findLargestIndex(this.linkedList);
    }

    public void stop() throws IOException, NotBoundException {
    }

    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    public void fix(BenchmarkPhase phase) {
    }

}
