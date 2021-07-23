package xyz.acygn.mokapot.benchmarksuite.programs.primes;

import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.Scanner;

/**
 * Example program which tests a given number for primality.
 * <p>
 * Note that the program runs only locally.
 *
 * @author Alexandra Paduraru
 */
public class LocalPrimeChecker extends BenchmarkProgram {
    private long n = 999983L;
    private PrimeImpl primeChecker;

    /**
     * Main method which reads a number, either from the command line or from
     * the user and checks whether it is prime.
     *
     * @param args The number to be checked for primality.
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        LocalPrimeChecker thisExample = new LocalPrimeChecker();

        thisExample.getNumber(args);
        thisExample.distribute();
        thisExample.executeAlgorithm();
    }


    @Override
    public ExampleType getType() {
        return ExampleType.LOCAL;
    }

    @Override
    public int getNumberOfRequiredPeers() {
        return 0;
    }

    @Override
    public void distribute() throws IOException {
        primeChecker = new PrimeImpl();
    }

    @Override
    public void executeAlgorithm() throws IOException {
        primeChecker.isPrime(n);
    }

    @Override
    public void stop() throws IOException, NotBoundException {
        // not needed in local version
    }

    @Override
    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    @Override
    public void fix(BenchmarkPhase phase) {
    }


    /**
     * Read a number from the user or use the command line argument if available.
     *
     * @param args
     * @throws IOException
     */
    @SuppressWarnings("Duplicates")
    private void getNumber(String[] args) {
        if (args.length > 0) {
            n = Long.parseLong(args[0]);
        } else {
            System.out.println("Please input a number to be checked for primalityt: ");
            Scanner reader = new Scanner(System.in);
            n = reader.nextLong();

            reader.close();
        }
    }
}
