package xyz.acygn.mokapot.benchmarksuite.programs;

import xyz.acygn.mokapot.benchmarksuite.benchmark.Benchmark;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;

import java.io.IOException;
import java.rmi.NotBoundException;

/**
 * <p>
 * Defines an object that can be benchmarked by the benchmark-runner
 * script. The methods in the interface represent the "phases" of
 * program execution as relevant to a benchmark.
 * </p>
 * <p>
 * A program that is to be benchmarked should implement this interface,
 * and execute all of its core logic within the benchmark phase methods
 * (this allows, obviously, for calls to helper methods made from within
 * the benchmarking phase methods).
 * </p>
 * <p>
 * Benchmarkable objects are used by {@link Benchmark} objects. The
 * methods defined in Benchmarkable are called by an implementation of
 * the run() method defined in Benchmark.
 * </p>
 *
 * @author Marcello De Bernardi 20/06/2017.
 */
public interface Benchmarkable {
    /**
     * Returns the type of the Benchmarkable program, in the sense of whether
     * this example program is a mokapot example, an RMI example, or a local example.
     *
     * @return type of example
     */
    ExampleType getType();

    /**
     * Returns the number of peers that the example program will require to run correctly.
     * This value may be 0 for local implementations, 1 for a client-server architecture,
     * or n for a peer-to-peer architecture example.
     *
     * @return number of peers, 0 or greater
     */
    int getNumberOfRequiredPeers();

    /**
     * Performs all operations to be executed in preparation of
     * benchmarking. This could include, but is not limited to, setting
     * up a mokapot {@link xyz.acygn.mokapot.DistributedCommunicator},
     * obtaining an RMI server stub, and so on.
     * <p>
     * <p>Crucially, there is no strict definition of what an implementation
     * of this method should do, as it is not intended to be benchmarked,
     * and the setup requirements of different example programs may
     * vary due to their different structures. For example, there is no
     * obligation for the parameters to be used; a locally executing
     * example program would not need to use any peers.</p>
     *
     * @param peers     peers the example program is made aware of
     * @param localPort TCP port on which example program will communicate
     * @throws IOException       commonly thrown by a variety of mokapot/rmi operations
     * @throws NotBoundException throws by RMI setup operations
     */
    void setup(int localPort, RemoteProcess... peers) throws IOException, NotBoundException;

    /**
     * Performs every operation required to establish a predetermined
     * object topology across the network, and <strong>nothing else</strong>.
     * Implementation of this method should not include any operations
     * extraneous to this definition, as the performance of this
     * method is intended to be benchmarked. This includes logging,
     * print statements, and so on.
     *
     * @throws IOException commonly thrown by a variety of mokapot/rmi operations
     */
    void distribute() throws IOException;

    /**
     * Performs every operation required to produce a result, and <strong>nothing
     * else</strong>, operating on the assumption that the entire object distribution
     * over the network has already been established by the time this method is
     * benchmarkable called. Implementations of this method should adhere strictly to the
     * above definition, as the performance of this method is intended to be
     * benchmarked. This includes avoiding logging, print statements, and so on.
     *
     * @throws IOException commonly thrown by a variety of mokapot/rmi operations
     */
    void executeAlgorithm() throws IOException;

    /**
     * Performs any operations required to terminate the running of an example
     * program so as to free the benchmarking sequence to move on to another
     * example. Implementations of this method should take care of things such
     * stopping communication for mokapot {@link xyz.acygn.mokapot.DistributedCommunicator}s,
     * unexport any exported remote objects for RMI, and so on. The performance
     * of this method is not intended to be benchmarked.
     *
     * @throws IOException       thrown by a variety of mokapot/rmi operations
     * @throws NotBoundException thrown by RMI operations
     */
    void stop() throws IOException, NotBoundException;

    /**
     * <p>Checks whether the example program being benchmarked is in a condition
     * such that a fixing operation is required.</p>
     * <p>Inclusion of this functionality in the interface, rather than as a part
     * of each individual example program's implementation, is due to the fact that
     * the runner script needs to be able to trigger fixes at such times that they
     * will not affect the data being collected. That is, the runner script needs
     * to be able to control the fixing.</p>
     */
    boolean requiresFix(BenchmarkPhase phase);

    /**
     * Fixes whatever condition is in need of being fixed for the benchmarking
     * to continue. The implementing class decides what this means. This method
     * would be called in the case that requiresFix() returns true.
     */
    void fix(BenchmarkPhase phase);


    /**
     * Used to identify the correct type of peer to allocate to a running example program.
     */
    enum ExampleType {
        MOKAPOT, RMI, LOCAL
    }
}
