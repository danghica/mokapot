package xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;

import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist.Cell;
import xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist.Finder;

/**
 * A benchmark in which a remote {@link Finder} traverses a remote
 * linked list ({@link Cell}) to find the largest element.
 *
 * @author Kelsey McKenna
 */
public abstract class TraverseLinkedListBenchmark extends BenchmarkProgram {
    static int listSize = 100;
    Cell<Integer> linkedList;
    Finder<Integer> finder;

    public static void main(String[] args) throws IOException, NotBoundException {
        TraverseLinkedListBenchmark program = new TraverseLinkedListRMIBenchmark();
        program.setup(program instanceof TraverseLinkedListMokapotBenchmark ? 15239 : 0);
        program.distribute();
        program.executeAlgorithm();
        program.stop();
    }
}
