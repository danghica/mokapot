package xyz.acygn.mokapot.benchmarksuite.benchmark;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;


import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase.POST_DISTRIBUTION;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase.POST_EXECUTION;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase.PRE_DISTRIBUTION;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase.WHILE_DISTRIBUTION_BOTTOM;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase.WHILE_DISTRIBUTION_TOP;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase.WHILE_EXECUTION_BOTTOM;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase.WHILE_EXECUTION_TOP;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkResult.BenchmarkMetric.AVG_DIST;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkResult.BenchmarkMetric.AVG_EXEC;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkResult.BenchmarkMetric.MAX_DIST;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkResult.BenchmarkMetric.MAX_EXEC;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkResult.BenchmarkMetric.MIN_DIST;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkResult.BenchmarkMetric.MIN_EXEC;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkResult.BenchmarkMetric.STDDEV_DIST;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkResult.BenchmarkMetric.STDDEV_EXEC;

/**
 * <p>
 * Implementation of {@link Benchmark} designed to handle {@link Benchmarkable}
 * example programs.
 * </p>
 * @author Marcello De Bernardi 22/06/2017.
 */
public class BenchmarkImp implements Benchmark {
    // logging
    private static final Logger LOGGER = Logger.getLogger(Runner.class.getName());
    private static final Level LOGGING_LEVEL = Level.FINE;

    // setup logging
    static {
        try {
            LOGGER.setLevel(LOGGING_LEVEL);
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private Benchmarkable benchmarkable;
    private int distributionIterations;
    private int executionIterations;
    private List<RemoteProcess> peers;
    private int localPort;
    private final BenchmarkGroup benchmarkGroup;


    /**
     * Constructs a Benchmark, including a {@link Benchmarkable} object to benchmark, the
     * number of iterations to use in benchmarking, any remote peers to connect to, as well
     * as the port on which the benchmarkable program should communicate. How the latter two
     * parameters are used is dependent on the particular Benchmarkable program.
     *
     * @param benchmarkable example program to run
     * @param distributionIterations the desired number of times to run the distribution stage of the benchmark
     * @param executionIterations    how many times each section of the program is to be executed
     * @param peers         any remote hosts available for connecting
     * @param localPort     the tcp port on which the example program should communicate
     */
    BenchmarkImp(Benchmarkable benchmarkable,
                 int distributionIterations,
                 int executionIterations,
                 List<RemoteProcess> peers,
                 int localPort,
                 BenchmarkGroup benchmarkGroup) {
        this.benchmarkable = benchmarkable;
        this.distributionIterations = distributionIterations;
        this.executionIterations = executionIterations;
        this.peers = peers;
        this.localPort = localPort;
        this.benchmarkGroup = benchmarkGroup;
    }


    @Override
    // todo break into smaller methods
    public BenchmarkResult run() throws IOException, NotBoundException {
        // times are in microseconds
        double minDist, minExec;
        double maxDist, maxExec;
        double avgDist, avgExec, stdDist, stdExec;
        double[] distTimes = new double[distributionIterations];
        double[] execTimes = new double[executionIterations];
        double start;
        int consecutiveExceptions = 0;
        int distributionExceptions = 0, executionExceptions = 0;

        benchmarkable.setup(localPort, peers.toArray(new RemoteProcess[peers.size()]));

        if (benchmarkable.requiresFix(PRE_DISTRIBUTION))
            benchmarkable.fix(PRE_DISTRIBUTION);

        final int distributionWarmupIterations = 5;

        // collect distribution times
        for (int i = 0, step = distributionIterations / 5;
             i < distributionWarmupIterations + distributionIterations;
             i++) {
            final int testIndex = i - distributionWarmupIterations;

            if (benchmarkable.requiresFix(WHILE_DISTRIBUTION_TOP))
                benchmarkable.fix(WHILE_DISTRIBUTION_TOP);

            // accept occasional exceptions, terminate if 5 in a row
            try {
                // If we are still warming up just distribute and do nothing else
                if (i < distributionWarmupIterations) {
                    benchmarkable.distribute();
                } else {
                    start = System.nanoTime();
                    benchmarkable.distribute();
                    distTimes[testIndex] = (System.nanoTime() - start) / 1000;
                    consecutiveExceptions = 0;
                    LOGGER.log(Level.FINE, benchmarkable.getClass().getSimpleName() + " iteration complete.");

                    // visual progress cue to terminal user
                    if (i % (distributionIterations / 5) == 0) {
                        System.out.print("+");
                    }
                }
            }
            catch (Exception e) {
                consecutiveExceptions++;
                distributionExceptions++;
                i--;

                LOGGER.log(Level.INFO, benchmarkable.getClass().getSimpleName() + " failed iteration:", e);

                if (consecutiveExceptions >= 5) {
                    System.out.print(distributionExceptions + " distribution exceptions ");
                    throw e;
                }
            }

            if (benchmarkable.requiresFix(WHILE_DISTRIBUTION_BOTTOM))
                benchmarkable.fix(WHILE_DISTRIBUTION_BOTTOM);
        }

        if (benchmarkable.requiresFix(POST_DISTRIBUTION)) benchmarkable.fix(POST_DISTRIBUTION);

        final int executionWarmupIterations = 5;

        // collect execution times
        for (int i = 0, step = executionIterations / 5;
             i < executionWarmupIterations + executionIterations;
             i++) {
            final int testIndex = i - distributionWarmupIterations;

            if (benchmarkable.requiresFix(WHILE_EXECUTION_TOP)) benchmarkable.fix(WHILE_EXECUTION_TOP);

            // accept occasional exceptions, terminate if 5 in a row
            try {
                // If we are still warming up just run the execution and do nothing else
                if (i < executionWarmupIterations) {
                    benchmarkable.executeAlgorithm();
                } else {
                    start = System.nanoTime();
                    benchmarkable.executeAlgorithm();
                    execTimes[testIndex] = (System.nanoTime() - start) / 1000;
                    consecutiveExceptions = 0;
                    LOGGER.log(Level.FINE, benchmarkable.getClass().getSimpleName() + " iteration complete.");

                    // visual progress cue to terminal user
                    if (i % (executionIterations / 5) == 0) {
                        System.out.print("+");
                    }
                }
            }
            catch (Exception e) {
                consecutiveExceptions++;
                executionExceptions++;
                i--;

                LOGGER.log(Level.INFO, benchmarkable.getClass().getSimpleName() + " failed iteration:", e);

                if (consecutiveExceptions >= 5) {
                    System.out.print(executionExceptions + " execution exceptions ");
                    throw e;
                }
            }

            if (benchmarkable.requiresFix(WHILE_EXECUTION_BOTTOM))
                benchmarkable.fix(WHILE_EXECUTION_BOTTOM);
        }

        if (benchmarkable.requiresFix(POST_EXECUTION)) benchmarkable.fix(POST_EXECUTION);

        System.out.print(" (" + distributionExceptions + "," + executionExceptions
                + ") (distribution,execution) failures ");

        benchmarkable.stop();

        /* Process collected data into desired metrics. To add new metrics to the resulting
        table, new values need to be added to the BenchmarkMetric enum within BenchmarkResult,
        for use in the .withMetric() method. */
        DoubleSummaryStatistics distStats = Arrays.stream(distTimes).summaryStatistics();
        DoubleSummaryStatistics execStats = Arrays.stream(execTimes).summaryStatistics();

        avgDist = distStats.getAverage();
        avgExec = execStats.getAverage();
        minDist = distStats.getMin();
        minExec = execStats.getMin();
        maxDist = distStats.getMax();
        maxExec = execStats.getMax();

        stdDist = Math.sqrt(Arrays.stream(distTimes).map((val) -> Math.pow((val - avgDist), 2)).sum() / distTimes.length);
        stdExec = Math.sqrt(Arrays.stream(execTimes).map((val) -> Math.pow((val - avgExec), 2)).sum() / execTimes.length);

        return new BenchmarkResult(getProgram().getSimpleName(), distributionIterations, executionIterations)
                .withMetric(AVG_DIST, avgDist)
                .withMetric(AVG_EXEC, avgExec)
                .withMetric(MIN_DIST, minDist)
                .withMetric(MIN_EXEC, minExec)
                .withMetric(MAX_DIST, maxDist)
                .withMetric(MAX_EXEC, maxExec)
                .withMetric(STDDEV_DIST, stdDist)
                .withMetric(STDDEV_EXEC, stdExec);
    }

    @Override
    public Benchmarkable.ExampleType getType() {
        return benchmarkable.getType();
    }

    @Override
    public Class getProgram() {
        return benchmarkable.getClass();
    }

    @Override
    public List<RemoteProcess> getPeers() {
        return peers;
    }

    @Override
    public BenchmarkGroup getBenchmarkGroup() {
        return benchmarkGroup;
    }
}
