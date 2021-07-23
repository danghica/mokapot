package xyz.acygn.mokapot.benchmarksuite.programs.uglynumber.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;

import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.uglynumber.Checker;

/**
 * A benchmark in which a remote {@link Checker} determines whether or not
 * a given number (known to be an ugly number) is an ugly number.
 * An ugly number is a number whose only prime factors are 2, 3 or 5.
 *
 * @author Kelsey McKenna
 */
public abstract class UglyNumberBenchmark extends BenchmarkProgram {
    private static final int uglyNumber = 30375000;
    Checker checker;

    public void executeAlgorithm() throws IOException {
        boolean ugly = checker.isUgly(uglyNumber);

        assert ugly;
    }

    public static void main(String[] args) throws IOException, NotBoundException {
        UglyNumberBenchmark program = new UglyNumberRMIBenchmark();
        program.setup(program instanceof UglyNumberMokapotBenchmark ? 15239 : 0);
        program.distribute();
        program.executeAlgorithm();
        program.stop();
    }
}
