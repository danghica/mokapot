package xyz.acygn.mokapot.benchmarksuite.programs.finder.benchmarks;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.List;

import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.finder.StringDataGenerator;
import xyz.acygn.mokapot.benchmarksuite.programs.finder.StringFinder;
import xyz.acygn.mokapot.benchmarksuite.programs.finder.StringFinderImpl;

/**
 * A benchmark in which a remote {@link StringFinder} object is used
 * to find a {@link String} in a remote list of {@link String}s using
 * the linear search algorithm.
 *
 * @author Kelsey McKenna
 */
public abstract class FindStringBenchmark extends BenchmarkProgram {

    static int listSize = 1000;

    StringDataGenerator generator;
    StringFinder finder;
    String wordToFind;
    List<String> words;

    /**
     * Method which checks whether the given word belongs to a randomly
     * generated list of String.
     */
    @Override
    public void executeAlgorithm() throws IOException {
        boolean result = finder.searchWord(words, wordToFind);
    }

    /**
     * Main method which generates a random list of words and reads one
     * word(either through the command line or as user input). Then it checks if
     * the given word is in the list.
     * <p>
     * A {@link StringFinderImpl} object is created remotely, which generates the
     * random list of words and carries out the search remotely as well.
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args) throws IllegalStateException, NotBoundException, IOException {
        FindStringBenchmark thisExample = new FindStringMokapotBenchmark();

        thisExample.setup(thisExample instanceof FindStringMokapotBenchmark ? 15239 : 0);
        thisExample.distribute();
        thisExample.executeAlgorithm();
        thisExample.stop();
    }

}
