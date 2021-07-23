package xyz.acygn.mokapot.benchmarksuite.benchmark;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.List;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;

/**
 * <p>
 * This interface defines a full instance of a benchmark that can be
 * executed by the benchmarking script ({@link Runner}), including the
 * program to be benchmarked (defined as a {@link Benchmarkable} object)
 * and any {@link RemoteProcess} peers (if any) required for running the
 * benchmark.
 * </p>
 * <p>
 * An implementation of this interface is free to define how the benchmark
 * is to be executed without major constraints. Implementations of the run()
 * method should be mindful of the structure of {@link BenchmarkResult}, and
 * no implementation of any of the methods should return any nulls.
 * Furthermore, implementation should allow setting a particular number of
 * iterations indicating how many times each test is performed.
 * </p>
 * <p>
 * This interface should be re-implemented whenever a specific procedure
 * for benchmarking is required. This could be required for programs
 * larger than the small example programs, in which case an extension
 * of the {@link Benchmarkable} interface might also be required.
 * </p>
 *
 * todo: change from interface to abstract class?
 * it's possible to implement this interface in ways that aren't consistent with
 * how the benchmarking is performed, perhaps should be an abstract class so as
 * to force certain behaviors on all extensions
 *
 * @author Marcello De Bernardi 20/06/2017.
 */
public interface Benchmark {
    /**
     * <p>
     * Executes the actual benchmark. Implementations of this method should
     * collect the benchmarking data in whatever way they see fit, and return
     * the collected values as a {@link BenchmarkResult} object. Implementations
     * should be mindful of the fact that BenchmarkResult is quite flexible (to
     * allow for different data to be collected in different benchmarks), but
     * the particular contents of the BenchmarkResult may not be handled by the
     * implementation of {@link Runner} without modifications to the latter.
     * </p>
     *
     * @return BenchmarkResult representing data collected from benchmark
     * @throws IOException       represents error in example program (see {@link Benchmarkable})
     * @throws NotBoundException represents error in example program (see above)
     */
    BenchmarkResult run() throws IOException, NotBoundException;

    /**
     * @return the type of the benchmark.
     */
    Benchmarkable.ExampleType getType();

    /**
     * <p>
     * Returns the Class object for the program to be benchmarked. Allows performing
     * a variety of reflection operations that may be useful to the implementation of
     * the benchmarking script.
     * </p>
     *
     * @return program Class object
     */
    Class getProgram();

    /**
     * <p>
     * Returns the list of {@link RemoteProcess} peers associated with this particular benchmark.
     * A RemoteProcess encapsulates a process that the example program being benchmarked can
     * connect to. RMI and mokapot examples by their nature require a number of peers.
     * </p>
     *
     * @return list of peers, possibly empty but never null
     */
    List<RemoteProcess> getPeers();

    /**
     * @return the benchmark group of which this benchmark is a member.
     */
    BenchmarkGroup getBenchmarkGroup();
}
