package xyz.acygn.mokapot.benchmarksuite.programs.bst.local;

import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.Random;

/**
 * Example program which creates a {@link BinarySearchTree}, performs in order
 * traversal on it and then increments all its nodes.
 * <p>
 * Note that this is running on the local machine.
 * <p>
 * This will be improved by distributing the tasks performed on the BST to other
 * machines as well.
 *
 * @author Alexandra Paduraru
 */
public class LocalBSTMain extends BenchmarkProgram {
    private static final long seed = 14329;

    //benchmark
    private static final int noOfNodes = 100;
    private static BinarySearchTree bst;

    /**
     * Main method which creates a {@link BinarySearchTree}, by inserting nodes
     * locally with random values. Then in order traversal and increments all
     * nodes.
     *
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        LocalBSTMain thisExample = new LocalBSTMain();

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
        bst = new BinarySearchTree(10);
        Random gen = new Random(seed);

        // adding nodes to the BST
        // insert nodes in the BST using the random generator
        for (int j = 1; j < noOfNodes; j++) {
            bst.insert(gen.nextInt(100));
        }

    }

    @Override
    public void executeAlgorithm() throws IOException {
        // traversing the BST
        bst.inOrderTraversal(bst.getRoot());

        // increment nodes and traverse again
        bst.incrementAllNodes();
        bst.inOrderTraversal(bst.getRoot());
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
}