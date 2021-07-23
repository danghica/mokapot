package xyz.acygn.mokapot.benchmarksuite.benchmark;

import java.util.HashMap;
import java.util.Map;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;


/**
 * <p>
 * A BenchmarkResult represents a bundle of benchmarking information collected for a single
 * {@link Benchmarkable} program.
 * </p>
 * <p>
 * The values of the result are input using setters rather than the constructor. This
 * ensures that new benchmark metrics can be later added without having to either break
 * existing benchmarking code, or having to add numerous overloaded constructors.
 * Setters are implemented in the method-chaining style to make the setting of values
 * less verbose.
 * </p>
 *
 * @author Marcello De Bernardi 03/07/2017.
 */
class BenchmarkResult {
    private int distributionIteartions;
    private int executionIterations;
    private String programName;
    private Map<BenchmarkMetric, Double> resultMap;


    /**
     * Creates a new BenchmarkResult for a program with the given name, executed the given
     * number of times.
     *
     * @param programName name of the {@link Benchmarkable} program
     * @param distributionIteration number of times objects are distributed
     * @param executionIterations  number of times each section was executed
     */
    BenchmarkResult(String programName, int distributionIteration, int executionIterations) {
        this.programName = programName;
        this.distributionIteartions = distributionIteration;
        this.executionIterations = executionIterations;
        resultMap = new HashMap<>();
    }


    /**
     * Sets the collected value for the given metric. Naturally, a metric's value must be set
     * with a call to this method before it can be accessed via the getMetric() method.
     *
     * @param metric the metric being measured
     * @param value  the measured value of the metric
     * @return this
     */
    BenchmarkResult withMetric(BenchmarkMetric metric, double value) {
        resultMap.put(metric, value);
        return this;
    }

    /**
     * Returns the name of the {@link Benchmarkable} program from which this BenchmarkResult's
     * data was collected.
     *
     * @return name of benchmarked program
     */
    String getProgramName() {
        return programName;
    }

    /**
     * Returns the number of iterations used to benchmark the program.
     *
     * @return number of benchmarking iterations
     */
    int getDistributionIterations() {
        return distributionIteartions;
    }

    int getExecutionIterations() {
        return executionIterations;
    }

    /**
     * Returns the recorded value of the given metric. A {@link NotCollectedException} is thrown
     * if there is no entry for the required metric, signifying that the desired metric was not
     * collected by the benchmarking procedure.
     *
     * @param metric metric for which value is required
     * @return value of metric
     * @throws NotCollectedException if no value found
     */
    Double getMetric(BenchmarkMetric metric) throws NotCollectedException {
        Double result = resultMap.get(metric);

        if (result != null) return result;
        else throw new NotCollectedException();
    }


    /**
     * The possible metrics that can be stored into the BenchmarkResult. We use an enum rather
     * than simple String names to help prevent accidental typing errors from breaking code.
     */
    enum BenchmarkMetric {
        AVG_DIST, AVG_EXEC, MAX_DIST, MAX_EXEC, MIN_DIST, MIN_EXEC, STDDEV_DIST, STDDEV_EXEC
    }

    /**
     * <p>
     * Thrown by a {@link BenchmarkResult} object when attempting to access a benchmark
     * data point that had not been collected. Can be thought of as a NullPointerException,
     * but has a name that more easily suggests to the programmer what the problem with
     * the code is.
     * </p><p>
     * Specifically, this exception would be thrown in cases where there is a call such as
     * result.getMetric(someMetric), whereby the result had been constructed <strong>without
     * </strong> a call to result.withMetric(someMetric, value).
     * </p>
     */
    class NotCollectedException extends RuntimeException {
        NotCollectedException() {
            super();
        }
    }

}
