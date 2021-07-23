package xyz.acygn.mokapot.benchmarksuite.benchmark;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;

/**
 * A class representing a group of benchmarks for the same feature.
 * For example, the group might be list of benchmarks for primality testing,
 * but each benchmark is implemented using a different technology,
 * e.g. local, Mokapot, RMI, etc.
 *
 * @author Kelsey McKenna
 */
public class BenchmarkGroup {

    private final List<Benchmarkable> benchmarks;
    private final Map<Benchmarkable.ExampleType, BenchmarkResult> results = new HashMap<>();

    BenchmarkGroup(Benchmarkable localBenchmark,
                   Benchmarkable rmiBenchmark,
                   Benchmarkable mokapotBenchmark) {
        verifyOrder(localBenchmark, rmiBenchmark, mokapotBenchmark);

        this.benchmarks = Arrays.asList(localBenchmark, rmiBenchmark, mokapotBenchmark);
    }

    private static void verifyOrder(Benchmarkable localBenchmark,
                                    Benchmarkable rmiBenchmark,
                                    Benchmarkable mokapotBenchmark) {
        if (localBenchmark.getType() != Benchmarkable.ExampleType.LOCAL) {
            throw new RuntimeException("First benchmark in group must test Local execution");
        }
        if (rmiBenchmark.getType() != Benchmarkable.ExampleType.RMI) {
            throw new RuntimeException("Second benchmark in group must test RMI execution");
        }
        if (mokapotBenchmark.getType() != Benchmarkable.ExampleType.MOKAPOT) {
            throw new RuntimeException("Third benchmark in group must test Mokapot execution");
        }
    }

    public List<Benchmarkable> getBenchmarks() {
        return benchmarks;
    }

    void addBenchmarkResult(Benchmarkable.ExampleType type, BenchmarkResult result) {
        results.put(type, result);
    }

    BenchmarkResult getBenchmarkResult(Benchmarkable.ExampleType type) {
        return results.get(type);
    }

}
