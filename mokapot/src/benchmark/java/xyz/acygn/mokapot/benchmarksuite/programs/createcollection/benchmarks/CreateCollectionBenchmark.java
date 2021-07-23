package xyz.acygn.mokapot.benchmarksuite.programs.createcollection.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.Set;

import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.createcollection.ExampleObject;
import xyz.acygn.mokapot.benchmarksuite.programs.createcollection.Generator;
import xyz.acygn.mokapot.benchmarksuite.programs.createcollection.ICollection;

/**
 * A benchmark in which remote {@link java.util.Collection}s are
 * added to a local list.
 *
 * @author Kelsey McKenna
 */
public abstract class CreateCollectionBenchmark extends BenchmarkProgram {

    static final int NUMBER_OF_COLLECTIONS = 10;
    static final int COLLECTION_MAX_SIZE = 30;
    static final int SEED = 5;

    Set<ICollection<ExampleObject>> collections;
    Generator generator;

    public static void main(String[] args) throws IOException, NotBoundException {
        CreateCollectionBenchmark benchmark = new CreateCollectionRMIBenchmark();

        benchmark.setup(benchmark instanceof CreateCollectionMokapotBenchmark ? 15239 : 0);
        benchmark.distribute();
        benchmark.executeAlgorithm();
        benchmark.stop();
    }

}
