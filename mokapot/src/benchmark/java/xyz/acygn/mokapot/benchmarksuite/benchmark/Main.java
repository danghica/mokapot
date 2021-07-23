package xyz.acygn.mokapot.benchmarksuite.benchmark;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;

/**
 * <p>
 * Entry point for the benchmarking application. Logging and benchmarking
 * iterations are specified in this class so as to partly separate them from the
 * more low-level implementation details of the benchmarking script.
 * </p><p>
 * The most important classes for benchmarking are
 * <ol>
 * <li>{@link Runner} contains the core benchmarking algorithm</li>
 * <li>{@link Benchmark} interface for a benchmark as seen by the runner
 * script</li>
 * <li>{@link BenchmarkImp} base implementation of the Benchmark interface,
 * could be wrapped or inherited if a more specialized implementation of
 * Benchmark is desired</li>
 * <li>{@link Benchmarkable} interface that must be implemented by example
 * programs if they are to be benchmarked</li>
 * <li>{@link BenchmarkResult} encapsulates the information collected from a
 * single benchmarked program</li>
 * <li>{@link RemoteProcess} encapsulates a process running on an arbitrary
 * host, which an example program can connect to via TCP. Currently the
 * benchmarking script only creates "remote" processes on the local host, but
 * the RemoteProcess class in of itself can in principle be used for truly
 * remote processes, so long as a {@link Process} reference to it can be
 * obtained.</li>
 * </ol>
 * </p><p>
 * The package also contains a number of enums and exceptions that are
 * inconsequential at the high level. Their purpose is explained in the
 * documentation of the classes where they are used.
 * </p><p>
 * Modifications or extensions to the benchmarking suite would be easily carried
 * out as follows:
 * <ol>
 * <li>Starting/stopping application + setting iterations: modify
 * {@link Main}</li>
 * <li>Overall structure of benchmarking procedure + creation of remote
 * processes: modify {@link Runner} and {@link RemoteProcess}</li>
 * <li>How data is collected and what data is collected: modify
 * {@link BenchmarkImp} and {@link BenchmarkResult}</li>
 * <li>Structure of example programs and how they are accessed by the
 * benchmarking script: modify {@link Benchmarkable} and the example programs
 * themselves.</li>
 * <li>How collected data is displayed: modify {@link Runner} and template files
 * in resources package</li>
 * </ol>
 * </p>
 *
 * @author Marcello De Bernardi
 */
public class Main {

    /**
     * The directory in which all benchmarking output (whether actual output, or
     * something like logs) is stored.
     */
    public static final String BENCHMARK_OUTPUT_DIR
            = "./build-internal/benchmark-output/";

    // logging
    private static final Logger LOGGER;
    private static final Level LOGGING_LEVEL = Level.SEVERE;

    static {
        LOGGER = Logger.getLogger(Main.class.getName());

        final String loggingPath = BENCHMARK_OUTPUT_DIR + "logs/";
        new File(loggingPath).mkdirs();

        try {
            /* The application terminates incorrectly if the static initializer fails
            to setup logging correctly. This is intentional behavior, as mokapot tends
            to have a lot of unpredictable bugs when the example programs are executed
            in loops and thus logging is vital. */
            FileHandler handler = new FileHandler(loggingPath + "benchmark_severe.log");
            handler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(handler);
            LOGGER.setLevel(LOGGING_LEVEL);
        } catch (IOException | SecurityException e) {

            e.printStackTrace();
        }
    }

    /**
     * Benchmarking entry point. Instantiates a Runner script.
     *
     * @param args [0] can optionally be used to specify iterations
     */
    public static void main(String[] args) {
        final int distributionIterations;
        final int executionIterations;

        if (args.length > 0) {
            distributionIterations = Integer.parseInt(args[0]);
            executionIterations = Integer.parseInt(args[1]);
        } else {
            distributionIterations = 10;
            executionIterations = 200;
        }

        try {
            Runner.getInstance(distributionIterations, executionIterations).collect().finish();
            System.out.println("\n---- BENCHMARKING COMPLETE ----");

            // program does not always terminate without this
            // todo figure out why this is the case
            System.exit(0);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            /* Runner only throws exceptions considered fatal to the
            benchmarking attempt. Thus if an exception is thrown from
            the code above, we log it and allow the program to terminate. */
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
