package xyz.acygn.mokapot.benchmarksuite.programs.validanagram.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.validanagram.Checker;
import xyz.acygn.mokapot.benchmarksuite.programs.validanagram.Generator;

/**
 * A benchmark in which a local and a remote {@link Checker}
 * determines whether or not two given strings are anagrams
 * of each other. The two {@link Checker}s then verify
 * that they obtained the same result.
 */
public abstract class ValidAnagramBenchmark extends BenchmarkProgram {
    private static final int SEED = 5;
    private static final int STRING_LENGTH = 10;
    private String firstString;
    private String secondString;
    Checker firstChecker;
    Checker secondChecker;

    public void distribute() throws IOException {
        this.firstString = (new Generator(SEED)).generateString(STRING_LENGTH);
        this.secondString = (new Generator(SEED)).generateString(STRING_LENGTH);
    }

    public void executeAlgorithm() throws RemoteException {
        firstChecker.checkAnagram(firstString, secondString);
        secondChecker.checkAnagram(secondString, firstString);
        boolean agreed = firstChecker.verifyAgreement() == this.secondChecker.verifyAgreement();
        boolean isAnagram = firstChecker.isAnagram();

        assert agreed;
        assert isAnagram;
    }

    public static void main(String[] args) throws IOException, NotBoundException {
        ValidAnagramBenchmark program = new ValidAnagramMokapotBenchmark();
        program.setup(program instanceof ValidAnagramMokapotBenchmark ? 15239 : 0);
        program.distribute();
        program.executeAlgorithm();
        program.stop();
    }
}
