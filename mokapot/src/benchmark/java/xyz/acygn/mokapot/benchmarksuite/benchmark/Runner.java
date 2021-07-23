package xyz.acygn.mokapot.benchmarksuite.benchmark;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkResult.BenchmarkMetric.AVG_DIST;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkResult.BenchmarkMetric.AVG_EXEC;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkResult.BenchmarkMetric.MAX_DIST;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkResult.BenchmarkMetric.MAX_EXEC;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkResult.BenchmarkMetric.MIN_DIST;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkResult.BenchmarkMetric.MIN_EXEC;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkResult.BenchmarkMetric.STDDEV_DIST;
import static xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkResult.BenchmarkMetric.STDDEV_EXEC;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.bst.local.LocalBSTMain;
import xyz.acygn.mokapot.benchmarksuite.programs.bst.mokapot.MokapotBSTMain;
import xyz.acygn.mokapot.benchmarksuite.programs.bst.rmi.RMIBSTMain;

import static xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType.LOCAL;
import static xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType.MOKAPOT;
import static xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType.RMI;
import xyz.acygn.mokapot.benchmarksuite.programs.checkequality.benchmarks.CheckEqualityLocalBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.checkequality.benchmarks.CheckEqualityMokapotBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.checkequality.benchmarks.CheckEqualityRMIBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.createcollection.benchmarks.CreateCollectionLocalBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.createcollection.benchmarks.CreateCollectionMokapotBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.createcollection.benchmarks.CreateCollectionRMIBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.dijkstra.benchmarks.DijkstraLocalBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.dijkstra.benchmarks.DijkstraMokapotBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.dijkstra.benchmarks.DijkstraRMIBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.finder.benchmarks.FindStringLocalBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.finder.benchmarks.FindStringMokapotBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.finder.benchmarks.FindStringRMIBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.benchmarks.MapReduceLocalBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.benchmarks.MapReduceMokapotBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.mapreduce.benchmarks.MapReduceRMIBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.matrix.benchmarks.MatrixLocalBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.matrix.benchmarks.MatrixMokapotBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.matrix.benchmarks.MatrixRMIBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.naivebayes.benchmarks.NaiveBayesLocalBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.naivebayes.benchmarks.NaiveBayesMokapotBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.naivebayes.benchmarks.NaiveBayesRMIBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.primes.LocalPrimeChecker;
import xyz.acygn.mokapot.benchmarksuite.programs.primes.MokapotPrimeChecker;
import xyz.acygn.mokapot.benchmarksuite.programs.primes.RMIPrimeChecker;
import xyz.acygn.mokapot.benchmarksuite.programs.sort.benchmarks.SortLocalBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.sort.benchmarks.SortMokapotBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.sort.benchmarks.SortRMIBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.student.benchmarks.StudentLocalBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.student.benchmarks.StudentMokapotBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.student.benchmarks.StudentRMIBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.transforminturns.benchmarks.TransformInTurnsLocalBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.transforminturns.benchmarks.TransformInTurnsMokapotBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.transforminturns.benchmarks.TransformInTurnsRMIBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist.benchmarks.TraverseLinkedListLocalBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist.benchmarks.TraverseLinkedListMokapotBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.traverselinkedlist.benchmarks.TraverseLinkedListRMIBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.uglynumber.benchmarks.UglyNumberLocalBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.uglynumber.benchmarks.UglyNumberMokapotBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.uglynumber.benchmarks.UglyNumberRMIBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.validanagram.benchmarks.ValidAnagramLocalBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.validanagram.benchmarks.ValidAnagramMokapotBenchmark;
import xyz.acygn.mokapot.benchmarksuite.programs.validanagram.benchmarks.ValidAnagramRMIBenchmark;

/**
 * <p>
 * Runs timing tests on all example programs and constructs a table with the
 * data. A runner is a singleton object because multiple instances of a Runner
 * should not be executing concurrently. Furthermore, it is a singleton rather
 * than a static utility class because:
 * </p>
 * <ol>
 * <li>A Runner performs a variety of stateful operations</li>
 * <li>Constructor + method chaining notation is convenient</li>
 * </ol>
 *
 * @author Marcello De Bernardi
 */
@SuppressWarnings("Duplicates")
class Runner {

    // logging framework
    private static final Logger LOGGER = Logger.getLogger(Runner.class.getName());
    private static final Level LOGGING_LEVEL = Level.FINE;
    // singleton instance
    private static Runner instance;

    // setup logging
    static {
        try {
            FileHandler handler = new FileHandler(
                    Main.BENCHMARK_OUTPUT_DIR + "logs/benchmark_fine.log");
            handler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(handler);
            LOGGER.setLevel(LOGGING_LEVEL);
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
        }
    }

    // file IO for results
    private BufferedWriter htmlWriter;
    private BufferedReader templateReader;
    private String htmlRowTemplate;
    private String htmlFileTemplate;
    // core benchmarking data
    private List<Benchmark> benchmarks;

    private static final DecimalFormat integerFormatter = new DecimalFormat("#");
    private static final DecimalFormat ratioFormatter = new DecimalFormat("#.00");

    /**
     * Constructor for benchmark runner that allows specifying the number of
     * iterations to be used for each benchmark. Larger values should minimize
     * the effects of non- determinism but also cause the benchmarking to take
     * longer.
     *
     * @param executionIterations how many times each benchmark is executed
     * @throws IOException if unable to access filesystem
     */
    private Runner(int distributionIterations, int executionIterations)
            throws IOException, InterruptedException, URISyntaxException {
        // file paths and IO objects
        String outPath = Main.BENCHMARK_OUTPUT_DIR + "benchmarks/";
        new File(outPath).mkdirs();

        String genTime = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .replace(":", "-");

        htmlWriter = new BufferedWriter(new FileWriter(outPath + "benchmark_" + genTime + ".html"));
        templateReader = new BufferedReader(
                new InputStreamReader(this.getClass().getResourceAsStream("/template.html")));
        htmlFileTemplate = templateReader.lines().collect(Collectors.joining(""));
        templateReader = new BufferedReader(
                new InputStreamReader(this.getClass().getResourceAsStream("/template_row.html")));
        htmlRowTemplate = templateReader.lines().collect(Collectors.joining(""));
        htmlFileTemplate = htmlFileTemplate.replace("$generated", genTime);

        // create benchmarks and start peers
        List<BenchmarkGroup> programList = new ArrayList<>();

        programList.add(new BenchmarkGroup(
                new LocalBSTMain(),
                new RMIBSTMain(),
                new MokapotBSTMain()));

        programList.add(new BenchmarkGroup(
    			new MapReduceLocalBenchmark(),
    			new MapReduceRMIBenchmark(),
    			new MapReduceMokapotBenchmark()));
        
        programList.add(new BenchmarkGroup(
                new SortLocalBenchmark(),
                new SortRMIBenchmark(),
                new SortMokapotBenchmark()));

        programList.add(new BenchmarkGroup(
                new CreateCollectionLocalBenchmark(),
                new CreateCollectionRMIBenchmark(),
                new CreateCollectionMokapotBenchmark()));

        programList.add(new BenchmarkGroup(
                new LocalPrimeChecker(),
                new RMIPrimeChecker(),
                new MokapotPrimeChecker()));

        programList.add(new BenchmarkGroup(
                new CheckEqualityLocalBenchmark(),
                new CheckEqualityRMIBenchmark(),
                new CheckEqualityMokapotBenchmark()));

        programList.add(new BenchmarkGroup(
                new NaiveBayesLocalBenchmark(),
                new NaiveBayesRMIBenchmark(),
                new NaiveBayesMokapotBenchmark()));

        programList.add(new BenchmarkGroup(
                new UglyNumberLocalBenchmark(),
                new UglyNumberRMIBenchmark(),
                new UglyNumberMokapotBenchmark()));

        programList.add(new BenchmarkGroup(
                new TraverseLinkedListLocalBenchmark(),
                new TraverseLinkedListRMIBenchmark(),
                new TraverseLinkedListMokapotBenchmark()));

        programList.add(new BenchmarkGroup(
                new ValidAnagramLocalBenchmark(),
                new ValidAnagramRMIBenchmark(),
                new ValidAnagramMokapotBenchmark()));

        programList.add(new BenchmarkGroup(
                new DijkstraLocalBenchmark(),
                new DijkstraRMIBenchmark(),
                new DijkstraMokapotBenchmark()));

        programList.add(new BenchmarkGroup(
                new FindStringLocalBenchmark(),
                new FindStringRMIBenchmark(),
                new FindStringMokapotBenchmark()));

        programList.add(new BenchmarkGroup(
                new MatrixLocalBenchmark(),
                new MatrixRMIBenchmark(),
                new MatrixMokapotBenchmark()));

        programList.add(new BenchmarkGroup(
                new StudentLocalBenchmark(),
                new StudentRMIBenchmark(),
                new StudentMokapotBenchmark()));

        programList.add(new BenchmarkGroup(
                new TransformInTurnsLocalBenchmark(),
                new TransformInTurnsRMIBenchmark(),
                new TransformInTurnsMokapotBenchmark()));

        benchmarks = createBenchmarks(programList, distributionIterations, executionIterations);
    }

    /**
     * Obtains a reference to the singleton instance of the Runner class.
     * Further calls to the method with different parameters destructively
     * changes the singleton instance.
     *
     * @param executionIterations number of times each data point is collected
     * @return Runner instance
     * @throws IOException if Runner can't be instantiated
     */
    static Runner getInstance(int distributionIterations, int executionIterations)
            throws IOException, InterruptedException, URISyntaxException {
        if (instance == null) {
            instance = new Runner(distributionIterations, executionIterations);
        }
        return instance;
    }

    /**
     * Executes example programs, collects results in the indicated file, and
     * logs activity. If writing to file fails, the resulting IOException is
     * thrown as this is considered a fatal failure for the runner.
     *
     * @throws IOException if unable to write to file
     */
    Runner collect() throws IOException {
        benchmarks.forEach(benchmark -> htmlFileTemplate = run(benchmark, htmlFileTemplate, htmlRowTemplate));

        try {
            htmlWriter.write(htmlFileTemplate.replace("$nextRow", ""));
            LOGGER.log(Level.FINE, "Successfully written benchmarking results to file.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to write to file, throwing error to Main");
            LOGGER.log(Level.FINE, e.getMessage(), e);

            throw e;
        }

        return this;
    }

    /**
     * Closes all filesystem resources associated with the Runner and logs the
     * results. If resources can't be closed, throws the resulting IOException
     * as this is considered a fatal failure for the Runner.
     *
     * @throws IOException if unable to close resources
     */
    void finish() throws IOException {
        try {
            templateReader.close();
            htmlWriter.close();

            LOGGER.log(Level.FINE, "Successfully closed filesystem resources.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to close filesystem resources, throwing error to Main");
            LOGGER.log(Level.FINE, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * HELPER: Runs the Benchmarkable example program. Failures in running the
     * example program are handled within this method and not propagated upward.
     *
     * @param benchmark full benchmark to execute
     * @param htmlFileTemplate string representing the current state of the
     * output file
     * @param htmlRowTemplate string representing the template for each row in
     * the output table
     */
    private String run(Benchmark benchmark, String htmlFileTemplate, String htmlRowTemplate) {
        String progName = benchmark.getProgram().getSimpleName();
        final BenchmarkGroup group = benchmark.getBenchmarkGroup();

        assert group != null;

        // perform benchmark
        try {
            System.out.print("Benchmarking " + progName + " ");
            BenchmarkResult result = benchmark.run();
            group.addBenchmarkResult(benchmark.getType(), result);

            System.out.println(" -> success");

            htmlFileTemplate = htmlFileTemplate
                    .replace("$nextRow", htmlRowTemplate)
                    .replace("$prog", progName);

            final Function<Function<BenchmarkResult, Double>, String> ratioCellFormatter = metricGetter -> {
                if (benchmark.getType() != MOKAPOT) {
                    return "X";
                }

                double thisResult = metricGetter.apply(result);
                double rmiResult = metricGetter.apply(group.getBenchmarkResult(RMI));

                return ratioFormatter.format(thisResult / rmiResult);
            };

            htmlFileTemplate = htmlFileTemplate
                    .replace("$distiter", String.valueOf(result.getDistributionIterations()))
                    .replace("$execiter", String.valueOf(result.getExecutionIterations()))
                    .replace("$_avgdist", integerFormatter.format(result.getMetric(AVG_DIST)))
                    .replace("$_ratiodist", ratioCellFormatter.apply(r -> r.getMetric(AVG_DIST)))
                    .replace("$_maxdist", integerFormatter.format(result.getMetric(MAX_DIST)))
                    .replace("$_mindist", integerFormatter.format(result.getMetric(MIN_DIST)))
                    .replace("$_stddist", integerFormatter.format(result.getMetric(STDDEV_DIST)))
                    .replace("$_avgexec", integerFormatter.format(result.getMetric(AVG_EXEC)))
                    .replace("$_ratioexec", ratioCellFormatter.apply(r -> r.getMetric(AVG_EXEC)))
                    .replace("$_maxexec", integerFormatter.format(result.getMetric(MAX_EXEC)))
                    .replace("$_minexec", integerFormatter.format(result.getMetric(MIN_EXEC)))
                    .replace("$_stdexec", integerFormatter.format(result.getMetric(STDDEV_EXEC)));

            return htmlFileTemplate;
        } // if benchmarking fails, log error and return
        catch (IOException | NotBoundException e) {
            LOGGER.log(Level.WARNING, "Skipped " + progName + " due to failure in benchmarked program.", e);
        } catch (BenchmarkResult.NotCollectedException e) {
            LOGGER.log(Level.WARNING, "Skipped " + progName + " due to uncollected benchmark metric.", e);
        }

        return htmlFileTemplate;
    }

    /**
     * HELPER: Creates a list of {@link Benchmark} objects to be eventually
     * executed by the runner script.
     *
     * @param programList benchmarkable programs to create benchmarks for
     * @param executionIterations benchmark iterations
     * @return list of benchmarks ready to be executed
     */
    private List<Benchmark> createBenchmarks(List<BenchmarkGroup> programList,
            int distributionIterations,
            int executionIterations)
            throws IOException, InterruptedException {
        int clientPort = 15300;

        List<Benchmark> resultList = new ArrayList<>();

        // for every registered benchmarkable, create benchmark
        for (BenchmarkGroup benchmarkGroup : programList) {
            for (Benchmarkable b : benchmarkGroup.getBenchmarks()) {
                List<RemoteProcess> peers = new ArrayList<>();
                
                if (BenchmarkData.getMokapotServers().isEmpty() || BenchmarkData.getRMIServers().isEmpty()) {
                		System.out.println("Warning: Either the Mokapot or the RMI Server list is empty.");
                		System.out.println("This benchmarking program needs at least one of each in order to function.");
                }

                // create peers, none for local benchmarks
                for (int j = 0; j < b.getNumberOfRequiredPeers() && b.getType() != LOCAL; j++) {
                    ServerInfo currentServer = null;
                    if (b.getType() == MOKAPOT) {
                        try {
                            currentServer = BenchmarkData.getMokapotServers().get(j);
                        } catch (Exception e) {
                            //Nothing to do.
                        }
                    } else if (b.getType() == RMI) {
                        try {
                            currentServer = BenchmarkData.getRMIServers().get(j);
                        } catch (Exception e) {
                            //Nothing to do.
                        }
                    }

                    if (currentServer != null) {
                        peers.add(new RemoteProcess(null, currentServer.getAddress(), currentServer.getPort()));
                    }
                }

                resultList.add(new BenchmarkImp(b,
                        distributionIterations,
                        executionIterations,
                        peers,
                        clientPort,
                        benchmarkGroup));

                clientPort++;

            }
        }

        return resultList;
    }

    /**
     * HELPER: General method for running a class in a new process, which allows
     * specifying whether the new process should inherit current IO (except for
     * the input, which is always the file containing the password for the .p12
     * key). Also allows passing the new process any command-line arguments.
     * NOTE: the latter arguments are for the class's main method, not the
     * command starting the process.
     *
     * @param processClass the class to perform in a new process
     * @param inheritIO whether the new process uses this process's IO locations
     * @param args CLI arguments to the main method of the class being started
     * @return the Process that is started
     * @throws IOException if unable to start
     */
    private Process newProcess(Class processClass, boolean inheritIO, String... args) throws IOException {
        String securityArg = "-Djava.security.manager";
        String securityPol = System.getProperty("java.security.policy");

        // java command arguments
        List<String> arguments = new ArrayList<>();
        arguments.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        arguments.add(securityArg);
        arguments.add("-Djava.security.policy=" + securityPol);
        arguments.add("-Dfile.encoding=UTF-8");
        arguments.add("-classpath");
        arguments.add(System.getProperty("java.class.path"));
        arguments.add(processClass.getCanonicalName());
        arguments.addAll(Arrays.asList(args));

        ProcessBuilder builder = new ProcessBuilder(arguments);
        if (inheritIO) {
            builder.inheritIO();
        }
        builder.redirectInput(new File(BenchmarkData.passwordLocation));

        final Process process = builder.start();

        Runtime.getRuntime().addShutdownHook(
                new Thread(process::destroy, "app-shutdown-hook"));

        return process;
    }
}
