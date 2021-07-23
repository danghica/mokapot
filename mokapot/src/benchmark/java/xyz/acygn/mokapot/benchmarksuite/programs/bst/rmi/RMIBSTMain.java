package xyz.acygn.mokapot.benchmarksuite.programs.bst.rmi;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Random;

import xyz.acygn.mokapot.benchmarksuite.benchmark.BenchmarkPhase;
import xyz.acygn.mokapot.benchmarksuite.programs.BenchmarkProgram;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable;
import xyz.acygn.mokapot.benchmarksuite.programs.Benchmarkable.ExampleType;
import xyz.acygn.mokapot.benchmarksuite.programs.bst.local.BinarySearchTree;
import xyz.acygn.mokapot.benchmarksuite.benchmark.RemoteProcess;
import xyz.acygn.mokapot.rmiserver.RMIServer;

/**
 * Distributed version of the BST example program, which creates a Binary Search
 * Tree with a remote root. The other nodes are added to the BST either remotely
 * or locally, using a random generator to decide how nodes are distributed.
 * 
 * The program performs in order traversal and increments all nodes of the
 * binary search tree.
 * 
 * @author Alexandra Paduraru
 *
 */
public class RMIBSTMain extends BenchmarkProgram {

	private static final long seed = 14329;
	
	private static int port;

	// benchmark
	public static final int noOfNodes = 100;
	private static RMIBinarySearchTree bst;

	@Override
	public ExampleType getType() {
		return ExampleType.RMI;
	}

	/**
	 * Main method which creates a {@link RMIBinarySearchTree}, by inserting
	 * nodes locally and remotely with random values. Then in order traversal is
	 * performed on the tree, all its nodes are incremented and the tree is
	 * traversed again.
	 * 
	 * @throws NotBoundException
	 * @throws IOException
	 * 
	 */
	public static void main(String[] args) throws IOException, NotBoundException {

		RMIBSTMain program = new RMIBSTMain();

		program.setup(0);
		program.distribute();
		program.executeAlgorithm();
		program.stop();
	}

	@Override
	public int getNumberOfRequiredPeers() {
		return 1;
	}

	/**
	 * Method that initialises a {@link RMIBinarySearchTree} object created
	 * remotely.
	 * 
	 * @return A {@link RMIBinarySearchTree} object
	 */
	@Override
	public void distribute() throws IOException {
		final int MAX_SIZE = 100;
		
		bst = new RMIBinarySearchTree(6, servers.get(0));
		Random gen = new Random(seed);

		// add nodes to the BST

		// using a random generator to decide whether nodes should be
		// created locally or remotely
		Random decider = new Random(seed);
		for (int i = 1; i < noOfNodes; i++) {
			// generate nodes remotely if false
			if (!decider.nextBoolean()) {
				int data = gen.nextInt(MAX_SIZE);
				bst.insertRemotely(data);
			}
			// generate nodes locally if true
			else {
				int data = gen.nextInt(MAX_SIZE);
				bst.insertLocally(data);
			}
		}
	}

	/**
	 * Method which performs traversal on a {@link RMIBinarySearchTree}, then
	 * increments all nodes and traverses again the tree.
	 */
	@Override
	public void executeAlgorithm() throws IOException {
		// traverse the tree
		ArrayList<RMIBSTNode> remoteResult = bst.inOrderTraversal(bst.getRoot());

		// incrementing all nodes
		bst.incrementAllNodes();

		// traverse again
		// System.out.println("\nTraversal after incrementing all
		// nodes...");
		remoteResult = bst.inOrderTraversal(bst.getRoot());

		// OBS: The code also contains the local version, used for comparing the
		// results, but this is commented out.
		// Currently not working. Use a print statement instead.

//		 BinarySearchTree localBST = new BinarySearchTree(6);
//		 
//		 //add nodes to the BST Random gen = new Random(seed);
//		  
//		 // using a random generator to decide whether nodes should be created
//		 //locally or remotely 
//		 for (int i = 1; i < noOfNodes; i++) { 
//			 //generate nodes remotely if false 
//			 int data = gen.nextInt(10);
//			 localBST.insert(data); }
//		  
//		 for(RMIBSTNode n : remoteResult) 
//			 System.out.print(n.getData() + " ");
//		 
//		 boolean compRes = compareResults(localBST.inOrderTraversal(localBST.getRoot()), remoteResult);
//		 
//		 localBST.incrementAllNodes();
//		 
//		 for(RMIBSTNode n : remoteResult) System.out.print(n.getData() + " ");
//		 	System.out.println(); 
//		 
//		 System.out.print("Checking if the remote and local version have  the same result..."); 
//		 compRes = compRes && compareResults(localBST.inOrderTraversal(localBST.getRoot()), remoteResult);
//		 if (compRes) System.out.println("OK"); else
//			 System.out.println("INCORRECT");

	}

	@Override
	public void stop() throws IOException, NotBoundException {
		//not needed for RMI
	}

	@Override
	public boolean requiresFix(BenchmarkPhase phase) {
		return false;
	}

	@Override
	public void fix(BenchmarkPhase phase) {
	}
}