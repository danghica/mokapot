package xyz.acygn.mokapot.benchmarksuite.programs.checkequality.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;

import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;

/**
 * A benchmark which compares two objects using the `Object.equals` method
 * and using the `==` operator. The two objects may be on different machines,
 * in which case the `==` comparison should be false, but the `Object.equals`
 * comparison should still be true.
 *
 * @author Kelsey McKenna
 */
abstract class CheckEqualityBenchmark extends BenchmarkProgram {

    public static void main(String[] args) throws IOException, NotBoundException {
        CheckEqualityBenchmark benchmark = new CheckEqualityMokapotBenchmark();

        benchmark.setup(benchmark instanceof CheckEqualityMokapotBenchmark ? 15239 : 0);
        benchmark.distribute();
        benchmark.executeAlgorithm();
        benchmark.stop();
    }

}
