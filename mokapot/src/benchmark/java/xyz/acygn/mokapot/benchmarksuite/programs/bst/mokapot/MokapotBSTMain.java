package xyz.acygn.mokapot.benchmarksuite.programs.bst.mokapot;

import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.CommunicationEndpoint;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkData;
import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.benchmarksuite.programs.bst.local.BSTNode;
import xyz.acygn.mokapot.util.ObjectIdentity;
import xyz.acygn.mokapot.util.Pair;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

/**
 * Distributed version of the BST example program, which creates a Binary Search
 * Tree with a remote root. The other nodes are added to the BST either remotely
 * or locally, using a random generator to decide that.
 * <p>
 * The program performs in order traversal and increments all nodes of the
 * binary search tree.
 *
 * @author Alexandra Paduraru
 */
public class MokapotBSTMain extends BenchmarkProgram {
    private static final long seed = 10245;
    //benchmark
    private static final int noOfNodes = 10;
    private static MokapotBinarySearchTree bst;

	@Override
	public ExampleType getType() {
		return ExampleType.MOKAPOT;
	}

    /**
     * Main method which creates a {@link MokapotBinarySearchTree}, by inserting
     * nodes with random values both locally and remotely. Then in order
     * traversal and increments all nodes of the tree.
     *
     * @throws NotBoundException
     */
    public static void main(String[] args) throws IllegalStateException, IOException, NotBoundException {
        MokapotBSTMain program = new MokapotBSTMain();

        program.setup(15238);
        program.distribute();
        program.executeAlgorithm();
        program.stop();
    }

    @Override
    public int getNumberOfRequiredPeers() {
        return 1;
    }

    @Override
    public void distribute() throws IOException {
    		final int MAX_SIZE = 100;
    	
        bst = new MokapotBinarySearchTree(communicator, 6, remotes.get(0));
        Random gen = new Random(seed);

        // add nodes to the BST
        // using a random generator to decide whether nodes should be created
        // locally or remotely
        Random decider = new Random(seed);
        for (int i = 1; i < noOfNodes; i++) {
            int data = gen.nextInt(MAX_SIZE);
            // generate nodes remotely if false
            if (!decider.nextBoolean()) {
                bst.insertRemotely(data);
            }
            // generate nodes locally if true
            else {
                bst.insertLocally(data);
            }
            // localBST.insert(data)
        }

    }

    @Override
    public void executeAlgorithm() throws IOException {
        // traverse the tree

        // System.out.println("Traversal...");
        ArrayList<BSTNode> result = bst.inOrderTraversal(bst.getRoot());

        // incrementing all nodes
        bst.incrementAllNodes();

        // traverse again
        result = bst.inOrderTraversal(bst.getRoot());
    }

    @Override
    public void stop() throws IOException, NotBoundException {
    		bst = null;
    		
    		System.gc();
    		System.runFinalization();
    		
    		communicator.runRemotely(() -> System.gc(), remotes.get(0));
    		communicator.runRemotely(() -> System.runFinalization(), remotes.get(0));

        communicator.stopCommunication();
    }

    @Override
    public boolean requiresFix(BenchmarkPhase phase) {
        return false;
    }

    @Override
    public void fix(BenchmarkPhase phase) {
    }
}
